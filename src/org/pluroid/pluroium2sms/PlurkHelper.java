/*
 * Copyright (C) 2011 The Pluroium Development Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pluroid.pluroium2sms;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pluroid.pluroium2sms.data.PlurkListItem;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

public class PlurkHelper {
    
    private static final String TAG = "PlurkHelper";
    private static final String API_KEY = "5RxREItWmVXGNvzaMTiwGgWIGpKfMAd3";
    private SharedPreferences sharedPref;

    private Context context;
    
    private String username;
    private String password;
    private boolean logined;

    private DefaultHttpClient httpClient;
    private BasicCookieStore cookieStore;
    private ClientConnectionManager connMgr;
    
    public static final int REGISTRATION_TIMEOUT = 30 * 1000; // ms
    public static final String USER_AGENT = "Pluroium/2.0";
    private static SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

    // API URI
    private static final String LOGIN_URL = "/Users/login";
    private static final String ADD_PLURK_URL = "/Timeline/plurkAdd";
    private static final String GET_PLURKS_URL = "/Timeline/getPlurks";
    private static final String GET_UNREAD_URL = "/Timeline/getUnreadPlurks";
    private static final String GET_RESPONSES_URL = "/Responses/get";
    private static final String RESPOND_PLURK_URL = "/Responses/responseAdd";
    private static final String UPLOAD_PHOTO_URL = "/Timeline/uploadPicture";
    
    // URL shorten options
    public static final int URL_SHORTEN_GOOGL = 0;
    private static final String GOOGL_SHORTEN_API_URL = "https://www.googleapis.com/urlshortener/v1/url";
    public static final int URL_SHORTEN_BITLY = 1;
    
    public PlurkHelper(Context context) {
        this.context = context;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        createHttpClient();
    }
    
    private void createHttpClient() {
        if (httpClient == null) {
        	connMgr = HttpClientFactory.createConnectionManager();
            httpClient = HttpClientFactory.createHttpClient(connMgr);
            cookieStore = new BasicCookieStore();
            
            String cookieStr = sharedPref.getString(Constant.PREF_COOKIE, "");
            if (cookieStr.length() > 0) {
                String[] token = cookieStr.split(";");
                BasicClientCookie cookie = new BasicClientCookie(token[0], token[1]);
                cookie.setDomain(token[2]);
                cookie.setPath(token[3]);
                cookie.setExpiryDate(new Date(Long.parseLong(token[4])));
                cookieStore.addCookie(cookie);            
            }
            httpClient.setCookieStore(cookieStore);
            
            httpClient.removeRequestInterceptorByClass(
                    org.apache.http.protocol.RequestExpectContinue.class);
        }
    }
    
    
    public boolean isLoginned() {
        boolean loginned = false;
        if (cookieStore != null) {
            List<Cookie> cookies = cookieStore.getCookies();
            if (cookies.size() > 0) {
                BasicClientCookie c = (BasicClientCookie) cookies.get(0);
                
                Date expiryDate = c.getExpiryDate();
                Date now = Calendar.getInstance().getTime();
                if (expiryDate.after(now)) {
                    loginned = true;
                }
            }
        }
        
        return loginned;
    }
    
    /**
     * API URI wrapper
     * @param uri
     * @return
     */
    public static String getApiUri(String uri) {
        return "http://www.plurk.com/API" + uri;
    }
    
    public void setAuth(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    /**
     * Login to plurk.
     * @return
     */
    public boolean login(boolean isRemember) {
        Date now = Calendar.getInstance().getTime();
        Cookie cookie = null;
        List<Cookie> cookies = cookieStore.getCookies();
        if (cookies != null) {
            if (cookies.size() > 0) {
                cookie = cookies.get(0);
            }
        }

        logined = true;
        if (cookie == null || cookie.getExpiryDate().before(now)) {
            logined = false;
            cookieStore.clear();
            Editor prefEditor = sharedPref.edit();
            prefEditor.putString(Constant.PREF_COOKIE, "");
            prefEditor.commit();

            try {
                HashMap<String, String> params = new HashMap<String, String>();
                params.put("username", username);
                params.put("password", password);

                Response resp = performRequest(getApiUri(LOGIN_URL), params);
                Log.d(TAG, "LOGIN status: " + resp.statusCode);
                if (resp.statusCode == HttpStatus.SC_OK) {    // successfully login
                    logined = true;
                    
                    Editor prefEdit = sharedPref.edit();
                    
                    prefEdit.putString(Constant.PREF_AUTH_USERNAME_KEY, username);
                    prefEdit.putString(Constant.PREF_AUTH_PASSWORD_KEY, password);
                    
                    // store cookie
                    cookie = cookieStore.getCookies().get(0);
                    String cookieStr = cookie.getName() + ";" + cookie.getValue() + ";" + cookie.getDomain() + ";" + 
                            cookie.getPath() + ";" + cookie.getExpiryDate().getTime();
                    prefEdit.putString(Constant.PREF_COOKIE, cookieStr);
                    prefEdit.commit();
                }
            } catch (Exception e) {
                Log.e(TAG, "Login failed: " + e.getMessage());
            }
        }
        
        return logined;
    }
    
    /**
     * Add a plurk
     */
    public boolean addPlurk(String qualifier, String content, boolean allowComment) {
        boolean result = false;
        
        try {
            HttpPost post = new HttpPost(getApiUri(ADD_PLURK_URL));
            Resources res = context.getResources();
            Configuration conf = res.getConfiguration();
            
            String lang = "en";
            if (conf.locale == Locale.TAIWAN) {
                lang = "tr_ch";
            }
            
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("api_key", API_KEY));
            params.add(new BasicNameValuePair("qualifier", qualifier));
            params.add(new BasicNameValuePair("lang", lang));
            params.add(new BasicNameValuePair("content", content));
            
            post.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
            HttpResponse resp = httpClient.execute(post);
            int status = resp.getStatusLine().getStatusCode();
            
            if (status == HttpStatus.SC_OK) {
                result = true;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Add plurk error: ", e);
        } finally {
            connMgr.shutdown();
        }
        
        return result;
    }
    
    
    /**
     * Get plurks
     * 
     * @param onlyMine
     * @param onlyPrivate
     * @param onlyResponded
     * @return 
     */
    public ArrayList<PlurkListItem> getPlurks(int whichView, int limit, String dateOffset) {
        
        ArrayList<PlurkListItem> ret = null;
        
        try {
            
            HashMap<String, String> params = new HashMap<String, String>();
            params.put("offset", dateOffset);
            params.put("limit", String.valueOf(limit));
            
            if (whichView == 1) {
                params.put("only_user", "true");
            }

            if (whichView == 2) {
                params.put("only_private", "true");
            }

            if (whichView == 3) {
                params.put("only_responded", "true");
            }
            

            String url;
            if (whichView == 4) {
                url = getApiUri(GET_UNREAD_URL);
            } else {
                url = getApiUri(GET_PLURKS_URL);
            }
            if (whichView == 5) {
                params.put("only_favorite", "true");
            }
            
            Response resp = performRequest(url, params);
            Log.d(TAG, "Resp status:" + resp.statusCode);
            if (resp.statusCode == HttpStatus.SC_BAD_REQUEST) {    // Requires login
                this.logined = false;
                return null;
            } else if (resp.statusCode == HttpStatus.SC_OK) {                
                JSONObject plurkObject = new JSONObject(resp.responseText);
                JSONObject plurkUsers = plurkObject.getJSONObject("plurk_users");
                JSONArray plurks = plurkObject.getJSONArray("plurks");
                int plurksCount = plurks.length();
                
                ret = new ArrayList<PlurkListItem>();
                for (int i = 0; i < plurksCount; ++i) {
                    PlurkListItem item = new PlurkListItem();

                    JSONObject plurk = plurks.getJSONObject(i);

                    String userId = plurk.getString("owner_id");
                    JSONObject userJson = plurkUsers.getJSONObject(userId);
                    String nickname;
                    try {
                        nickname = userJson.getString("display_name");
                        if (nickname == null || "null".equals(nickname) || nickname.equals("")) {
                            nickname = userJson.getString("nick_name");
                        }
                    } catch (JSONException e) {
                        nickname = userJson.getString("nick_name");
                    }
                    
                    String avatarIndex = userJson.getString("avatar");
                    if (avatarIndex == null || "null".equals(avatarIndex)) {
                        avatarIndex = "";
                    }
                    item.setAvatarIndex(avatarIndex);
                    
                    item.setNickname(nickname);
                    item.setPlurkId(plurk.getLong("plurk_id"));
                    item.setUserId(plurk.getLong("owner_id"));
                    item.setQualifier(plurk.getString("qualifier"));
                    
                    String q;
                    try {
                        q = plurk.getString("qualifier_translated");
                    } catch (JSONException e) {
                        q = plurk.getString("qualifier");
                    }
                    
                    item.setQualifierTranslated(q);
                    item.setRawContent(plurk.getString("content_raw"));
                    item.setContent(plurk.getString("content"));
                    item.setResponses(plurk.getInt("response_count"));
                    
                    String limited = plurk.getString("limited_to");
                    if (limited != null && !limited.equals("null")) {
                        item.setLimitTo(limited);
                    } else {
                        item.setLimitTo("");
                    }
                    
                    item.setHasSeen((byte)(1 - plurk.getInt("is_unread")));

                    String utcPosted = plurk.getString("posted");
                    //item.setUtcPosted(utcPosted);
                    item.setPosted(sdf.parse(utcPosted));
                    
                    // favoriters
                    int favCount = plurk.getInt("favorite_count");
                    item.setFavorites(favCount);
                    if (favCount > 0) {
                        JSONArray favs = plurk.getJSONArray("favorers");
                        List<String> f = new ArrayList<String>();
                        for (int j = 0; j < favs.length(); ++j) {
                            f.add(favs.getString(j));
                        }
                        item.setFavoriters(f);
                    }
                    
                    ret.add(item);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Get plurks failed! Reason: " + e.getMessage());
        }
        
        return ret;
    }
    
    
    public List<PlurkListItem> getResponses(String plurkId) {
        List<PlurkListItem> ret = null;
        try {
            HashMap<String, String> params = new HashMap<String, String>();
            params.put("plurk_id", plurkId);
            params.put("from_response", "0");
            
            Response resp = performRequest(getApiUri(GET_RESPONSES_URL), params);
            int status = resp.statusCode;
            if (status == HttpStatus.SC_OK) {
                ret = new ArrayList<PlurkListItem>();
                String respText = resp.responseText;
                JSONObject respond = new JSONObject(respText);
                try {
                    JSONObject friends = respond.getJSONObject("friends");
                    JSONArray responses = respond.getJSONArray("responses");
                    int respCount = responses.length();
                    
                    ret = new ArrayList<PlurkListItem>();
                    for (int i = 0; i < respCount; ++i) {
                        PlurkListItem item = new PlurkListItem();
                        
                        JSONObject jsonResp = responses.getJSONObject(i);
                        long userId = jsonResp.getLong("user_id");
                        item.setUserId(userId);
                        
                        JSONObject f = friends.getJSONObject(String.valueOf(userId));
                        String nickname;
                        try {
                            nickname = f.getString("display_name");
                            if (nickname == null || "null".equals(nickname) || nickname.equals("")) {
                                nickname = f.getString("nick_name");
                            }
                        } catch (JSONException e) {
                            nickname = f.getString("nick_name");
                        }
                        item.setNickname(nickname);
                        item.setQualifier(jsonResp.getString("qualifier"));
                        
                        String q;
                        try {
                            q = jsonResp.getString("qualifier_translated");
                        } catch (JSONException e) {
                            q = jsonResp.getString("qualifier");
                        }
                        
                        item.setQualifierTranslated(q);
                        
                        String avatarIndex = f.getString("avatar");
                        if (avatarIndex == null || "null".equals(avatarIndex)) {
                            avatarIndex = "";
                        }
                        item.setAvatarIndex(avatarIndex);
                        
                        item.setRawContent(jsonResp.getString("content_raw"));
                        item.setContent(jsonResp.getString("content"));
                        String utcPosted = jsonResp.getString("posted");
                        item.setPosted(sdf.parse(utcPosted));
                        
                        ret.add(item);
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Parsing responses error!", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Get ["+plurkId+"] responses failed! Reason:", e);
        }
        return ret;
    }
    
    
    public boolean addResponse(String plurkId, String qualifier, String content) {
        boolean result = false;
        
        try {
            HashMap<String, String> params = new HashMap<String, String>();
            params.put("api_key", API_KEY);
            params.put("plurk_id", plurkId);
            params.put("qualifier", qualifier);
            params.put("content", content);
            
            Configuration conf = context.getResources().getConfiguration();
            String lang = "en";
            if (conf.locale == Locale.TRADITIONAL_CHINESE) {
                lang = "tr_ch";
            }
            
            params.put("lang", lang);            
            Response resp = performRequest(getApiUri(RESPOND_PLURK_URL), params);

            if (resp.statusCode == HttpStatus.SC_OK) {
                result = true;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "addResponse failed!", e);
        } finally {
            connMgr.shutdown();
        }
        
        return result;
    }    
        
    public Bitmap getAvatar(String userId, String scale, String avatarIndex) {
        Bitmap avatar = null;
        try {
            File cacheDir = Utils.ensureCache(context, "avatars");
            if ("".equals(avatarIndex) || "null".equals(avatarIndex) || avatarIndex == null) {
                avatarIndex = "0";
            }
            String filename = scale+"-"+avatarIndex+".png";
            File d = new File(cacheDir, userId);
            if (d.exists()) {
                File f = new File(d, filename);
                if (f.exists()) {
                    FileInputStream fis = new FileInputStream(f);
                    avatar = BitmapFactory.decodeStream(fis);
                }   
            }   
    
        } catch (IOException e) {
            Log.e(TAG, "Read avatar cache failed!", e); 
        }
        
        if (avatar == null) {
            // re-fetch the avatar
            if ("0".equals(avatarIndex) || "null".equals(avatarIndex) || avatarIndex == null) {
                avatarIndex = "";
            }
            String suffix = ".gif";
            if ("big".equals(scale)) {
                suffix = ".jpg";
            }
            String url = "http://avatars.plurk.com/"+userId+"-"+scale+avatarIndex+suffix;
            InputStream is = null;
            try {
                Log.d(TAG, "Fetch avatar: " + url);
                URL avatarUrl = new URL(url);
                is = (InputStream) avatarUrl.getContent();

                avatar = BitmapFactory.decodeStream(is);

                if (avatar != null) {
                    File cacheDir = Utils.ensureCache(context, "avatars");
                    if ("".equals(avatarIndex)) {
                        avatarIndex = "0";
                    }
                    String filename = scale+"-"+avatarIndex+".png";
                    File d = new File(cacheDir, userId);
                    if (!d.exists()) {
                        d.mkdirs();
                    }
    
                    File f = new File(d, filename);
                    if (!f.exists()) {
                        f.createNewFile();
                        FileOutputStream fos = new FileOutputStream(f);
                        avatar.compress(CompressFormat.PNG, 100, fos);
                        fos.close();
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Fetch avatar fail!", e);
            }
        }
        
        return avatar;
    }
    
    /**
     * Shorten an URL
     */
    public String shortenUrl(String url, int option) {
        String newUrl = url;
        
        try {
            HttpPost post = new HttpPost();
            if (option == URL_SHORTEN_GOOGL) {
                post.setURI(URI.create(GOOGL_SHORTEN_API_URL));
                post.addHeader("Content-Type", "application/json");
                post.setEntity(new StringEntity("{\"longUrl\": \""+ url + "\"}", "utf-8"));
            }
            HttpResponse resp = httpClient.execute(post);
            int status = resp.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_OK) {
                String respText = getResponseText(resp.getEntity().getContent());
                Log.d(TAG, respText);
                JSONObject respond = new JSONObject(respText);
                
                if (option == URL_SHORTEN_GOOGL) {
                    newUrl = respond.getString("id");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "URL shortening error!", e);
        } finally {
            connMgr.shutdown();
        }
        
        return newUrl;
    }
    
    /**
     * Upload a picture to the specified dst
     * @param photoUri
     * @param dst
     * @return
     */
    public String uploadPicture(String photoPath, String contentType, String dst) {
        String photoUrl = null;
        
        try {
            String resize = sharedPref.getString(PluroiumApplication.PREF_RESIZE_PHOTO, "no");
            
            File f = Utils.resizePhoto(context, photoPath, resize);
            FileInputStream fis = new FileInputStream(f);
            String[] pathSegs = photoPath.split("/");

            ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
            writeFormField(baos, "api_key", API_KEY);
            writeFileField(baos, "image", pathSegs[pathSegs.length-1], contentType, fis);
            
            baos.write((twoHyphens + boundary + twoHyphens + CRLF).getBytes());
            fis.close();

            HttpPost post = new HttpPost(getApiUri(UPLOAD_PHOTO_URL));
            post.setHeader("Content-Type", "multipart/form-data; boundary="+boundary);
            post.setEntity(new ByteArrayEntity(baos.toByteArray()));
            
            HttpResponse resp = httpClient.execute(post);
            int status = resp.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_OK) {
                String respText = getResponseText(resp.getEntity().getContent());
                JSONObject jsonObj = new JSONObject(respText);
                photoUrl = jsonObj.getString("full");
            }
            
            f.delete();
            
        } catch (Exception e) {
            Log.e(TAG, "Upload photo failed!", e);
        } finally {
            connMgr.shutdown();
        }
        
        return photoUrl;
    }
    
    public boolean uploadPhoto(Uri photoUri, String caption) {
        boolean result = false;
        
        try {
            // get photo path
            Cursor cur = context.getContentResolver().query(photoUri, 
                    new String[] {android.provider.MediaStore.Images.ImageColumns.DATA,
                                  android.provider.MediaStore.Images.ImageColumns.MIME_TYPE}, 
                    null, null, null);
            cur.moveToFirst();
            String photoPath = cur.getString(0);
            String contentType = cur.getString(1);
            cur.close();

            String resize = sharedPref.getString(PluroiumApplication.PREF_RESIZE_PHOTO, "no");
            
            File f = Utils.resizePhoto(context, photoPath, resize);
            
            FileInputStream fis = new FileInputStream(f);
            String[] pathSegs = photoPath.split("/");

            ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
            writeFormField(baos, "api_key", API_KEY);
            writeFileField(baos, "image", pathSegs[pathSegs.length-1], contentType, fis);
            
            baos.write((twoHyphens + boundary + twoHyphens + CRLF).getBytes());
            fis.close();

            HttpPost post = new HttpPost(getApiUri(UPLOAD_PHOTO_URL));
            post.setHeader("Content-Type", "multipart/form-data; boundary="+boundary);
            post.setEntity(new ByteArrayEntity(baos.toByteArray()));
            
            HttpResponse resp = httpClient.execute(post);
            int status = resp.getStatusLine().getStatusCode();
            String respText = getResponseText(resp.getEntity().getContent());
            if (status == HttpStatus.SC_OK) {
                JSONObject jsonObj = new JSONObject(respText);
                String photoUrl = jsonObj.getString("full");
                
                result = addPlurk("shares", photoUrl + " " + caption, true);
            } else {
                Log.e(TAG, "Upload resp: (" + status + ") " + respText);
            }
            
            f.delete();
        } catch (Exception e) {
            Log.e(TAG, "Upload photo error: ", e);
        } finally {
            connMgr.shutdown();
        }
        
        return result;
    }
    
    
    /**
     * Build response text.
     * 
     * @param resp
     * @return
     * @throws IOException
     */
    private String getResponseText(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8192);
        StringBuffer respText = new StringBuffer();
        String line;

        while ((line = br.readLine()) != null) {
            respText.append(line);
        }

        br.close();
        
        return respText.toString();
    }
    
    private static final String twoHyphens = "--";
    private static final String boundary = "---------------------------3111649156260459601432063067";
    private static final String CRLF = "\r\n";
    
    private void writeFormField(ByteArrayOutputStream baos, String fieldName, String fieldValue) throws IOException {
        baos.write((twoHyphens + boundary + CRLF).getBytes());
        baos.write(("Content-Disposition: form-data;name=\"" + fieldName + "\"" + CRLF).getBytes("UTF-8"));
        baos.write((CRLF + fieldValue + CRLF).getBytes());
    }
    
    private void writeFileField(ByteArrayOutputStream baos, String fieldName, String fileName, 
            String contentType, FileInputStream fis) throws IOException {
 
        baos.write((twoHyphens + boundary + CRLF).getBytes());
        baos.write(("Content-Disposition: form-data;name=\"" + fieldName + "\";filename=\"" + fileName + 
                "\"" + CRLF).getBytes("UTF-8"));

        baos.write(("Content-Type: " + contentType + CRLF + CRLF).getBytes());
        
        int ba = fis.available();
        int maxSize = 8192;
        int bufSize = Math.min(ba, maxSize);
        byte[] buf = new byte[bufSize];
        
        while (fis.read(buf, 0, bufSize) > 0) {
            baos.write(buf);
            ba = fis.available();
            bufSize = Math.min(ba, maxSize);
        }
        
        baos.write(CRLF.getBytes());
    }
    
    public static class Response {
        public int statusCode;
        public String responseText;
    }
    
    private Response performRequest(String url, Map<String, String> reqParams) {
        Response rsp = null;
        
        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("api_key", API_KEY));
        for (String key : reqParams.keySet()) {
            params.add(new BasicNameValuePair(key, reqParams.get(key)));
        }
        HttpEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(params, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            // this should never happen.
            throw new AssertionError(e);
        }
        HttpPost post = new HttpPost(url);
        post.addHeader(entity.getContentType());
        post.addHeader("Accept-Encoding", "gzip,deflate");
        post.setEntity(entity);
        
        try {
            HttpResponse resp = httpClient.execute(post);
            rsp = new Response();
            rsp.statusCode = resp.getStatusLine().getStatusCode();
            HttpEntity respEntity = resp.getEntity();
            InputStream is = null;
            if (respEntity.getContentEncoding() != null) {
                String contentEncoding = respEntity.getContentEncoding().getValue();
                if ("gzip".equals(contentEncoding)) {
                    is = new GZIPInputStream(respEntity.getContent());
                } else if ("deflate".equals(contentEncoding)) {
                    is = new InflaterInputStream(respEntity.getContent(), new Inflater(true));
                } else {
                    is = respEntity.getContent();
                }
            } else {
                is = respEntity.getContent();
            }
            
            rsp.responseText = getResponseText(is);
        } catch (IOException e) {
            Log.e(TAG, "Network access error!", e);
        } finally {
            connMgr.shutdown();
        }
        
        return rsp;
    }
}
