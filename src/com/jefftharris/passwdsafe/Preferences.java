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
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences);

        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);

        Preference pref = findPreference(PasswdSafeApp.PREF_FILE_DIR);
        pref.setDefaultValue(PasswdSafeApp.PREF_FILE_DIR_DEF);
        pref.setSummary(prefs.getString(PasswdSafeApp.PREF_FILE_DIR,
                                        PasswdSafeApp.PREF_FILE_DIR_DEF));
        pref.setOnPreferenceChangeListener(
            new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference,
                                              Object newValue)
            {
                // TODO jeff: reset to default on empty string??
                preference.setSummary(newValue.toString());
                return true;
            }
        });

        /*
        ListPreference listPref = (ListPreference)
            findPreference(PasswdSafeApp.PREF_FILE_CLOSE_TIMEOUT);
        listPref.setEntries(PasswdSafeApp.PREF_FILE_CLOSE_ENTRIES);
        listPref.setEntryValues(PasswdSafeApp.PREF_FILE_CLOSE_ENTRY_VALUES);
        */
    }
}
