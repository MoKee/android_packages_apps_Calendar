/*
 * Copyright (C) 2015 The MoKee OpenSource Project
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

package com.android.calendar;

import java.util.Calendar;

import com.mokee.cloud.misc.FetchChineseHolidayTask;
import com.mokee.cloud.misc.FetchChineseWorkdayTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.mokee.utils.MoKeeUtils;

public class DataUpgradeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Fetch Cloud Chinese holiday and workday
        SharedPreferences mHolidayPref = context.getSharedPreferences("chinese_holiday", Context.MODE_WORLD_READABLE);
        SharedPreferences mWorkDayPref = context.getSharedPreferences("chinese_workday", Context.MODE_WORLD_READABLE);
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        if (MoKeeUtils.isSupportLanguage(true) && MoKeeUtils.isOnline(context)) {
            if (!mHolidayPref.getBoolean("has" + year, false)) {
                new FetchChineseHolidayTask(mHolidayPref, ((CalendarApplication) context.getApplicationContext()).getQueue(), year).execute();
            }
            if (!mWorkDayPref.getBoolean("has" + year, false)) {
                new FetchChineseWorkdayTask(mWorkDayPref, ((CalendarApplication) context.getApplicationContext()).getQueue(), year).execute();
            }
        }
    }

}
