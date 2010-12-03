package org.pluroid.pluroium;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;

import org.pluroid.pluroium.data.PlurkListItem;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
	private String qualifier;
	private String avatarIndex;
	private TextView headerText;
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
	private MenuItem refreshItem;
	
	private Context context;
	private PlurkHelper plurkHelper;
	private List<PlurkListItem> responses;
	private boolean loading;
	
	private static final int MENU_REFRESH = 1;
	
	private static final int MSG_LOAD_RESPONSES = 1;
	private static final int MSG_LOAD_RESPONSES_DONE = 2;
	
	private static final int RESPONDING_DIALOG = 1;

	private static HashMap<String, Integer> qualifierColorMap;

	private InputMethodManager ime;
	
	static {
        qualifierColorMap = new HashMap<String, Integer>();
        qualifierColorMap.put("loves", Color.rgb(0xb2, 0x0c, 0x0c));
        qualifierColorMap.put("likes", Color.rgb(0xcb, 0x27, 0x28));
        qualifierColorMap.put("shares", Color.rgb(0xa7, 0x49, 0x49));
        qualifierColorMap.put("gives", Color.rgb(0x62, 0x0e, 0x0e));
        qualifierColorMap.put("hates", Color.rgb(0x00, 0x00, 0x00));
        qualifierColorMap.put("wants", Color.rgb(0x8d, 0xb2, 0x4e));
        qualifierColorMap.put("wishes", Color.rgb(0x5b, 0xb0, 0x17));
        qualifierColorMap.put("needs", Color.rgb(0x7a, 0x9a, 0x37));
        qualifierColorMap.put("will", Color.rgb(0xb4, 0x6d, 0xb9));
        qualifierColorMap.put("hopes", Color.rgb(0xe0, 0x5b, 0xe9));
        qualifierColorMap.put("asks", Color.rgb(0x83, 0x61, 0xbc));
        qualifierColorMap.put("has", Color.rgb(0x77, 0x77, 0x77));
        qualifierColorMap.put("was", Color.rgb(0x52, 0x52, 0x52));
        qualifierColorMap.put("wonders", Color.rgb(0x2e, 0x4e, 0x9e));
        qualifierColorMap.put("feels", Color.rgb(0x2d, 0x83, 0xbe));
        qualifierColorMap.put("thinks", Color.rgb(0x68, 0x9c, 0xc1));
        qualifierColorMap.put("says", Color.rgb(0xe2, 0x56, 0x0b));
        qualifierColorMap.put("is", Color.rgb(0xe5, 0x7c, 0x43));        
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		
		plurkHelper = new PlurkHelper(this);
		ime = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		initView();
		ime.hideSoftInputFromWindow(responseText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
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
		refreshItem = menu.add(0, MENU_REFRESH, 0, R.string.menu_refresh_title)
		.setIcon(R.drawable.ic_menu_refresh);
	
		if (loading) {
			refreshItem.setEnabled(false);
		}
		
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_REFRESH:
			msgHandler.sendEmptyMessage(MSG_LOAD_RESPONSES);
			break;
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
		
		ImageView avatarView = (ImageView) hdView.findViewById(R.id.single_plurk_author_avatar);
		new FetchAvatarTask().execute(avatarView, plurkData.getString("userId"), plurkData.getString("avatar_index"));
		TextView nicknameView = (TextView) hdView.findViewById(R.id.single_plurk_owner);
		nicknameView.setText(plurkData.getString("nickname"));
		TextView qualifierView = (TextView) hdView.findViewById(R.id.single_plurk_qualifier);
		qualifierView.setText(plurkData.getString("qualifier_translated"));
		
		String q = plurkData.getString("qualifier");
		if (!q.equals(":")) {
			qualifierView.setBackgroundColor(qualifierColorMap.get(q));
		} else {
			qualifierView.setTextColor(Color.BLACK);
		}
		
		TextView contentView = (TextView) hdView.findViewById(R.id.single_plurk_content);
		contentView.setText(Html.fromHtml(plurkData.getString("content"), new Html.ImageGetter() {
			public Drawable getDrawable(String source) {
				try {
					URLConnection conn = new URL(source).openConnection();
					conn.connect();
					
					InputStream is = conn.getInputStream();
					Bitmap bmp = BitmapFactory.decodeStream(is);
					Drawable d = new BitmapDrawable(bmp);
					d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
					is.close();
					return d;
				} catch (Exception e) {
					return null;
				}
			}
		},  null));
		contentView.setMovementMethod(LinkMovementMethod.getInstance());
		TextView postView = (TextView) hdView.findViewById(R.id.single_plurk_posted);
		postView.setText(plurkData.getString("posted"));
		responseTitle = (TextView) hdView.findViewById(R.id.single_plurk_responses_title);

		avatarIndex = plurkData.getString("avatar_index");
		plurkId = plurkData.getString("plurk_id");
		qualifier = plurkData.getString("qualifier");
		
		headerText = (TextView) findViewById(R.id.single_page_title);
		headerText.setOnClickListener(this);
		progressBar = (ProgressBar) findViewById(R.id.single_load_progressing);
		
		responseQualifier = (Spinner) findViewById(R.id.single_plurk_response_qualifier);
		responseText = (EditText) findViewById(R.id.single_plurk_response_text);
		responseText.setOnClickListener(this);
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
				loading = true;
				if (refreshItem != null) {
					refreshItem.setEnabled(false);
				}
				new Thread() {
					@Override
					public void run() {
						responses = plurkHelper.getResponses(plurkId);
						msgHandler.sendEmptyMessage(MSG_LOAD_RESPONSES_DONE);
					}
				}.start();
				break;
			case MSG_LOAD_RESPONSES_DONE:
				loading = false;
				if (refreshItem != null) {
					refreshItem.setEnabled(true);
				}
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
		if (v == headerText) {
			responsesView.setSelectionFromTop(0, 0);
		} else if (v == responseText) {
			if (bottomPanel.getVisibility() == View.GONE) {
				bottomPanel.setVisibility(View.VISIBLE);
			}
		} else if (v == cancelButton) {
			completeInput();
		} else if (v == postButton) {
			showDialog(RESPONDING_DIALOG);
			new RespondingTask().execute("");
		}
	}
	
	private void completeInput() {
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
}
