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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
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
        
    /** Dialog IDs */
    private static final int DIALOG_LOGIN_FAIL = 1;
    
    private ProgressBar progressBar;
    private EditText nameField;
    private EditText passwordField;
    private CheckBox rememberBox;
    private Button loginButton;
    
    private PlurkHelper plurkHelper;
    private Bundle extras;
    
    private SharedPreferences sharedPref;
    private InputMethodManager ime;
    private LoginTask loginTask;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVisible(false);
        
        extras = getIntent().getExtras();

        plurkHelper = new PlurkHelper(this);
        ime = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    }
    
    @Override
    public void onStart() {
        super.onStart();
        
        if (!plurkHelper.isLoginned()) {
            initView();
            setVisible(true);
        } else {
            launchMain();
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        if (loginTask != null) {
            loginTask.cancel(true);
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
        
        progressBar = (ProgressBar) findViewById(R.id.title_refresh_progress);
        
        loginButton = (Button) findViewById(R.id.login_button);
        loginButton.setOnClickListener(this);
        
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
        
        // TODO: check username/password
        String username = nameField.getText().toString();
        String password = passwordField.getText().toString();        
        plurkHelper.setAuth(username, password);
        
        // login plurk
        loginTask = new LoginTask();
        loginTask.execute("");
    }
    
    private void toggleForm(boolean enabled) {
        progressBar.setVisibility(enabled ? View.INVISIBLE : View.VISIBLE);
        nameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        rememberBox.setEnabled(enabled);
        loginButton.setEnabled(enabled);
        loginButton.setText(enabled ? R.string.login_button_text : R.string.loging_button_text);
    }
    
    private class LoginTask extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(String... arg0) {
            return plurkHelper.login(rememberBox.isChecked());
        }
        
        @Override
        protected void onPostExecute(Boolean isOk) {
            if (isOk) {
                launchMain();
            } else {
                toggleForm(true);
                showDialog(DIALOG_LOGIN_FAIL);                
            }
        }
    }
}
