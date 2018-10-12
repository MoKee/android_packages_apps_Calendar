/*
 * Copyright (C) 2015-2018 The MoKee Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.simplemobiletools.calendar.net;

import com.mokee.os.Build;
import com.simplemobiletools.calendar.helpers.ConstantsKt;

import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class OkHttpRestClient {

    private static OkHttpClient client = new OkHttpClient();

    public static void post(String url, Callback callback) {
        RequestBody params = new FormBody.Builder()
                .add("version", Build.VERSION).build();
        post(url, params, callback);
    }

    public static void post(String url, RequestBody params, Callback callback) {
        Request request = new Request.Builder().url(getAbsoluteUrl(url)).post(params).build();
        client.newCall(request).enqueue(callback);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return ConstantsKt.BASE_URL + relativeUrl;
    }

}
