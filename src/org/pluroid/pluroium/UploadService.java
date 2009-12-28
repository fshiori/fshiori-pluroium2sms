package org.pluroid.pluroium;

import org.ericsk.pluroid.IUploadService;
import org.ericsk.pluroid.IUploadServiceCallback;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

public class UploadService extends Service {
	
	private static final String TAG = "UploadService";

	private final RemoteCallbackList<IUploadServiceCallback> callbacks  
		= new RemoteCallbackList<IUploadServiceCallback>();
	
	private NotificationManager notificationManager;
	
	private Uri photoUri;
	private String photoText;
	
	private static final int MSG_START_UPLOAD = 1;
	private static final int MSG_DONE_UPLOAD = 2;
	
	private PlurkHelper plurkHelper;
	
	@Override
	public void onCreate() {
		super.onCreate();
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		plurkHelper = new PlurkHelper(this);
		
		Log.v(TAG, "onCreate");
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.v(TAG, "onStart");
		
		Bundle extras = intent.getExtras();
		photoUri = (Uri) extras.getParcelable("photo_uri");
		photoText = extras.getString("photo_text");

		msgHandler.sendEmptyMessage(MSG_START_UPLOAD);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.v(TAG, "onDestroy");

		callbacks.kill();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.v(TAG, "onBind");

		if (IUploadService.class.getName().equals(intent.getAction())) {
			return binder;
		}
		
		return null;
	}

	private final IUploadService.Stub binder = new IUploadService.Stub() {
		
		public void unregisterCallback(IUploadServiceCallback callback)
				throws RemoteException {
			if (callback != null) {
				callbacks.unregister(callback);
			}
		}
		
		public void registerCallback(IUploadServiceCallback callback)
				throws RemoteException {
			if (callback != null) {
				callbacks.register(callback);
			}
		}
	};
	
	private final Handler msgHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Notification notif;
			PendingIntent contentIntent = PendingIntent.getActivity(UploadService.this, 0, new Intent(UploadService.this, PlurkActivity.class), 0);				

			switch (msg.what) {
			case MSG_START_UPLOAD:
				
				notif = new Notification(android.R.drawable.stat_sys_upload,
						"Uploading...", System.currentTimeMillis());
				
				notif.flags = Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_AUTO_CANCEL;
				notif.ledOffMS = 1000;
				notif.ledOnMS = 500;
				notif.ledARGB = Color.YELLOW;

				String text = getResources().getString(R.string.upload_photo_notify_start);
				notif.tickerText = text;
				notif.setLatestEventInfo(UploadService.this, "Uploading photo...", text, contentIntent);
				notificationManager.notify(0, notif);
				
				new Thread(uploadPhotoThread).start();
				break;
			case MSG_DONE_UPLOAD:
				notif = new Notification(android.R.drawable.stat_sys_upload_done,
						"Upload complete.", System.currentTimeMillis());
				
				notif.flags = Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_AUTO_CANCEL;
				notif.ledOffMS = 1000;
				notif.ledOnMS = 500;
				notif.ledARGB = Color.BLUE;
				
				String doneText = getResources().getString(R.string.upload_photo_notify_done);
				notif.tickerText = doneText;
				notif.setLatestEventInfo(UploadService.this, "Uploading photo done.", doneText, contentIntent);

				notificationManager.notify(0, notif);
				
				break;
			default:
				super.handleMessage(msg);
			}
		}
	};
	
	private Runnable uploadPhotoThread = new Runnable() {
		public void run() {
			plurkHelper.uploadPhoto(photoUri, photoText);
			msgHandler.sendEmptyMessage(MSG_DONE_UPLOAD);
		}
	};
}
