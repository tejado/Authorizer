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
import java.util.Date;
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
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
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
                        // TODO: FIELDS SET ON NEW FILES!!!!
                        itsFileData.setHdrLastSaveApp(
                            PasswdSafeApp.getAppTitle(PasswdSafeApp.this) +
                            " " +
                            PasswdSafeApp.getAppVersion(PasswdSafeApp.this));
                        itsFileData.setHdrLastSaveUser("User");
                        itsFileData.setHdrLastSaveHost(Build.MODEL);
                        itsFileData.setHdrLastSaveTime(new Date());
                        itsFileData.save();
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

    public static final String NEW_INTENT =
        "com.jefftharris.passwdsafe.action.NEW";
    public static final String VIEW_INTENT =
        "com.jefftharris.passwdsafe.action.VIEW";
    public static final String FILE_TIMEOUT_INTENT =
        "com.jefftharris.passwdsafe.action.FILE_TIMEOUT";

    public static final int RESULT_MODIFIED = Activity.RESULT_FIRST_USER;

    public static final String PREF_FILE_DIR = "fileDirPref";
    public static final String PREF_FILE_DIR_DEF =
        Environment.getExternalStorageDirectory().toString();

    public static final String PREF_FILE_CLOSE_TIMEOUT = "fileCloseTimeoutPref";
    public static final String PREF_FILE_CLOSE_TIMEOUT_DEF = "300";
    public static final String[] PREF_FILE_CLOSE_ENTRIES =
    {
        "None", "30 seconds", "5 minutes", "15 minutes", "1 hour"
    };
    public static final String[] PREF_FILE_CLOSE_ENTRY_VALUES =
    {
        "", "30", "300", "900", "3600"
    };

    public static final String PREF_DEF_FILE = "defFilePref";
    public static final String PREF_DEF_FILE_DEF = "";
    public static final String PREF_DEF_FILE_NONE = "None";

    public static final String PREF_GROUP_RECORDS = "groupRecordsPref";
    public static final boolean PREF_GROUP_RECORDS_DEF = true;

    public static final String PREF_PASSWD_ENC = "passwordEncodingPref";
    public static final String PREF_PASSWD_ENC_DEF =
        PwsFile.DEFAULT_PASSWORD_CHARSET;

    public static final String PREF_SEARCH_CASE_SENSITIVE =
        "searchCaseSensitivePref";
    public static final boolean PREF_SEARCH_CASE_SENSITIVE_DEF = false;
    public static final String PREF_SEARCH_REGEX = "searchRegexPref";
    public static final boolean PREF_SEARCH_REGEX_DEF = false;

    public static final String PREF_SHOW_BACKUP_FILES = "showBackupFilesPref";
    public static final boolean PREF_SHOW_BACKUP_FILES_DEF = false;

    public static final String PREF_SORT_CASE_SENSITIVE =
        "sortCaseSensitivePref";
    public static final boolean PREF_SORT_CASE_SENSITIVE_DEF = true;

    public static final String PREF_GEN_LOWER = "passwdGenLower";
    public static final boolean PREF_GEN_LOWER_DEF = true;
    public static final String PREF_GEN_UPPER = "passwdGenUpper";
    public static final boolean PREF_GEN_UPPER_DEF = true;
    public static final String PREF_GEN_DIGITS = "passwdGenDigits";
    public static final boolean PREF_GEN_DIGITS_DEF = true;
    public static final String PREF_GEN_SYMBOLS = "passwdGenSymbols";
    public static final boolean PREF_GEN_SYMBOLS_DEF = false;
    public static final String PREF_GEN_EASY = "passwdGenEasy";
    public static final boolean PREF_GEN_EASY_DEF = false;
    public static final String PREF_GEN_HEX = "passwdGenHex";
    public static final boolean PREF_GEN_HEX_DEF = false;
    public static final String PREF_GEN_LENGTH = "passwdGenLength";
    public static final String PREF_GEN_LENGTH_DEF = "8";

    private PasswdFileData itsFileData = null;
    private WeakHashMap<Activity, Object> itsFileDataActivities =
        new WeakHashMap<Activity, Object>();
    private AlarmManager itsAlarmMgr;
    private PendingIntent itsCloseIntent;
    private int itsFileCloseTimeout = 300*1000;
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
            prefsEdit.putString(PREF_FILE_DIR, dirPref);
            fileListEdit.commit();
            prefsEdit.commit();
        }

        updateFileCloseTimeoutPref(prefs);
        setPasswordEncodingPref(prefs);
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

        if (key.equals(PREF_FILE_CLOSE_TIMEOUT)) {
            updateFileCloseTimeoutPref(prefs);
        } else if (key.equals(PREF_PASSWD_ENC)) {
            setPasswordEncodingPref(prefs);
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

    public static String getFileCloseTimeoutPref(SharedPreferences prefs)
    {
        return prefs.getString(PREF_FILE_CLOSE_TIMEOUT,
                               PREF_FILE_CLOSE_TIMEOUT_DEF);
    }

    public static String getFileDirPref(SharedPreferences prefs)
    {
        return prefs.getString(PREF_FILE_DIR, PREF_FILE_DIR_DEF);
    }

    public static String getDefFilePref(SharedPreferences prefs)
    {
        return prefs.getString(PREF_DEF_FILE, PREF_DEF_FILE_DEF);
    }

    public static boolean getGroupRecordsPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_GROUP_RECORDS, PREF_GROUP_RECORDS_DEF);
    }

    public static String getPasswordEncodingPref(SharedPreferences prefs)
    {
        return prefs.getString(PREF_PASSWD_ENC, PREF_PASSWD_ENC_DEF);
    }

    public static boolean getPasswordGenLowerPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_GEN_LOWER, PREF_GEN_LOWER_DEF);
    }

    public static boolean getPasswordGenUpperPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_GEN_UPPER, PREF_GEN_UPPER_DEF);
    }

    public static boolean getPasswordGenDigitsPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_GEN_DIGITS, PREF_GEN_DIGITS_DEF);
    }

    public static boolean getPasswordGenSymbolsPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_GEN_SYMBOLS, PREF_GEN_SYMBOLS_DEF);
    }

    public static boolean getPasswordGenEasyPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_GEN_EASY, PREF_GEN_EASY_DEF);
    }

    public static boolean getPasswordGenHexPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_GEN_HEX, PREF_GEN_HEX_DEF);
    }

    public static int getPasswordGenLengthPref(SharedPreferences prefs)
    {
        return Integer.parseInt(
            prefs.getString(PREF_GEN_LENGTH, PREF_GEN_LENGTH_DEF));
    }

    public static boolean getSearchCaseSensitivePref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_SEARCH_CASE_SENSITIVE,
                                PREF_SEARCH_CASE_SENSITIVE_DEF);
    }

    public static boolean getSearchRegexPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_SEARCH_REGEX, PREF_SEARCH_REGEX_DEF);
    }

    public static boolean getShowBackupFilesPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_SHOW_BACKUP_FILES,
                                PREF_SHOW_BACKUP_FILES_DEF);
    }

    public static boolean getSortCaseSensitivePref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_SORT_CASE_SENSITIVE,
                                PREF_SORT_CASE_SENSITIVE_DEF);
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
        String timeoutStr = getFileCloseTimeoutPref(prefs);
        dbginfo(TAG, "new file close timeout: " + timeoutStr);
        if (timeoutStr.length() == 0) {
            cancelFileDataTimer();
            itsFileCloseTimeout = 0;
        } else {
            try {
                itsFileCloseTimeout = Integer.parseInt(timeoutStr) * 1000;
                touchFileDataTimer();
            } catch (NumberFormatException e) {
            }
        }
    }

    private static void setPasswordEncodingPref(SharedPreferences prefs)
    {
        PwsFile.setPasswordEncoding(getPasswordEncodingPref(prefs));
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

        for (Map.Entry<Activity, Object> entry :
            itsFileDataActivities.entrySet()) {
            dbginfo(TAG, "closeFileData activity:" + entry.getKey());
            entry.getKey().finish();
        }
        itsFileDataActivities.clear();
    }
}
