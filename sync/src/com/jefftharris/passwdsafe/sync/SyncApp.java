/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.sync.lib.Provider;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;

/**
 *  Application class for PasswdSafe Sync
 */
public class SyncApp extends Application
{
    private static final String TAG = "SyncApp";

    private Handler itsHandler = null;
    private SyncUpdateHandler itsSyncUpdateHandler;
    private SyncUpdateHandler.GDriveState itsSyncGDriveState =
            SyncUpdateHandler.GDriveState.OK;

    /* (non-Javadoc)
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate()
    {
        PasswdSafeUtil.dbginfo(TAG, "onCreate");
        super.onCreate();

        SyncDb.initializeDb(getApplicationContext());
        itsHandler = new Handler(Looper.getMainLooper());

        getDbxProvider().init();

        // TODO: kick off gdrive sync on startup?
    }


    /* (non-Javadoc)
     * @see android.app.Application#onTerminate()
     */
    @Override
    public void onTerminate()
    {
        PasswdSafeUtil.dbginfo(TAG, "onTerminate");
        getDbxProvider().fini();
        SyncDb.finalizeDb();
        super.onTerminate();
    }


    /** Get the Sync application */
    public static SyncApp get(Context ctx)
    {
        return (SyncApp)ctx.getApplicationContext();
    }


    /** Set the callback for sync updates */
    public void setSyncUpdateHandler(SyncUpdateHandler handler)
    {
        itsSyncUpdateHandler = handler;
        if (itsSyncUpdateHandler != null) {
            itsSyncUpdateHandler.updateGDriveState(itsSyncGDriveState);
        }
    }

    /** Update the state of a Google Drive sync.  This method can be
     * called from a background thread. */
    public void updateGDriveSyncState(final SyncUpdateHandler.GDriveState state)
    {
        itsHandler.post(new Runnable() {
            @Override
            public void run()
            {
                itsSyncGDriveState = state;
                if (itsSyncUpdateHandler != null) {
                    itsSyncUpdateHandler.updateGDriveState(state);
                }
            }
        });
    }


    /** Get the Dropbox provider */
    private Provider getDbxProvider()
    {
        return ProviderFactory.getProvider(ProviderType.DROPBOX, this);
    }
}
