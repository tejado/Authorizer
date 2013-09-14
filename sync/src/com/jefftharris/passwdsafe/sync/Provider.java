/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import java.io.File;

import android.accounts.Account;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.jefftharris.passwdsafe.lib.ProviderType;

/**
 * The Provider interface encapsulates a service that provides files which are
 * synchronized
 */
public abstract class Provider
{
    /** Get the account for the named provider */
    public abstract Account getAccount(String acctName);

    /** Check whether a provider can be added */
    public abstract void checkProviderAdd(SQLiteDatabase db)
            throws Exception;

    /** Cleanup a provider when deleted */
    public abstract void cleanupOnDelete(String acctName);

    /** Update a provider's sync frequency */
    public abstract void updateSyncFreq(Account acct, int freq);

    /** Sync a provider */
    public abstract void sync(Account acct, DbProvider provider,
                              SQLiteDatabase db,
                              boolean manual, SyncLogRecord logrec)
            throws Exception;

    /** Insert a local file */
    public abstract long insertLocalFile(long providerId, String title,
                                         SQLiteDatabase db)
            throws Exception;

    /** Update a local file */
    public abstract void updateLocalFile(DbFile file,
                                         String localFileName,
                                         File localFile,
                                         SQLiteDatabase db)
            throws Exception;

    /** Delete a local file */
    public abstract void deleteLocalFile(DbFile file, SQLiteDatabase db)
            throws Exception;

    /** Get the provider implementation for the type */
    public static Provider getProvider(ProviderType type, Context ctx)
    {
        switch (type) {
        case GDRIVE: {
            return new GDriveProvider(ctx);
        }
        case DROPBOX: {
            return new DropboxProvider(ctx);
        }
        }
        return null;
    }
}
