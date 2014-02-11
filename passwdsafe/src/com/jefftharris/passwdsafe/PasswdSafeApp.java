/*
 * Copyright (©) 2009-2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.WeakHashMap;

import org.pwsafe.lib.file.PwsFile;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdFileUri;
import com.jefftharris.passwdsafe.file.PasswdPolicy;
import com.jefftharris.passwdsafe.file.PasswdRecordFilter;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.pref.FileTimeoutPref;

public class PasswdSafeApp extends Application
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    // TODO: Remove all @SuppressWarnings("deprecation")

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

        /** Set the UUID of the last viewed record */
        @Override
        public final void setLastViewedRecord(String uuid)
        {
            PasswdSafeApp.this.setLastViewedRecord(uuid);
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

    public static final String DEBUG_AUTO_FILE =
        null;
        //Preferences.PREF_FILE_DIR_DEF + "/test.psafe3";

    public static final String EXPIRATION_TIMEOUT_INTENT =
        "com.jefftharris.passwdsafe.action.EXPIRATION_TIMEOUT";
    public static final String FILE_TIMEOUT_INTENT =
        "com.jefftharris.passwdsafe.action.FILE_TIMEOUT";
    public static final String CHOOSE_RECORD_INTENT =
        "com.jefftharris.passwdsafe.action.CHOOSE_RECORD_INTENT";

    public static final int RESULT_MODIFIED = Activity.RESULT_FIRST_USER;
    public static final String RESULT_DATA_UUID = "uuid";

    private PasswdFileData itsFileData = null;
    private String itsLastViewedRecord = null;
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
            PasswdSafeUtil.dbginfo(TAG, "Moving dir pref \"%s\" to main",
                                   dirPref);

            SharedPreferences.Editor fileListEdit = fileListPrefs.edit();
            SharedPreferences.Editor prefsEdit = prefs.edit();
            fileListEdit.remove(dirPrefName);
            prefsEdit.putString(Preferences.PREF_FILE_DIR, dirPref);
            fileListEdit.commit();
            prefsEdit.commit();
        }
        Preferences.upgradePasswdPolicy(prefs, this);
        Preferences.upgradeDefaultFilePref(prefs);

        updateFileCloseTimeoutPref(prefs);
        updateFileCloseScreenOffPref(prefs);
        setPasswordEncodingPref(prefs);
        setPasswordDefaultSymsPref(prefs);
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
        PasswdSafeUtil.dbginfo(TAG, "onTerminate");
        closeFileData(false);
        super.onTerminate();
    }

    /* (non-Javadoc)
     * @see android.content.SharedPreferences.OnSharedPreferenceChangeListener#onSharedPreferenceChanged(android.content.SharedPreferences, java.lang.String)
     */
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        PasswdSafeUtil.dbginfo(TAG, "Preference change: %s, value: %s",
                               key, prefs.getAll().get(key));

        if (key.equals(Preferences.PREF_FILE_CLOSE_TIMEOUT)) {
            updateFileCloseTimeoutPref(prefs);
        } else if (key.equals(Preferences.PREF_FILE_CLOSE_SCREEN_OFF)) {
            updateFileCloseScreenOffPref(prefs);
        } else if (key.equals(Preferences.PREF_PASSWD_ENC)) {
            setPasswordEncodingPref(prefs);
        } else if (key.equals(Preferences.PREF_PASSWD_DEFAULT_SYMS)) {
            setPasswordDefaultSymsPref(prefs);
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
    public static PasswdFileUri getFileUriFromIntent(Intent intent,
                                                     Context ctx)
    {
        Uri uri = intent.getData();
        Uri.Builder builder = uri.buildUpon();
        builder.fragment("");
        builder.query("");
        return new PasswdFileUri(builder.build(), ctx);
    }

    public synchronized ActivityPasswdFile accessPasswdFile
    (
         PasswdFileUri uri,
         PasswdFileActivity activity
    )
    {
        if ((itsFileData == null) || (itsFileData.getUri() == null) ||
            (!itsFileData.getUri().equals(uri))) {
            itsFileDataActivities.remove(activity);
            checkScreenOffReceiver();
            closeFileData(false);
        }

        PasswdSafeUtil.dbgverb(TAG, "access uri: %s, data: %s",
                               uri, itsFileData);
        return new AppActivityPasswdFile(itsFileData, activity);
    }

    public synchronized ActivityPasswdFile accessOpenFile
    (
        PasswdFileActivity activity
    )
    {
        PasswdSafeUtil.dbgverb(TAG, "access open file data: %s", itsFileData);
        if (itsFileData == null) {
            return null;
        }
        return new AppActivityPasswdFile(itsFileData, activity);
    }


    /** Access an open password file. The data returned should only be used for
     * short durations. */
    public synchronized PasswdFileData accessOpenFileData()
    {
        PasswdSafeUtil.dbgverb(TAG, "access open file data: %s", itsFileData);
        if (itsFileData != null) {
            touchFileDataTimer();
        }
        return itsFileData;
    }

    /** Get the UUID of the last viewed record */
    public synchronized String getLastViewedRecord()
    {
        return itsLastViewedRecord;
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
        PasswdFileUri uri = null;
        if (actFile != null) {
            PasswdFileData fileData = actFile.getFileData();
            if (fileData != null) {
                uri = fileData.getUri();
            }
        }
        return getAppFileTitle(uri, ctx);
    }

    public static final String getAppFileTitle(PasswdFileUri uri, Context ctx)
    {
        StringBuilder builder =
                new StringBuilder(PasswdSafeUtil.getAppTitle(ctx));
        if (uri != null) {
            builder.append(" - ");
            builder.append(uri.getIdentifier(ctx, true));
        }
        return builder.toString();

    }

    private synchronized final
    void updateFileCloseTimeoutPref(SharedPreferences prefs)
    {
        FileTimeoutPref pref = Preferences.getFileCloseTimeoutPref(prefs);
        PasswdSafeUtil.dbginfo(TAG, "new file close timeout: %s", pref);
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

    /** Set the default password policy symbols from user preferences */
    private static void setPasswordDefaultSymsPref(SharedPreferences prefs)
    {
        PasswdPolicy.setPrefsDefaultSymbols(
                Preferences.getPasswdDefaultSymbolsPref(prefs));
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
        PasswdSafeUtil.dbgverb(TAG, "touch timer timeout: %d",
                               itsFileCloseTimeout);
        if ((itsFileData != null) && (itsFileCloseTimeout != 0) &&
            !itsFileTimerPaused) {
            if (itsCloseIntent == null) {
                itsCloseIntent =
                    PendingIntent.getBroadcast(this, 0,
                                               FILE_TIMEOUT_INTENT_OBJ, 0);
            }
            PasswdSafeUtil.dbgverb(TAG, "register adding timer");
            itsAlarmMgr.set(AlarmManager.ELAPSED_REALTIME,
                            SystemClock.elapsedRealtime() + itsFileCloseTimeout,
                            itsCloseIntent);
        }
    }

    private synchronized final void touchFileData(Activity activity)
    {
        PasswdSafeUtil.dbgverb(TAG, "touch activity: %s, data: %s",
                               activity, itsFileData);
        if (itsFileData != null) {
            itsFileDataActivities.put(activity, null);
            checkScreenOffReceiver();
            touchFileDataTimer();
        }
    }

    private synchronized final void releaseFileData(Activity activity)
    {
        PasswdSafeUtil.dbgverb(TAG, "release activity: %s", activity);
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

    /** Set the UUID of the last viewed record */
    private synchronized final void setLastViewedRecord(String uuid)
    {
        PasswdSafeUtil.dbginfo(TAG, "setViewedRecord: %s", uuid);
        itsLastViewedRecord = uuid;
    }

    private synchronized final void closeFileData(boolean isTimeout)
    {
        PasswdSafeUtil.dbginfo(TAG, "closeFileData data: %s", itsFileData);
        if (itsFileData != null) {
            itsFileData.close();
            itsFileData = null;

            if (isTimeout) {
                itsIsOpenDefault = true;
            }

            if (itsIsFileCloseClearClipboard) {
                PasswdSafeUtil.copyToClipboard("", this);
            }
        }
        itsLastViewedRecord = null;

        cancelFileDataTimer();

        for (Map.Entry<Activity, Object> entry :
            itsFileDataActivities.entrySet()) {
            PasswdSafeUtil.dbgverb(TAG, "closeFileData activity: %s",
                                   entry.getKey());
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
            PasswdSafeUtil.dbginfo(TAG, "add screen off receiver");
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            itsScreenOffReceiver = new FileTimeoutReceiver();
            registerReceiver(itsScreenOffReceiver, filter);
        } else if ((itsScreenOffReceiver != null) && !haveActivities) {
            PasswdSafeUtil.dbginfo(TAG, "remove screen off receiver");
            unregisterReceiver(itsScreenOffReceiver);
            itsScreenOffReceiver = null;
        }
    }
}
