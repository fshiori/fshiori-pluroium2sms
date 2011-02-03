package org.pluroid.pluroium;

import android.app.Application;

public class PluroiumApplication extends Application {

	public static String[] qualifiers = {
        "says",
		":",
        "loves",
        "likes",
        "shares",
        "gives",
        "hates",
        "wants",
        "wishes",
        "needs",
        "will",
        "hopes",
        "asks",
        "has",
        "was",
        "wonders",
        "feels",
        "thinks",
        "is"
    };
	
	public static final String PREF_RESIZE_PHOTO = "pref_upload_resize";
	public static final String PREF_USE_EXTERNAL = "pref_use_external";
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
}
