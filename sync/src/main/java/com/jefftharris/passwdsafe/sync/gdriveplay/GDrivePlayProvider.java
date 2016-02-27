/**
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.gdriveplay;

import java.io.File;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableNotifiedException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.jefftharris.passwdsafe.lib.ApiCompat;
import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.sync.SyncAdapter;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.NewAccountTask;
import com.jefftharris.passwdsafe.sync.lib.Provider;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;

/**
 * The GDrivePlayProvider class encapsulates a sync provider for Google Drive
 * using the Google Play service.
 */
public class GDrivePlayProvider
        implements Provider,
                   GoogleApiClient.ConnectionCallbacks,
                   GoogleApiClient.OnConnectionFailedListener
{
    private static final String TAG = "GDrivePlayProvider";

    private static final String PREF_ACCOUNT_NAME = "gdrivePlayAccountName";

    private final Context itsContext;
    private AccountLinker itsAcctLinker;
    private GoogleApiClient itsClient;
    private boolean itsIsPendingAdd = false;

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
        updateAcct();
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#fini()
     */
    @Override
    public void fini()
    {
        if ((itsClient != null) && itsClient.isConnected()) {
            itsClient.disconnect();
        }
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
        Pair<Boolean, String> rc = itsAcctLinker.handleActivityResult(
                activityResult, activityData);
        if (rc.first) {
            itsAcctLinker.disconnect();
            itsAcctLinker = null;

            setAcctName(rc.second);
            updateAcct();

            if (rc.second == null) {
                return null;
            }

            // TODO play: use updated play services and acct id for unique value
            // with name as display name
            return new NewAccountTask(providerAcctUri, rc.second,
                                      ProviderType.GDRIVE_PLAY,
                                      false, itsContext)
            {
                @Override
                protected void doAccountUpdate(ContentResolver cr)
                {
                    itsIsPendingAdd = true;
                    try {
                        super.doAccountUpdate(cr);
                    } finally {
                        itsIsPendingAdd = false;
                    }

                }
            };
        } else {
            return null;
        }
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
        return (itsClient != null) && itsClient.isConnected();
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
        if (itsIsPendingAdd) {
            return;
        }

        Account acct = getAccount(acctName);
        setAcctName(null);
        updateAcct();

        String scope = "oauth2:" + Drive.SCOPE_FILE;
        try {
            String token = GoogleAuthUtil.getTokenWithNotification(
                    itsContext, acct, scope, null);
            PasswdSafeUtil.dbginfo(TAG, "Remove token for %s, scope: %s",
                                   acctName, scope);
            if (token != null) {
                GoogleAuthUtil.clearToken(itsContext, token);
            }
        } catch (UserRecoverableNotifiedException e) {
            Log.e(TAG, "Recoverable error", e);
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
        PasswdSafeUtil.dbginfo(TAG, "updateSyncFreq acct %s, freq %d",
                               acct, freq);
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
    @Override
    public void requestSync(boolean manual)
    {
        PasswdSafeUtil.dbginfo(TAG, "requestSync manual %b", manual);
        Account acct = getAccount(getAcctName());
        Bundle extras = new Bundle();
        extras.putBoolean(SyncAdapter.SYNC_EXTRAS_FULL, true);
        ApiCompat.requestManualSync(acct, PasswdSafeContract.CONTENT_URI,
                                    extras);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#sync(android.accounts.Account, com.jefftharris.passwdsafe.sync.lib.DbProvider, android.database.sqlite.SQLiteDatabase, boolean, com.jefftharris.passwdsafe.sync.lib.SyncLogRecord)
     */
    @Override
    public void sync(Account acct,
                     DbProvider provider,
                     SQLiteDatabase db,
                     boolean full,
                     SyncLogRecord logrec) throws Exception
    {
        PasswdSafeUtil.dbginfo(TAG, "sync");
        GDrivePlaySyncer sync = new GDrivePlaySyncer(acct.name, provider, db,
                                                     logrec, itsContext);
        sync.sync();
        // TODO play: implement sync
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#insertLocalFile(long, java.lang.String, android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public long insertLocalFile(long providerId, String title, SQLiteDatabase db)
            throws SQLException
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
                                SQLiteDatabase db)
    {
        // TODO play: implement updateLocalFile
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#deleteLocalFile(com.jefftharris.passwdsafe.sync.lib.DbFile, android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void deleteLocalFile(DbFile file, SQLiteDatabase db)
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
        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(ctx)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .setAccountName(acctName);
        if (connCbs != null) {
            builder.addConnectionCallbacks(connCbs);
        }
        if (connFailedListener != null) {
            builder.addOnConnectionFailedListener(connFailedListener);
        }
        return builder.build();
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        PasswdSafeUtil.dbginfo(TAG, "onConnected: %s", bundle);
        notifyProviderChange();

        DriveFolder root = Drive.DriveApi.getRootFolder(itsClient);
        root.listChildren(itsClient).setResultCallback(
                new ResultCallback<DriveApi.MetadataBufferResult>()
                {
                    @Override
                    public void onResult(
                            @NonNull DriveApi.MetadataBufferResult result)
                    {
                        try {
                            MetadataBuffer buf = result.getMetadataBuffer();
                            if (buf != null) {
                                try {
                                    PasswdSafeUtil.dbginfo(TAG, "root count: %d",
                                                           buf.getCount());
                                    for (Metadata meta: buf) {
                                        PasswdSafeUtil.dbginfo(TAG, "root item: %s", meta);
                                    }
                                } finally {
                                    buf.release();
                                }
                            }
                        } finally {
                            result.release();
                        }
                    }
                });
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        PasswdSafeUtil.dbginfo(TAG, "onConnectionSuspended %d", i);
        notifyProviderChange();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        PasswdSafeUtil.dbginfo(TAG, "onConnectionFailed %s", connectionResult);
        notifyProviderChange();
    }

    private void notifyProviderChange()
    {
        ContentResolver cr = itsContext.getContentResolver();
        cr.notifyChange(PasswdSafeContract.Providers.CONTENT_URI, null);
    }

    private synchronized String getAcctName()
    {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(itsContext);
        return prefs.getString(PREF_ACCOUNT_NAME, null);
    }

    private synchronized void setAcctName(String acctName)
    {
        PasswdSafeUtil.dbginfo(TAG, "setAcctName %s", acctName);
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(itsContext);
        prefs.edit().putString(PREF_ACCOUNT_NAME, acctName).apply();
    }

    private synchronized void updateAcct()
    {
        String acctName = getAcctName();
        if (acctName != null) {
            if ((itsClient == null) ||
                (!itsClient.isConnected() && !itsClient.isConnecting())) {
                itsClient = createClient(itsContext, acctName, this, this);
                itsClient.connect();
            }
        } else {
            if ((itsClient != null) &&
                (itsClient.isConnected() || itsClient.isConnecting())) {
                itsClient.disconnect();
                itsClient = null;
            }
        }
    }
}
