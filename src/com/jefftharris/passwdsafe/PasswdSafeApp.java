/*
 * Copyright (©) 2009-2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.NoSuchAlgorithmException;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.WeakHashMap;

import org.pwsafe.lib.file.PwsFile;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdPolicy;
import com.jefftharris.passwdsafe.file.PasswdRecordFilter;
import com.jefftharris.passwdsafe.pref.FileTimeoutPref;
import com.jefftharris.passwdsafe.view.AbstractDialogClickListener;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
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
            boolean closeFile = true;
            PasswdSafeApp app = (PasswdSafeApp)context.getApplicationContext();
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                closeFile = app.itsIsFileCloseScreenOff;
            }
            if (closeFile) {
                Log.i(TAG, "File timeout: " + intent);
                app.closeFileData(true);
            }
        }
    }

    public static final boolean DEBUG = false;
    public static final String DEBUG_AUTO_FILE =
        null;
        //Preferences.PREF_FILE_DIR_DEF + "/test.psafe3";

    public static final String NEW_INTENT =
        "com.jefftharris.passwdsafe.action.NEW";
    public static final String VIEW_INTENT =
        "com.jefftharris.passwdsafe.action.VIEW";
    public static final String EXPIRATION_TIMEOUT_INTENT =
        "com.jefftharris.passwdsafe.action.EXPIRATION_TIMEOUT";
    public static final String FILE_TIMEOUT_INTENT =
        "com.jefftharris.passwdsafe.action.FILE_TIMEOUT";
    public static final String CHOOSE_RECORD_INTENT =
        "com.jefftharris.passwdsafe.action.CHOOSE_RECORD_INTENT";

    public static final int RESULT_MODIFIED = Activity.RESULT_FIRST_USER;
    public static final String RESULT_DATA_UUID = "uuid";

    private PasswdFileData itsFileData = null;
    private WeakHashMap<Activity, Object> itsFileDataActivities =
        new WeakHashMap<Activity, Object>();
    private PasswdPolicy itsDefaultPasswdPolicy = null;
    private AlarmManager itsAlarmMgr;
    private NotificationMgr itsNotifyMgr;
    private PendingIntent itsCloseIntent;
    private int itsFileCloseTimeout =
        Preferences.PREF_FILE_CLOSE_TIMEOUT_DEF.getTimeout();
    private boolean itsIsFileCloseScreenOff =
                    Preferences.PREF_FILE_CLOSE_SCREEN_OFF_DEF;
    private boolean itsIsFileCloseClearClipboard =
        Preferences.PREF_FILE_CLOSE_CLEAR_CLIPBOARD_DEF;
    private boolean itsIsOpenDefault = true;
    private boolean itsFileTimerPaused = false;
    private BroadcastReceiver itsScreenOffReceiver = null;

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
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);

        itsAlarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        itsNotifyMgr = new NotificationMgr(this,
                                           itsAlarmMgr,
                                           getPasswdExpiryNotifPref(prefs));

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
        Preferences.upgradePasswdPolicy(prefs, this);

        updateFileCloseTimeoutPref(prefs);
        updateFileCloseScreenOffPref(prefs);
        setPasswordEncodingPref(prefs);
        setFileCloseClearClipboardPref(prefs);
        itsDefaultPasswdPolicy = Preferences.getDefPasswdPolicyPref(prefs,
                                                                    this);
    }

    /* (non-Javadoc)
     * @see android.app.Application#onTerminate()
     */
    @Override
    public void onTerminate()
    {
        dbginfo(TAG, "onTerminate");
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
        } else if (key.equals(Preferences.PREF_FILE_CLOSE_SCREEN_OFF)) {
            updateFileCloseScreenOffPref(prefs);
        } else if (key.equals(Preferences.PREF_PASSWD_ENC)) {
            setPasswordEncodingPref(prefs);
        } else if (key.equals(Preferences.PREF_FILE_CLOSE_CLEAR_CLIPBOARD)) {
            setFileCloseClearClipboardPref(prefs);
        } else if (key.equals(Preferences.PREF_PASSWD_EXPIRY_NOTIF)) {
            itsNotifyMgr.setPasswdExpiryFilter(getPasswdExpiryNotifPref(prefs));
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

    /**
     * Sanitize an intent URI for the open file URI. Removes fragments and query
     * parameters
     */
    public static Uri getFileUriFromIntent(Intent intent)
    {
        Uri uri = intent.getData();
        Uri.Builder builder = uri.buildUpon();
        builder.fragment("");
        builder.query("");
        return builder.build();
    }

    public synchronized ActivityPasswdFile accessPasswdFile
    (
         Uri uri,
         PasswdFileActivity activity
    )
    {
        if ((itsFileData == null) || (itsFileData.getUri() == null) ||
            (!itsFileData.getUri().equals(uri))) {
            itsFileDataActivities.remove(activity);
            checkScreenOffReceiver();
            closeFileData(false);
        }

        dbgverb(TAG, "access uri:" + uri + ", data:" + itsFileData);
        return new AppActivityPasswdFile(itsFileData, activity);
    }

    public synchronized ActivityPasswdFile accessOpenFile
    (
        PasswdFileActivity activity
    )
    {
        dbgverb(TAG, "access open file data: " + itsFileData);
        if (itsFileData == null) {
            return null;
        }
        return new AppActivityPasswdFile(itsFileData, activity);
    }


    /** Get the default password policy */
    public synchronized PasswdPolicy getDefaultPasswdPolicy()
    {
        return itsDefaultPasswdPolicy;
    }


    /** Set the default password policy */
    public synchronized void setDefaultPasswdPolicy(PasswdPolicy policy)
    {
        itsDefaultPasswdPolicy = policy;
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        Preferences.setDefPasswdPolicyPref(policy, prefs);
    }


    /** Get the notification manager */
    public NotificationMgr getNotifyMgr()
    {
        return itsNotifyMgr;
    }


    public static final String getAppFileTitle(ActivityPasswdFile actFile,
                                               Context ctx)
    {
        Uri uri = null;
        if (actFile != null) {
            PasswdFileData fileData = actFile.getFileData();
            if (fileData != null) {
                uri = fileData.getUri();
            }
        }
        return getAppFileTitle(uri, ctx);
    }

    public static final String getAppFileTitle(Uri uri, Context ctx)
    {
        StringBuilder builder = new StringBuilder(getAppTitle(ctx));
        if (uri != null) {
            builder.append(" - ");
            builder.append(PasswdFileData.getUriIdentifier(uri, ctx, true));
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
        try {
            ClipboardManager clipMgr = (ClipboardManager)
                ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            clipMgr.setText(str);
        } catch (Throwable e) {
            showErrorMsg(ctx.getString(R.string.copy_clipboard_error), ctx);
        }
    }

    public static void showFatalMsg(Throwable t, Activity activity)
    {
        showFatalMsg(t, t.toString(), activity, true);
    }

    public static void showFatalMsg(String msg, Activity activity)
    {
        showFatalMsg(null, msg, activity, true);
    }

    public static void showFatalMsg(String msg,
                                    Activity activity,
                                    boolean copyTrace)
    {
        showFatalMsg(null, msg, activity, copyTrace);
    }

    public static void showFatalMsg(Throwable t,
                                    String msg,
                                    Activity activity)
    {
        showFatalMsg(t, msg, activity, true);
    }

    public static void showFatalMsg(Throwable t,
                                    String msg,
                                    final Activity activity,
                                    boolean copyTrace)
    {
        if (copyTrace && (t != null)) {
            StringWriter writer = new StringWriter();
            t.printStackTrace(new PrintWriter(writer));
            String trace = writer.toString();
            Log.e(TAG, trace);
            copyToClipboard(trace, activity);
        }

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

        AlertDialog.Builder dlg = new AlertDialog.Builder(activity)
        .setTitle(getAppTitle(activity) + " - " +
                  activity.getString(R.string.error))
        .setMessage(msg)
        .setCancelable(false)
        .setPositiveButton(
             copyTrace ? R.string.copy_trace_and_close : R.string.close,
             dlgClick)
        .setOnCancelListener(dlgClick);
        dlg.show();
    }

    public static void showErrorMsg(String msg, Context context)
    {
        AlertDialog.Builder dlg = new AlertDialog.Builder(context)
        .setTitle(getAppTitle(context) + " - " +
                  context.getString(R.string.error))
        .setMessage(msg)
        .setCancelable(true)
        .setNeutralButton(R.string.close, new OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });
        dlg.show();
    }

    /** Log a debug message at info level */
    public static void dbginfo(String tag, String msg)
    {
        if (DEBUG)
            Log.i(tag, msg);
    }

    /** Log a formatted debug message at info level */
    public static void dbginfo(String tag, String fmt, Object... args)
    {
        if (DEBUG) {
            Log.i(tag, String.format(fmt, args));
        }
    }

    /** Log a debug message at verbose level */
    public static void dbgverb(String tag, String msg)
    {
        if (DEBUG)
            Log.v(tag, msg);
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

    private final void updateFileCloseScreenOffPref(SharedPreferences prefs)
    {
        itsIsFileCloseScreenOff =
                        Preferences.getFileCloseScreenOffPref(prefs);
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

    /** Get the password expiration filter for notifications from a
     * preference */
    private static PasswdRecordFilter.ExpiryFilter
        getPasswdExpiryNotifPref(SharedPreferences prefs)
    {
        return Preferences.getPasswdExpiryNotifPref(prefs).getFilter();
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
        dbgverb(TAG, "touch timer timeout: " + itsFileCloseTimeout);
        if ((itsFileData != null) && (itsFileCloseTimeout != 0) &&
            !itsFileTimerPaused) {
            if (itsCloseIntent == null) {
                itsCloseIntent =
                    PendingIntent.getBroadcast(this, 0,
                                               FILE_TIMEOUT_INTENT_OBJ, 0);
            }
            dbgverb(TAG, "register adding timer");
            itsAlarmMgr.set(AlarmManager.ELAPSED_REALTIME,
                            SystemClock.elapsedRealtime() + itsFileCloseTimeout,
                            itsCloseIntent);
        }
    }

    private synchronized final void touchFileData(Activity activity)
    {
        dbgverb(TAG, "touch activity:" + activity + ", data:" + itsFileData);
        if (itsFileData != null) {
            itsFileDataActivities.put(activity, null);
            checkScreenOffReceiver();
            touchFileDataTimer();
        }
    }

    private synchronized final void releaseFileData(Activity activity)
    {
        dbgverb(TAG, "release activity:" + activity);
        itsFileDataActivities.remove(activity);
        checkScreenOffReceiver();
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

            if (itsIsFileCloseClearClipboard) {
                copyToClipboard("", this);
            }
        }

        cancelFileDataTimer();

        for (Map.Entry<Activity, Object> entry :
            itsFileDataActivities.entrySet()) {
            dbgverb(TAG, "closeFileData activity:" + entry.getKey());
            entry.getKey().finish();
        }
        itsFileDataActivities.clear();
        checkScreenOffReceiver();
    }

    /** Check whether the screen off receiver should be added or removed */
    private synchronized final void checkScreenOffReceiver()
    {
        boolean haveActivities = !itsFileDataActivities.isEmpty();
        if ((itsScreenOffReceiver == null) && haveActivities) {
            dbginfo(TAG, "add screen off receiver");
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            itsScreenOffReceiver = new FileTimeoutReceiver();
            registerReceiver(itsScreenOffReceiver, filter);
        } else if ((itsScreenOffReceiver != null) && !haveActivities) {
            dbginfo(TAG, "remove screen off receiver");
            unregisterReceiver(itsScreenOffReceiver);
            itsScreenOffReceiver = null;
        }
    }
}
