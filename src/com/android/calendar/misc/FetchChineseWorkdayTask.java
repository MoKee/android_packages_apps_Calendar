/*
 * Copyright (C) 2015-2016 The MoKee Open Source Project
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

package com.android.calendar.misc;

import java.net.URI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;

import com.android.calendar.requests.ChineseCalendarRequest;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;

public class FetchChineseWorkdayTask extends AsyncTask<Void, Void, Void> implements
        Response.Listener<String>, Response.ErrorListener {

    private SharedPreferences mPrefs;
    private RequestQueue mQueue;
    private String mYear;

    public FetchChineseWorkdayTask(SharedPreferences prefs, RequestQueue queue, int year) {
        mPrefs = prefs;
        mQueue = queue;
        mYear = String.valueOf(year);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        fetchChineseHoliday(mPrefs, mQueue);
        return null;
    }

    private void fetchChineseHoliday(SharedPreferences mPrefs, RequestQueue mQueue) {
        String url = URI.create("http://cloud.mokeedev.com/calendar/cnWorkdays")
                .toASCIIString();
        ChineseCalendarRequest workdayRequest = new ChineseCalendarRequest(Request.Method.POST, url,
                this, this);
        workdayRequest.setRetryPolicy(new DefaultRetryPolicy(ChineseCalendarRequest.CALENDAR_REQUEST_TIMEOUT, ChineseCalendarRequest.CALENDAR_REQUEST_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        mQueue.add(workdayRequest);
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        VolleyLog.e("Error: ", error.getMessage());
    }

    @Override
    public void onResponse(String response) {
        try {
            JSONObject holidayYear = new JSONObject(response).getJSONObject(mYear);
            JSONArray month = holidayYear.getJSONArray("month");
            JSONArray days = holidayYear.getJSONArray("day");
            Editor editor = mPrefs.edit();
            for (int i = 0; i < month.length(); i++) {
                for (String day : days.getString(i).split(",")) {
                    editor.putBoolean(mYear + "-" + month.getString(i) + "-" + day, true);
                }
            }
            editor.putBoolean("has" + mYear, true).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
