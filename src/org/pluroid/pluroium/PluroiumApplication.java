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
