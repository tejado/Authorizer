/*
 * Copyright (©) 2009-2010 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.content.SharedPreferences;
import android.os.Bundle;
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
    private Preference itsFileDirPref;
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

        itsFileDirPref = findPreference(PasswdSafeApp.PREF_FILE_DIR);
        itsFileDirPref.setDefaultValue(PasswdSafeApp.PREF_FILE_DIR_DEF);
        onSharedPreferenceChanged(prefs, PasswdSafeApp.PREF_FILE_DIR);

        itsFileClosePref = (ListPreference)
            findPreference(PasswdSafeApp.PREF_FILE_CLOSE_TIMEOUT);
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
        if (key.equals(PasswdSafeApp.PREF_FILE_DIR)) {
            String pref = prefs.getString(PasswdSafeApp.PREF_FILE_DIR,
                                          PasswdSafeApp.PREF_FILE_DIR_DEF);
            if (pref.length() == 0) {
                pref = PasswdSafeApp.PREF_FILE_DIR_DEF;
                SharedPreferences.Editor edit = prefs.edit();
                edit.putString(PasswdSafeApp.PREF_FILE_DIR, pref);
                edit.commit();
            }

            itsFileDirPref.setSummary(pref);
        } else if (key.equals(PasswdSafeApp.PREF_FILE_CLOSE_TIMEOUT)) {
            itsFileClosePref.setSummary(
                fileCloseValueToEntry(
                    PasswdSafeApp.getFileCloseTimeoutPref(prefs)));
        }
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
}
