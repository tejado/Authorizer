/*
 * Copyright (©) 2009-2010 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.File;

import org.pwsafe.lib.file.PwsFile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * The Preferences class defines the activity for managing preferences on the
 * application
 *
 * @author Jeff Harris
 */
public class Preferences extends PreferenceActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    public static final String INTENT_SCREEN = "screen";
    public static final String SCREEN_PASSWORD_OPTIONS = "passwordOptions";

    private EditTextPreference itsFileDirPref;
    private ListPreference itsDefFilePref;
    private ListPreference itsFileClosePref;
    private ListPreference itsPasswdEncPref;
    private ListPreference itsFontSizePref;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences);

        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        itsFileDirPref = (EditTextPreference)
            findPreference(PasswdSafeApp.PREF_FILE_DIR);
        itsDefFilePref = (ListPreference)
            findPreference(PasswdSafeApp.PREF_DEF_FILE);
        itsFileClosePref = (ListPreference)
            findPreference(PasswdSafeApp.PREF_FILE_CLOSE_TIMEOUT);

        itsFileDirPref.setDefaultValue(PasswdSafeApp.PREF_FILE_DIR_DEF);
        updateFileDirPrefs(PasswdSafeApp.getFileDirPref(prefs), prefs);

        onSharedPreferenceChanged(prefs, PasswdSafeApp.PREF_DEF_FILE);

        itsFileClosePref.setEntries(FileTimeoutPref.getDisplayNames());
        itsFileClosePref.setEntryValues(FileTimeoutPref.getValues());
        onSharedPreferenceChanged(prefs, PasswdSafeApp.PREF_FILE_CLOSE_TIMEOUT);

        itsPasswdEncPref = (ListPreference)
            findPreference(PasswdSafeApp.PREF_PASSWD_ENC);
        String[] charsets =
            PwsFile.ALL_PASSWORD_CHARSETS.toArray(new String[0]);
        itsPasswdEncPref.setEntries(charsets);
        itsPasswdEncPref.setEntryValues(charsets);
        itsPasswdEncPref.setDefaultValue(PasswdSafeApp.PREF_PASSWD_ENC_DEF);
        onSharedPreferenceChanged(prefs, PasswdSafeApp.PREF_PASSWD_ENC);

        onSharedPreferenceChanged(prefs, PasswdSafeApp.PREF_GEN_LENGTH);
        onSharedPreferenceChanged(prefs, PasswdSafeApp.PREF_GEN_HEX);

        itsFontSizePref = (ListPreference)
            findPreference(PasswdSafeApp.PREF_FONT_SIZE);
        itsFontSizePref.setEntries(FontSizePref.getDisplayNames());
        itsFontSizePref.setEntryValues(FontSizePref.getValues());
        onSharedPreferenceChanged(prefs, PasswdSafeApp.PREF_FONT_SIZE);

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
        if (key.equals(PasswdSafeApp.PREF_FILE_DIR)) {
            String pref = PasswdSafeApp.getFileDirPref(prefs);
            if (pref.length() == 0) {
                pref = PasswdSafeApp.PREF_FILE_DIR_DEF;
                itsFileDirPref.setText(pref);
            }
            itsDefFilePref.setValue(PasswdSafeApp.PREF_DEF_FILE_DEF);
            updateFileDirPrefs(pref, prefs);
        } else if (key.equals(PasswdSafeApp.PREF_DEF_FILE)) {
            itsDefFilePref.setSummary(
                defFileValueToEntry(PasswdSafeApp.getDefFilePref(prefs)));
        } else if (key.equals(PasswdSafeApp.PREF_FILE_CLOSE_TIMEOUT)) {
            itsFileClosePref.setSummary(
                PasswdSafeApp.getFileCloseTimeoutPref(prefs).getDisplayName());
        } else if (key.equals(PasswdSafeApp.PREF_PASSWD_ENC)) {
            itsPasswdEncPref.setSummary(
                PasswdSafeApp.getPasswordEncodingPref(prefs));
        } else if (key.equals(PasswdSafeApp.PREF_GEN_LENGTH)) {
            Preference pref = findPreference(PasswdSafeApp.PREF_GEN_LENGTH);
            pref.setSummary(
                Integer.toString(
                    PasswdSafeApp.getPasswordGenLengthPref(prefs)));
        } else if (key.equals(PasswdSafeApp.PREF_GEN_HEX)) {
            boolean isHex = PasswdSafeApp.getPasswordGenHexPref(prefs);
            for (String id: new String[] { PasswdSafeApp.PREF_GEN_LOWER,
                                           PasswdSafeApp.PREF_GEN_UPPER,
                                           PasswdSafeApp.PREF_GEN_DIGITS,
                                           PasswdSafeApp.PREF_GEN_SYMBOLS,
                                           PasswdSafeApp.PREF_GEN_EASY }) {
                Preference pref = findPreference(id);
                pref.setEnabled(!isHex);
            }
        } else if (key.equals(PasswdSafeApp.PREF_FONT_SIZE)) {
            itsFontSizePref.setSummary(
                PasswdSafeApp.getFontSizePref(prefs).getDisplayName());
        }
    }

    private final void updateFileDirPrefs(String summary,
                                          SharedPreferences prefs)
    {
        itsFileDirPref.setSummary(summary);

        File fileDir = new File(PasswdSafeApp.getFileDirPref(prefs));
        FileList.FileData[] files = FileList.getFiles(fileDir, false);
        String[] entries = new String[files.length + 1];
        String[] entryValues = new String[files.length + 1];
        entries[0] = PasswdSafeApp.PREF_DEF_FILE_NONE;
        entryValues[0] = PasswdSafeApp.PREF_DEF_FILE_DEF;
        for (int i = 0; i < files.length; ++i) {
            entries[i + 1] = files[i].toString();
            entryValues[i + 1] = entries[i + 1];
        }

        itsDefFilePref.setEntries(entries);
        itsDefFilePref.setEntryValues(entryValues);
    }

    private static String defFileValueToEntry(String value)
    {
        if (value.equals(PasswdSafeApp.PREF_DEF_FILE_DEF)) {
            return PasswdSafeApp.PREF_DEF_FILE_NONE;
        } else {
            return value;
        }
    }
}
