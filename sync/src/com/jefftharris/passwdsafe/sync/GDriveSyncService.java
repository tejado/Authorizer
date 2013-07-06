/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * The GDriveSyncService is a service that syncs password files from Google
 * Drive
 */
public class GDriveSyncService extends Service
{
    private static final Object itsLock = new Object();
    private static GDriveSyncAdapter itsSyncAdapter = null;

    /** Constructor */
    public GDriveSyncService()
    {
    }

    /* (non-Javadoc)
     * @see android.app.Service#onCreate()
     */
    @Override
    public void onCreate()
    {
        synchronized (itsLock) {
            if (itsSyncAdapter == null) {
                itsSyncAdapter = new GDriveSyncAdapter(getApplicationContext(),
                                                       true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return itsSyncAdapter.getSyncAdapterBinder();
    }
}
