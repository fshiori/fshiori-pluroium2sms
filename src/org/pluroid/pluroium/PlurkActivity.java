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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.pluroid.pluroium.data.PlurkListItem;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class PlurkActivity extends ListActivity 
    implements View.OnClickListener, OnItemClickListener, OnScrollListener {

    private static final String TAG = "PlurkActivity";
    
    // REQUEST CODE
    private static final int REQUEST_SETTINGS = 1;
    private static final int REQUEST_PHOTO_PICK = 2;
    
    // MENU ITEM IDs
    private static final int MENU_VIEW             = Menu.FIRST;
    private static final int MENU_UPLOAD_PHOTO     = Menu.FIRST + 1;
    private static final int MENU_SETTINGS         = Menu.FIRST + 2;
    private static final int MENU_LOGOUT           = Menu.FIRST + 3;
    
    // Context Menu
    private static final int CONTEXT_MENU_READ     = 1;
    
    private static final int MSG_LOADING_FAIL	= 2;
    private static final int MSG_PLURK_DONE     = 6;
    private static final int MSG_SWITCH_VIEW    = 7;
    
    // Dialog IDs
    private static final int DIALOG_CONFIRM_LOGOUT = 1;
    private static final int DIALOG_PLURKING = 2;
    private static final int DIALOG_SWITCH_VIEW = 3;
    
    private SharedPreferences sharedPref;
    private ArrayList<PlurkListItem> newPlurks;
    private ListView plurkListView;
    private PlurkListAdapter plurkListAdapter;
    private TextView listFooter;
    private ProgressDialog plurkingDialog;
    private AlertDialog switchViewDialog;
    
    private boolean loading = false;
    private int currentPlurksView = 0;
    
    private static SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    private static SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US);
    // for the `responded plurks` (ericsk: strange...)
    private static SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
    
    private MenuItem switchViewItem;
    
    private LoadPlurksTask loadPlurksTask;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        clearTimeStamp();        
        initView();
        loadPlurks();
    }
    
    @Override
    protected void onStop() {
        super.onStop();

        if (loadPlurksTask != null) {
            loadPlurksTask.cancel(true);
            loading = false;
        }
    }
    
    @Override
    protected void onDestroy() {
      super.onDestroy();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {        
        
        switchViewItem = menu.add(0, MENU_VIEW, 0, R.string.menu_view).setIcon(android.R.drawable.ic_menu_view);
        
        menu.add(0, MENU_UPLOAD_PHOTO, 0, R.string.menu_upload_photo_title)
            .setIcon(android.R.drawable.ic_menu_upload);
        
        menu.add(0, MENU_SETTINGS, 0, R.string.menu_preferences_title).setIcon(android.R.drawable.ic_menu_preferences);
        
        menu.add(0, MENU_LOGOUT, 0, R.string.menu_logout)
            .setIcon(android.R.drawable.ic_menu_close_clear_cancel);

        if (loading) {
            switchViewItem.setEnabled(false);
        }
        
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_VIEW:
            showDialog(DIALOG_SWITCH_VIEW);
            break;
        case MENU_UPLOAD_PHOTO:
            Intent pickIntent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            startActivityForResult(pickIntent, REQUEST_PHOTO_PICK);
            break;
        case MENU_SETTINGS:
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivityForResult(settingsIntent, REQUEST_SETTINGS);
            break;
        case MENU_LOGOUT:
            showDialog(DIALOG_CONFIRM_LOGOUT);
            break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.v(TAG, "onConfigurationChanged");
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_CONFIRM_LOGOUT:
            return new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_logout_confirm_title)
                        .setMessage(R.string.dialog_logout_confirm_message)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                performLogout();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .create();
        case DIALOG_PLURKING:
            if (plurkingDialog == null) {
                plurkingDialog = new ProgressDialog(this);
                plurkingDialog.setMessage(getResources().getString(R.string.plurking));
                plurkingDialog.setIndeterminate(true);
                plurkingDialog.setCancelable(true);
            }
            return plurkingDialog;
        case DIALOG_SWITCH_VIEW:
            if (switchViewDialog == null) {                
                ArrayAdapter<String> viewsAdapter = 
                    new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.plurk_types));

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getResources().getString(R.string.dialog_switch_view_title));
                builder.setCancelable(true);
                builder.setAdapter(viewsAdapter, new DialogInterface.OnClickListener() {                    
                    public void onClick(DialogInterface dialog, int which) {
                        currentPlurksView = which;
                        msgHandler.sendEmptyMessage(MSG_SWITCH_VIEW);
                    }
                });
                
                switchViewDialog = builder.create();
            }
            return switchViewDialog;
        default:
            return super.onCreateDialog(id);
        }
    }
    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        switch (requestCode) {
        case REQUEST_SETTINGS:
            break;
        case REQUEST_PHOTO_PICK:
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    Uri photoUri = data.getData();
                    Intent sharePhotoIntent = new Intent(this, SharePhotoActivity.class);
                    Bundle extras = new Bundle();
                    extras.putParcelable(Intent.EXTRA_STREAM, photoUri);
                    sharePhotoIntent.putExtras(extras);
                    startActivity(sharePhotoIntent);
                }
            }
            break;
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
    
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        
        int ind = 0;        
        menu.add(0, CONTEXT_MENU_READ, ind++, R.string.context_menu_read_title);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        super.onContextItemSelected(item);
        
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "Bad MenuInfo");
            return false;
        }
        
        PlurkListItem plurkItem = plurkListAdapter.getItem(info.position);
        
        Intent intent = new Intent();
        Bundle data;
        
        switch (item.getItemId()) {
        case CONTEXT_MENU_READ:
            intent.setClass(PlurkActivity.this, SinglePlurkActivity.class);
            
            data = new Bundle();
            data.putString("plurk_id", String.valueOf(plurkItem.getPlurkId()));
            data.putString("userId", String.valueOf(plurkItem.getUserId()));
            data.putString("avatar_index", plurkItem.getAvatarIndex());
            data.putParcelable("avatar", plurkItem.getAvatar());
            data.putString("nickname", plurkItem.getNickname());
            data.putString("qualifier", plurkItem.getQualifier());
            data.putString("qualifier_translated", plurkItem.getQualifierTranslated());
            data.putCharSequence("content", plurkItem.getRawContent());
            data.putString("posted", plurkItem.getPosted().toLocaleString());
            intent.putExtras(data);
            startActivity(intent);
            
            return true;
        }
        
        return false;
    }
    
    
    private void clearTimeStamp() {
        Editor prefEdit = sharedPref.edit();
        prefEdit.putString(Constant.LAST_TIME, "").commit();
        if (plurkListAdapter != null) {
            plurkListAdapter.clear();
        }
    }
    
    private void initView() {
        setContentView(R.layout.plurk);
        
        plurkListView = getListView();
        plurkListView.setItemsCanFocus(true);
        plurkListView.setClickable(true);
        plurkListView.setOnCreateContextMenuListener(this);
        plurkListView.setOnItemClickListener(this);
        plurkListView.setOnScrollListener(this);
        plurkListAdapter = new PlurkListAdapter(this);
        
        listFooter = (TextView) LayoutInflater.from(this).inflate(R.layout.loading_footer, null);
        plurkListView.addFooterView(listFooter);
        listFooter.setVisibility(View.GONE);
        
        plurkListView.setAdapter(plurkListAdapter);
    }
    
    private void loadPlurks() {
        loading = true;

        PlurkHelper plurkHelper = new PlurkHelper(this);
        if (!plurkHelper.isLoginned()) {
            startActivity(new Intent(this, LaunchActivity.class));
            finish();
            return;
        }
        
        loadPlurksTask = new LoadPlurksTask();
        loadPlurksTask.execute("");
    }
    
    private void performLogout() {
        // clear the cookie
        Editor prefEditor = sharedPref.edit();
        prefEditor.putString(Constant.PREF_COOKIE, "");
        prefEditor.commit();
        
        Intent intent = new Intent(this, LaunchActivity.class);
        startActivity(intent);
        finish();
    }
    
    /**
     * Message handler
     */
    private Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            PlurkHelper plurkHelper = new PlurkHelper(PlurkActivity.this);
            // check if the login is required
            if (!plurkHelper.isLoginned()) {
                performLogout();
                return;
            }
            
            switch (msg.what) {
            case MSG_PLURK_DONE:
                clearTimeStamp();
                plurkingDialog.cancel();
                listFooter.setText(R.string.plurk_list_loading_title);
                loadPlurks();
                break;
            case MSG_SWITCH_VIEW:
                clearTimeStamp();
                listFooter.setText(R.string.plurk_list_loading_title);
                loadPlurks();
                break;
            default:
                super.handleMessage(msg);
            }
        }
    };

    public void onClick(View v) {
        if (!loading) {
            if (v == listFooter) {
                listFooter.setText(R.string.plurk_list_loading_title);
                loadPlurks();
            } 
        }    
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    	PlurkListItem item = (PlurkListItem) plurkListAdapter.getItem(position);
    	if (item != null) {
	        Intent intent = new Intent(PlurkActivity.this, SinglePlurkActivity.class);
	
	        Bundle data = new Bundle();
	        data.putString("plurk_id", String.valueOf(item.getPlurkId()));
	        data.putString("userId", String.valueOf(item.getUserId()));
	        data.putString("avatar_index", item.getAvatarIndex());
	        data.putParcelable("avatar", item.getAvatar());
	        data.putString("nickname", item.getNickname());
	        data.putString("qualifier", item.getQualifier());
	        data.putString("qualifier_translated", item.getQualifierTranslated());
	        data.putCharSequence("content", item.getContent());
	        data.putString("posted", sdf2.format(item.getPosted()));
	        intent.putExtras(data);
	        startActivity(intent);
    	}
    }
    
    private class LoadPlurksTask extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(String... arg0) {
            TimeZone tz = TimeZone.getDefault();
            int offset = tz.getRawOffset();
            Date nowDate;
            String timeOffset;
            
            String timeStart = sharedPref.getString(Constant.LAST_TIME, "");
            if (timeStart.length() == 0) {
                long now = Calendar.getInstance().getTime().getTime() + 30000;
                nowDate = new Date(now-offset);
                timeOffset = sdf.format(nowDate);
            } else {
                nowDate = new Date(Long.parseLong(timeStart)-offset);
                timeOffset = sdf3.format(nowDate);
            }
            
            // Don't try to query for more than 20. Otherwise plurk.com will return only private/personal plurks.
            PlurkHelper plurkHelper = new PlurkHelper(PlurkActivity.this);
            newPlurks = plurkHelper.getPlurks(currentPlurksView, 20, timeOffset);
            if (newPlurks != null) {
                int size = newPlurks.size();
                if (size > 0) {
                    Date utcPost = newPlurks.get(size-1).getPosted();                
                    Editor prefEdit = sharedPref.edit();
                    prefEdit.putString(Constant.LAST_TIME, String.valueOf(utcPost.getTime()));
                    prefEdit.commit();    
                }
                return true;
            } else {
                msgHandler.sendMessage(Message.obtain(msgHandler, MSG_LOADING_FAIL));
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean isOk) {
            if (isOk) {
                plurkListAdapter.addPlurks(newPlurks);
            } else {
            	// TODO: while the `get plurks` task failed...
            }
            
            loading = false;
            listFooter.setVisibility(View.GONE);
        }
        
    }

    /**
     * Refresh button on the title 
     * @param view
     */
    public void onRefreshClick(View view) {
        if (!loading) {
            clearTimeStamp();
            loadPlurks();
        }
    }
    
    /**
     * Compose title button
     */
    public void onComposeClick(View view) {        
        Intent composeIntent = new Intent(this, ComposeActivity.class);
        startActivity(composeIntent);
    }

    public void onScroll(AbsListView view, int firstVisibleItem,
            int visibleItemCount, int totalItemCount) {
        
        if (!loading) {
            if (visibleItemCount < totalItemCount && 
                    totalItemCount - 1 <= firstVisibleItem + visibleItemCount) {
                listFooter.setVisibility(View.VISIBLE);
                loadPlurks();
            }
        }
    }

    public void onScrollStateChanged(AbsListView view, int scrollState) {
        
    }
}
