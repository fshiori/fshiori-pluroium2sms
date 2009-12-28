package org.pluroid.pluroium;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pluroid.pluroium.data.PlurkListItem;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.util.Log;

public class PlurkHelper {
	
	private static final String TAG = "PlurkHelper";
	private static final String API_KEY = "PUT_YOUR_API_KEY_HERE";
	private SharedPreferences sharedPref;

	private Context context;
	
	private String username;
	private String password;
	private boolean logined;

	private static DefaultHttpClient httpClient;
	private static BasicCookieStore cookieStore;
	private static HashMap<Long, Bitmap> avatarCache;
	private static HashMap<String, Drawable> imgSrcCache;

	private static SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

	// API URI
	private static final String LOGIN_URL = "/Users/login";
	private static final String ADD_PLURK_URL = "/Timeline/plurkAdd";
	private static final String GET_PLURKS_URL = "/Timeline/getPlurks";
	private static final String GET_UNREAD_URL = "/Timeline/getUnreadPlurks";
	private static final String GET_RESPONSES_URL = "/Responses/get";
	private static final String RESPOND_PLURK_URL = "/Responses/responseAdd";
	private static final String UPLOAD_PHOTO_URL = "/Timeline/uploadPicture";
	
	static {
        imgSrcCache = new HashMap<String, Drawable>();
		avatarCache = new HashMap<Long, Bitmap>();
		httpClient = new DefaultHttpClient();
		cookieStore = new BasicCookieStore();
		httpClient.setCookieStore(cookieStore);
	}
	
	public PlurkHelper(Context context) {
		this.context = context;
		
		sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		// initial static members
		
		// read cookie
		cookieStore.clear();
		
		String cookieStr = sharedPref.getString(Constant.PREF_COOKIE, "");
		if (cookieStr.length() > 0) {
			String[] token = cookieStr.split(";");
			BasicClientCookie cookie = new BasicClientCookie(token[0], token[1]);
			cookie.setDomain(token[2]);
			cookie.setPath(token[3]);
			cookie.setExpiryDate(new Date(Long.parseLong(token[4])));
			cookieStore.addCookie(cookie);			
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

			try {
				List<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("api_key", API_KEY));
				params.add(new BasicNameValuePair("username", username));
				params.add(new BasicNameValuePair("password", password));
				
				HttpPost post = new HttpPost(getApiUri(LOGIN_URL));
				post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
				
				HttpResponse resp = httpClient.execute(post);
				int status = resp.getStatusLine().getStatusCode();
				
				if (status == HttpStatus.SC_OK) {	// successfully login
					logined = true;
					
					Editor prefEdit = sharedPref.edit();
					
					prefEdit.putString(Constant.PREF_AUTH_USERNAME_KEY, isRemember ? username : "");
					prefEdit.putString(Constant.PREF_AUTH_PASSWORD_KEY, isRemember ? password : "");
					
					// store cookie
					cookie = cookieStore.getCookies().get(0);
					String cookieStr = cookie.getName() + ";" + cookie.getValue() + ";" + cookie.getDomain() + ";" + 
							cookie.getPath() + ";" + cookie.getExpiryDate().getTime();
					prefEdit.putString(Constant.PREF_COOKIE, cookieStr);
					prefEdit.commit();
				}
			} catch (Exception e) {
				Log.e(TAG, "Login faield: " + e.getMessage());
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
            if (conf.locale == Locale.TRADITIONAL_CHINESE) {
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
			
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("api_key", API_KEY));
			params.add(new BasicNameValuePair("offset", dateOffset));
			params.add(new BasicNameValuePair("limit", String.valueOf(limit)));
			
			Log.d(TAG, whichView + "," + limit + ", " + dateOffset);
			
			if (whichView == 1) {
	            params.add(new BasicNameValuePair("only_user", "true"));
	        }

	        if (whichView == 2) {
	            params.add(new BasicNameValuePair("only_private", "true"));
	        }

	        if (whichView == 3) {
	            params.add(new BasicNameValuePair("only_responded", "true"));
	        }

	        HttpPost post;
	        if (whichView == 4) {
	        	post = new HttpPost(getApiUri(GET_UNREAD_URL));
	        } else {
	        	post = new HttpPost(getApiUri(GET_PLURKS_URL));
	        }
	        
	        
	        post.setEntity(new UrlEncodedFormEntity(params,"utf-8"));
            HttpResponse resp = httpClient.execute(post);
            int status = resp.getStatusLine().getStatusCode();
            
            if (status == HttpStatus.SC_OK) {
            	String responseText = getResponseText(resp.getEntity().getContent());
            	
            	JSONObject plurkObject = new JSONObject(responseText);
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
                    String content = plurk.getString("content");
                    item.setRawContent(content);
                    item.setContent((SpannableStringBuilder) Html.fromHtml(content, new Html.ImageGetter() {
						public Drawable getDrawable(String source) {
							try {
								if (imgSrcCache.containsKey(source)) {
									return imgSrcCache.get(source);
								} else {
									URLConnection conn = new URL(source).openConnection();
									conn.connect();
									
									InputStream is = conn.getInputStream();
									Bitmap bmp = BitmapFactory.decodeStream(is);
									Drawable d = new BitmapDrawable(bmp);
									d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
									is.close();
									imgSrcCache.put(source, d);
									return d;
								}
							} catch (Exception e) {
								return null;
							}
						}
					}, null));
                    item.setResponses(plurk.getInt("response_count"));
                    
                    String limited = plurk.getString("limited_to");
                    if (limited != null && !limited.equals("null")) {
                        item.setLimitTo(limited);
                    } else {
                        item.setLimitTo("");
                    }
                    
                    item.setHasSeen((byte)(1 - plurk.getInt("is_unread")));
                    String utcPosted = plurk.getString("posted");
                    item.setUtcPosted(utcPosted);

                    String posted = sdf.parse(utcPosted).toLocaleString();
                    item.setPosted(posted);
                    
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
			HttpPost post = new HttpPost(getApiUri(GET_RESPONSES_URL));
			
			List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("api_key", API_KEY));
            params.add(new BasicNameValuePair("plurk_id", plurkId));
            params.add(new BasicNameValuePair("from_response", "0"));
            post.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
            
            HttpResponse resp = httpClient.execute(post);
            int status = resp.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_OK) {
            	ret = new ArrayList<PlurkListItem>();
            	String respText = getResponseText(resp.getEntity().getContent());
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
                        String content = jsonResp.getString("content");
                        item.setRawContent(content);
                        item.setContent((SpannableStringBuilder) Html.fromHtml(content, new Html.ImageGetter() {
    						public Drawable getDrawable(String source) {
    							try {
    								if (imgSrcCache.containsKey(source)) {
    									return imgSrcCache.get(source);
    								} else {
    									URLConnection conn = new URL(source).openConnection();
    									conn.connect();
    									
    									InputStream is = conn.getInputStream();
    									Bitmap bmp = BitmapFactory.decodeStream(is);
    									Drawable d = new BitmapDrawable(bmp);
    									d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
    									is.close();
    									imgSrcCache.put(source, d);
    									return d;
    								}
    							} catch (Exception e) {
    								return null;
    							}
    						}
    					}, null));
                        String utcPosted = jsonResp.getString("posted");
                        item.setUtcPosted(utcPosted);

                        String posted = sdf.parse(utcPosted).toLocaleString();
                        item.setPosted(posted);
                        
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
			HttpPost post = new HttpPost(getApiUri(RESPOND_PLURK_URL));
			List<NameValuePair> params = new ArrayList<NameValuePair>();
	        params.add(new BasicNameValuePair("api_key", API_KEY));
	        params.add(new BasicNameValuePair("plurk_id", plurkId));
	        params.add(new BasicNameValuePair("qualifier", qualifier));
	        params.add(new BasicNameValuePair("content", content));
	        
            Configuration conf = context.getResources().getConfiguration();
            String lang = "en";
            if (conf.locale == Locale.TRADITIONAL_CHINESE) {
                lang = "tr_ch";
            }
            
            params.add(new BasicNameValuePair("lang", lang));
	        
	        post.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
            HttpResponse resp = httpClient.execute(post);

            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                result = true;
            }
	        
		} catch (Exception e) {
			Log.e(TAG, "addResponse failed!", e);
		}
		
		return result;
	}
	
		
	public Bitmap getAvatar(long userId, String scale, String avatarIndex) {
		Bitmap avatar = null;
		
		avatar = avatarCache.get(userId);
		if ("0".equals(avatarIndex)) {
			avatarIndex = "";
		}
		if (avatar == null) {
			String url = "http://avatars.plurk.com/"+userId+"-"+scale+avatarIndex+".gif";
			BufferedInputStream bis = null;
			try {
				URL avatarUrl = new URL(url);
				URLConnection conn = avatarUrl.openConnection();
				conn.connect();
				bis = new BufferedInputStream(conn.getInputStream(), 8192);
				avatar = BitmapFactory.decodeStream(bis);
				
				avatarCache.put(userId, avatar);
				
			} catch (MalformedURLException e) {
				Log.e(TAG, "avatar url error: " + url);
			} catch (IOException e) {
				Log.e(TAG, "fetching avatar fail! ("+url+")");
			} finally {
				if (bis != null) {
					try {
						bis.close();
					} catch (IOException e) {
					}
				}
			}
		}
		
		return avatar;
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

        	FileInputStream fis = new FileInputStream(photoPath);
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
		} catch (Exception e) {
			Log.e(TAG, "Upload photo error: ", e);
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
    private String getResponseText(InputStream resp) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(resp, "UTF-8"), 8192);
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
}
