/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.pref.FileTimeoutPref;

/**
 * The FileTimeoutReceiver class manages a timeout for file activity
 */
class FileTimeoutReceiver extends BroadcastReceiver
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private final Activity itsActivity;
    private final AlarmManager itsAlarmMgr;
    private final PendingIntent itsCloseIntent;
    private int itsFileCloseTimeout = 0;

    private static final String TAG = "FileTimeoutReceiver";

    /**
     * Constructor
     */
    public FileTimeoutReceiver(Activity act)
    {
        itsActivity = act;
        itsAlarmMgr =
                (AlarmManager)itsActivity.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(PasswdSafeApp.FILE_TIMEOUT_INTENT);
        intent.setPackage("com.jefftharris.passwdsafe");
        itsCloseIntent = PendingIntent.getBroadcast(itsActivity,
                                                    0, intent, 0);
        itsActivity.registerReceiver(
                this, new IntentFilter(PasswdSafeApp.FILE_TIMEOUT_INTENT));

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(itsActivity);
        prefs.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(prefs, Preferences.PREF_FILE_CLOSE_TIMEOUT);
    }

    /**
     * Handle when the activity is destroyed
     */
    public void onDestroy()
    {
        cancel();
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(itsActivity);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        itsActivity.unregisterReceiver(this);
    }

    /**
     * Touch the file timeout timer
     */
    public void touch()
    {
        if (itsFileCloseTimeout != 0) {
            itsAlarmMgr.set(AlarmManager.ELAPSED_REALTIME,
                            SystemClock.elapsedRealtime() + itsFileCloseTimeout,
                            itsCloseIntent);
        }
    }

    /**
     * Cancel the file timeout timer
     */
    public void cancel()
    {
        itsAlarmMgr.cancel(itsCloseIntent);
    }

    @Override
    public void onReceive(Context ctx, Intent intent)
    {
        Log.i(TAG, "File timeout");
        itsActivity.finish();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        switch (key) {
        case Preferences.PREF_FILE_CLOSE_TIMEOUT: {
            FileTimeoutPref pref = Preferences.getFileCloseTimeoutPref(prefs);
            PasswdSafeUtil.dbginfo(TAG, "new file close timeout: %s", pref);
            itsFileCloseTimeout = pref.getTimeout();
            if (itsFileCloseTimeout == 0) {
                cancel();
            } else {
                touch();
            }
            break;
        }
        }
    }
}
