/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import java.util.List;
import java.util.Arrays;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.TextUtils;
import android.util.Log;

import net.tjado.authorizer.OutputInterface;
import net.tjado.passwdsafe.file.PasswdFileUri;
import net.tjado.passwdsafe.file.PasswdPolicy;
import net.tjado.passwdsafe.lib.ApiCompat;
import net.tjado.passwdsafe.pref.FileBackupPref;
import net.tjado.passwdsafe.pref.FileTimeoutPref;
import net.tjado.passwdsafe.pref.PasswdExpiryNotifPref;
import net.tjado.passwdsafe.pref.RecordSortOrderPref;
import net.tjado.passwdsafe.view.ConfirmPromptDialog;

import org.pwsafe.lib.file.PwsFile;

import java.io.File;

/**
 * Fragment for PasswdSafe preferences
 */
public class PreferencesFragment extends PreferenceFragmentCompat
        implements ConfirmPromptDialog.Listener,
                   SharedPreferences.OnSharedPreferenceChangeListener,
                   Preference.OnPreferenceClickListener
{
    /** Listener interface for owning activity */
    public interface Listener
    {
        /** Update the view for preferences */
        void updateViewPreferences();
    }

    /** Action confirmed via ConfirmPromptDialog */
    private enum ConfirmAction
    {
        CLEAR_ALL_NOTIFS,
        CLEAR_ALL_SAVED
    }

    private static final int REQUEST_DEFAULT_FILE = 0;
    private static final int REQUEST_CLEAR_ALL_NOTIFS = 1;
    private static final int REQUEST_CLEAR_ALL_SAVED = 2;

    private static final String CONFIRM_ARG_ACTION = "action";

    private static final String TAG = "PreferencesFragment";

    private Listener itsListener;
    private EditTextPreference itsFileDirPref;
    private Preference itsDefFilePref;
    private ListPreference itsFileClosePref;
    private ListPreference itsUsbkbdLangPref;
    private ListPreference itsFileBackupPref;
    private ListPreference itsPasswdEncPref;
    private ListPreference itsPasswdExpiryNotifPref;
    private EditTextPreference itsPasswdDefaultSymsPref;
    private ListPreference itsRecordSortOrderPref;

    /**
     * Create a new instance
     */
    public static PreferencesFragment newInstance()
    {
        return new PreferencesFragment();
    }

    @Override
    public void onAttach(Context ctx)
    {
        super.onAttach(ctx);
        if (ctx instanceof Listener) {
            itsListener = (Listener)ctx;
        }
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s)
    {
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences prefs = Preferences.getSharedPrefs(getContext());
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
        Preference clearAllSavedPref =
                findPreference(Preferences.PREF_PASSWD_CLEAR_ALL_SAVED);
        clearAllSavedPref.setOnPreferenceClickListener(this);

        itsRecordSortOrderPref = (ListPreference)
                findPreference(Preferences.PREF_RECORD_SORT_ORDER);
        itsRecordSortOrderPref.setEntries(
                RecordSortOrderPref.getDisplayNames(res));
        itsRecordSortOrderPref.setEntryValues(RecordSortOrderPref.getValues());
        onSharedPreferenceChanged(prefs, Preferences.PREF_RECORD_SORT_ORDER);

        itsUsbkbdLangPref = (ListPreference)
                findPreference(Preferences.PREF_USBKBD_LANGUAGE);
        itsUsbkbdLangPref.setEntries(R.array.usbkbd_languages_titels);
        itsUsbkbdLangPref.setEntryValues(R.array.usbkbd_languages_values);
        onSharedPreferenceChanged(prefs, Preferences.PREF_USBKBD_LANGUAGE);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (itsListener != null) {
            itsListener.updateViewPreferences();
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        itsListener = null;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        SharedPreferences prefs = Preferences.getSharedPrefs(getContext());
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        switch (key) {
        case Preferences.PREF_FILE_DIR: {
            File pref = Preferences.getFileDirPref(prefs);
            if (pref == null) {
                itsFileDirPref.setText(null);
                itsFileDirPref.setSummary(null);
                break;
            }
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
            new DefaultFileResolver(
                    Preferences.getDefFilePref(prefs)).execute();
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
        case Preferences.PREF_DISPLAY_THEME_LIGHT: {
            ApiCompat.recreateActivity(getActivity());
            break;
        }
        case Preferences.PREF_DISPLAY_LIST_TREEVIEW: {
            ApiCompat.recreateActivity(getActivity());
            break;
        }
        case Preferences.PREF_USBKBD_ENABLE: {
            ApiCompat.recreateActivity(getActivity());
            break;
        }
        case Preferences.PREF_USBKBD_LANGUAGE: {
            OutputInterface.Language pref = Preferences.getUsbkbdLanguagePref(prefs);

            List<String> myOptions = Arrays.asList((getResources().getStringArray(R.array.usbkbd_languages_values)));

            int value = myOptions.indexOf(pref.name());

            itsUsbkbdLangPref.setSummary(getResources().getStringArray(R.array.usbkbd_languages_titels)[value]);
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
            Bundle confirmArgs = new Bundle();
            confirmArgs.putString(CONFIRM_ARG_ACTION,
                                  ConfirmAction.CLEAR_ALL_NOTIFS.name());
            DialogFragment dlg = app.getNotifyMgr().createClearAllPrompt(
                    act, confirmArgs);
            dlg.setTargetFragment(this, REQUEST_CLEAR_ALL_NOTIFS);
            dlg.show(getFragmentManager(), "clearNotifsConfirm");
            return true;
        }
        case Preferences.PREF_PASSWD_CLEAR_ALL_SAVED: {
            Bundle confirmArgs = new Bundle();
            confirmArgs.putString(CONFIRM_ARG_ACTION,
                                  ConfirmAction.CLEAR_ALL_SAVED.name());
            ConfirmPromptDialog dlg = ConfirmPromptDialog.newInstance(
                    getString(R.string.clear_all_saved_passwords),
                    getString(R.string.erase_all_saved_passwords),
                    getString(R.string.clear), confirmArgs);
            dlg.setTargetFragment(this, REQUEST_CLEAR_ALL_SAVED);
            dlg.show(getFragmentManager(), "clearSavedConfirm");
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
            setDefFilePref((val != null) ? val.getData().toString() : null);
            break;
        }
        default: {
            super.onActivityResult(requestCode, resultCode, data);
            break;
        }
        }
    }

    @Override
    public void promptCanceled()
    {
    }

    @Override
    public void promptConfirmed(Bundle confirmArgs)
    {
        ConfirmAction action;
        try {
            action = ConfirmAction.valueOf(
                    confirmArgs.getString(CONFIRM_ARG_ACTION));
        } catch (Exception e) {
            return;
        }

        switch (action) {
        case CLEAR_ALL_NOTIFS: {
            Activity act = getActivity();
            PasswdSafeApp app = (PasswdSafeApp)act.getApplication();
            app.getNotifyMgr().handleClearAllConfirmed();
            break;
        }
        case CLEAR_ALL_SAVED: {
            SavedPasswordsMgr passwdMgr = new SavedPasswordsMgr(getContext());
            passwdMgr.removeAllSavedPasswords();
            break;
        }
        }
    }

    /**
     * Set the default file preference
     */
    private void setDefFilePref(String prefVal)
    {
        SharedPreferences.Editor editor =
                itsDefFilePref.getSharedPreferences().edit();
        editor.putString(Preferences.PREF_DEF_FILE, prefVal);
        editor.apply();
    }

    /**
     * Background task to resolve the default file URI and set the preference's
     * summary
     */
    private final class DefaultFileResolver
            extends AsyncTask<Void, Void, PasswdFileUri>
    {
        private PasswdFileUri.Creator itsUriCreator;

        /**
         * Constructor
         */
        public DefaultFileResolver(Uri fileUri)
        {
            if (fileUri != null) {
                itsUriCreator = new PasswdFileUri.Creator(fileUri,
                                                          getContext());
            }
        }

        @Override
        protected final void onPreExecute()
        {
            super.onPreExecute();
            if (itsUriCreator != null) {
                itsUriCreator.onPreExecute();
            }
        }

        @Override
        protected PasswdFileUri doInBackground(Void... params)
        {
            return (itsUriCreator != null) ?
                   itsUriCreator.finishCreate() : null;
        }

        @Override
        protected void onPostExecute(PasswdFileUri result)
        {
            if (!isResumed()) {
                return;
            }
            String summary;
            if (result == null) {
                summary = getString(R.string.none);
                if (itsUriCreator != null) {
                    Throwable resolveEx = itsUriCreator.getResolveEx();
                    if (resolveEx != null) {
                        Log.e(TAG, "Error resolving default file", resolveEx);
                        summary =
                                getString(R.string.file_not_found_perm_denied);
                        setDefFilePref(null);
                    }
                }
            } else {
                summary = result.getIdentifier(getContext(), false);
            }
            itsDefFilePref.setSummary(summary);
        }
    }
}
