/*
 * Copyright (©) 2009-2010 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.File;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * The Preferences class defines the activity for managing preferences on the
 * application
 *
 * @author Jeff Harris
 */
public class Preferences extends PreferenceActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private EditTextPreference itsFileDirPref;
    private ListPreference itsDefFilePref;
    private ListPreference itsFileClosePref;

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

        itsFileClosePref.setEntries(PasswdSafeApp.PREF_FILE_CLOSE_ENTRIES);
        itsFileClosePref.setEntryValues(
            PasswdSafeApp.PREF_FILE_CLOSE_ENTRY_VALUES);
        onSharedPreferenceChanged(prefs, PasswdSafeApp.PREF_FILE_CLOSE_TIMEOUT);
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
        Log.i("foo", "pref changed key: " + key);
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
                fileCloseValueToEntry(
                    PasswdSafeApp.getFileCloseTimeoutPref(prefs)));
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

    private static String fileCloseValueToEntry(String value)
    {
        for (int i = 0;
             i < PasswdSafeApp.PREF_FILE_CLOSE_ENTRY_VALUES.length;
             ++i) {
            if (PasswdSafeApp.PREF_FILE_CLOSE_ENTRY_VALUES[i].equals(value)) {
                return PasswdSafeApp.PREF_FILE_CLOSE_ENTRIES[i];
            }
        }
        return "Unknown";
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
