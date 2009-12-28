package org.pluroid.pluroium;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.pluroid.pluroium.data.PlurkListItem;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class PlurkActivity extends Activity 
	implements View.OnClickListener, OnItemClickListener {

	private static final String TAG = "PlurkActivity";
	
	// REQUEST CODE
	private static final int REQUEST_SETTINGS = 1;
	private static final int REQUEST_PHOTO_PICK = 2;
	
	// MENU ITEM IDs
	private static final int MENU_COMPOSE 		= Menu.FIRST;
	private static final int MENU_REFRESH		= Menu.FIRST + 1;
	private static final int MENU_VIEW 			= Menu.FIRST + 2;
	private static final int MENU_UPLOAD_PHOTO 	= Menu.FIRST + 3;
	private static final int MENU_SETTINGS 		= Menu.FIRST + 4;
	private static final int MENU_LOGOUT 		= Menu.FIRST + 5;
	
	// Context Menu
	private static final int CONTEXT_MENU_READ = 1;
	
	private static final int MSG_LOADING_SUCCESS	= 1;
	private static final int MSG_LOADING_FAIL 		= 2;
	private static final int MSG_AVATAR_SETTING 	= 4;
	private static final int MSG_AVATAR_LOADED 		= 5;
	private static final int MSG_PLURK_DONE			= 6;
	private static final int MSG_SWITCH_VIEW		= 7;
	
	// Dialog IDs
	private static final int DIALOG_CONFIRM_LOGOUT = 1;
	private static final int DIALOG_PLURKING = 2;
	private static final int DIALOG_SWITCH_VIEW = 3;
	
	private SharedPreferences sharedPref;
	private ArrayList<PlurkListItem> newPlurks;
	private ListView plurkListView;
	private TextView headerText;
	private PlurkListAdapter plurkListAdapter;
	private TextView listFooter;
	private ProgressBar progressBar;
	private ProgressDialog plurkingDialog;
	private AlertDialog switchViewDialog;
	
	private EditText plurkContent;
	private Button plurkButton;

	private PlurkHelper plurkHelper;
	private List<String> avatarQueue;
	private boolean loading = false;
	private int currentPlurksView = 0;
	private int avatarStartIndex = 0;
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
	private static SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
	
	private InputMethodManager ime;
	private MenuItem refreshItem;
	private MenuItem switchViewItem;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.v(TAG, "onCreate");
        
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        plurkHelper = new PlurkHelper(this);
        avatarQueue = new ArrayList<String>();
        
        ime = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        
        clearTimeStamp();        
        initView();
    	loadPlurks();

    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {    	
    	
    	menu.add(0, MENU_COMPOSE, 0, R.string.menu_compose_title)
    		.setIcon(android.R.drawable.ic_menu_edit);
    	
    	refreshItem = menu.add(0, MENU_REFRESH, 0, R.string.menu_refresh_title)
    		.setIcon(R.drawable.ic_menu_refresh);
    	
    	
    	switchViewItem = menu.add(0, MENU_VIEW, 0, R.string.menu_view).setIcon(android.R.drawable.ic_menu_view);
    	    	
    	menu.add(0, MENU_UPLOAD_PHOTO, 0, R.string.menu_upload_photo_title)
    		.setIcon(android.R.drawable.ic_menu_upload);
    	
    	menu.add(0, MENU_SETTINGS, 0, R.string.menu_preferences_title).setIcon(android.R.drawable.ic_menu_preferences);
    	
    	menu.add(0, MENU_LOGOUT, 0, R.string.menu_logout)
    		.setIcon(android.R.drawable.ic_lock_power_off);

    	if (loading) {
    		refreshItem.setEnabled(false);
    		switchViewItem.setEnabled(false);
    	}
    	
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case MENU_COMPOSE:
    		Intent composeIntent = new Intent(this, ComposeActivity.class);
    		startActivity(composeIntent);
    		break;
    	case MENU_REFRESH:
    		clearTimeStamp();
    		listFooter.setText(R.string.plurk_list_loading_title);
			refreshItem.setEnabled(false);
    		loadPlurks();
    		break;
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
    		data.putString("avatar_index", plurkItem.getAvatarIndex());
    		data.putParcelable("avatar", plurkItem.getAvatar());
    		data.putString("nickname", plurkItem.getNickname());
    		data.putString("qualifier", plurkItem.getQualifier());
    		data.putString("qualifier_translated", plurkItem.getQualifierTranslated());
    		data.putCharSequence("content", plurkItem.getRawContent());
    		data.putString("posted", plurkItem.getPosted());
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
		avatarStartIndex = 0;
        if (avatarQueue != null) {
        	avatarQueue.clear();
        }
    }
    
    private void initView() {
        setContentView(R.layout.plurk);

        headerText = (TextView) findViewById(R.id.page_title);
        
        LayoutInflater inflater = LayoutInflater.from(this);
        plurkListView = (ListView) findViewById(R.id.plurk_list_view);
        plurkListView.setItemsCanFocus(true);
        plurkListView.setClickable(true);
        plurkListView.setOnCreateContextMenuListener(this);
        plurkListView.setOnItemClickListener(this);
        plurkListAdapter = new PlurkListAdapter(this);
        
        listFooter = (TextView) inflater.inflate(R.layout.loading_footer, null);
        listFooter.setOnClickListener(this);
                
        plurkListView.addFooterView(listFooter);
        plurkListView.setAdapter(plurkListAdapter);
                
        progressBar = (ProgressBar) findViewById(R.id.load_progressing);
        plurkButton = (Button) findViewById(R.id.plurk_button);
        plurkButton.setOnClickListener(this);
        
        plurkContent = (EditText) findViewById(R.id.plurk_text);
    }
    
    private void loadPlurks() {
    	progressBar.setVisibility(View.VISIBLE);
    	loading = true;
    	
    	if (refreshItem != null) {
    		refreshItem.setEnabled(false);
    		switchViewItem.setEnabled(false);
    	}
    	
    	if (plurkButton != null) {
    		plurkButton.setEnabled(false);
    	}
    	
    	if (!plurkHelper.isLoginned()) {
    		startActivity(new Intent(this, LaunchActivity.class));
    		finish();
    	}
    	
    	new Thread() {
    		@Override
    		public void run() {
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
        			timeOffset = sdf2.format(nowDate);
    			}
    	        
    	        newPlurks = plurkHelper.getPlurks(currentPlurksView, 25, timeOffset);
    	        if (newPlurks != null) {
    	        	int size = newPlurks.size();
					if (size > 0) {
						String utcPosted = newPlurks.get(size-1).getUtcPosted();				
						Editor prefEdit = sharedPref.edit();
						try {
							prefEdit.putString(Constant.LAST_TIME, String.valueOf(sdf.parse(utcPosted).getTime()));
							prefEdit.commit();
						} catch (ParseException e) {
							Log.e(TAG, "parse date error!");
						}
		
						int index = avatarStartIndex;
		    	        for (PlurkListItem item : newPlurks) {
		    	        	avatarQueue.add(index + ";" + item.getUserId() + ";"+item.getAvatarIndex());
		    	        	index++;
		    	        }
		    	        avatarStartIndex = index;
					}
	    	        msgHandler.sendMessage(Message.obtain(msgHandler, MSG_LOADING_SUCCESS));
    	        } else {
	    	        msgHandler.sendMessage(Message.obtain(msgHandler, MSG_LOADING_FAIL));
    	        }
    		}
    	}.start();
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
    
    private class AvatarObj {
    	public Bitmap avatar;
    	public int index;
    	
    	public AvatarObj(Bitmap avatar, int index) {
    		this.avatar = avatar;
    		this.index = index;
    	}    	
    }
    
    private void startAvatarThread() {
    	progressBar.setVisibility(View.VISIBLE);
    	new Thread() {
    		@Override
    		public void run() {
    			int qSize = avatarQueue.size();
    			for (int i = 0; i < qSize; ++i) {
    				String[] token = avatarQueue.get(i).split(";");
    				String avatarIndex = "";
    				if (token.length > 2) {
    					avatarIndex = token[2];
    				}
    				
    				Bitmap avatar = plurkHelper.getAvatar(Long.parseLong(token[1]), "medium", avatarIndex);
    				msgHandler.sendMessage(Message.obtain(
    						msgHandler, MSG_AVATAR_SETTING, new AvatarObj(avatar, Integer.parseInt(token[0]))));
    			}
				msgHandler.sendEmptyMessageDelayed(MSG_AVATAR_LOADED, 0);
    			avatarQueue.clear();
    		}
    	}.start();
    }
        
    /**
	 * Message handler
	 */
	private Handler msgHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
			switch (msg.what) {
			case MSG_LOADING_SUCCESS:
				progressBar.setVisibility(View.INVISIBLE);
				plurkListAdapter.addPlurks(newPlurks);
				listFooter.setText(R.string.plurk_list_more_title);
				
				boolean loadAvatar = sharedPref.getBoolean(Constant.PREF_LOAD_AVATAR, true);
				if (loadAvatar) {
					startAvatarThread();
				} else {
					loading = false;
					progressBar.setVisibility(View.INVISIBLE);
					if (refreshItem != null) {
						refreshItem.setEnabled(true);
			    		switchViewItem.setEnabled(true);
					}
					if (plurkButton != null) {
						plurkButton.setEnabled(true);
					}
				}
				break;
			case MSG_LOADING_FAIL:
				loading = false;
				if (refreshItem != null) {
					refreshItem.setEnabled(true);
		    		switchViewItem.setEnabled(true);
				}
				if (plurkButton != null) {
					plurkButton.setEnabled(true);
				}
				progressBar.setVisibility(View.INVISIBLE);
				listFooter.setText(R.string.plurk_list_more_title);
				break;
			case MSG_AVATAR_SETTING:
				AvatarObj obj = (AvatarObj) msg.obj;
				plurkListAdapter.setAvatar(obj.avatar, obj.index);
				break;
			case MSG_AVATAR_LOADED:
				loading = false;
				progressBar.setVisibility(View.INVISIBLE);
				if (refreshItem != null) {
					refreshItem.setEnabled(true);
		    		switchViewItem.setEnabled(true);
				}
				if (plurkButton != null) {
					plurkButton.setEnabled(true);
				}
				break;
			case MSG_PLURK_DONE:
	    		clearTimeStamp();
				plurkingDialog.cancel();
				listFooter.setText(R.string.plurk_list_loading_title);
				plurkContent.getText().clear();
	    		loadPlurks();
				break;
			case MSG_SWITCH_VIEW:
				headerText.setText("Pluroium - " + getResources().getStringArray(R.array.plurk_types)[currentPlurksView]);
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
			} else if (v == plurkButton) {
				if (ime != null) {
					ime.hideSoftInputFromWindow(plurkContent.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				}
				
				showDialog(DIALOG_PLURKING);
				new Thread(){
					public void run() {
						plurkHelper.addPlurk("says", plurkContent.getText().toString(), true);
						msgHandler.sendEmptyMessage(MSG_PLURK_DONE);
					}
				}.start();
			} 
		}	
	}

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Intent intent = new Intent(PlurkActivity.this, SinglePlurkActivity.class);
		PlurkListItem item = (PlurkListItem) plurkListAdapter.getItem(position);
		
		Bundle data = new Bundle();
		data.putString("plurk_id", String.valueOf(item.getPlurkId()));
		data.putString("avatar_index", item.getAvatarIndex());
		data.putParcelable("avatar", item.getAvatar());
		data.putString("nickname", item.getNickname());
		data.putString("qualifier", item.getQualifier());
		data.putString("qualifier_translated", item.getQualifierTranslated());
		data.putCharSequence("content", item.getRawContent());
		data.putString("posted", item.getPosted());
		intent.putExtras(data);
		startActivity(intent);
	}
}