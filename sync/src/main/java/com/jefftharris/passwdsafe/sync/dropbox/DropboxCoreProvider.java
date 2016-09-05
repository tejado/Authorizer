/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
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
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.InvalidAccessTokenException;
import com.dropbox.core.android.Auth;
import com.dropbox.core.android.AuthActivity;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;
import com.jefftharris.passwdsafe.lib.ObjectHolder;
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
    private static final String DROPBOX_SYNC_APP_KEY = "jaafb7iju45c60f";
    private static final String PREF_MIGRATE_TOKEN = "dropboxMigrateToken";
    private static final String PREF_OAUTH2_TOKEN = "dropboxOAuth2Token";
    private static final String PREF_OATH_KEY = "dropboxOAuthKey";
    private static final String PREF_OATH_SECRET = "dropboxOAuthSecret";
    private static final String PREF_USER_ID = "dropboxUserId";

    private static final String TAG = "DropboxCoreProvider";

    private DbxClientV2 itsClient;
    private String itsUserId = null;
    private boolean itsIsPendingAdd = false;
    private final ArrayList<TokenRevokeTask> itsRevokeTasks = new ArrayList<>();

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
        updateDropboxAcct();
    }

    @Override
    public void fini()
    {
        super.fini();
        for (TokenRevokeTask task: itsRevokeTasks) {
            task.cancel(true);
        }
        itsRevokeTasks.clear();
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
        AuthActivity.result = null;
        Auth.startOAuth2Authentication(activity, DROPBOX_SYNC_APP_KEY);
    }


    @Override
    public NewAccountTask finishAccountLink(int activityResult,
                                            Intent activityData,
                                            Uri providerAcctUri)
    {
        String authToken = Auth.getOAuth2Token();
        if (authToken == null) {
            PasswdSafeUtil.dbginfo(TAG, "finishAccountLink auth failed");
            return null;
        }
        saveAuthData(authToken);
        updateDropboxAcct();

        // If user already exists, this is a re-authorization so don't trigger
        // a new account task
        if (itsUserId != null) {
            return null;
        }

        return new NewAccountTask(providerAcctUri, null, ProviderType.DROPBOX,
                                  false, getContext())
        {
            @Override
            protected void doAccountUpdate(ContentResolver cr)
            {
                itsIsPendingAdd = true;
                try {
                    FullAccount acct = itsClient.users().getCurrentAccount();
                    itsNewAcct = acct.getAccountId();
                    setUserId(itsNewAcct);
                    super.doAccountUpdate(cr);
                } catch (DbxException e) {
                    Log.e(TAG, "Error retrieving account", e);
                } finally {
                    itsIsPendingAdd = false;
                }
            }
        };
    }


    @Override
    public void unlinkAccount()
    {
        if (itsClient != null) {
            TokenRevokeTask task = new TokenRevokeTask();
            task.execute(itsClient);
            itsRevokeTasks.add(task);
        }
        saveAuthData(null);
        setUserId(null);
        updateDropboxAcct();
    }


    @Override
    public boolean isAccountAuthorized()
    {
        return (itsClient != null);
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
        if (!itsIsPendingAdd) {
            unlinkAccount();
        }
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
    public boolean checkSyncConnectivity(Account acct) throws Exception
    {
        final ObjectHolder<Boolean> online = new ObjectHolder<>(false);
        useDropboxService(new DropboxUser()
        {
            @Override
            public void useDropbox() throws Exception
            {
                DropboxCoreSyncer.getDisplayName(itsClient);
                online.set(true);
            }
        });
        return online.get();
    }

    @Override
    public void sync(Account acct,
                     final DbProvider provider,
                     final SQLiteDatabase db,
                     final SyncLogRecord logrec) throws Exception
    {
        useDropboxService(new DropboxUser()
        {
            @Override
            public void useDropbox() throws Exception
            {
                new DropboxCoreSyncer(itsClient, provider, db,
                                      logrec, getContext()).sync();
            }
        });
    }

    /** List files */
    public List<ProviderRemoteFile> listFiles(String path)
            throws DbxException
    {
        List<ProviderRemoteFile> files = new ArrayList<>();
        ListFolderResult result = itsClient.files().listFolder(path);
        do {
            for (Metadata child: result.getEntries()) {
                files.add(new DropboxCoreProviderFile(child));
            }

            if (result.getHasMore()) {
                result = itsClient.files()
                                  .listFolderContinue(result.getCursor());
            } else {
                result = null;
            }
        } while(result != null);

        return files;
    }

    /**
     * Interface for users of Dropbox
     */
    private interface DropboxUser
    {
        /**
         * Callback to use the client
         */
        void useDropbox() throws Exception;
    }

    /**
     * Use the Dropbox service
     */
    private void useDropboxService(DropboxUser user) throws Exception
    {
        try {
            boolean authorized = isAccountAuthorized();
            PasswdSafeUtil.dbginfo(TAG, "account authorized: %b", authorized);
            if (authorized) {
                user.useDropbox();
            }
        } catch (InvalidAccessTokenException e) {
            Log.e(TAG, "unlinked error", e);
            saveAuthData(null);
            updateDropboxAcct();
            throw e;

            // TODO: notification when providers fail to sync with auth error?
        }

    }

    /** Update the Dropbox account client based on availability of
     *  authentication information */
    private synchronized void updateDropboxAcct()
    {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());

        boolean haveAuth = false;
        String authToken = prefs.getString(PREF_OAUTH2_TOKEN, null);
        if (authToken != null) {
            itsClient = new DbxClientV2(new DbxRequestConfig("PasswdSafe"),
                                        authToken);
            haveAuth = true;
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
            itsClient = null;
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
            boolean didMigrate = false;

            try {
                Context appctx = getContext().getApplicationContext();
                String accts =
                        appctx.getSharedPreferences("dropbox-credentials",
                                                    Context.MODE_PRIVATE)
                              .getString("accounts", null);
                if (accts != null) {
                    JSONArray jsonAccounts = new JSONArray(accts);
                    if (jsonAccounts.length() > 0) {
                        JSONObject acct = jsonAccounts.getJSONObject(0);
                        String userId = acct.getString("userId");
                        PasswdSafeUtil.dbginfo(TAG, "migrate user: %s", userId);
                        editor.putString(PREF_USER_ID, userId);
                        didMigrate = true;
                    }
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

                    didMigrate = true;
                    String dirpfx = "/Apps/PasswdSafe Sync";
                    for (DbFile dbfile: SyncDb.getFiles(provider.itsId, db)) {
                        SyncDb.updateRemoteFile(
                                dbfile.itsId,
                                (dirpfx + dbfile.itsRemoteId).toLowerCase(),
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

            if (didMigrate) {
                NotifUtils.showNotif(NotifUtils.Type.DROPBOX_MIGRATED,
                                     getContext());
            }
        }
    }

    /**
     * Background task to revoke a token
     */
    private class TokenRevokeTask extends AsyncTask<DbxClientV2, Void, Void>
    {
        @Override
        protected Void doInBackground(DbxClientV2... clients)
        {
            PasswdSafeUtil.dbginfo(TAG, "revoking auth tokens");
            for (DbxClientV2 client: clients) {
                try {
                    client.auth().tokenRevoke();
                } catch (DbxException e) {
                    Log.e(TAG, "Error revoking auth token", e);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            itsRevokeTasks.remove(this);
        }
    }
}
