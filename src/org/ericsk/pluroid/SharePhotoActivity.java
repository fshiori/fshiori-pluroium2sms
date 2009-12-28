package org.ericsk.pluroid;

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
			extras.putString("back_activity", "org.ericsk.pluroid.SharePhotoActivity");
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
			Intent uploadIntent = new Intent("org.ericsk.pluroid.UPLOAD_SERVICE");
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
