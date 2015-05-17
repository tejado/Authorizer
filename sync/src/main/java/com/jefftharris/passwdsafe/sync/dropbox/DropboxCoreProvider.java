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
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AbstractSession;
import com.dropbox.client2.session.AppKeyPair;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncTimerProvider;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.NewAccountTask;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;

import java.util.List;

/**
 *  The DropboxCoreProvider class encapsulates Dropbox
 */
public class DropboxCoreProvider extends AbstractSyncTimerProvider
{
    // TODO migrate auth token
    // TODO oauth 1 tokens from migration
    // TODO: remove old dbx classes and gradle exclusions

    private static final String DROPBOX_SYNC_APP_KEY = "jaafb7iju45c60f";
    private static final String DROPBOX_SYNC_APP_SECRET = "gabkj5758t39urh";
    private static final String PREF_OAUTH2_TOKEN = "dropboxOAuth2Token";
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
        boolean authorized = isAccountAuthorized();
        PasswdSafeUtil.dbginfo(TAG, "sync authorized: %b", authorized);
        if (authorized) {
            new DropboxCoreSyncer(itsApi, provider, db,
                                  logrec, getContext()).sync();
        }
    }


    /** Update the Dropbox account client based on availability of
     *  authentication information */
    private synchronized void updateDropboxAcct()
    {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());

        AbstractSession session = itsApi.getSession();
        String authToken = prefs.getString(PREF_OAUTH2_TOKEN, null);
        if (authToken != null) {
            session.setOAuth2AccessToken(authToken);

            itsUserId = prefs.getString(PREF_USER_ID, null);
            if (itsUserId != null) {
                try {
                    updateProviderSyncFreq(itsUserId);
                    requestSync(false);
                } catch (Exception e) {
                    Log.e(TAG, "updateDropboxAcct failure", e);
                }
            }
        } else {
            itsUserId = null;
            session.unlink();
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
        editor.apply();
    }
}
