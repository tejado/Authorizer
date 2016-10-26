/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.pref.FileTimeoutPref;

/**
 * The FileTimeoutReceiver class manages a timeout for file activity
 */
public class FileTimeoutReceiver extends BroadcastReceiver
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private final Activity itsActivity;
    private final AlarmManager itsAlarmMgr;
    private final PendingIntent itsCloseIntent;
    private int itsFileCloseTimeout = 0;
    private boolean itsIsCloseScreenOff =
            Preferences.PREF_FILE_CLOSE_SCREEN_OFF_DEF;
    private boolean itsIsPaused = true;

    private static final String TAG = "AuthFileTimeoutReceiver";

    /**
     * Constructor
     */
    public FileTimeoutReceiver(Activity act)
    {
        itsActivity = act;
        itsAlarmMgr = (AlarmManager)
                itsActivity.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(PasswdSafeApp.FILE_TIMEOUT_INTENT);
        intent.setPackage(BuildConfig.APPLICATION_ID);
        itsCloseIntent = PendingIntent.getBroadcast(itsActivity, 0, intent, 0);
        IntentFilter filter =
                new IntentFilter(PasswdSafeApp.FILE_TIMEOUT_INTENT);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        itsActivity.registerReceiver(this, filter);

        SharedPreferences prefs = Preferences.getSharedPrefs(itsActivity);
        prefs.registerOnSharedPreferenceChangeListener(this);
        updatePrefs(prefs);
    }

    /**
     * Handle when the activity is destroyed
     */
    public void onDestroy()
    {
        cancel();
        SharedPreferences prefs = Preferences.getSharedPrefs(itsActivity);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        itsActivity.unregisterReceiver(this);
    }

    /**
     * Update the file timeout
     * @param paused Whether the timeout is paused
     */
    public void updateTimeout(boolean paused)
    {
        if (paused) {
            if (!itsIsPaused) {
                itsIsPaused = true;
                cancel();
            }
        } else {
            itsIsPaused = false;
            if (itsFileCloseTimeout != 0) {
                itsAlarmMgr.set(
                        AlarmManager.ELAPSED_REALTIME,
                        SystemClock.elapsedRealtime() + itsFileCloseTimeout,
                        itsCloseIntent);
            }
        }
    }

    @Override
    public void onReceive(Context ctx, Intent intent)
    {
        boolean close = false;
        switch (intent.getAction()) {
        case PasswdSafeApp.FILE_TIMEOUT_INTENT: {
            Log.i(TAG, "File timeout");
            close = true;
            break;
        }
        case Intent.ACTION_SCREEN_OFF: {
            if (itsIsCloseScreenOff) {
                Log.i(TAG, "Screen off");
                close = true;
            }
            break;
        }
        }
        if (close) {
            itsActivity.finish();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        switch (key) {
        case Preferences.PREF_FILE_CLOSE_SCREEN_OFF:
        case Preferences.PREF_FILE_CLOSE_TIMEOUT: {
            updatePrefs(prefs);
            break;
        }
        }
    }

    /**
     * Update the preferences
     */
    private void updatePrefs(SharedPreferences prefs)
    {
        FileTimeoutPref pref = Preferences.getFileCloseTimeoutPref(prefs);
        itsIsCloseScreenOff = Preferences.getFileCloseScreenOffPref(prefs);
        PasswdSafeUtil.dbginfo(TAG, "update prefs timeout: %s, screen: %b",
                               pref, itsIsCloseScreenOff);

        itsFileCloseTimeout = pref.getTimeout();
        if (itsFileCloseTimeout == 0) {
            cancel();
        } else {
            updateTimeout(itsIsPaused);
        }
    }

    /**
     * Cancel the file timeout timer
     */
    private void cancel()
    {
        itsAlarmMgr.cancel(itsCloseIntent);
    }
}
