/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

/**
 *  Activity to show the sync logs fragment
 */
public class SyncLogsActivity extends FragmentActivity
{
    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle args)
    {
        super.onCreate(args);

        if (args == null) {
            SyncLogsFragment logs = new SyncLogsFragment();
            logs.setArguments(getIntent().getExtras());
            FragmentManager mgr = getSupportFragmentManager();
            mgr.beginTransaction().add(android.R.id.content,logs).commit();
        }
    }
}
