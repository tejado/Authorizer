/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.owncloud;

import java.io.File;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncTimerProvider;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.NewAccountTask;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;

/**
 *  Implements a provider for the ownCloud service
 */
public class OwncloudProvider extends AbstractSyncTimerProvider
{
    private static final String TAG = "OwncloudProvider";

    /** Constructor */
    public OwncloudProvider(Context ctx)
    {
        super(ProviderType.OWNCLOUD, ctx, TAG);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#startAccountLink(android.app.Activity, int)
     */
    @Override
    public void startAccountLink(Activity activity, int requestCode)
    {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#finishAccountLink(int, android.content.Intent, android.net.Uri)
     */
    @Override
    public NewAccountTask finishAccountLink(int activityResult,
                                            Intent activityData,
                                            Uri providerAcctUri)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#unlinkAccount()
     */
    @Override
    public void unlinkAccount()
    {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#isAccountAuthorized()
     */
    @Override
    public boolean isAccountAuthorized()
    {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#getAccount(java.lang.String)
     */
    @Override
    public Account getAccount(String acctName)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#checkProviderAdd(android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void checkProviderAdd(SQLiteDatabase db) throws Exception
    {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#cleanupOnDelete(java.lang.String)
     */
    @Override
    public void cleanupOnDelete(String acctName) throws Exception
    {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#requestSync(boolean)
     */
    @Override
    public void requestSync(boolean manual)
    {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#sync(android.accounts.Account, com.jefftharris.passwdsafe.sync.lib.DbProvider, android.database.sqlite.SQLiteDatabase, boolean, boolean, com.jefftharris.passwdsafe.sync.lib.SyncLogRecord)
     */
    @Override
    public void sync(Account acct,
                     DbProvider provider,
                     SQLiteDatabase db,
                     boolean manual,
                     boolean full,
                     SyncLogRecord logrec) throws Exception
    {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#insertLocalFile(long, java.lang.String, android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public long insertLocalFile(long providerId, String title, SQLiteDatabase db)
                                                                                 throws Exception
    {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#updateLocalFile(com.jefftharris.passwdsafe.sync.lib.DbFile, java.lang.String, java.io.File, android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void updateLocalFile(DbFile file,
                                String localFileName,
                                File localFile,
                                SQLiteDatabase db) throws Exception
    {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#deleteLocalFile(com.jefftharris.passwdsafe.sync.lib.DbFile, android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void deleteLocalFile(DbFile file, SQLiteDatabase db)
                                                               throws Exception
    {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractSyncTimerProvider#getAccountUserId()
     */
    @Override
    protected String getAccountUserId()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
