/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.dropbox;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AbstractSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncTimerProvider;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.NewAccountTask;
import com.jefftharris.passwdsafe.sync.lib.NotifUtils;
import com.jefftharris.passwdsafe.sync.lib.ProviderRemoteFile;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 *  The DropboxCoreProvider class encapsulates Dropbox
 */
public class DropboxCoreProvider extends AbstractSyncTimerProvider
{
    // TODO: remove old dbx classes and gradle exclusions

    private static final String DROPBOX_SYNC_APP_KEY = "jaafb7iju45c60f";
    private static final String DROPBOX_SYNC_APP_SECRET = "gabkj5758t39urh";
    private static final String PREF_MIGRATE_TOKEN = "dropboxMigrateToken";
    private static final String PREF_OAUTH2_TOKEN = "dropboxOAuth2Token";
    private static final String PREF_OATH_KEY = "dropboxOAuthKey";
    private static final String PREF_OATH_SECRET = "dropboxOAuthSecret";
    private static final String PREF_USER_ID = "dropboxUserId";

    private static final String TAG = "DropboxCoreProvider";

    private DropboxAPI<AndroidAuthSession> itsApi;
    private String itsUserId = null;

    /** Constructor */
    public DropboxCoreProvider(Context ctx)
    {
        super(ProviderType.DROPBOX, ctx, TAG);
    }


    @Override
    public void init()
    {
        super.init();
        doMigration();
        itsApi = new DropboxAPI<>(new AndroidAuthSession(
                new AppKeyPair(DROPBOX_SYNC_APP_KEY, DROPBOX_SYNC_APP_SECRET)));
        updateDropboxAcct();
    }


    @Override
    protected String getAccountUserId()
    {
        return itsUserId;
    }


    @Override
    public void startAccountLink(FragmentActivity activity, int requestCode)
    {
        if (isAccountAuthorized()) {
            unlinkAccount();
        }
        itsApi.getSession().startOAuth2Authentication(activity);
    }


    @Override
    public NewAccountTask finishAccountLink(int activityResult,
                                            Intent activityData,
                                            Uri providerAcctUri)
    {
        AndroidAuthSession session = itsApi.getSession();
        if (!session.authenticationSuccessful()) {
            PasswdSafeUtil.dbginfo(TAG, "finishAccountLink auth failed");
            return null;
        }

        try {
            session.finishAuthentication();
            saveAuthData(session.getOAuth2AccessToken());
            updateDropboxAcct();
            if (itsUserId != null) {
                return null;
            }
            return new NewAccountTask(providerAcctUri, null,
                                      ProviderType.DROPBOX, false, getContext())
            {
                @Override
                protected void doAccountUpdate(ContentResolver cr)
                {
                    try {
                        DropboxAPI.Account acct = itsApi.accountInfo();
                        itsNewAcct = Long.toString(acct.uid);
                        setUserId(itsNewAcct);
                        super.doAccountUpdate(cr);
                    } catch (DropboxException e) {
                        Log.e(TAG, "Error retrieving account", e);
                    }
                }
            };
        } catch (Exception e) {
            Log.e(TAG, "Error authenticating", e);
            return null;
        }
    }


    @Override
    public void unlinkAccount()
    {
        saveAuthData(null);
        setUserId(null);
        updateDropboxAcct();
    }


    @Override
    public boolean isAccountAuthorized()
    {
        return itsApi.getSession().isLinked();
    }


    @Override
    public Account getAccount(String acctName)
    {
        return new Account(acctName, SyncDb.DROPBOX_ACCOUNT_TYPE);
    }


    @Override
    public void checkProviderAdd(SQLiteDatabase db) throws Exception
    {
        List<DbProvider> providers = SyncDb.getProviders(db);
        for (DbProvider provider: providers) {
            if (provider.itsType == ProviderType.DROPBOX) {
                throw new Exception("Only one Dropbox account allowed");
            }
        }
    }


    @Override
    public void cleanupOnDelete(String acctName) throws Exception
    {
        unlinkAccount();
    }


    @Override
    public void requestSync(boolean manual)
    {
        boolean authorized = isAccountAuthorized();
        PasswdSafeUtil.dbginfo(TAG, "requestSync authorized: %b", authorized);
        if (authorized) {
            doRequestSync(manual);
        }
    }


    @Override
    public void sync(Account acct,
                     DbProvider provider,
                     SQLiteDatabase db,
                     boolean full,
                     SyncLogRecord logrec) throws Exception
    {
        try {
            boolean authorized = isAccountAuthorized();
            PasswdSafeUtil.dbginfo(TAG, "sync authorized: %b", authorized);
            if (authorized) {
                new DropboxCoreSyncer(itsApi, provider, db,
                                      logrec, getContext()).sync();
            }
        } catch (DropboxUnlinkedException e) {
            Log.e(TAG, "unlinked error", e);
            saveAuthData(null);
            updateDropboxAcct();
            throw e;
        } catch (DropboxServerException e) {
            if (e.error == DropboxServerException._401_UNAUTHORIZED) {
                Log.e(TAG, "unauthorized error", e);
                saveAuthData(null);
                updateDropboxAcct();
            }
            throw e;
        }
    }


    /** List files */
    public List<ProviderRemoteFile> listFiles(String path)
            throws DropboxException
    {

        DropboxAPI.Entry pathEntry = itsApi.metadata(path, 0, null, true, null);
        List<ProviderRemoteFile> files =
                new ArrayList<>(pathEntry.contents.size());
        for (DropboxAPI.Entry child: pathEntry.contents) {
            files.add(new DropboxCoreProviderFile(child));
        }

        return files;
    }


    /** Update the Dropbox account client based on availability of
     *  authentication information */
    private synchronized void updateDropboxAcct()
    {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());

        AbstractSession session = itsApi.getSession();
        boolean haveAuth = false;
        String authToken = prefs.getString(PREF_OAUTH2_TOKEN, null);
        if (authToken != null) {
            session.setOAuth2AccessToken(authToken);
            haveAuth = true;
        } else {
            String authKey = prefs.getString(PREF_OATH_KEY, null);
            String authSecret = prefs.getString(PREF_OATH_SECRET, null);
            if ((authKey != null) && (authSecret != null)) {
                session.setAccessTokenPair(new AccessTokenPair(authKey,
                                                               authSecret));
                haveAuth = true;
            } else {
                session.unlink();
            }
        }

        if (haveAuth) {
            itsUserId = prefs.getString(PREF_USER_ID, null);
            if (itsUserId != null) {
                try {
                    updateProviderSyncFreq(itsUserId);
                } catch (Exception e) {
                    Log.e(TAG, "updateDropboxAcct failure", e);
                }
            }
            requestSync(false);
        } else {
            itsUserId = null;
            updateSyncFreq(null, 0);
        }

        PasswdSafeUtil.dbginfo(TAG, "init auth %b", isAccountAuthorized());
    }


    /** Update the account's user ID */
    private synchronized void setUserId(String user)
    {
        PasswdSafeUtil.dbginfo(TAG, "updateUserId: %s", user);
        itsUserId = user;

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_USER_ID, itsUserId);
        editor.apply();
    }


    /** Save or clear authentication data */
    private synchronized void saveAuthData(String oauth2Token)
    {
        PasswdSafeUtil.dbginfo(TAG, "saveAuthData: %b", oauth2Token);
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = prefs.edit();
        if (oauth2Token != null) {
            editor.putString(PREF_OAUTH2_TOKEN, oauth2Token);
        } else {
            editor.remove(PREF_OAUTH2_TOKEN);
        }
        editor.remove(PREF_OATH_KEY);
        editor.remove(PREF_OATH_SECRET);
        editor.apply();
    }


    /** Migrate from previous Dropbox */
    private void doMigration()
    {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());

        boolean migrate = prefs.getBoolean(PREF_MIGRATE_TOKEN, true);
        if (migrate) {
            PasswdSafeUtil.dbginfo(TAG, "doMigration");
            SharedPreferences.Editor editor = prefs.edit();

            try {
                Context appctx = getContext().getApplicationContext();
                JSONArray jsonAccounts = new JSONArray(
                        appctx.getSharedPreferences("dropbox-credentials",
                                                    Context.MODE_PRIVATE)
                              .getString("accounts", null));
                if (jsonAccounts.length() > 0) {
                    JSONObject acct = jsonAccounts.getJSONObject(0);
                    String userId = acct.getString("userId");
                    PasswdSafeUtil.dbginfo(TAG, "migrate user: %s", userId);
                    editor.putString(PREF_USER_ID, userId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error migrating token", e);
            }

            editor.putBoolean(PREF_MIGRATE_TOKEN, false);
            editor.apply();

            SyncDb syncDb = SyncDb.acquire();
            try {
                SQLiteDatabase db = syncDb.getDb();
                for (DbProvider provider: SyncDb.getProviders(db)) {
                    if (provider.itsType != ProviderType.DROPBOX) {
                        continue;
                    }

                    String dirpfx = "/Apps/PasswdSafe Sync";
                    for (DbFile dbfile: SyncDb.getFiles(provider.itsId, db)) {
                        SyncDb.updateRemoteFile(
                                dbfile.itsId, dirpfx + dbfile.itsRemoteId,
                                dbfile.itsRemoteTitle,
                                dirpfx + dbfile.itsRemoteFolder,
                                dbfile.itsRemoteModDate, dbfile.itsRemoteHash,
                                db);
                    }
                }
            } catch (SQLException e) {
                Log.e(TAG, "Error migrating files", e);
            } finally {
                syncDb.release();
            }

            NotifUtils.showNotif(NotifUtils.Type.DROPBOX_MIGRATED,
                                 getContext());
        }
    }
}
