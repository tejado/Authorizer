/*
 * Copyright (©) 2013-2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.gdrive;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableNotifiedException;
import com.google.android.gms.common.AccountPicker;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.jefftharris.passwdsafe.lib.ApiCompat;
import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.sync.R;
import com.jefftharris.passwdsafe.sync.SyncAdapter;
import com.jefftharris.passwdsafe.sync.SyncApp;
import com.jefftharris.passwdsafe.sync.SyncUpdateHandler;
import com.jefftharris.passwdsafe.sync.lib.AbstractProvider;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.NewAccountTask;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;

import java.io.IOException;
import java.util.Collections;

/**
 * The GDriveProvider class encapsulates Google Drive
 */
public class GDriveProvider extends AbstractProvider
{
    public static final String ABOUT_FIELDS = "user";
    public static final String FILE_FIELDS =
            "id,name,mimeType,trashed,fileExtension,modifiedTime," +
            "md5Checksum,parents";
    public static final String FOLDER_MIME =
            "application/vnd.google-apps.folder";

    private static final String PREF_ACCOUNT_NAME = "gdriveAccountName";
    private static final String PREF_MIGRATION = "gdriveMigration";

    private static final int MIGRATION_V3API = 1;

    private static final String TAG = "GDriveProvider";

    private final Context itsContext;
    private String itsAccountName;

    // TODO: remove notion of full sync

    /** Constructor */
    public GDriveProvider(Context ctx)
    {
        itsContext = ctx;
    }


    @Override
    public void init()
    {
        checkMigration();
        updateAcct();
        requestSync(false);
    }


    @Override
    public void fini()
    {
    }

    @Override
    public void startAccountLink(FragmentActivity activity, int requestCode)
    {
        Intent intent = AccountPicker.newChooseAccountIntent(
                null, null,
                new String[] { GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE },
                true, null, null, null, null);
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            String msg = itsContext.getString(
                    R.string.google_acct_not_available);
            Log.e(TAG, msg, e);
            PasswdSafeUtil.showErrorMsg(msg, activity);
        }
    }

    @Override
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

        setAcctName(accountName);
        updateAcct();
        return new NewAccountTask(acctProviderUri, accountName,
                                  ProviderType.GDRIVE, false, itsContext);
    }

    @Override
    public void unlinkAccount()
    {
    }

    @Override
    public boolean isAccountAuthorized()
    {
        return !TextUtils.isEmpty(itsAccountName);
    }


    @Override
    public Account getAccount(String acctName)
    {
        GoogleAccountManager acctMgr = new GoogleAccountManager(itsContext);
        return acctMgr.getAccountByName(acctName);
    }

    @Override
    public void checkProviderAdd(SQLiteDatabase db)
            throws Exception
    {
    }

    @Override
    public void cleanupOnDelete(String acctName)
    {
        Account acct = getAccount(acctName);
        setAcctName(null);
        updateAcct();
        try {
            GoogleAccountCredential credential = getAcctCredential(itsContext);
            String token = GoogleAuthUtil.getTokenWithNotification(
                    itsContext, acct, credential.getScope(), null);
            PasswdSafeUtil.dbginfo(TAG, "Remove token for %s", acctName);
            if (token != null) {
                GoogleAuthUtil.clearToken(itsContext, token);
            }
        } catch (Exception e) {
            PasswdSafeUtil.dbginfo(TAG, e, "No auth token for %s", acctName);
        }
    }

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

    @Override
    public void requestSync(boolean manual)
    {
        PasswdSafeUtil.dbginfo(TAG, "requestSync manual %b", manual);
        if (isAccountAuthorized()) {
            Account acct = getAccount(itsAccountName);
            Bundle extras = new Bundle();
            extras.putBoolean(SyncAdapter.SYNC_EXTRAS_FULL, true);
            ApiCompat.requestManualSync(acct, PasswdSafeContract.CONTENT_URI,
                                        extras);
        }
    }

    @Override
    public void sync(Account acct, DbProvider provider,
                     SQLiteDatabase db,
                     boolean full,
                     SyncLogRecord logrec) throws Exception
    {
        Pair<Drive, String> driveService = getDriveService(acct, itsContext);
        
        GDriveSyncer sync = new GDriveSyncer(driveService.first, provider, db,
                                             logrec, itsContext);
        SyncUpdateHandler.GDriveState syncState =
                SyncUpdateHandler.GDriveState.OK;
        try {
            sync.sync();
            syncState = sync.getSyncState();
        } catch (UserRecoverableAuthIOException e) {
            PasswdSafeUtil.dbginfo(TAG, e, "Recoverable google auth error");
            GoogleAuthUtil.clearToken(itsContext, driveService.second);
            syncState = SyncUpdateHandler.GDriveState.AUTH_REQUIRED;
        } catch (GoogleAuthIOException e) {
            Log.e(TAG, "Google auth error", e);
            GoogleAuthUtil.clearToken(itsContext, driveService.second);
            throw e;
        } finally {
            SyncApp.get(itsContext).updateGDriveSyncState(syncState);
        }
    }

    /**
     * Set the account name
     */
    private synchronized void setAcctName(String acctName)
    {
        PasswdSafeUtil.dbginfo(TAG, "setAcctName %s", acctName);
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(itsContext);
        prefs.edit().putString(PREF_ACCOUNT_NAME, acctName).apply();
    }

    /**
     * Update the account from saved authentication information
     */
    private synchronized void updateAcct()
    {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(itsContext);
        itsAccountName = prefs.getString(PREF_ACCOUNT_NAME, null);
    }

    /**
     * Check whether any migrations are needed
     */
    private void checkMigration()
    {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(itsContext);
        int migration = prefs.getInt(PREF_MIGRATION, 0);

        if (migration < MIGRATION_V3API) {
            // Set the account name from the db provider
            SyncDb syncDb = SyncDb.acquire();
            try {
                SQLiteDatabase db = syncDb.beginTransaction();
                for (DbProvider provider: SyncDb.getProviders(db)) {
                    if (provider.itsType != ProviderType.GDRIVE) {
                        continue;
                    }

                    setAcctName(provider.itsAcct);
                }
                db.setTransactionSuccessful();
            } catch (SQLException e) {
                Log.e(TAG, "Error migrating account", e);
            } finally {
                syncDb.endTransactionAndRelease();
            }

            prefs.edit().putInt(PREF_MIGRATION, MIGRATION_V3API).apply();
        }
    }

    /** Get the Google account credential */
    private static GoogleAccountCredential getAcctCredential(Context ctx)
    {
        return GoogleAccountCredential.usingOAuth2(
                ctx, Collections.singletonList(DriveScopes.DRIVE));
    }

    /**
     * Retrieve a authorized service object to send requests to the Google
     * Drive API. On failure to retrieve an access token, a notification is
     * sent to the user requesting that authorization be granted for the
     * {@code https://www.googleapis.com/auth/drive} scope.
     *
     * @return An authorized service object and its auth token.
     */
    private static Pair<Drive, String> getDriveService(Account acct,
                                                       Context ctx)
    {
        Drive drive = null;
        String token = null;
        try {
            GoogleAccountCredential credential = getAcctCredential(ctx);
            credential.setBackOff(new ExponentialBackOff());
            credential.setSelectedAccountName(acct.name);

            token = GoogleAuthUtil.getTokenWithNotification(
                    ctx, acct, credential.getScope(),
                    null, PasswdSafeContract.AUTHORITY, null);

            Drive.Builder builder = new Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    JacksonFactory.getDefaultInstance(), credential);
            builder.setApplicationName(ctx.getString(R.string.app_name));
            drive = builder.build();
        } catch (UserRecoverableNotifiedException e) {
            // User notified
            PasswdSafeUtil.dbginfo(TAG, e, "User notified auth exception");
            try {
                GoogleAuthUtil.clearToken(ctx, null);
            } catch(Exception ioe) {
                Log.e(TAG, "getDriveService clear failure", e);
            }
        } catch (GoogleAuthException e) {
            // Unrecoverable
            Log.e(TAG, "Unrecoverable auth exception", e);
        }
        catch (IOException e) {
            // Transient
            PasswdSafeUtil.dbginfo(TAG, e, "Transient error");
        } catch (Exception e) {
            Log.e(TAG, "Token exception", e);
        }
        return new Pair<>(drive, token);
    }
}
