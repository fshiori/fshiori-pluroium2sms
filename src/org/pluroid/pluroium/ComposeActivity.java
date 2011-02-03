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

package org.pluroid.pluroium;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
 
public class ComposeActivity extends Activity implements 
        View.OnClickListener, DialogInterface.OnClickListener {

    private static final String TAG = "ComposeActivity";
    
    // Dialog IDs
    private static final int DIALOG_EMOTION_LIST = 1;
    private static final int DIALOG_PHOTO_OPTIONS = 2;
    private static final int DIALOG_LOCATION_OPTIONS = 3;
    private static final int DIALOG_PLURKING = 4;
    private static final int DIALOG_UPLOADING = 5;
    private static final int DIALOG_LOCATING = 6;
    
    // Photo Options
    private static final int PHOTO_OPTION_CAMERA = 0;
    private static final int PHOTO_OPTION_GALLERY = 1;
    
    // Location Options
    private static final int LOCATION_OPTION_LOCATE = 0;
    
    // Request constants
    private static final int REQ_TAKE_PICTURE = 0;
    private static final int REQ_CHOOSE_PICTURE = 1;
    private static final int REQ_PICK_LOCATION = 2;
    
    private static final int MSG_PLURK_DONE = 1;
    private static final int MSG_LOCATION_DONE = 2;
        
    private LocationManager locationManager;
    private LocationListener gpsListener;
    private LocationListener networkListener;
    
    private AlertDialog emotionDialog;
    private AlertDialog photoOptionDialog;
    private AlertDialog locationOptionDialog;
    private ProgressDialog plurkingDialog;
    private ProgressDialog uploadingDialog;
    private ProgressDialog locatingDialog;
    private EditText plurkContent;
    private Spinner plurkQualifier;
    
    private Button plurkButton;
    private Button cancelButton;
    
    // camera result
    private Uri outputFileUri;
    
    private boolean locationGenerated = false;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        
        initView();
        
        Intent intent = getIntent();
        if (intent != null) {
            handleIntent(intent);
        }
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        
        Resources res = getResources();
        
        switch (id) {
        case DIALOG_PLURKING:
            if (plurkingDialog == null) {
                plurkingDialog = new ProgressDialog(this);
                plurkingDialog.setMessage(getResources().getString(R.string.plurking));
                plurkingDialog.setIndeterminate(true);
                plurkingDialog.setCancelable(true);
            }
            return plurkingDialog;
        case DIALOG_UPLOADING:
            if (uploadingDialog == null) {
                uploadingDialog = new ProgressDialog(this);
                uploadingDialog.setMessage(getResources().getString(R.string.compose_uploading_message));
                uploadingDialog.setIndeterminate(true);
                uploadingDialog.setCancelable(true);
            }
            return uploadingDialog;
        case DIALOG_LOCATING:
            if (locatingDialog == null) {
                locatingDialog = new ProgressDialog(this);
                locatingDialog.setMessage(getResources().getString(R.string.compose_locating_message));
                locatingDialog.setIndeterminate(true);
                locatingDialog.setCancelable(true);
            }
            return locatingDialog;
        case DIALOG_EMOTION_LIST:
            if (emotionDialog == null) {
                String[] icons = res.getStringArray(R.array.emotion_icons);
                final String[] texts = res.getStringArray(R.array.emotion_text);
                
                final int N = icons.length;
                
                List<Map<String, String>> entries = new ArrayList<Map<String, String>>();
                for (int i = 0; i < N; ++i) {
                    Map<String, String> entry = new HashMap<String, String>();
                    
                    entry.put("icon", icons[i]);
                    entry.put("text", texts[i]);
                    
                    entries.add(entry);
                }
                
                final SimpleAdapter adapter = new SimpleAdapter(
                        this,
                        entries,
                        R.layout.emotion_menu,
                        new String[] {"icon", "text"},
                        new int[] {R.id.smiley_icon, R.id.smiley_text});
                
                SimpleAdapter.ViewBinder viewBinder = new SimpleAdapter.ViewBinder() {
                    
                    public boolean setViewValue(View view, Object data,
                            String textRepresentation) {
                        if (view instanceof ImageView) {
                            String filepath = (String) data + ".gif";
                            try {
                                Drawable icon = new BitmapDrawable(getAssets().open(filepath));
                                ((ImageView) view).setImageDrawable(icon);
                            } catch (IOException e) {

                            }
                            return true;
                        }
                        return false;
                    }
                };
                
                adapter.setViewBinder(viewBinder);
                
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(res.getString(R.string.compose_emotion_dialog_title));
                builder.setCancelable(true);
                builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                    
                    public void onClick(DialogInterface dialog, int which) {
                        Editable edit = plurkContent.getEditableText();
                        edit.append(texts[which]+" ");
                    }
                });
                
                emotionDialog = builder.create();
            }
            return emotionDialog;
        case DIALOG_PHOTO_OPTIONS:
            if (photoOptionDialog == null) {
                ArrayAdapter<String> optsAdapter = 
                    new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                            res.getStringArray(R.array.compose_camera_options));
                
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(res.getString(R.string.compose_camera_options_title));
                builder.setCancelable(true);
                builder.setAdapter(optsAdapter, this);
                photoOptionDialog = builder.create();
            }
            
            return photoOptionDialog;
        case DIALOG_LOCATION_OPTIONS:
            if (locationOptionDialog == null) {
                ArrayAdapter<String> optsAdapter = 
                    new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                            res.getStringArray(R.array.compose_location_options));
                
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(res.getString(R.string.compose_location_options_title));
                builder.setCancelable(true);
                builder.setAdapter(optsAdapter, this);
                locationOptionDialog = builder.create();
            }
            return locationOptionDialog;
        }
        
        return null;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK) {
            String photoPath = null;
            String contentType = "image/jpeg";
            
            switch (requestCode) {
            case REQ_TAKE_PICTURE:
                photoPath = outputFileUri.getPath();
                break;
            case REQ_CHOOSE_PICTURE:
                if (data != null) {
                    Uri photoUri = data.getData();
                    // get photo path
                    Cursor cur = getContentResolver().query(photoUri, 
                            new String[] {android.provider.MediaStore.Images.ImageColumns.DATA,
                                          android.provider.MediaStore.Images.ImageColumns.MIME_TYPE}, 
                            null, null, null);
                    cur.moveToFirst();
                    photoPath = cur.getString(0);
                    contentType = cur.getString(1);
                    cur.close();

                }
                break;
            case REQ_PICK_LOCATION:
                if (data != null) {
                    Bundle extras = data.getExtras();
                    new GenerateMapUrlTask().execute(extras.getDouble("lat"), extras.getDouble("lng"));
                }
                break;
            }
            
            if (photoPath != null) {
                new UploadPhotoTask().execute(photoPath, contentType);
            }
        }
        
    }
    
    private void initView() {
        setContentView(R.layout.compose);
        
        plurkContent = (EditText) findViewById(R.id.compose_content);
        plurkQualifier = (Spinner) findViewById(R.id.compose_qualifier);
        
        plurkButton = (Button) findViewById(R.id.compose_plurk_button);
        plurkButton.setOnClickListener(this);
        
        cancelButton = (Button) findViewById(R.id.compose_cancel_button);
        cancelButton.setOnClickListener(this);
    }

    public void onClick(View v) {
        if (v == plurkButton) {
            showDialog(DIALOG_PLURKING);
            new Thread(){
                public void run() {
                    PlurkHelper plurkHelper = new PlurkHelper(ComposeActivity.this);
                    plurkHelper.addPlurk(PluroiumApplication.qualifiers[plurkQualifier.getSelectedItemPosition()],
                            plurkContent.getText().toString(), true);
                    msgHandler.sendEmptyMessage(MSG_PLURK_DONE);
                }
            }.start();
        } else if (v == cancelButton) {
            finish();
        }
    }
    
    protected void handleIntent(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            plurkQualifier.setSelection(4);
            plurkContent.append(intent.getStringExtra(Intent.EXTRA_TEXT));
            String sub = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            if (sub != null) {
                plurkContent.append(" (" + sub + ")");
            }
        }
    }
    
    private Handler msgHandler = new Handler() {
        
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_PLURK_DONE:
                plurkingDialog.cancel();
                finish();
                break;
            case MSG_LOCATION_DONE:
                if (!locationGenerated) {
                    locationGenerated = true;
                    String url = (String) msg.obj;
                    String content = plurkContent.getText().toString();
                    if (content.length() > 0) {
                        plurkContent.append(" ");
                    }
                    plurkContent.append(url);
                }
                locatingDialog.cancel();
                break;
            default:
                super.handleMessage(msg);
            }
        }
    };
    
    public void onEmotionClick(View view) {
        showDialog(DIALOG_EMOTION_LIST);
    }
    
    public void onHyperlinkClick(View view) {
        
    }
    
    public void onCameraClick(View view) {
        showDialog(DIALOG_PHOTO_OPTIONS);
    }
    
    public void onMapClick(View view) {
        showDialog(DIALOG_LOCATION_OPTIONS);
    }
    
    /**
     * AlertDialog click listener.
     */
    public void onClick(DialogInterface dialog, int which) {
        if (dialog == photoOptionDialog) {
            switch (which) {
            case PHOTO_OPTION_CAMERA:
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File tmpFile = new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis() + ".jpg");
                outputFileUri = Uri.fromFile(tmpFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                startActivityForResult(intent, REQ_TAKE_PICTURE);
                break;
            case PHOTO_OPTION_GALLERY:
                Intent pickIntent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(pickIntent, REQ_CHOOSE_PICTURE);
                break;
            }
        } else if (dialog == locationOptionDialog) {
            switch (which) {
            case LOCATION_OPTION_LOCATE:
                showDialog(DIALOG_LOCATING);
                locationGenerated = false;
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    gpsListener = new LocationListener() {

                        public void onLocationChanged(Location location) {
                            double lat = location.getLatitude(),
                                   lng = location.getLongitude();
                            
                            Log.d(TAG, "Geo: (" + lat + ", " + lng + ")");
                            String mapUrl = "http://maps.google.com/maps?ll=" + lat + "," + lng;
                            
                            PlurkHelper plurkHelper = new PlurkHelper(ComposeActivity.this);
                            msgHandler.sendMessage(msgHandler.obtainMessage(MSG_LOCATION_DONE, 
                                    plurkHelper.shortenUrl(mapUrl, PlurkHelper.URL_SHORTEN_GOOGL)));
                            clearLocationListeners();
                        }

                        public void onProviderDisabled(String provider) {
                        }

                        public void onProviderEnabled(String provider) {
                        }

                        public void onStatusChanged(String provider,
                                int status, Bundle extras) {
                        }
                        
                    };
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);
                }
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    networkListener = new LocationListener() {

                        public void onLocationChanged(Location location) {
                            double lat = location.getLatitude(),
                               lng = location.getLongitude();
                        
                            String mapUrl = "http://maps.google.com/maps?ll=" + lat + "," + lng;
                        
                            PlurkHelper plurkHelper = new PlurkHelper(ComposeActivity.this);

                            msgHandler.sendMessage(msgHandler.obtainMessage(MSG_LOCATION_DONE, 
                                plurkHelper.shortenUrl(mapUrl, PlurkHelper.URL_SHORTEN_GOOGL)));
                            clearLocationListeners();
                        }

                        public void onProviderDisabled(String provider) {
                        }

                        public void onProviderEnabled(String provider) {
                        }

                        public void onStatusChanged(String provider,
                                int status, Bundle extras) {
                        }
                        
                    };
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networkListener);
                }
                break;
            }
        }
    }
    
    private class UploadPhotoTask extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            showDialog(DIALOG_UPLOADING);
        }
        
        @Override
        protected String doInBackground(String... args) {
            PlurkHelper plurkHelper = new PlurkHelper(ComposeActivity.this);
            return plurkHelper.uploadPicture(args[0], args[1], "plurk");
        }

        @Override
        protected void onPostExecute(String url) {
            if (url != null) {
                String content = plurkContent.getText().toString();
                if (content.length() > 0) {
                    plurkContent.append(" ");
                }
                plurkContent.append(url);
            }
            uploadingDialog.cancel();
        }
    }
    
    private class GenerateMapUrlTask extends AsyncTask<Double, Integer, String> {
        
        @Override
        protected void onPreExecute() {
            showDialog(DIALOG_LOCATING);
        }
        
        @Override
        protected String doInBackground(Double... args) {
            PlurkHelper plurkHelper = new PlurkHelper(ComposeActivity.this);
            return plurkHelper.shortenUrl("http://maps.google.com/maps?ll=" + String.valueOf(args[0]) + "," + String.valueOf(args[1]), PlurkHelper.URL_SHORTEN_GOOGL);
        }
        
        @Override
        protected void onPostExecute(String url) {
            if (url != null && url.length() > 0) {
                PlurkHelper plurkHelper = new PlurkHelper(ComposeActivity.this);

                msgHandler.sendMessage(msgHandler.obtainMessage(MSG_LOCATION_DONE, 
                        plurkHelper.shortenUrl(url, PlurkHelper.URL_SHORTEN_GOOGL)));
            }
            locatingDialog.cancel();
        }
    }
    
    private void clearLocationListeners() {
        if (gpsListener != null) {
            locationManager.removeUpdates(gpsListener);
        }
        
        if (networkListener != null) {
            locationManager.removeUpdates(networkListener);
        }
    }
    
}
