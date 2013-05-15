/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

/**
 * The GDriveSyncAdapter class syncs files in a background thread
 */
public class GDriveSyncAdapter extends AbstractThreadedSyncAdapter
{
    private Context itsContext = null;
    private static final String TAG = "GDriveSyncAdapter";

    /** Constructor */
    public GDriveSyncAdapter(Context context, boolean autoInitialize)
    {
        super(context, autoInitialize);
        itsContext = context;
        Log.i(TAG, "GDriveSyncAdapter ctor");
    }

    /* (non-Javadoc)
     * @see android.content.AbstractThreadedSyncAdapter#onPerformSync(android.accounts.Account, android.os.Bundle, java.lang.String, android.content.ContentProviderClient, android.content.SyncResult)
     */
    @Override
    public void onPerformSync(Account account,
                              Bundle extras,
                              String authority,
                              ContentProviderClient provider,
                              SyncResult syncResult)
    {
        GDriveSyncer syncer = new GDriveSyncer(itsContext, provider, account);
        try {
            syncer.performSync();
        } finally {
            syncer.close();
        }
    }
}
