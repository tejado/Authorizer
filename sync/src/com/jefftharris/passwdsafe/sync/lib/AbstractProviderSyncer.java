/*
 * Copyright (©) 2014-2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.jefftharris.passwdsafe.lib.PasswdSafeContract;

/**
 * Common implementation of the sync operation for a provider
 */
public abstract class AbstractProviderSyncer<ProviderClientT>
        extends ProviderSyncer<ProviderClientT>
{
    /** Constructor */
    public AbstractProviderSyncer(ProviderClientT providerClient,
                                  DbProvider provider, SQLiteDatabase db,
                                  SyncLogRecord logrec, Context ctx,
                                  String tag)
    {
        super(providerClient, provider, db, logrec, ctx, tag);
    }


    /** Sync the provider */
    public final void sync()
            throws Exception
    {
        itsLogrec.setFullSync(true);
        List<AbstractSyncOper<ProviderClientT>> opers = null;

        try {
            itsDb.beginTransaction();
            opers = performSync();
            itsDb.setTransactionSuccessful();
        } catch (Exception e) {
            throw updateSyncException(e);
        } finally {
            itsDb.endTransaction();
        }

        if (opers != null) {
            for (AbstractSyncOper<ProviderClientT> oper: opers) {
                try {
                    itsLogrec.addEntry(oper.getDescription(itsContext));
                    oper.doOper(itsProviderClient, itsContext);
                    try {
                        itsDb.beginTransaction();
                        oper.doPostOperUpdate(itsDb, itsContext);
                        itsDb.setTransactionSuccessful();
                    } finally {
                        itsDb.endTransaction();
                    }
                } catch (Exception e) {
                    e = updateSyncException(e);
                    Log.e(itsTag, "Sync error for file " + oper.getFile(), e);
                    itsLogrec.addFailure(e);
                }
            }
        }

        itsContext.getContentResolver().notifyChange(
                PasswdSafeContract.CONTENT_URI, null, false);
    }

    /** Perform a sync of the files */
    protected abstract List<AbstractSyncOper<ProviderClientT>> performSync()
            throws Exception;

    /** Update an exception thrown during syncing */
    protected Exception updateSyncException(Exception e)
    {
        return e;
    }
}
