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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class SharePhotoActivity extends Activity 
    implements View.OnClickListener {

    private Uri photoUri;
    private Button uploadButton;
    private Button cancelButton;
    private EditText photoText;
    
    private PlurkHelper plurkHelper;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        photoUri = getIntent().getExtras().getParcelable(Intent.EXTRA_STREAM);

        plurkHelper = new PlurkHelper(this);
        if (!plurkHelper.isLoginned()) {
            // Login first
            Intent loginIntent = new Intent(this, LaunchActivity.class);
            Bundle extras = new Bundle();
            extras.putString("back_activity", "org.pluroid.pluroium.SharePhotoActivity");
            extras.putParcelable(Intent.EXTRA_STREAM, photoUri);
            loginIntent.putExtras(extras);
            startActivity(loginIntent);
            finish();
        } else {
            initView();
        }
    }
    
    private void initView() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.share_photo);

        
        ImageView photoView = (ImageView) findViewById(R.id.shared_photo);
        photoView.setImageURI(photoUri);
        
        
        uploadButton = (Button) findViewById(R.id.shared_photo_upload_button);
        uploadButton.setOnClickListener(this);
        
        cancelButton = (Button) findViewById(R.id.shared_photo_cancel_button);
        cancelButton.setOnClickListener(this);
        
        photoText = (EditText) findViewById(R.id.shared_photo_text);
    }

    public void onClick(View v) {
        if (v == uploadButton) {
            Intent uploadIntent = new Intent("org.pluroid.pluroium.UPLOAD_SERVICE");
            Bundle data = new Bundle();
            data.putParcelable("photo_uri", photoUri);
            data.putString("photo_text", photoText.getText().toString());
            uploadIntent.putExtras(data);
            
            startService(uploadIntent);
            
            Toast.makeText(this, R.string.upload_start_msg, Toast.LENGTH_LONG).show();
            
            finish();
        } else if (v == cancelButton) {
            finish();
        }
    }
}
