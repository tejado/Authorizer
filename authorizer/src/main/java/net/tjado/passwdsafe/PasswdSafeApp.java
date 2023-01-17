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
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.text.TextUtils;

import com.mikepenz.iconics.Iconics;
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;

import androidx.annotation.Nullable;

import net.tjado.passwdsafe.file.PasswdExpiryFilter;
import net.tjado.passwdsafe.file.PasswdFileUri;
import net.tjado.passwdsafe.file.PasswdPolicy;
import net.tjado.passwdsafe.file.PasswdRecordFilter;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;

import org.pwsafe.lib.file.PwsFile;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public final class PasswdSafeApp extends Application
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    public static final String DEBUG_AUTO_FILE =
            null;
            //"/document/primary:test.psafe3";

    public static final String EXPIRATION_TIMEOUT_INTENT =
        "net.tjado.passwdsafe.action.EXPIRATION_TIMEOUT";
    public static final String FILE_TIMEOUT_INTENT =
        "net.tjado.passwdsafe.action.FILE_TIMEOUT";
    public static final String CHOOSE_RECORD_INTENT =
        "net.tjado.passwdsafe.action.CHOOSE_RECORD_INTENT";

    public static final String RESULT_DATA_UUID = "uuid";

    private PasswdPolicy itsDefaultPasswdPolicy = null;
    private NotificationMgr itsNotifyMgr;
    private boolean itsIsOpenDefault = true;
    private final ExecutorService itsThreadExecutor = Executors.newSingleThreadExecutor();

    private static final String TAG = "PasswdSafeApp";

    static {
        System.loadLibrary("PasswdSafe");
    }

    /* (non-Javadoc)
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate()
    {
        super.onCreate();
        PasswdRecordFilter.initMatches(getApplicationContext());
        SharedPreferences prefs = Preferences.getSharedPrefs(this);

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
            fileListEdit.apply();
            prefsEdit.apply();
        }
        Preferences.upgrade(prefs, this);

        Iconics.registerFont(new MaterialDesignIconic());

        initPrefs(prefs);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs,
                                          @Nullable String key)
    {
        if (key == null) {
            initPrefs(prefs);
            itsNotifyMgr.setPasswdExpiryFilter(getPasswdExpiryNotifPref(prefs));
        } else {
            PasswdSafeUtil.dbginfo(TAG, "Preference change: %s, value: %s", key,
                                   prefs.getAll().get(key));

            switch (key) {
            case Preferences.PREF_PASSWD_ENC: {
                setPasswordEncodingPref(prefs);
                break;
            }
            case Preferences.PREF_PASSWD_DEFAULT_SYMS: {
                setPasswordDefaultSymsPref(prefs);
                break;
            }
            case Preferences.PREF_PASSWD_EXPIRY_NOTIF: {
                itsNotifyMgr.setPasswdExpiryFilter(
                        getPasswdExpiryNotifPref(prefs));
                break;
            }
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
     * Sanitize an intent URI for a file to open. Removes fragments and query
     * params
     */
    public static Uri getOpenUriFromIntent(Intent intent)
    {
        Uri uri = intent.getData();
        if (uri == null) {
            return null;
        }
        Uri.Builder builder = uri.buildUpon();
        builder.fragment("");
        if (uri.isHierarchical()) {
            builder.query("");
        }
        return builder.build();
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
        SharedPreferences prefs = Preferences.getSharedPrefs(this);
        Preferences.setDefPasswdPolicyPref(policy, prefs);
    }


    /** Get the notification manager */
    public NotificationMgr getNotifyMgr()
    {
        return itsNotifyMgr;
    }


    /**
     * Setup the theme on an activity
     */
    public static void setupTheme(Activity act)
    {
        setupActTheme(act, false);
    }

    /**
     * Setup the theme on a dialog activity
     */
    public static void setupDialogTheme(Activity act)
    {
        setupActTheme(act, true);
    }

    /**
     * Get pref for display treeview
     */
    public static boolean getDisplayTreeView(Activity act)
    {
        SharedPreferences prefs = Preferences.getSharedPrefs(act);
        return Preferences.getDisplayListTreeView(prefs);
    }

    /**
     * Get a title for a URI
     */
    public static String getAppFileTitle(PasswdFileUri uri, Context ctx)
    {
        return getAppTitle((uri != null) ? uri.getIdentifier(ctx, true) : null, ctx);
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

    /**
     * Schedule a background task
     */
    public static void scheduleTask(Runnable run, Context ctx)
    {
        PasswdSafeApp app = (PasswdSafeApp)ctx.getApplicationContext();
        app.itsThreadExecutor.submit(run);
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

    /** Get the password expiration filter for notifications from a
     * preference */
    private static PasswdExpiryFilter
        getPasswdExpiryNotifPref(SharedPreferences prefs)
    {
        return Preferences.getPasswdExpiryNotifPref(prefs).getFilter();
    }

    /**
     * Setup the theme on a normal or dialog activity
     */
    private static void setupActTheme(Activity act, boolean isDialog)
    {
        int uimode = Configuration.UI_MODE_NIGHT_UNDEFINED;

        SharedPreferences prefs = Preferences.getSharedPrefs(act);
        switch (Preferences.getDisplayTheme(prefs)) {
        case FOLLOW_SYSTEM: {
            uimode = act.getResources().getConfiguration().uiMode &
                     Configuration.UI_MODE_NIGHT_MASK;
            break;
        }
        case LIGHT: {
            uimode = Configuration.UI_MODE_NIGHT_NO;
            break;
        }
        case DARK: {
            uimode = Configuration.UI_MODE_NIGHT_YES;
            break;
        }
        }

        switch (uimode) {
        case Configuration.UI_MODE_NIGHT_NO:
        case Configuration.UI_MODE_NIGHT_UNDEFINED: {
            act.setTheme(isDialog ? R.style.PwsAppTheme_Dialog :
                                 R.style.PwsAppTheme);
            break;
        }
        case Configuration.UI_MODE_NIGHT_YES: {
            act.setTheme(isDialog ? R.style.PwsAppThemeDark_Dialog :
                                 R.style.PwsAppThemeDark);
            break;
        }
        }
    }

    /**
     * Initialize settings from preferences
     */
    private void initPrefs(SharedPreferences prefs)
    {
        setPasswordEncodingPref(prefs);
        setPasswordDefaultSymsPref(prefs);
        itsDefaultPasswdPolicy = Preferences.getDefPasswdPolicyPref(prefs,
                                                                    this);
    }
}
