/*
 * Copyright (©) 2009-2010 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.WeakHashMap;

import org.pwsafe.lib.file.PwsFile;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.util.Log;

public class PasswdSafeApp extends Application
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    public class AppActivityPasswdFile extends ActivityPasswdFile
    {
        /// The file data
        PasswdFileData itsFileData;

        public AppActivityPasswdFile(PasswdFileData fileData,
                                     PasswdFileActivity activity)
        {
            super(activity);
            itsFileData = fileData;

            touch();
        }

        /**
         * @return the fileData
         */
        @Override
        public final PasswdFileData getFileData()
        {
            synchronized (PasswdSafeApp.this) {
                touch();
                return itsFileData;
            }
        }

        @Override
        public final boolean isOpen()
        {
            synchronized (PasswdSafeApp.this) {
                return (itsFileData != null);
            }
        }

        @Override
        public final void setFileData(PasswdFileData fileData)
        {
            synchronized (PasswdSafeApp.this) {
                PasswdSafeApp.this.setFileData(fileData, getActivity());
                itsFileData = fileData;
            }
        }

        /**
         * Save the file.  Will likely be called in a background thread.
         * @throws IOException
         * @throws ConcurrentModificationException
         * @throws NoSuchAlgorithmException
         */
        @Override
        protected final void doSave()
            throws NoSuchAlgorithmException, ConcurrentModificationException,
                   IOException
        {
            synchronized (PasswdSafeApp.this) {
                if (itsFileData != null) {
                    cancelFileDataTimer();
                    try {
                        itsFileData.save(PasswdSafeApp.this);
                    } finally {
                        touchFileDataTimer();
                    }
                }
            }
        }

        @Override
        public final void touch()
        {
            touchFileData(getActivity());
        }

        @Override
        public final void release()
        {
            releaseFileData(getActivity());
        }

        @Override
        public final void close()
        {
            synchronized (PasswdSafeApp.this) {
                PasswdSafeApp.this.setFileData(null, getActivity());
                itsFileData = null;
            }
        }

        @Override
        public final void pauseFileTimer()
        {
            PasswdSafeApp.this.pauseFileTimer();
        }

        @Override
        public final void resumeFileTimer()
        {
            PasswdSafeApp.this.resumeFileTimer();
        }
    }

    public static class FileTimeoutReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "File timeout");
            PasswdSafeApp app = (PasswdSafeApp)context.getApplicationContext();
            app.closeFileData(true);
        }

    }

    static {
        Security.removeProvider("BC");
        Security.addProvider(new BCProvider());
    }

    public static final boolean DEBUG = false;
    public static final boolean DEBUG_AUTOOPEN = true;
    public static final String DEBUG_AUTO_FILE =
        Preferences.PREF_FILE_DIR_DEF + "/test.psafe3";

    public static final String NEW_INTENT =
        "com.jefftharris.passwdsafe.action.NEW";
    public static final String VIEW_INTENT =
        "com.jefftharris.passwdsafe.action.VIEW";
    public static final String FILE_TIMEOUT_INTENT =
        "com.jefftharris.passwdsafe.action.FILE_TIMEOUT";

    public static final int RESULT_MODIFIED = Activity.RESULT_FIRST_USER;

    private PasswdFileData itsFileData = null;
    private WeakHashMap<Activity, Object> itsFileDataActivities =
        new WeakHashMap<Activity, Object>();
    private AlarmManager itsAlarmMgr;
    private PendingIntent itsCloseIntent;
    private int itsFileCloseTimeout =
        Preferences.PREF_FILE_CLOSE_TIMEOUT_DEF.getTimeout();
    private boolean itsIsFileCloseClearClipboard =
        Preferences.PREF_FILE_CLOSE_CLEAR_CLIPBOARD_DEF;
    private boolean itsIsOpenDefault = true;
    private boolean itsFileTimerPaused = false;

    private static final Intent FILE_TIMEOUT_INTENT_OBJ =
        new Intent(FILE_TIMEOUT_INTENT);
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

        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        // Move the fileDirPref from the FileList class to the preferences
        String dirPrefName = "dir";
        SharedPreferences fileListPrefs = getSharedPreferences("FileList",
                                                               MODE_PRIVATE);
        if ((fileListPrefs != null) && fileListPrefs.contains(dirPrefName)) {
            String dirPref = fileListPrefs.getString(dirPrefName, "");
            dbginfo(TAG, "Moving dir pref \"" + dirPref + "\" to main");

            SharedPreferences.Editor fileListEdit = fileListPrefs.edit();
            SharedPreferences.Editor prefsEdit = prefs.edit();
            fileListEdit.remove(dirPrefName);
            prefsEdit.putString(Preferences.PREF_FILE_DIR, dirPref);
            fileListEdit.commit();
            prefsEdit.commit();
        }

        updateFileCloseTimeoutPref(prefs);
        setPasswordEncodingPref(prefs);
        setFileCloseClearClipboardPref(prefs);
    }

    /* (non-Javadoc)
     * @see android.app.Application#onTerminate()
     */
    @Override
    public void onTerminate()
    {
        closeFileData(false);
        super.onTerminate();
    }

    /* (non-Javadoc)
     * @see android.content.SharedPreferences.OnSharedPreferenceChangeListener#onSharedPreferenceChanged(android.content.SharedPreferences, java.lang.String)
     */
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        dbginfo(TAG, "Preference change: " + key + ", value: " +
                prefs.getAll().get(key));

        if (key.equals(Preferences.PREF_FILE_CLOSE_TIMEOUT)) {
            updateFileCloseTimeoutPref(prefs);
        } else if (key.equals(Preferences.PREF_PASSWD_ENC)) {
            setPasswordEncodingPref(prefs);
        } else if (key.equals(Preferences.PREF_FILE_CLOSE_CLEAR_CLIPBOARD)) {
            setFileCloseClearClipboardPref(prefs);
        }
    }

    public boolean checkOpenDefault()
    {
        if (itsIsOpenDefault) {
            itsIsOpenDefault = false;
            return true;
        } else {
            return false;
        }
    }

    public synchronized ActivityPasswdFile accessPasswdFile
    (
         File file,
         PasswdFileActivity activity
    )
    {
        if ((itsFileData == null) || (itsFileData.getFile() == null) ||
            (!itsFileData.getFile().equals(file))) {
            closeFileData(false);
        }

        dbginfo(TAG, "access file:" + file+ ", data:" + itsFileData);
        return new AppActivityPasswdFile(itsFileData, activity);
    }

    public static final String getAppFileTitle(ActivityPasswdFile actFile,
                                               Context ctx)
    {
        File file = null;
        if (actFile != null) {
            PasswdFileData fileData = actFile.getFileData();
            if (fileData != null) {
                file= fileData.getFile();
            }
        }
        return getAppFileTitle(file, ctx);
    }

    public static final String getAppFileTitle(File file, Context ctx)
    {
        StringBuilder builder = new StringBuilder(getAppTitle(ctx));
        if (file != null) {
            builder.append(" - ");
            builder.append(file.getName());
        }
        return builder.toString();

    }

    public static final String getAppTitle(Context ctx)
    {
        return ctx.getString(R.string.app_name);
    }

    public static final String getAppVersion(Context ctx)
    {
        String version;
        try {
            PackageManager pkgMgr = ctx.getPackageManager();
            PackageInfo pkgInfo = pkgMgr.getPackageInfo(ctx.getPackageName(),
                                                        0);
            version = pkgInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = "Unknown";
        }
        return version;
    }

    public static void copyToClipboard(String str, Context ctx)
    {
        ClipboardManager clipMgr = (ClipboardManager)
            ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        clipMgr.setText(str);
    }

    public static void showFatalMsg(Throwable t, Activity activity)
    {
        showFatalMsg(t, t.toString(), activity);
    }

    public static void showFatalMsg(Throwable t, String msg, Activity activity)
    {
        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));
        Log.e(TAG, writer.toString());
        showFatalMsg(msg, activity);
    }

    public static void showFatalMsg(String msg, final Activity activity)
    {
        AbstractDialogClickListener dlgClick = new AbstractDialogClickListener()
        {
            @Override
            public final void onOkClicked(DialogInterface dialog)
            {
                activity.finish();
            }

            @Override
            public final void onCancelClicked(DialogInterface dialog)
            {
                activity.finish();
            }
        };

        new AlertDialog.Builder(activity)
        .setMessage(msg)
        .setCancelable(false)
        .setPositiveButton("Close", dlgClick)
        .setOnCancelListener(dlgClick)
        .show();
    }

    public static void dbginfo(String tag, String msg)
    {
        if (DEBUG)
            Log.i(tag, msg);
    }

    private synchronized final
    void updateFileCloseTimeoutPref(SharedPreferences prefs)
    {
        FileTimeoutPref pref = Preferences.getFileCloseTimeoutPref(prefs);
        dbginfo(TAG, "new file close timeout: " + pref);
        itsFileCloseTimeout = pref.getTimeout();
        if (itsFileCloseTimeout == 0) {
            cancelFileDataTimer();
        } else {
            touchFileDataTimer();
        }
    }

    private static void setPasswordEncodingPref(SharedPreferences prefs)
    {
        PwsFile.setPasswordEncoding(Preferences.getPasswordEncodingPref(prefs));
    }

    private final void setFileCloseClearClipboardPref(SharedPreferences prefs)
    {
        itsIsFileCloseClearClipboard =
            Preferences.getFileCloseClearClipboardPref(prefs);
    }

    private synchronized final void pauseFileTimer()
    {
        cancelFileDataTimer();
        itsFileTimerPaused = true;
    }

    private synchronized final void resumeFileTimer()
    {
        itsFileTimerPaused = false;
        touchFileDataTimer();
    }

    private synchronized final void cancelFileDataTimer()
    {
        if (itsCloseIntent != null) {
            itsAlarmMgr.cancel(itsCloseIntent);
            itsCloseIntent = null;
        }
    }

    private synchronized final void touchFileDataTimer()
    {
        dbginfo(TAG, "touch timer timeout: " + itsFileCloseTimeout);
        if ((itsFileData != null) && (itsFileCloseTimeout != 0) &&
            !itsFileTimerPaused) {
            if (itsCloseIntent == null) {
                itsCloseIntent =
                    PendingIntent.getBroadcast(this, 0,
                                               FILE_TIMEOUT_INTENT_OBJ, 0);
            }
            dbginfo(TAG, "register adding timer");
            itsAlarmMgr.set(AlarmManager.ELAPSED_REALTIME,
                            SystemClock.elapsedRealtime() + itsFileCloseTimeout,
                            itsCloseIntent);
        }
    }

    private synchronized final void touchFileData(Activity activity)
    {
        dbginfo(TAG, "touch activity:" + activity + ", data:" + itsFileData);
        if (itsFileData != null) {
            itsFileDataActivities.put(activity, null);
            touchFileDataTimer();
        }
    }

    private synchronized final void releaseFileData(Activity activity)
    {
        dbginfo(TAG, "release activity:" + activity);
        itsFileDataActivities.remove(activity);
    }

    private synchronized final void setFileData(PasswdFileData fileData,
                                                Activity activity)
    {
        closeFileData(false);
        itsFileData = fileData;
        touchFileData(activity);
    }

    private synchronized final void closeFileData(boolean isTimeout)
    {
        dbginfo(TAG, "closeFileData data:" + itsFileData);
        if (itsFileData != null) {
            itsFileData.close();
            itsFileData = null;

            if (isTimeout) {
                itsIsOpenDefault = true;
            }
        }

        cancelFileDataTimer();

        if (itsIsFileCloseClearClipboard) {
            copyToClipboard("", this);
        }

        for (Map.Entry<Activity, Object> entry :
            itsFileDataActivities.entrySet()) {
            dbginfo(TAG, "closeFileData activity:" + entry.getKey());
            entry.getKey().finish();
        }
        itsFileDataActivities.clear();
    }
}
