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

import java.util.List;

import org.pluroid.pluroium.data.PlurkListItem;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

public class SinglePlurkActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "SinglePlurkActivity";

    private String plurkId;
    private String userId;
    private String qualifier;
    private String avatarIndex;
    private TextView responseTitle;
    private ListView responsesView;
    private PlurkResponseAdapter responseAdapter;
    private ProgressBar progressBar;
    private ProgressDialog progressDlg;
    private Spinner responseQualifier;
    private EditText responseText;
    private Button postButton;
    private Button cancelButton;
    private View bottomPanel;
    
    private Context context;
    private List<PlurkListItem> responses;
    
    private static final int MSG_LOAD_RESPONSES = 1;
    private static final int MSG_LOAD_RESPONSES_DONE = 2;
    
    private static final int RESPONDING_DIALOG = 1;

    private InputMethodManager ime;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        ime = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        initView();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }
    
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case RESPONDING_DIALOG:
            if (progressDlg == null) {
                progressDlg = new ProgressDialog(this);
                progressDlg.setMessage(getResources().getString(R.string.respoding));
                progressDlg.setIndeterminate(true);
                progressDlg.setCancelable(true);
            }
            return progressDlg;
        default:
            return super.onCreateDialog(id);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        }
        return true;
    }    
    
    private void initView() {    
        setContentView(R.layout.single_plurk);
        
        responsesView = (ListView) findViewById(R.id.plurk_responses);
        View hdView = LayoutInflater.from(this).inflate(R.layout.single_plurk_header, null);
        responsesView.addHeaderView(hdView);
        responsesView.setItemsCanFocus(true);
        responseAdapter = new PlurkResponseAdapter(this);
        responsesView.setAdapter(responseAdapter);
        
        Bundle plurkData = getIntent().getExtras();
        avatarIndex = plurkData.getString("avatar_index");
        plurkId = plurkData.getString("plurk_id");
        qualifier = plurkData.getString("qualifier");
        userId = plurkData.getString("userId");
        
        ImageView avatarView = (ImageView) hdView.findViewById(R.id.plurk_item_avatar);
        new FetchAvatarTask().execute(avatarView, userId, avatarIndex);
        
        TextView contentView = (TextView) hdView.findViewById(R.id.plurk_item_content);
        TextView name = (TextView) hdView.findViewById(R.id.plurk_item_displayname);
        name.setText(plurkData.getString("nickname"));
        TextView qualifierView = (TextView) hdView.findViewById(R.id.plurk_item_qualifier);
        qualifierView.setText(plurkData.getString("qualifier_translated"));

        Resources res = context.getResources();
        int colorId = res.getIdentifier("qualifier_" + qualifier, "color", "org.ericsk.pluroid");
        
        if (colorId > 0) {
            qualifierView.setTextColor(Color.WHITE);
            qualifierView.setBackgroundColor(res.getColor(colorId));
        } else {
            qualifierView.setTextColor(Color.BLACK);
            qualifierView.setBackgroundColor(Color.TRANSPARENT);
        }
        
        contentView.setText(Html.fromHtml(plurkData.getString("content"), new ImageGetter(this), null));
        contentView.setMovementMethod(LinkMovementMethod.getInstance());
        TextView postView = (TextView) hdView.findViewById(R.id.plurk_item_posted);
        postView.setText(plurkData.getString("posted"));
        responseTitle = (TextView) hdView.findViewById(R.id.single_plurk_responses_title);
        
        progressBar = (ProgressBar) findViewById(R.id.title_refresh_progress);
        
        responseQualifier = (Spinner) findViewById(R.id.single_plurk_response_qualifier);
        responseText = (EditText) findViewById(R.id.single_plurk_response_text);
        responseText.setOnClickListener(this);
        responseText.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && bottomPanel.getVisibility() == View.GONE) {
                    bottomPanel.setVisibility(View.VISIBLE);
                }
            }            
        });
        postButton = (Button) findViewById(R.id.single_plurk_post_button);
        postButton.setOnClickListener(this);
        cancelButton = (Button) findViewById(R.id.single_plurk_cancel_button);
        cancelButton.setOnClickListener(this);
        
        bottomPanel = findViewById(R.id.single_plurk_buttons);
        
        
        msgHandler.sendEmptyMessage(MSG_LOAD_RESPONSES);
    }
    
    private Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_LOAD_RESPONSES:
                responseAdapter.clear();
                progressBar.setVisibility(View.VISIBLE);
                new Thread() {
                    @Override
                    public void run() {
                        PlurkHelper plurkHelper = new PlurkHelper(SinglePlurkActivity.this);
                        responses = plurkHelper.getResponses(plurkId);
                        msgHandler.sendEmptyMessage(MSG_LOAD_RESPONSES_DONE);
                    }
                }.start();
                break;
            case MSG_LOAD_RESPONSES_DONE:
                progressBar.setVisibility(View.INVISIBLE);
                if (responses != null) {
                    responseTitle.setText(responses.size() + " " + getResources().getString(R.string.single_responses_title));
                    responseAdapter.addResponses(responses);
                }
                break;
            default:
                super.handleMessage(msg);
            }
        }
    };

    public void onClick(View v) {
        if (v == responseText) {
        } else if (v == cancelButton) {
            completeInput();
        } else if (v == postButton) {
            showDialog(RESPONDING_DIALOG);
            new RespondingTask().execute("");
        }
    }
    
    private void completeInput() {
        responsesView.requestFocus();
        bottomPanel.setVisibility(View.GONE);
        responseText.getText().clear();
        
        if (ime != null) {
            ime.hideSoftInputFromWindow(bottomPanel.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
    
    private class RespondingTask extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(String... arg0) {
            String resp = responseText.getText().toString();
            PlurkHelper plurkHelper = new PlurkHelper(SinglePlurkActivity.this);
            plurkHelper.addResponse(plurkId, 
                    PluroiumApplication.qualifiers[responseQualifier.getSelectedItemPosition()], 
                    resp); 
            return true;
        }
        
        @Override
        protected void onPostExecute(Boolean isOk) {
            completeInput();
            progressDlg.cancel();
            msgHandler.sendEmptyMessage(MSG_LOAD_RESPONSES);
        }
    }
    
    private class FetchAvatarTask extends AsyncTask<Object, Integer, Bitmap> {

        ImageView imageView;
    
        @Override
        protected Bitmap doInBackground(Object... params) {
            imageView = (ImageView) params[0];
    
            PlurkHelper p = new PlurkHelper(context);
            Bitmap avatar = p.getAvatar((String) params[1], "medium", (String) params[2]);
    
            return avatar;
        }   
    
        @Override
        protected void onPostExecute(Bitmap avatar) {
            if (avatar != null) {
                imageView.setImageBitmap(avatar);
            }   
        }   
    }
    
    public void onRefreshClick(View view) {
        msgHandler.sendEmptyMessage(MSG_LOAD_RESPONSES);
    }
}
