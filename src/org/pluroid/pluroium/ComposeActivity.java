package org.pluroid.pluroium;

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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;

public class ComposeActivity extends Activity implements View.OnClickListener {

	private static final String TAG = "ComposeActivity";
	
	private static final int DIALOG_EMOTION_LIST = 1;
	private static final int DIALOG_PLURKING = 2;
	
	private static final int MSG_PLURK_DONE = 1;
	
	private PlurkHelper plurkHelper;
	
	private AlertDialog emotionDialog;
	private ProgressDialog plurkingDialog;
	private ImageButton insertEmotionButton;
	private EditText plurkContent;
	private Spinner plurkQualifier;
	
	private Button plurkButton;
	private Button cancelButton;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		plurkHelper = new PlurkHelper(this);
		
		initView();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_PLURKING:
    		if (plurkingDialog == null) {
    			plurkingDialog = new ProgressDialog(this);
    			plurkingDialog.setMessage(getResources().getString(R.string.plurking));
    			plurkingDialog.setIndeterminate(true);
    			plurkingDialog.setCancelable(true);
    		}
    		return plurkingDialog;
		case DIALOG_EMOTION_LIST:
			if (emotionDialog == null) {
				String[] icons = getResources().getStringArray(R.array.emotion_icons);
				final String[] texts = getResources().getStringArray(R.array.emotion_text);
				
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
				builder.setTitle(getResources().getString(R.string.compose_emotion_dialog_title));
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
		}
		
		return null;
	}
	
	private void initView() {
		setContentView(R.layout.compose);
		
		insertEmotionButton = (ImageButton) findViewById(R.id.compose_emotion);
		insertEmotionButton.setOnClickListener(this);
		
		plurkContent = (EditText) findViewById(R.id.compose_content);
		plurkQualifier = (Spinner) findViewById(R.id.compose_qualifier);
		
		plurkButton = (Button) findViewById(R.id.compose_plurk_button);
		plurkButton.setOnClickListener(this);
		
		cancelButton = (Button) findViewById(R.id.compose_cancel_button);
		cancelButton.setOnClickListener(this);
	}

	public void onClick(View v) {
		if (v == insertEmotionButton) {
			showDialog(DIALOG_EMOTION_LIST);
		} else if (v == plurkButton) {
			showDialog(DIALOG_PLURKING);
			new Thread(){
				public void run() {
					plurkHelper.addPlurk(PluroiumApplication.qualifiers[plurkQualifier.getSelectedItemPosition()],
							plurkContent.getText().toString(), true);
					msgHandler.sendEmptyMessage(MSG_PLURK_DONE);
				}
			}.start();
		} else if (v == cancelButton) {
			finish();
		}
	}
	
	private Handler msgHandler = new Handler() {
		
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_PLURK_DONE:
				Log.v(TAG, "Plurking Done.");
				plurkingDialog.cancel();
				finish();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	};
}
