/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Activity for PasswdSafe preferences
 */
public class PreferencesActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
        setTitle(PasswdSafeApp.getAppTitle(getString(R.string.preferences),
                                           this));
    }
}
