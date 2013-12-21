/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import java.io.File;

import android.accounts.Account;
import android.database.sqlite.SQLiteDatabase;

/**
 * The Provider interface encapsulates a service that provides files which are
 * synchronized
 */
public interface Provider
{
    /** Get the account for the named provider */
    public Account getAccount(String acctName);

    /** Check whether a provider can be added */
    public void checkProviderAdd(SQLiteDatabase db)
            throws Exception;

    /** Cleanup a provider when deleted */
    public void cleanupOnDelete(String acctName);

    /** Update a provider's sync frequency */
    public void updateSyncFreq(Account acct, int freq);

    /** Sync a provider */
    public void sync(Account acct,
                     DbProvider provider,
                     SQLiteDatabase db,
                     boolean manual,
                     SyncLogRecord logrec)
            throws Exception;

    /** Insert a local file */
    public long insertLocalFile(long providerId,
                                String title,
                                SQLiteDatabase db)
            throws Exception;

    /** Update a local file */
    public void updateLocalFile(DbFile file,
                                String localFileName,
                                File localFile,
                                SQLiteDatabase db)
            throws Exception;

    /** Delete a local file */
    public void deleteLocalFile(DbFile file, SQLiteDatabase db)
            throws Exception;
}
