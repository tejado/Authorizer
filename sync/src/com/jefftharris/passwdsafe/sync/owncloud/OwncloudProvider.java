/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.owncloud;

import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.AccountPicker;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncTimerProvider;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.NewAccountTask;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;
import com.owncloud.android.lib.common.accounts.AccountTypeUtils;

/**
 *  Implements a provider for the ownCloud service
 */
public class OwncloudProvider extends AbstractSyncTimerProvider
{
    private static final String PREF_AUTH_ACCOUNT = "owncloudAccount";

    private static final String TAG = "OwncloudProvider";

    private String itsAccountName = null;
    private String itsUserName = null;
    private Uri itsUri = null;
    private boolean itsIsSyncAuthError= false;

    /** Constructor */
    public OwncloudProvider(Context ctx)
    {
        super(ProviderType.OWNCLOUD, ctx, TAG);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#init()
     */
    @Override
    public void init()
    {
        super.init();
        updateOwncloudAcct();
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#startAccountLink(android.app.Activity, int)
     */
    @Override
    public void startAccountLink(Activity activity, int requestCode)
    {
        Intent intent = AccountPicker.newChooseAccountIntent(
                null, null, new String[] { SyncDb.OWNCLOUD_ACCOUNT_TYPE },
                true, null, null, null, null);
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Error starting account picker", e);
            PasswdSafeUtil.showErrorMsg(e.getMessage(), activity);
        }
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#finishAccountLink(int, android.content.Intent, android.net.Uri)
     */
    @Override
    public NewAccountTask finishAccountLink(int activityResult,
                                            Intent activityData,
                                            Uri providerAcctUri)
    {
        String accountName = null;
        do {
            if ((activityResult != Activity.RESULT_OK) ||
                    (activityData == null)) {
                break;
            }

            Bundle b = activityData.getExtras();
            accountName = b.getString(AccountManager.KEY_ACCOUNT_NAME);
            Log.i(TAG, "Selected account: " + accountName);
            if (TextUtils.isEmpty(accountName)) {
                accountName = null;
                break;
            }
        } while(false);

        saveAuthData(accountName);
        updateOwncloudAcct();

        if (accountName == null) {
            return null;
        }
        return new NewAccountTask(providerAcctUri, accountName,
                ProviderType.OWNCLOUD, getContext())
        {
            @Override
            protected void doAccountUpdate(ContentResolver cr)
            {
                Activity act = getActivity();
                String authToken = OwncloudSyncer.getAuthToken(
                        getAccount(itsNewAcct), act, act);
                if (authToken != null) {
                    super.doAccountUpdate(cr);
                }
            }
        };
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#unlinkAccount()
     */
    @Override
    public void unlinkAccount()
    {
        saveAuthData(null);
        updateOwncloudAcct();
        AccountManager acctMgr = AccountManager.get(getContext());
        acctMgr.invalidateAuthToken(
                SyncDb.OWNCLOUD_ACCOUNT_TYPE,
                AccountTypeUtils.getAuthTokenTypePass(
                        SyncDb.OWNCLOUD_ACCOUNT_TYPE));
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#isAccountAuthorized()
     */
    @Override
    public boolean isAccountAuthorized()
    {
        return (itsAccountName != null) && !itsIsSyncAuthError;
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#getAccount(java.lang.String)
     */
    @Override
    public Account getAccount(String acctName)
    {
        return new Account(acctName, SyncDb.OWNCLOUD_ACCOUNT_TYPE);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#checkProviderAdd(android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void checkProviderAdd(SQLiteDatabase db) throws Exception
    {
        List<DbProvider> providers = SyncDb.getProviders(db);
        for (DbProvider provider: providers) {
            if (provider.itsType == ProviderType.OWNCLOUD) {
                throw new Exception("Only one ownCloud account allowed");
            }
        }
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#cleanupOnDelete(java.lang.String)
     */
    @Override
    public void cleanupOnDelete(String acctName) throws Exception
    {
        unlinkAccount();
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#requestSync(boolean)
     */
    @Override
    public void requestSync(boolean manual)
    {
        PasswdSafeUtil.dbginfo(TAG, "requestSync client: %b", itsAccountName);
        if (itsAccountName == null) {
            return;
        }
        doRequestSync(manual);
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
        PasswdSafeUtil.dbginfo(TAG, "sync client: %b", itsAccountName);
        if (itsAccountName == null) {
            return;
        }
        OwncloudSyncer syncer =
                new OwncloudSyncer(getAccount(itsAccountName), itsUserName,
                                   itsUri, provider, db, logrec, getContext());
        try {
            syncer.sync();
        } finally {
            itsIsSyncAuthError = !syncer.isAuthorized();
        }
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractSyncTimerProvider#getAccountUserId()
     */
    @Override
    protected String getAccountUserId()
    {
        return itsAccountName;
    }

    /** Update the ownCloud account client based on availability of
     *  authentication information. */
    private synchronized void updateOwncloudAcct()
    {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());

        itsAccountName = prefs.getString(PREF_AUTH_ACCOUNT, null);
        PasswdSafeUtil.dbginfo(TAG, "updateOwncloudAcct token %b",
                               itsAccountName);

        String userName = null;
        Uri uri = null;

        if (itsAccountName != null) {
            int pos = itsAccountName.indexOf('@');
            if (pos != -1) {
                userName = itsAccountName.substring(0, pos);
                Uri.Builder builder = new Uri.Builder();
                // TODO: can't hard-code this
                builder.scheme("https");
                builder.authority(itsAccountName.substring(pos + 1));
                builder.path("/owncloud");
                uri = builder.build();
            } else {
                itsAccountName = null;
            }
        }

        itsUserName = userName;
        itsUri = uri;
        if (itsUri != null) {
            try {
                updateProviderSyncFreq(itsAccountName);
                requestSync(false);
            } catch (Exception e) {
                Log.e(TAG, "updateOwncloudAcct failure", e);
            }
        } else {
            updateSyncFreq(null, 0);
        }
    }

    /** Save or clear the ownCloud authentication data */
    private synchronized void saveAuthData(String accountName)
    {
        PasswdSafeUtil.dbginfo(TAG, "saveAuthData: %b", accountName);
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = prefs.edit();
        if (accountName != null) {
            editor.putString(PREF_AUTH_ACCOUNT, accountName);
        } else {
            editor.remove(PREF_AUTH_ACCOUNT);
        }
        editor.commit();
    }
}
