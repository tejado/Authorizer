/**
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.gdriveplay;

import java.io.File;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.util.Pair;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableNotifiedException;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.NewAccountTask;
import com.jefftharris.passwdsafe.sync.lib.Provider;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;

/**
 * The GDrivePlayProvider class encapsulates a sync provider for Google Drive
 * using the Google Play service.
 */
public class GDrivePlayProvider implements Provider
{
    private static final String TAG = "GDrivePlayProvider";

    private final Context itsContext;
    private AccountLinker itsAcctLinker;

    /** Constructor */
    public GDrivePlayProvider(Context ctx)
    {
        itsContext = ctx;
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#init()
     */
    @Override
    public void init()
    {
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#fini()
     */
    @Override
    public void fini()
    {
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#startAccountLink(android.app.Activity, int)
     */
    @Override
    public void startAccountLink(FragmentActivity activity, int requestCode)
    {
        if (itsAcctLinker != null) {
            itsAcctLinker.disconnect();
        }

        itsAcctLinker = new AccountLinker(activity, requestCode, itsContext);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#finishAccountLink(int, android.content.Intent, android.net.Uri)
     */
    @Override
    public NewAccountTask finishAccountLink(int activityResult,
                                            Intent activityData,
                                            Uri providerAcctUri)
    {
        if (itsAcctLinker == null) {
            return null;
        }
        Pair<Boolean, NewAccountTask> rc = itsAcctLinker.handleActivityResult(
                activityResult, activityData, providerAcctUri);
        if (rc.first) {
            itsAcctLinker.disconnect();
            itsAcctLinker = null;
        }
        return rc.second;
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#unlinkAccount()
     */
    @Override
    public void unlinkAccount()
    {
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#isAccountAuthorized()
     */
    @Override
    public boolean isAccountAuthorized()
    {
        return false;
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#getAccount(java.lang.String)
     */
    @Override
    public Account getAccount(String acctName)
    {
        return new Account(acctName, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#checkProviderAdd(android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void checkProviderAdd(SQLiteDatabase db) throws Exception
    {
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#cleanupOnDelete(java.lang.String)
     */
    @Override
    public void cleanupOnDelete(String acctName) throws Exception
    {
        // TODO play: cleanup unlinkAccount vs. cleanupOnDelete
        String scope = "oauth2:" + Scopes.DRIVE_FILE;
        try {
            String token = GoogleAuthUtil.getTokenWithNotification(
                    itsContext, acctName, scope, null);
            PasswdSafeUtil.dbginfo(TAG, "Remove token for %s, scope: %s",
                                   acctName, scope);
            if (token != null) {
                GoogleAuthUtil.invalidateToken(itsContext, token);
            }

            // TODO play: plus.Account.revokeAccessAndDisconnect(client)
        } catch (UserRecoverableNotifiedException e) {
            throw e;
        } catch (Exception e) {
            PasswdSafeUtil.dbginfo(TAG, e, "No auth token for %s, scope: %s",
                                   acctName, scope);
        }
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#updateSyncFreq(android.accounts.Account, int)
     */
    @Override
    public void updateSyncFreq(Account acct, int freq)
    {
        // TODO play: implement updateSyncFreq
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#requestSync(boolean)
     */
    @Override
    public void requestSync(boolean manual)
    {
        // TODO play: implement requestSync
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#sync(android.accounts.Account, com.jefftharris.passwdsafe.sync.lib.DbProvider, android.database.sqlite.SQLiteDatabase, boolean, com.jefftharris.passwdsafe.sync.lib.SyncLogRecord)
     */
    @Override
    public void sync(Account acct,
                     DbProvider provider,
                     SQLiteDatabase db,
                     boolean manual,
                     boolean full,
                     SyncLogRecord logrec) throws Exception
    {
        // TODO play: implement sync
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#insertLocalFile(long, java.lang.String, android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public long insertLocalFile(long providerId, String title, SQLiteDatabase db)
            throws Exception
    {
        // TODO play: implement insertLocalFile
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
        // TODO play: implement updateLocalFile
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#deleteLocalFile(com.jefftharris.passwdsafe.sync.lib.DbFile, android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void deleteLocalFile(DbFile file, SQLiteDatabase db)
            throws Exception
    {
        // TODO play: implement deleteLocalFile
    }

    /** Create a GDrive API client */
    public static GoogleApiClient createClient(
            Context ctx,
            String acctName,
            GoogleApiClient.ConnectionCallbacks connCbs,
            GoogleApiClient.OnConnectionFailedListener connFailedListener)
    {
        GoogleApiClient.Builder builder =
                new GoogleApiClient.Builder(ctx)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .setAccountName(acctName)
                .addConnectionCallbacks(connCbs)
                .addOnConnectionFailedListener(connFailedListener);
        return builder.build();
    }
}
