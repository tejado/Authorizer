/*
 * Copyright (©) 2009-2010 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.util.Map;
import java.util.WeakHashMap;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

public class PasswdSafeApp extends Application
{
    public static class FileTimeoutReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "File timeout");
            PasswdSafeApp app = (PasswdSafeApp)context.getApplicationContext();
            app.closeFileData();
        }

    }

    public static final boolean DEBUG = false;

    private PasswdFileData itsFileData = null;
    private WeakHashMap<Activity, Object> itsFileDataActivities =
        new WeakHashMap<Activity, Object>();
    private AlarmManager itsAlarmMgr;
    private PendingIntent itsCloseIntent;

    public static final String VIEW_INTENT =
        "com.jefftharris.passwdsafe.action.VIEW";
    public static final String FILE_TIMEOUT_INTENT =
        "com.jefftharris.passwdsafe.action.FILE_TIMEOUT";

    private static final String TAG = "PasswdSafeApp";

    public PasswdSafeApp()
    {
    }

    /* (non-Javadoc)
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate()
    {
        super.onCreate();
        itsAlarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
    }

    /* (non-Javadoc)
     * @see android.app.Application#onTerminate()
     */
    @Override
    public void onTerminate()
    {
        closeFileData();
        super.onTerminate();
    }

    public synchronized PasswdFileData getFileData(String fileName,
                                                   Activity activity)
    {
        if ((itsFileData == null) || (itsFileData.itsFileName == null) ||
            (!itsFileData.itsFileName.equals(fileName))) {
            closeFileData();
        }

        dbginfo(TAG, "getFileData fileName:" + fileName +
                ", data:" + itsFileData);
        registerActivityForFileData(activity);

        return itsFileData;
    }

    public synchronized void setFileData(PasswdFileData fileData,
                                         Activity activity)
    {
        closeFileData();
        itsFileData = fileData;
        registerActivityForFileData(activity);
    }

    public static void showFatalMsg(String msg, final Activity activity)
    {
        new AlertDialog.Builder(activity)
        .setMessage(msg)
        .setCancelable(false)
        .setPositiveButton("Close", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                activity.finish();
            }
        })
        .show();
    }

    public static void dbginfo(String tag, String msg)
    {
        if (DEBUG)
            Log.i(tag, msg);
    }

    private synchronized void closeFileData()
    {
        dbginfo(TAG, "closeFileData data:" + itsFileData);
        if (itsFileData != null) {
            itsFileData.close();
            itsFileData = null;
        }

        for (Map.Entry<Activity, Object> entry :
            itsFileDataActivities.entrySet()) {
            dbginfo(TAG, "closeFileData activity:" + entry.getKey());
            entry.getKey().finish();
        }
        itsFileDataActivities.clear();

        if (itsCloseIntent != null) {
            itsAlarmMgr.cancel(itsCloseIntent);
            itsCloseIntent = null;
        }
    }

    private synchronized void registerActivityForFileData(Activity activity)
    {
        dbginfo(TAG, "register activity:" + activity + ", data:" + itsFileData);
        if (itsFileData != null) {
            itsFileDataActivities.put(activity, null);
            if (itsCloseIntent != null)
                itsAlarmMgr.cancel(itsCloseIntent);
            itsCloseIntent =
                PendingIntent.getBroadcast(this, 0,
                                           new Intent(FILE_TIMEOUT_INTENT), 0);
            dbginfo(TAG, "register adding timer");
            itsAlarmMgr.set(AlarmManager.ELAPSED_REALTIME,
                            SystemClock.elapsedRealtime() + 300*1000,
                            itsCloseIntent);
        }
    }
}
