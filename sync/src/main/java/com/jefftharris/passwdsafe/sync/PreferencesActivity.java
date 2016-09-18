/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;

/**
 * Preferences activity
 */
public class PreferencesActivity extends AppCompatActivity
{
    /**
     * Fragment for the preferences
     */
    public static final class PreferencesFragment
            extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState,
                                        String rootKey)
        {
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        if (savedInstanceState == null) {
            PreferencesFragment frag = new PreferencesFragment();
            frag.setArguments(getIntent().getExtras());
            FragmentManager fragMgr = getSupportFragmentManager();
            fragMgr.beginTransaction().replace(R.id.contents, frag).commit();
        }
    }
}
