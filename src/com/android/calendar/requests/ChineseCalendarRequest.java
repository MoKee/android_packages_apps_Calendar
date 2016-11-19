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

package com.android.calendar.requests;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.mokee.os.Build;

public class ChineseCalendarRequest extends StringRequest {

	// DefaultRetryPolicy values for Volley
	public static final int CALENDAR_REQUEST_TIMEOUT = 5000;
	public static final int CALENDAR_REQUEST_MAX_RETRIES = 3;

	public ChineseCalendarRequest(int method, String url,
			Response.Listener<String> listener,
			Response.ErrorListener errorListener) {
		super(method, url, listener, errorListener);
	}

	@Override
	public Map<String, String> getHeaders() throws AuthFailureError {
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("Cache-Control", "no-cache");
		return headers;
	}

	@Override
	protected Map<String, String> getParams() throws AuthFailureError {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("version", Build.VERSION);
		return params;
	}

	@Override
	protected Response<String> parseNetworkResponse(NetworkResponse response) {
		try {
			String result = new String(response.data, "UTF-8");
			return Response.success(result, HttpHeaderParser.parseCacheHeaders(response));
		} catch (UnsupportedEncodingException e) {
			return Response.error(new ParseError(e));
		}
	}

}
