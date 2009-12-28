package org.pluroid.pluroium;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;

public class LaunchActivity extends Activity implements View.OnClickListener {
	
	private static final String TAG = "LaunchActivity";
	
	/** MSG IDs */
	private static final int MSG_LOGIN_SUCCESS = 1;
	private static final int MSG_LOGIN_FAILED = 2;
	
	/** Dialog IDs */
	private static final int DIALOG_LOGIN_FAIL = 1;
	
	private ProgressBar progressBar;
	private EditText nameField;
	private EditText passwordField;
	private CheckBox rememberBox;
	private Button loginButton;
	private Button signupButton;
	
	private PlurkHelper plurkHelper;
	private Bundle extras;
	
	private SharedPreferences sharedPref;
	private InputMethodManager ime;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setVisible(false);
		
		extras = getIntent().getExtras();

		plurkHelper = new PlurkHelper(this);
		ime = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		
		
	}
	
	@Override
	public void onStart() {
		super.onStart();
		// Check auth
		sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		String cookieStr = sharedPref.getString(Constant.PREF_COOKIE, "");
		
		if (cookieStr.length() == 0) {
			initView();
			setVisible(true);
		} else {
			launchMain();
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
	
	/**
	 * Button click listener
	 */
	public void onClick(View view) {
		
		if (view == loginButton) {
			performLogin();
		} else if (view == signupButton) {
			
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		
		switch (id) {
		case DIALOG_LOGIN_FAIL:			
			return new AlertDialog.Builder(this)
						.setPositiveButton(R.string.ok, null)
						.setTitle(R.string.login_fail_dialog_title)
						.setMessage(R.string.login_fail_message)
						.create();			
		}
		
		return super.onCreateDialog(id);
	}
	
	/**
	 * Initialize the sign in/up form.
	 */
	private void initView() {
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.login);
		
		progressBar = (ProgressBar) findViewById(R.id.progressing);
		
		loginButton = (Button) findViewById(R.id.login_button);
		loginButton.setOnClickListener(this);
		
		signupButton = (Button) findViewById(R.id.signup_button);
		signupButton.setOnClickListener(this);

		String username = sharedPref.getString(Constant.PREF_AUTH_USERNAME_KEY, "");
		String password = sharedPref.getString(Constant.PREF_AUTH_PASSWORD_KEY, "");

		rememberBox = (CheckBox) findViewById(R.id.login_remember);
		nameField = (EditText) findViewById(R.id.login_name);
		if (username.length() > 0) {
			nameField.setText(username);
			rememberBox.setChecked(true);
		}
		passwordField = (EditText) findViewById(R.id.login_password);
		if (password.length() > 0) {
			passwordField.setText(password);
			rememberBox.setChecked(true);
		}
		
	}
	
	/**
	 * Have logined, redirect to main activity.
	 */
	private void launchMain() {
		
		Class<?> targetClass = PlurkActivity.class;
		Intent intent = new Intent();
				
		if (extras != null) {
			String backActivity = extras.getString("back_activity");
			try {
				targetClass = Class.forName(backActivity);
			} catch (ClassNotFoundException e) {
				Log.e(TAG, "Unknown Activity");
			}
			intent.putExtras(extras);
		} else {	
			if (passwordField != null) {
				ime.hideSoftInputFromWindow(passwordField.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			}
		}
		intent.setClass(this, targetClass);
		startActivity(intent);		
		finish();
	}
	
	/**
	 * Perform login action.
	 */
	private void performLogin() {
		toggleForm(false);
		
		String username = nameField.getText().toString();
		String password = passwordField.getText().toString();		
		plurkHelper.setAuth(username, password);
		
		// login plurk
		new Thread() {
			public void run() {
				Message msg;
				if (plurkHelper.login(rememberBox.isChecked())) {
					msg = Message.obtain(msgHandler, MSG_LOGIN_SUCCESS);
				} else {
					msg = Message.obtain(msgHandler, MSG_LOGIN_FAILED);
				}
				msgHandler.sendMessage(msg);
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
			case MSG_LOGIN_SUCCESS:
				launchMain();
				break;
			case MSG_LOGIN_FAILED:
				toggleForm(true);
				showDialog(DIALOG_LOGIN_FAIL);
				break;
			}
			super.handleMessage(msg);
		}
	};
	
	private void toggleForm(boolean enabled) {
		progressBar.setVisibility(enabled ? View.INVISIBLE : View.VISIBLE);
		nameField.setEnabled(enabled);
		passwordField.setEnabled(enabled);
		rememberBox.setEnabled(enabled);
		loginButton.setEnabled(enabled);
		loginButton.setText(enabled ? R.string.login_button_text : R.string.loging_button_text);
		signupButton.setEnabled(enabled);
	}
}
