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
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
}
