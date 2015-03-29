/*
 * Copyright (©) 2013-2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.gdrive;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.sync.SyncApp;
import com.jefftharris.passwdsafe.sync.SyncUpdateHandler;
import com.jefftharris.passwdsafe.sync.lib.AbstractProvider;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.NewAccountTask;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;

/**
 * The GDriveProvider class encapsulates Google Drive
 */
public class GDriveProvider extends AbstractProvider
{
    public static final String ABOUT_FIELDS = "largestChangeId";
    public static final String FILE_FIELDS =
            "id,title,mimeType,labels(trashed),fileExtension,modifiedDate," +
            "downloadUrl,md5Checksum,parents(id,isRoot)";
    public static final String FOLDER_MIME =
            "application/vnd.google-apps.folder";

    private static final String TAG = "GDriveProvider";

    private final Context itsContext;


    /** Constructor */
    public GDriveProvider(Context ctx)
    {
        itsContext = ctx;
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#init()
     */
    public void init()
    {
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#fini()
     */
    public void fini()
    {
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#startAccountLink(android.app.Activity, int)
     */
    public void startAccountLink(FragmentActivity activity, int requestCode)
    {
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#finishAccountLink()
     */
    public NewAccountTask finishAccountLink(int activityResult,
                                            Intent activityData,
                                            Uri acctProviderUri)
    {
        if (activityData == null) {
            return null;
        }

        Bundle b = activityData.getExtras();
        String accountName = b.getString(AccountManager.KEY_ACCOUNT_NAME);
        Log.i(TAG, "Selected account: " + accountName);
        if (TextUtils.isEmpty(accountName)) {
            return null;
        }
        return new NewAccountTask(acctProviderUri, accountName,
                                  ProviderType.GDRIVE, false, itsContext);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#unlinkAccount()
     */
    public void unlinkAccount()
    {
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#isAccountAuthorized()
     */
    public boolean isAccountAuthorized()
    {
        return false;
    }


    /** Get the account for the named provider */
    @Override
    public Account getAccount(String acctName)
    {
        GoogleAccountManager acctMgr = new GoogleAccountManager(itsContext);
        return acctMgr.getAccountByName(acctName);
    }


    /** Check whether a provider can be added */
    @Override
    public void checkProviderAdd(SQLiteDatabase db)
            throws Exception
    {
    }


    /** Cleanup a provider when deleted */
    @Override
    public void cleanupOnDelete(String acctName)
    {
        GDriveSyncer.reset();
        try {
            GoogleAccountCredential credential =
                    GDriveSyncer.getAcctCredential(itsContext);
            String token = GoogleAuthUtil.getToken(itsContext, acctName,
                                                   credential.getScope());
            PasswdSafeUtil.dbginfo(TAG, "Remove token for %s", acctName);
            if (token != null) {
                GoogleAuthUtil.invalidateToken(itsContext, token);
            }
        } catch (Exception e) {
            PasswdSafeUtil.dbginfo(TAG, e, "No auth token for %s", acctName);
        }
    }


    /** Update a provider's sync frequency */
    @Override
    public void updateSyncFreq(Account acct, int freq)
    {
        if (acct != null) {
            ContentResolver.removePeriodicSync(acct,
                                               PasswdSafeContract.AUTHORITY,
                                               new Bundle());
            ContentResolver.setSyncAutomatically(
                    acct, PasswdSafeContract.AUTHORITY, false);
            if (freq > 0) {
                ContentResolver.setSyncAutomatically(
                        acct, PasswdSafeContract.AUTHORITY, true);
                ContentResolver.addPeriodicSync(acct,
                                                PasswdSafeContract.AUTHORITY,
                                                new Bundle(), freq);
            }
        }
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#requestSync(boolean)
     */
    public void requestSync(boolean manual)
    {
    }


    /** Sync a provider */
    @Override
    public void sync(Account acct, DbProvider provider,
                     SQLiteDatabase db,
                     boolean manual, boolean full,
                     SyncLogRecord logrec) throws Exception
    {
        GDriveSyncer sync = new GDriveSyncer(acct, provider, db,
                                             full, logrec, itsContext);
        SyncUpdateHandler.GDriveState syncState =
                SyncUpdateHandler.GDriveState.OK;
        try {
            syncState = sync.sync();
        } finally {
            SyncApp.get(itsContext).updateGDriveSyncState(syncState);
        }
    }
}
