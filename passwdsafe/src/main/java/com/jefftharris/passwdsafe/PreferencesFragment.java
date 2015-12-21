/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.TextUtils;

import com.jefftharris.passwdsafe.file.PasswdFileUri;
import com.jefftharris.passwdsafe.file.PasswdPolicy;
import com.jefftharris.passwdsafe.pref.FileBackupPref;
import com.jefftharris.passwdsafe.pref.FileTimeoutPref;
import com.jefftharris.passwdsafe.pref.PasswdExpiryNotifPref;
import com.jefftharris.passwdsafe.pref.RecordSortOrderPref;

import org.pwsafe.lib.file.PwsFile;

import java.io.File;

/**
 * Fragment for PasswdSafe preferences
 */
public class PreferencesFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener,
                   Preference.OnPreferenceClickListener
{
    private static final int REQUEST_DEFAULT_FILE = 0;

    private EditTextPreference itsFileDirPref;
    private Preference itsDefFilePref;
    private ListPreference itsFileClosePref;
    private ListPreference itsFileBackupPref;
    private ListPreference itsPasswdEncPref;
    private ListPreference itsPasswdExpiryNotifPref;
    private EditTextPreference itsPasswdDefaultSymsPref;
    private ListPreference itsRecordSortOrderPref;

    @Override
    public void onCreatePreferences(Bundle bundle, String s)
    {
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        Resources res = getResources();

        itsFileDirPref = (EditTextPreference)
                findPreference(Preferences.PREF_FILE_DIR);
        itsFileDirPref.setDefaultValue(Preferences.PREF_FILE_DIR_DEF);
        onSharedPreferenceChanged(prefs, Preferences.PREF_FILE_DIR);

        itsDefFilePref = findPreference(Preferences.PREF_DEF_FILE);
        itsDefFilePref.setOnPreferenceClickListener(this);
        onSharedPreferenceChanged(prefs, Preferences.PREF_DEF_FILE);

        itsFileClosePref = (ListPreference)
                findPreference(Preferences.PREF_FILE_CLOSE_TIMEOUT);
        itsFileClosePref.setEntries(FileTimeoutPref.getDisplayNames(res));
        itsFileClosePref.setEntryValues(FileTimeoutPref.getValues());
        onSharedPreferenceChanged(prefs, Preferences.PREF_FILE_CLOSE_TIMEOUT);

        itsFileBackupPref = (ListPreference)
                findPreference(Preferences.PREF_FILE_BACKUP);
        itsFileBackupPref.setEntries(FileBackupPref.getDisplayNames(res));
        itsFileBackupPref.setEntryValues(FileBackupPref.getValues());
        onSharedPreferenceChanged(prefs, Preferences.PREF_FILE_BACKUP);

        itsPasswdEncPref = (ListPreference)
                findPreference(Preferences.PREF_PASSWD_ENC);
        String[] charsets =  PwsFile.ALL_PASSWORD_CHARSETS.toArray(
                new String[PwsFile.ALL_PASSWORD_CHARSETS.size()]);
        itsPasswdEncPref.setEntries(charsets);
        itsPasswdEncPref.setEntryValues(charsets);
        itsPasswdEncPref.setDefaultValue(Preferences.PREF_PASSWD_ENC_DEF);
        onSharedPreferenceChanged(prefs, Preferences.PREF_PASSWD_ENC);

        itsPasswdExpiryNotifPref = (ListPreference)
                findPreference(Preferences.PREF_PASSWD_EXPIRY_NOTIF);
        itsPasswdExpiryNotifPref.setEntries(
                PasswdExpiryNotifPref.getDisplayNames(res));
        itsPasswdExpiryNotifPref.setEntryValues(
                PasswdExpiryNotifPref.getValues());
        onSharedPreferenceChanged(prefs, Preferences.PREF_PASSWD_EXPIRY_NOTIF);

        itsPasswdDefaultSymsPref = (EditTextPreference)
                findPreference(Preferences.PREF_PASSWD_DEFAULT_SYMS);
        itsPasswdDefaultSymsPref.setDialogMessage(
                getString(R.string.default_symbols_empty_pref,
                          PasswdPolicy.SYMBOLS_DEFAULT));
        itsPasswdDefaultSymsPref.setDefaultValue(PasswdPolicy.SYMBOLS_DEFAULT);
        onSharedPreferenceChanged(prefs, Preferences.PREF_PASSWD_DEFAULT_SYMS);

        Preference clearNotifsPref =
                findPreference(Preferences.PREF_PASSWD_CLEAR_ALL_NOTIFS);
        clearNotifsPref.setOnPreferenceClickListener(this);

        itsRecordSortOrderPref = (ListPreference)
                findPreference(Preferences.PREF_RECORD_SORT_ORDER);
        itsRecordSortOrderPref.setEntries(
                RecordSortOrderPref.getDisplayNames(res));
        itsRecordSortOrderPref.setEntryValues(RecordSortOrderPref.getValues());
        onSharedPreferenceChanged(prefs, Preferences.PREF_RECORD_SORT_ORDER);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        switch (key) {
        case Preferences.PREF_FILE_DIR: {
            File pref = Preferences.getFileDirPref(prefs);
            if (TextUtils.isEmpty(pref.toString())) {
                pref = new File(Preferences.PREF_FILE_DIR_DEF);
                itsFileDirPref.setText(pref.toString());
            }
            if (!TextUtils.equals(pref.toString(), itsFileDirPref.getText())) {
                itsFileDirPref.setText(pref.toString());
            }
            itsFileDirPref.setSummary(pref.toString());
            break;
        }
        case Preferences.PREF_DEF_FILE: {
            new DefaultFileResolver().execute(
                    Preferences.getDefFilePref(prefs));
            break;
        }
        case Preferences.PREF_FILE_CLOSE_TIMEOUT: {
            FileTimeoutPref pref = Preferences.getFileCloseTimeoutPref(prefs);
            itsFileClosePref.setSummary(pref.getDisplayName(getResources()));
            break;
        }
        case Preferences.PREF_FILE_BACKUP: {
            FileBackupPref pref = Preferences.getFileBackupPref(prefs);
            itsFileBackupPref.setSummary(pref.getDisplayName(getResources()));
            break;
        }
        case Preferences.PREF_PASSWD_ENC: {
            itsPasswdEncPref.setSummary(
                    Preferences.getPasswordEncodingPref(prefs));
            break;
        }
        case Preferences.PREF_PASSWD_EXPIRY_NOTIF: {
            PasswdExpiryNotifPref pref =
                    Preferences.getPasswdExpiryNotifPref(prefs);
            Resources res = getResources();
            itsPasswdExpiryNotifPref.setSummary(pref.getDisplayName(res));
            break;
        }
        case Preferences.PREF_PASSWD_DEFAULT_SYMS: {
            String val = Preferences.getPasswdDefaultSymbolsPref(prefs);
            itsPasswdDefaultSymsPref.setSummary(
                    getString(R.string.symbols_used_by_default, val));
            break;
        }
        case Preferences.PREF_RECORD_SORT_ORDER: {
            RecordSortOrderPref pref =
                    Preferences.getRecordSortOrderPref(prefs);
            Resources res = getResources();
            itsRecordSortOrderPref.setSummary(pref.getDisplayName(res));
            break;
        }
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference)
    {
        switch (preference.getKey()) {
        case Preferences.PREF_DEF_FILE: {
            Intent intent = new Intent(Intent.ACTION_CREATE_SHORTCUT, null,
                                       getContext(),
                                       LauncherFileShortcuts.class);
            intent.putExtra(LauncherFileShortcuts.EXTRA_IS_DEFAULT_FILE, true);
            startActivityForResult(intent, REQUEST_DEFAULT_FILE);
            return true;
        }
        case Preferences.PREF_PASSWD_CLEAR_ALL_NOTIFS: {
            Activity act = getActivity();
            PasswdSafeApp app = (PasswdSafeApp)act.getApplication();
            app.getNotifyMgr().clearAllNotifications(act);
            return true;
        }
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode) {
        case REQUEST_DEFAULT_FILE: {
            if (resultCode != Activity.RESULT_OK) {
                break;
            }
            Intent val = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
            String prefVal = (val != null) ? val.getData().toString() : null;
            SharedPreferences.Editor editor =
                    itsDefFilePref.getSharedPreferences().edit();
            editor.putString(Preferences.PREF_DEF_FILE, prefVal);
            editor.apply();
            break;
        }
        default: {
            super.onActivityResult(requestCode, resultCode, data);
            break;
        }
        }
    }

    /**
     * Background task to resolve the default file URI and set the preference's
     * summary
     */
    private final class DefaultFileResolver
            extends AsyncTask<Uri, Void, PasswdFileUri>
    {
        @Override
        protected PasswdFileUri doInBackground(Uri... params)
        {
            Uri uri = params[0];
            if (uri == null) {
                return null;
            }
            return new PasswdFileUri(uri, getContext());
        }

        @Override
        protected void onPostExecute(PasswdFileUri result)
        {
            String summary;
            if (result == null) {
                summary = getString(R.string.none);
            } else {
                summary = result.getIdentifier(getContext(), false);
            }
            itsDefFilePref.setSummary(summary);
        }
    }
}
