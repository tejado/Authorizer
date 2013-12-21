/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.dropbox;

import java.util.List;

import android.accounts.Account;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.dropbox.sync.android.DbxPath;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.Provider;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;

/**
 *  The DropboxProvider class encapsulates Dropbox
 */
public class DropboxProvider implements Provider
{
    private final Context itsContext;

    private static final String TAG = "DEX DropboxProvider";

    /** Constructor */
    public DropboxProvider(Context ctx)
    {
        PasswdSafeUtil.dbginfo(TAG, "DEX DBX PROVIDER");
        itsContext = ctx;
        SyncLogRecord logrec = new SyncLogRecord("ff", "gg", true);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.Provider#getAccount(java.lang.String)
     */
    public Account getAccount(String acctName)
    {
        return new Account(acctName, SyncDb.DROPBOX_ACCOUNT_TYPE);
    }


    /** Check whether a provider can be added */
    public void checkProviderAdd(SQLiteDatabase db)
            throws Exception
    {
        List<DbProvider> providers = SyncDb.getProviders(db);
        for (DbProvider provider: providers) {
            if (provider.itsType == ProviderType.DROPBOX) {
                throw new Exception("Only one Dropbox account allowed");
            }
        }
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.Provider#cleanupOnDelete(java.lang.String)
     */
    public void cleanupOnDelete(String acctName)
    {
        //SyncApp.get(itsContext).unlinkDropbox();
        PasswdSafeUtil.dbginfo(TAG, "cleanupOnDelete");
    }

    /** Update a provider's sync frequency */
    public void updateSyncFreq(Account acct, int freq)
    {
        PasswdSafeUtil.dbginfo(TAG, "updateSyncFreq");
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.Provider#sync(android.accounts.Account, com.jefftharris.passwdsafe.sync.SyncDb.DbProvider, android.database.sqlite.SQLiteDatabase, com.jefftharris.passwdsafe.sync.SyncLogRecord)
     */
    public void sync(Account acct,
                     DbProvider provider,
                     SQLiteDatabase db,
                     boolean manual,
                     SyncLogRecord logrec) throws Exception
    {
        PasswdSafeUtil.dbginfo(TAG, "sync");
    }


    /** Insert a local file */
    public long insertLocalFile(long providerId, String title,
                                SQLiteDatabase db)
            throws Exception
    {

        long fileId = SyncDb.addLocalFile(providerId, title,
                                          System.currentTimeMillis(), db);

        DbxPath path = new DbxPath(DbxPath.ROOT, title);
        SyncDb.updateRemoteFile(fileId, path.toString(), path.getName(), null,
                                -1, db);
        return fileId;
    }


    /** Update a local file */
    public synchronized void updateLocalFile(DbFile file,
                                             String localFileName,
                                             java.io.File localFile,
                                             SQLiteDatabase db)
            throws Exception
    {
        PasswdSafeUtil.dbginfo(TAG, "updateLocalFile");
    }


    /** Delete a local file */
    public void deleteLocalFile(DbFile file, SQLiteDatabase db)
            throws Exception
    {
        PasswdSafeUtil.dbginfo(TAG, "deleteLocalFile");
    }
}
