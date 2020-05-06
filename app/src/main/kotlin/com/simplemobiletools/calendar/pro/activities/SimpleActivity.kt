package com.simplemobiletools.calendar.pro.activities

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.provider.CalendarContract
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.refreshCalDAVCalendars
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.helpers.ensureBackgroundThread

open class SimpleActivity : BaseSimpleActivity() {
    val CALDAV_REFRESH_DELAY = 3000L
    val calDAVRefreshHandler = Handler()
    var calDAVRefreshCallback: (() -> Unit)? = null

    override fun getAppIconIDs() = arrayListOf(
        R.mipmap.ic_launcher
    )

    override fun getAppLauncherName() = getString(R.string.app_launcher_name)

    fun Context.syncCalDAVCalendars(callback: () -> Unit) {
        calDAVRefreshCallback = callback
        ensureBackgroundThread {
            val uri = CalendarContract.Calendars.CONTENT_URI
            contentResolver.unregisterContentObserver(calDAVSyncObserver)
            contentResolver.registerContentObserver(uri, false, calDAVSyncObserver)
            refreshCalDAVCalendars(config.caldavSyncedCalendarIds, true)
        }
    }

    // caldav refresh content observer triggers multiple times in a row at updating, so call the callback only a few seconds after the (hopefully) last one
    private val calDAVSyncObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            if (!selfChange) {
                calDAVRefreshHandler.removeCallbacksAndMessages(null)
                calDAVRefreshHandler.postDelayed({
                    ensureBackgroundThread {
                        unregisterObserver()
                        calDAVRefreshCallback?.invoke()
                        calDAVRefreshCallback = null
                    }
                }, CALDAV_REFRESH_DELAY)
            }
        }
    }

    private fun unregisterObserver() {
        contentResolver.unregisterContentObserver(calDAVSyncObserver)
    }
}
