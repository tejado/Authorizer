/*
 * Copyright (©) 2009-2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.File;

import org.pwsafe.lib.file.PwsFile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.jefftharris.passwdsafe.file.PasswdFileUri;
import com.jefftharris.passwdsafe.file.PasswdPolicy;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.pref.FileBackupPref;
import com.jefftharris.passwdsafe.pref.FileTimeoutPref;
import com.jefftharris.passwdsafe.pref.FontSizePref;
import com.jefftharris.passwdsafe.pref.PasswdExpiryNotifPref;

/**
 * The Preferences class defines the activity for managing preferences on the
 * application
 *
 * @author Jeff Harris
 */
public class Preferences extends PreferenceActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    public static final String PREF_FILE_DIR = "fileDirPref";
    public static final String PREF_FILE_DIR_DEF =
        Environment.getExternalStorageDirectory().toString();

    public static final String PREF_FILE_CLOSE_TIMEOUT = "fileCloseTimeoutPref";
    public static final FileTimeoutPref PREF_FILE_CLOSE_TIMEOUT_DEF =
        FileTimeoutPref.TO_5_MIN;
    public static final String PREF_FILE_CLOSE_SCREEN_OFF =
                    "fileCloseScreenOffPref";
    public static final boolean PREF_FILE_CLOSE_SCREEN_OFF_DEF = false;

    public static final String PREF_FILE_BACKUP = "fileBackupPref";
    public static final FileBackupPref PREF_FILE_BACKUP_DEF =
        FileBackupPref.BACKUP_1;

    public static final String PREF_FILE_CLOSE_CLEAR_CLIPBOARD =
        "fileCloseClearClipboardPref";
    public static final boolean PREF_FILE_CLOSE_CLEAR_CLIPBOARD_DEF = true;

    public static final String PREF_FILE_OPEN_READ_ONLY =
        "fileOpenReadOnly";
    public static final boolean PREF_FILE_OPEN_READ_ONLY_DEF = false;

    public static final String PREF_DEF_FILE = "defFilePref";
    public static final String PREF_DEF_FILE_DEF = "";

    public static final String PREF_GROUP_RECORDS = "groupRecordsPref";
    public static final boolean PREF_GROUP_RECORDS_DEF = true;

    public static final String PREF_PASSWD_ENC = "passwordEncodingPref";
    public static final String PREF_PASSWD_ENC_DEF =
        PwsFile.DEFAULT_PASSWORD_CHARSET;
    public static final String PREF_PASSWD_EXPIRY_NOTIF =
        "passwordExpiryNotifyPref";
    public static final PasswdExpiryNotifPref PREF_PASSWD_EXPIRY_NOTIF_DEF =
        PasswdExpiryNotifPref.IN_TWO_WEEKS;
    public static final String PREF_PASSWD_DEFAULT_SYMS =
        "passwordDefaultSymbolsPref";
    public static final String PREF_PASSWD_CLEAR_ALL_NOTIFS =
        "passwordClearAllNotifsPref";

    public static final String PREF_SEARCH_CASE_SENSITIVE =
        "searchCaseSensitivePref";
    public static final boolean PREF_SEARCH_CASE_SENSITIVE_DEF = false;
    public static final String PREF_SEARCH_REGEX = "searchRegexPref";
    public static final boolean PREF_SEARCH_REGEX_DEF = false;

    public static final String PREF_SHOW_HIDDEN_FILES = "showBackupFilesPref";
    public static final boolean PREF_SHOW_HIDDEN_FILES_DEF = false;

    public static final String PREF_SORT_CASE_SENSITIVE =
        "sortCaseSensitivePref";
    public static final boolean PREF_SORT_CASE_SENSITIVE_DEF = true;

    private static final String PREF_GEN_LOWER = "passwdGenLower";
    private static final boolean PREF_GEN_LOWER_DEF = true;
    private static final String PREF_GEN_UPPER = "passwdGenUpper";
    private static final boolean PREF_GEN_UPPER_DEF = true;
    private static final String PREF_GEN_DIGITS = "passwdGenDigits";
    private static final boolean PREF_GEN_DIGITS_DEF = true;
    private static final String PREF_GEN_SYMBOLS = "passwdGenSymbols";
    private static final boolean PREF_GEN_SYMBOLS_DEF = false;
    private static final String PREF_GEN_EASY = "passwdGenEasy";
    private static final boolean PREF_GEN_EASY_DEF = false;
    private static final String PREF_GEN_HEX = "passwdGenHex";
    private static final boolean PREF_GEN_HEX_DEF = false;
    private static final String PREF_GEN_LENGTH = "passwdGenLength";
    private static final String PREF_GEN_LENGTH_DEF = "8";
    public static final String PREF_DEF_PASSWD_POLICY = "defaultPasswdPolicy";
    public static final String PREF_DEF_PASSWD_POLICY_DEF = "";

    public static final String PREF_FONT_SIZE = "fontSizePref";
    public static final FontSizePref PREF_FONT_SIZE_DEF = FontSizePref.NORMAL;

    public static final String INTENT_SCREEN = "screen";
    public static final String SCREEN_PASSWORD_OPTIONS = "passwordOptions";

    private static final String TAG = "Preferences";

    private static final int REQUEST_DEFAULT_FILE = 0;

    private EditTextPreference itsFileDirPref;
    private Preference itsDefFilePref;
    private ListPreference itsFileClosePref;
    private ListPreference itsFileBackupPref;
    private ListPreference itsPasswdEncPref;
    private ListPreference itsPasswdExpiryNotifPref;
    private EditTextPreference itsPasswdDefaultSymsPref;
    private ListPreference itsFontSizePref;


    public static FileTimeoutPref getFileCloseTimeoutPref(SharedPreferences prefs)
    {
        try {
            return FileTimeoutPref.prefValueOf(
                prefs.getString(PREF_FILE_CLOSE_TIMEOUT,
                                PREF_FILE_CLOSE_TIMEOUT_DEF.getValue()));
        } catch (IllegalArgumentException e) {
            return PREF_FILE_CLOSE_TIMEOUT_DEF;
        }
    }

    public static boolean getFileCloseScreenOffPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_FILE_CLOSE_SCREEN_OFF,
                                PREF_FILE_CLOSE_SCREEN_OFF_DEF);
    }

    public static FileBackupPref getFileBackupPref(SharedPreferences prefs)
    {
        try {
            return FileBackupPref.prefValueOf(
                prefs.getString(PREF_FILE_BACKUP,
                                PREF_FILE_BACKUP_DEF.getValue()));
        } catch (IllegalArgumentException e) {
            return PREF_FILE_BACKUP_DEF;
        }
    }

    public static boolean getFileCloseClearClipboardPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_FILE_CLOSE_CLEAR_CLIPBOARD,
                                PREF_FILE_CLOSE_CLEAR_CLIPBOARD_DEF);
    }

    public static boolean getFileOpenReadOnlyPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_FILE_OPEN_READ_ONLY,
                                PREF_FILE_OPEN_READ_ONLY_DEF);
    }

    public static void setFileOpenReadOnlyPref(boolean readonly,
                                               SharedPreferences prefs)
    {
        SharedPreferences.Editor prefsEdit = prefs.edit();
        prefsEdit.putBoolean(PREF_FILE_OPEN_READ_ONLY, readonly);
        prefsEdit.commit();
    }

    public static File getFileDirPref(SharedPreferences prefs)
    {
        return new File(prefs.getString(PREF_FILE_DIR, PREF_FILE_DIR_DEF));
    }

    public static void setFileDirPref(File dir, SharedPreferences prefs)
    {
        SharedPreferences.Editor prefsEdit = prefs.edit();
        prefsEdit.putString(Preferences.PREF_FILE_DIR, dir.toString());
        prefsEdit.commit();
    }

    public static Uri getDefFilePref(SharedPreferences prefs)
    {
        String defFile = prefs.getString(PREF_DEF_FILE, PREF_DEF_FILE_DEF);
        if (TextUtils.isEmpty(defFile)) {
            return null;
        }
        return Uri.parse(defFile);
    }

    public static FontSizePref getFontSizePref(SharedPreferences prefs)
    {
        try {
            return FontSizePref.valueOf(
                prefs.getString(PREF_FONT_SIZE, PREF_FONT_SIZE_DEF.toString()));
        } catch (IllegalArgumentException e) {
            return PREF_FONT_SIZE_DEF;
        }
    }

    public static boolean getGroupRecordsPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_GROUP_RECORDS, PREF_GROUP_RECORDS_DEF);
    }

    public static String getPasswordEncodingPref(SharedPreferences prefs)
    {
        return prefs.getString(PREF_PASSWD_ENC, PREF_PASSWD_ENC_DEF);
    }

    /** Get the password expiration notification preference */
    public static PasswdExpiryNotifPref getPasswdExpiryNotifPref
    (
         SharedPreferences prefs
    )
    {
        try {
            return PasswdExpiryNotifPref.prefValueOf(
                prefs.getString(PREF_PASSWD_EXPIRY_NOTIF,
                                PREF_PASSWD_EXPIRY_NOTIF_DEF.getValue()));
        } catch (IllegalArgumentException e) {
            return PREF_PASSWD_EXPIRY_NOTIF_DEF;
        }
    }

    /** Get the symbols used by default in a password policy */
    public static String getPasswdDefaultSymbolsPref(SharedPreferences prefs)
    {
        String val = prefs.getString(PREF_PASSWD_DEFAULT_SYMS, null);
        if (TextUtils.isEmpty(val)) {
            val = PasswdPolicy.SYMBOLS_DEFAULT;
        }
        return val;
    }

    /** Upgrade the default password policy preference if needed */
    public static void upgradePasswdPolicy(SharedPreferences prefs,
                                           Context ctx)
    {
        if (prefs.contains(PREF_DEF_PASSWD_POLICY)) {
            PasswdSafeUtil.dbginfo(TAG, "Have default policy");
            return;
        }

        SharedPreferences.Editor prefsEdit = prefs.edit();
        String policyStr = PREF_DEF_PASSWD_POLICY_DEF;
        if (prefs.contains(PREF_GEN_LOWER) ||
            prefs.contains(PREF_GEN_UPPER) ||
            prefs.contains(PREF_GEN_DIGITS) ||
            prefs.contains(PREF_GEN_SYMBOLS) ||
            prefs.contains(PREF_GEN_EASY) ||
            prefs.contains(PREF_GEN_HEX) ||
            prefs.contains(PREF_GEN_LENGTH)) {
            PasswdSafeUtil.dbginfo(TAG, "Upgrade old prefs");

            int flags = 0;
            if (prefs.getBoolean(PREF_GEN_HEX, PREF_GEN_HEX_DEF)) {
                flags |= PasswdPolicy.FLAG_USE_HEX_DIGITS;
            } else {
                if (prefs.getBoolean(PREF_GEN_EASY, PREF_GEN_EASY_DEF)) {
                    flags |= PasswdPolicy.FLAG_USE_EASY_VISION;
                }

                if (prefs.getBoolean(PREF_GEN_LOWER, PREF_GEN_LOWER_DEF)) {
                    flags |= PasswdPolicy.FLAG_USE_LOWERCASE;
                }
                if (prefs.getBoolean(PREF_GEN_UPPER, PREF_GEN_UPPER_DEF)) {
                    flags |= PasswdPolicy.FLAG_USE_UPPERCASE;
                }
                if (prefs.getBoolean(PREF_GEN_DIGITS, PREF_GEN_DIGITS_DEF)) {
                    flags |= PasswdPolicy.FLAG_USE_DIGITS;
                }
                if (prefs.getBoolean(PREF_GEN_SYMBOLS, PREF_GEN_SYMBOLS_DEF)) {
                    flags |= PasswdPolicy.FLAG_USE_SYMBOLS;
                }
            }
            int length;
            try {
                length = Integer.parseInt(prefs.getString(PREF_GEN_LENGTH,
                                                          PREF_GEN_LENGTH_DEF));
            } catch (NumberFormatException e) {
                length = Integer.parseInt(PREF_GEN_LENGTH_DEF);
            }
            PasswdPolicy policy = PasswdPolicy.createDefaultPolicy(ctx, flags,
                                                                   length);
            policyStr = policy.toHdrPolicyString();

            prefsEdit.remove(PREF_GEN_LOWER);
            prefsEdit.remove(PREF_GEN_UPPER);
            prefsEdit.remove(PREF_GEN_DIGITS);
            prefsEdit.remove(PREF_GEN_SYMBOLS);
            prefsEdit.remove(PREF_GEN_EASY);
            prefsEdit.remove(PREF_GEN_HEX);
            prefsEdit.remove(PREF_GEN_LENGTH);
        }

        PasswdSafeUtil.dbginfo(TAG, "Save new default policy: %s", policyStr);
        prefsEdit.putString(PREF_DEF_PASSWD_POLICY, policyStr);
        prefsEdit.commit();
    }

    /** Upgrade the default file preference if needed */
    public static void upgradeDefaultFilePref(SharedPreferences prefs)
    {
        Uri defFileUri = getDefFilePref(prefs);
        if ((defFileUri != null) && (defFileUri.getScheme() == null)) {
            File defDir = getFileDirPref(prefs);
            File def = new File(defDir, defFileUri.getPath());
            defFileUri = Uri.fromFile(def);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREF_DEF_FILE, defFileUri.toString());
            editor.commit();
        }
    }

    /** Get the default password policy preference */
    public static PasswdPolicy getDefPasswdPolicyPref(SharedPreferences prefs,
                                                      Context ctx)
    {
        String policyStr = prefs.getString(PREF_DEF_PASSWD_POLICY,
                                           PREF_DEF_PASSWD_POLICY_DEF);
        PasswdPolicy policy = null;
        if (!TextUtils.isEmpty(policyStr)) {
            try {
                policy = PasswdPolicy.parseHdrPolicy(
                    policyStr, 0, 0, PasswdPolicy.Location.DEFAULT).first;
            } catch (Exception e) {
                // Use default
            }
        }
        if (policy == null) {
            policy = PasswdPolicy.createDefaultPolicy(ctx);
        }
        return policy;
    }

    /** Set the default password policy preference */
    public static void setDefPasswdPolicyPref(PasswdPolicy policy,
                                              SharedPreferences prefs)
    {
        SharedPreferences.Editor prefsEdit = prefs.edit();
        prefsEdit.putString(PREF_DEF_PASSWD_POLICY, policy.toHdrPolicyString());
        prefsEdit.commit();
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

    public static boolean getShowHiddenFilesPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_SHOW_HIDDEN_FILES,
                                PREF_SHOW_HIDDEN_FILES_DEF);
    }

    public static boolean getSortCaseSensitivePref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_SORT_CASE_SENSITIVE,
                                PREF_SORT_CASE_SENSITIVE_DEF);
    }

    /** Called when the activity is first created. */
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences);

        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        itsFileDirPref = (EditTextPreference)findPreference(PREF_FILE_DIR);
        itsFileClosePref = (ListPreference)
            findPreference(PREF_FILE_CLOSE_TIMEOUT);
        itsFileBackupPref = (ListPreference)
            findPreference(PREF_FILE_BACKUP);

        itsFileDirPref.setDefaultValue(PREF_FILE_DIR_DEF);
        onSharedPreferenceChanged(prefs, PREF_FILE_DIR);

        itsDefFilePref = findPreference(PREF_DEF_FILE);
        itsDefFilePref.setOnPreferenceClickListener(
            new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    Intent intent = new Intent(Intent.ACTION_CREATE_SHORTCUT,
                                               null, Preferences.this,
                                               LauncherFileShortcuts.class);
                    intent.putExtra(LauncherFileShortcuts.EXTRA_IS_DEFAULT_FILE,
                                    true);
                    startActivityForResult(intent, REQUEST_DEFAULT_FILE);
                    return true;
                }
            });
        onSharedPreferenceChanged(prefs, PREF_DEF_FILE);

        Resources res = getResources();
        itsFileClosePref.setEntries(FileTimeoutPref.getDisplayNames(res));
        itsFileClosePref.setEntryValues(FileTimeoutPref.getValues());
        onSharedPreferenceChanged(prefs, PREF_FILE_CLOSE_TIMEOUT);

        itsFileBackupPref.setEntries(FileBackupPref.getDisplayNames(res));
        itsFileBackupPref.setEntryValues(FileBackupPref.getValues());
        onSharedPreferenceChanged(prefs, PREF_FILE_BACKUP);

        itsPasswdEncPref = (ListPreference)findPreference(PREF_PASSWD_ENC);
        String[] charsets =
            PwsFile.ALL_PASSWORD_CHARSETS.toArray(new String[0]);
        itsPasswdEncPref.setEntries(charsets);
        itsPasswdEncPref.setEntryValues(charsets);
        itsPasswdEncPref.setDefaultValue(PREF_PASSWD_ENC_DEF);
        onSharedPreferenceChanged(prefs, PREF_PASSWD_ENC);

        itsPasswdExpiryNotifPref =
            (ListPreference)findPreference(PREF_PASSWD_EXPIRY_NOTIF);
        itsPasswdExpiryNotifPref.setEntries(
            PasswdExpiryNotifPref.getDisplayNames(res));
        itsPasswdExpiryNotifPref.setEntryValues(
            PasswdExpiryNotifPref.getValues());
        onSharedPreferenceChanged(prefs, PREF_PASSWD_EXPIRY_NOTIF);

        itsPasswdDefaultSymsPref =
                (EditTextPreference)findPreference(PREF_PASSWD_DEFAULT_SYMS);
        itsPasswdDefaultSymsPref.getEditText().setHint(
                PasswdPolicy.SYMBOLS_DEFAULT);
        itsPasswdDefaultSymsPref.setDefaultValue(PasswdPolicy.SYMBOLS_DEFAULT);
        onSharedPreferenceChanged(prefs, PREF_PASSWD_DEFAULT_SYMS);

        Preference pref = findPreference(PREF_PASSWD_CLEAR_ALL_NOTIFS);
        pref.setOnPreferenceClickListener(
            new Preference.OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                PasswdSafeApp app = (PasswdSafeApp)getApplication();
                app.getNotifyMgr().clearAllNotifications(Preferences.this);
                return true;
            }
        });

        itsFontSizePref = (ListPreference) findPreference(PREF_FONT_SIZE);
        itsFontSizePref.setEntries(FontSizePref.getDisplayNames(res));
        itsFontSizePref.setEntryValues(FontSizePref.getValues());
        onSharedPreferenceChanged(prefs, PREF_FONT_SIZE);

        Intent intent = getIntent();
        String screen = intent.getStringExtra(INTENT_SCREEN);
        if (screen != null) {
            Preference scr = findPreference(screen);
            getPreferenceScreen().onItemClick(null, null, scr.getOrder(), 0);
        }
    }

    /* (non-Javadoc)
     * @see android.preference.PreferenceActivity#onDestroy()
     */
    @Override
    protected void onDestroy()
    {
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    /* (non-Javadoc)
     * @see android.content.SharedPreferences.OnSharedPreferenceChangeListener#onSharedPreferenceChanged(android.content.SharedPreferences, java.lang.String)
     */
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        if (key.equals(PREF_FILE_DIR)) {
            File pref = getFileDirPref(prefs);
            if (pref.toString().length() == 0) {
                pref = new File(PREF_FILE_DIR_DEF);
                itsFileDirPref.setText(pref.toString());
            }
            // Make sure text editor is in sync with preference value
            if (!pref.toString().equals(itsFileDirPref.getText())) {
                itsFileDirPref.setText(pref.toString());
            }
            itsFileDirPref.setSummary(pref.toString());
        } else if (key.equals(PREF_DEF_FILE)) {
            new DefaultFileResolver().execute(getDefFilePref(prefs));
        } else if (key.equals(PREF_FILE_CLOSE_TIMEOUT)) {
            itsFileClosePref.setSummary(
                getFileCloseTimeoutPref(prefs).getDisplayName(getResources()));
        } else if (key.equals(PREF_FILE_BACKUP)) {
            itsFileBackupPref.setSummary(
                getFileBackupPref(prefs).getDisplayName(getResources()));
        } else if (key.equals(PREF_PASSWD_ENC)) {
            itsPasswdEncPref.setSummary(getPasswordEncodingPref(prefs));
        } else if (key.equals(PREF_PASSWD_EXPIRY_NOTIF)) {
            itsPasswdExpiryNotifPref.setSummary(
               getPasswdExpiryNotifPref(prefs).getDisplayName(getResources()));
        } else if (key.equals(PREF_PASSWD_DEFAULT_SYMS)) {
            String val = getPasswdDefaultSymbolsPref(prefs);
            itsPasswdDefaultSymsPref.setSummary(
                getString(R.string.symbols_used_by_default, val));
        } else if (key.equals(PREF_FONT_SIZE)) {
            itsFontSizePref.setSummary(
                getFontSizePref(prefs).getDisplayName(getResources()));
        }
    }

    /* (non-Javadoc)
     * @see android.preference.PreferenceActivity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data)
    {
        if (requestCode == REQUEST_DEFAULT_FILE) {
            if (resultCode == RESULT_OK) {
                Intent val =
                        data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
                SharedPreferences.Editor editor = itsDefFilePref.getEditor();
                String prefVal =
                        (val != null) ? val.getData().toString() : null;
                editor.putString(PREF_DEF_FILE, prefVal);
                editor.commit();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /** Background task to resolve the default file URI and set the
     * preference's summary */
    private final class DefaultFileResolver
            extends AsyncTask<Uri, Void, PasswdFileUri>
    {
        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected PasswdFileUri doInBackground(Uri... params)
        {
            Uri uri = params[0];
            if (uri == null) {
                return null;
            }
            return new PasswdFileUri(uri, Preferences.this);
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(PasswdFileUri result)
        {
            String summary;
            if (result == null) {
                summary = getString(R.string.none);
            } else {
                summary = result.getIdentifier(Preferences.this, false);
            }
            itsDefFilePref.setSummary(summary);
        }
    }
}
