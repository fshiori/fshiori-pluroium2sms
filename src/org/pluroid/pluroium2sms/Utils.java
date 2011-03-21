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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.preference.PreferenceManager;

public class Utils {

    private static final String TAG = "Utils";
    
    private static final float MEDIUM_SIZE = 640.0f;    
    private static final float SMALL_SIZE = 240.0f;
    
    private static File getCacheDirectory(Context context, String dirname) {
        
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean useExternal = sharedPrefs.getBoolean(PluroiumApplication.PREF_USE_EXTERNAL, false);
        File directory;

        if (useExternal && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            directory = Environment.getExternalStorageDirectory();
        } else {
            directory = context.getCacheDir();
        }

        File subdir = new File(directory, "pluroid");
        if (!subdir.exists()) {
            subdir.mkdirs();
        }

        return new File(subdir, dirname);
    }
    
    public static File ensureCache(Context context, String dirname) throws IOException {
        File cacheDirectory = getCacheDirectory(context, dirname);
        if (!cacheDirectory.exists()) {
            cacheDirectory.mkdirs();
            new File(cacheDirectory, ".nomedia").createNewFile();
        }   
        return cacheDirectory;
    }
    
    public static File resizePhoto(Context context, String filePath, String resize) throws IOException {
        BitmapFactory.Options option;
        Bitmap originalBitmap, resizedBitmap;
        
        File cacheDirectory = getCacheDirectory(context, "images");
        if (!cacheDirectory.exists()) {
            cacheDirectory.mkdirs();
            new File(cacheDirectory, ".nomedia").createNewFile();
        }
        
        File newFilePath = new File(cacheDirectory, System.currentTimeMillis() + ".jpg");
        
        option = new BitmapFactory.Options();
        option.inJustDecodeBounds = true;
        originalBitmap = BitmapFactory.decodeFile(filePath, option);
        int w = option.outWidth, h = option.outHeight;
        int rw, rh;
        float newSize = "no".equals(resize) ? 0.0f : "medium".equals(resize) ? MEDIUM_SIZE : SMALL_SIZE;
        
        if (newSize == 0.0f) {
            return new File(filePath);
        } else {
            if (w > h && w >= newSize) {
                rw = (int) newSize;
                rh = (int) (h * newSize / w);
            } else if (h > w && h >= newSize) {
                rh = (int) newSize;
                rw = (int) (w * newSize / h);
            } else {
                rh = h;
                rw = w;
            }
            
            int scale = 1;
            while (true) {
                if (w / 2 < newSize || h / 2 < newSize) {
                    break;
                }
                w /= 2;
                h /= 2;
                scale++;
            }
            option = new BitmapFactory.Options();
            option.inSampleSize = scale - 1;
            originalBitmap = BitmapFactory.decodeFile(filePath, option);
            resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, rw, rh, true);
            
            FileOutputStream fos = new FileOutputStream(newFilePath);
            resizedBitmap.compress(CompressFormat.JPEG, 100, fos);
            fos.close();
            
            return newFilePath;
        }
    }
}
