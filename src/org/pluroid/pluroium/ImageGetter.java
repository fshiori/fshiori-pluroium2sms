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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.Log;

public class ImageGetter implements Html.ImageGetter {

    private static final String TAG = "ImageGetter";
    private static final int MAX_IMAGE_WIDTH = 80;
    
    private Context context;
    private String[] emotionArray;
    
    public ImageGetter(Context context) {
        this.context = context;
        
        emotionArray = context.getResources().getStringArray(R.array.emotion_icons);
        Arrays.sort(emotionArray, 0, emotionArray.length);
    }
    
    public Drawable getDrawable(String url) {
        try {
            String filename = url.substring(url.lastIndexOf("/") + 1);
            filename = filename.substring(0, filename.lastIndexOf("."));
            Drawable d;
            
            if (Arrays.binarySearch(emotionArray, filename) >= 0) {
                Log.d(TAG, "Emotion hit!");
                d = Drawable.createFromStream(context.getAssets().open(filename + ".gif"), filename);
            } else {
                File cacheDir = Utils.ensureCache(context, "image_cache");
                File f = new File(cacheDir, filename + ".png");
                if (!f.exists()) {
                    downloadImage(url, f);
                }
                d = Drawable.createFromPath(f.getAbsolutePath());
            }
            setBound(d);
            return d;
        } catch (Exception e) {
            Log.d(TAG, "Download: " + url + " error!", e);
        }
        
        return null;
    }

    protected void downloadImage(String url, File f) throws IOException {
        URL imageUrl = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
        conn.setDoInput(true);
        conn.connect();
        InputStream is = conn.getInputStream();
        Bitmap bmp = BitmapFactory.decodeStream(is);
        FileOutputStream fos = new FileOutputStream(f);
        bmp.compress(CompressFormat.PNG, 85, fos);
    }
    
    protected void setBound(Drawable d) {
        int width = d.getIntrinsicWidth();
        int height = d.getIntrinsicHeight();
        if ((width > MAX_IMAGE_WIDTH) || (height > MAX_IMAGE_WIDTH)) {
            float ratio = (float) MAX_IMAGE_WIDTH / (float) Math.max(width,height);
            width *= ratio;
            height *= ratio;
        }
        d.setBounds(0, 0, width, height);
    }
}
