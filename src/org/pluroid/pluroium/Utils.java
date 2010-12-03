package org.pluroid.pluroium;

import java.io.File;
import java.io.IOException;

import android.content.Context;

public class Utils {

	private static File getCacheDirectory(Context context) {
        File directory = context.getCacheDir();
        File subdir = new File(directory, "pluroid");
        if (!subdir.exists()) {
            subdir.mkdirs();
        }

        return new File(subdir, "images");
    }
	
	public static File ensureCache(Context context) throws IOException {
        File cacheDirectory = getCacheDirectory(context);
        if (!cacheDirectory.exists()) {
            cacheDirectory.mkdirs();
            new File(cacheDirectory, ".nomedia").createNewFile();
        }   
        return cacheDirectory;
    }
	
}
