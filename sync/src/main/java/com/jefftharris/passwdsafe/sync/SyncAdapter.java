/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;

import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.Provider;
import com.jefftharris.passwdsafe.sync.lib.ProviderSync;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncHelper;

/**
 * The SyncAdapter class syncs files in a background thread
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter
{
    private Context itsContext = null;
    private static final String TAG = "SyncAdapter";

    /** Constructor */
    public SyncAdapter(Context context)
    {
        super(context, true);
        itsContext = context;
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
        boolean manual = (extras != null) &&
                extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL);

        // TODO: need a method to acquire/release providers to prevent deletes

        DbProvider dbprovider;
        Provider providerImpl = null;
        SyncDb syncDb = SyncDb.acquire();
        try {
            SQLiteDatabase db = syncDb.getDb();
            dbprovider = SyncHelper.getDbProviderForAcct(account, db);
            if (dbprovider != null) {
                providerImpl = ProviderFactory.getProvider(dbprovider.itsType,
                                                           itsContext);
            }
        } finally {
            syncDb.release();
        }

        if (providerImpl == null) {
            Log.e(TAG, "onPerformSync no provider for " + account);
            return;
        }
        new ProviderSync(account, dbprovider,
                         providerImpl, itsContext).sync(manual);
    }
}
