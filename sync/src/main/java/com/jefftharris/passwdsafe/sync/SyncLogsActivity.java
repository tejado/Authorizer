/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

/**
 *  Activity to show the sync logs fragment
 */
public class SyncLogsActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle args)
    {
        super.onCreate(args);
        setContentView(R.layout.activity_sync_logs);

        if (args == null) {
            SyncLogsFragment logs = new SyncLogsFragment();
            logs.setArguments(getIntent().getExtras());
            FragmentManager mgr = getSupportFragmentManager();
            mgr.beginTransaction().replace(R.id.contents, logs).commit();
        }
    }
}
