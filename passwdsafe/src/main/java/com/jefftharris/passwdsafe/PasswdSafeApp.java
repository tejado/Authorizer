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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdFileUri;
import com.jefftharris.passwdsafe.file.PasswdPolicy;
import com.jefftharris.passwdsafe.file.PasswdRecordFilter;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

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
                    itsFileData.save(PasswdSafeApp.this);
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
        }

        @Override
        public final void resumeFileTimer()
        {
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
    private final WeakHashMap<Activity, Object> itsFileDataActivities =
            new WeakHashMap<>();
    private PasswdPolicy itsDefaultPasswdPolicy = null;
    private NotificationMgr itsNotifyMgr;
    private boolean itsIsFileCloseScreenOff =
            Preferences.PREF_FILE_CLOSE_SCREEN_OFF_DEF;
    private boolean itsIsFileCloseClearClipboard =
        Preferences.PREF_FILE_CLOSE_CLEAR_CLIPBOARD_DEF;
    private boolean itsIsOpenDefault = true;
    private BroadcastReceiver itsScreenOffReceiver = null;

    private static final String TAG = "PasswdSafeApp";

    static {
        System.loadLibrary("PasswdSafe");
    }

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

        AlarmManager alarmMgr =
                (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        itsNotifyMgr = new NotificationMgr(this,
                                           alarmMgr,
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
        closeFileData();
        super.onTerminate();
    }

    /* (non-Javadoc)
     * @see android.content.SharedPreferences.OnSharedPreferenceChangeListener#onSharedPreferenceChanged(android.content.SharedPreferences, java.lang.String)
     */
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        PasswdSafeUtil.dbginfo(TAG, "Preference change: %s, value: %s",
                               key, prefs.getAll().get(key));

        switch (key) {
        case Preferences.PREF_FILE_CLOSE_SCREEN_OFF: {
            updateFileCloseScreenOffPref(prefs);
            break;
        }
        case Preferences.PREF_PASSWD_ENC: {
            setPasswordEncodingPref(prefs);
            break;
        }
        case Preferences.PREF_PASSWD_DEFAULT_SYMS: {
            setPasswordDefaultSymsPref(prefs);
            break;
        }
        case Preferences.PREF_FILE_CLOSE_CLEAR_CLIPBOARD: {
            setFileCloseClearClipboardPref(prefs);
            break;
        }
        case Preferences.PREF_PASSWD_EXPIRY_NOTIF: {
            itsNotifyMgr.setPasswdExpiryFilter(getPasswdExpiryNotifPref(prefs));
            break;
        }
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
        return new PasswdFileUri(getOpenUriFromIntent(intent), ctx);
    }

    /**
     * Sanitize an intent URI for a file to open. Removes fragments and query
     * params
     */
    public static Uri getOpenUriFromIntent(Intent intent)
    {
        Uri uri = intent.getData();
        Uri.Builder builder = uri.buildUpon();
        builder.fragment("");
        builder.query("");
        return builder.build();
    }

    public synchronized ActivityPasswdFile accessPasswdFile
    (
         PasswdFileUri uri,
         PasswdFileActivity activity
    )
    {
        if ((itsFileData == null) || (itsFileData.getUri() == null) ||
            (!itsFileData.getUri().equals(uri))) {
            itsFileDataActivities.remove(activity.getActivity());
            checkScreenOffReceiver();
            closeFileData();
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
        return itsFileData;
    }

    /** Close an open file */
    public synchronized void closeOpenFile()
    {
        PasswdSafeUtil.dbgverb(TAG, "close file");
        checkScreenOffReceiver();
        closeFileData();
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


    public static String getAppFileTitle(ActivityPasswdFile actFile,
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

    public static String getAppFileTitle(PasswdFileUri uri, Context ctx)
    {
        return getAppTitle((uri != null) ? uri.getIdentifier(ctx, true) : null,
                           ctx);
    }

    /**
     * Get a title for the application
     */
    public static String getAppTitle(String title, Context ctx)
    {
        StringBuilder builder = new StringBuilder();
        if (!TextUtils.isEmpty(title)) {
            builder.append(title);
            builder.append(" - ");
        }
        builder.append(PasswdSafeUtil.getAppTitle(ctx));
        return builder.toString();
    }

    private void updateFileCloseScreenOffPref(SharedPreferences prefs)
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

    private void setFileCloseClearClipboardPref(SharedPreferences prefs)
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

    private synchronized void touchFileData(Activity activity)
    {
        PasswdSafeUtil.dbgverb(TAG, "touch activity: %s, data: %s",
                               activity, itsFileData);
        if (itsFileData != null) {
            itsFileDataActivities.put(activity, null);
            checkScreenOffReceiver();
        }
    }

    private synchronized void releaseFileData(Activity activity)
    {
        PasswdSafeUtil.dbgverb(TAG, "release activity: %s", activity);
        itsFileDataActivities.remove(activity);
        checkScreenOffReceiver();
    }

    private synchronized void setFileData(PasswdFileData fileData,
                                          Activity activity)
    {
        closeFileData();
        itsFileData = fileData;
        touchFileData(activity);
    }

    /** Set the UUID of the last viewed record */
    private synchronized void setLastViewedRecord(String uuid)
    {
        PasswdSafeUtil.dbginfo(TAG, "setViewedRecord: %s", uuid);
        itsLastViewedRecord = uuid;
    }

    private synchronized void closeFileData()
    {
        PasswdSafeUtil.dbginfo(TAG, "closeFileData data: %s", itsFileData);
        if (itsFileData != null) {
            itsFileData.close();
            itsFileData = null;

            if (itsIsFileCloseClearClipboard) {
                PasswdSafeUtil.copyToClipboard("", this);
            }
        }
        itsLastViewedRecord = null;

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
    private synchronized void checkScreenOffReceiver()
    {
        boolean haveActivities = !itsFileDataActivities.isEmpty();
        if ((itsScreenOffReceiver == null) && haveActivities) {
            PasswdSafeUtil.dbginfo(TAG, "add screen off receiver");
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            //itsScreenOffReceiver = new FileTimeoutReceiver();
            registerReceiver(itsScreenOffReceiver, filter);
        } else if ((itsScreenOffReceiver != null) && !haveActivities) {
            PasswdSafeUtil.dbginfo(TAG, "remove screen off receiver");
            unregisterReceiver(itsScreenOffReceiver);
            itsScreenOffReceiver = null;
        }
    }
}
