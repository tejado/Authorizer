/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.box;

import java.io.File;
import java.util.List;

import android.accounts.Account;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.box.boxandroidlibv2.BoxAndroidClient;
import com.box.boxandroidlibv2.activities.OAuthActivity;
import com.box.boxandroidlibv2.dao.BoxAndroidOAuthData;
import com.box.boxjavalibv2.BoxClient;
import com.box.boxjavalibv2.authorization.OAuthRefreshListener;
import com.box.boxjavalibv2.dao.BoxOAuthToken;
import com.box.boxjavalibv2.dao.BoxObject;
import com.box.boxjavalibv2.dao.BoxUser;
import com.box.boxjavalibv2.exceptions.BoxJSONException;
import com.box.boxjavalibv2.interfaces.IAuthData;
import com.box.boxjavalibv2.requests.requestobjects.BoxDefaultRequestObject;
import com.box.boxjavalibv2.resourcemanagers.BoxUsersManager;
import com.box.restclientv2.exceptions.BoxSDKException;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncTimerProvider;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.NewAccountTask;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;

/**
 * Implements a provider for the Box.com service
 */
public class BoxProvider extends AbstractSyncTimerProvider
{
    private static final String BOX_CLIENT_ID =
            "rjgu7xf2ih5fvzb1cdhdnfmr4ncw1jes";
    private static final String BOX_CLIENT_SECRET =
            "nuHnpyoGIEYceudysLyBvcBsWSHJdXUy";

    private static final String PREF_AUTH_ACCESS_TOKEN = "boxAccessToken";
    private static final String PREF_AUTH_EXPIRES_IN = "boxExpiresIn";
    private static final String PREF_AUTH_REFRESH_TOKEN = "boxRefreshToken";
    private static final String PREF_AUTH_TOKEN_TYPE = "boxTokenType";
    private static final String PREF_AUTH_USER_ID = "boxUserId";

    private static final String TAG = "BoxProvider";

    private BoxAndroidClient itsClient;
    private String itsUserId;

    /** Constructor */
    public BoxProvider(Context ctx)
    {
        super(ProviderType.BOX, ctx, TAG);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#init()
     */
    @Override
    public void init()
    {
        super.init();
        updateBoxAcct();
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#startAccountLink(android.app.Activity, int)
     */
    @Override
    public void startAccountLink(Activity activity, int requestCode)
    {
        Intent intent = OAuthActivity.createOAuthActivityIntent(
                activity, BOX_CLIENT_ID, BOX_CLIENT_SECRET, false);
        intent.putExtra("redirecturl", "https://127.0.0.1");
        activity.startActivityForResult(intent, requestCode);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#finishAccountLink()
     */
    @Override
    public NewAccountTask finishAccountLink(int activityResult,
                                            Intent activityData,
                                            Uri acctProviderUri)
    {
        BoxAndroidOAuthData authdata = null;
        if (activityResult == Activity.RESULT_CANCELED) {
            String failure = null;
            if (activityData != null) {
                failure = activityData.getStringExtra(
                        OAuthActivity.ERROR_MESSAGE);
            }
            Log.e(TAG, "Box auth failed: " + failure);
        } else {
            authdata = activityData.getParcelableExtra(
                    OAuthActivity.BOX_CLIENT_OAUTH);
        }
        saveAuthData(authdata);
        updateBoxAcct();

        return new NewAccountTask(acctProviderUri, null, ProviderType.BOX,
                                  getContext())
        {
            /* (non-Javadoc)
             * @see com.jefftharris.passwdsafe.sync.lib.NewAccountTask#doAccountUpdate(android.content.ContentResolver)
             */
            @Override
            protected void doAccountUpdate(ContentResolver cr)
            {
                BoxUser user = getCurrentUser();
                if (user != null) {
                    itsNewAcct = user.getId();
                } else {
                    itsNewAcct = null;
                }
                setUserId(user);
                super.doAccountUpdate(cr);
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
        setUserId(null);
        updateBoxAcct();
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#isAccountAuthorized()
     */
    @Override
    public synchronized boolean isAccountAuthorized()
    {
        return (itsClient != null);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#getAccount(java.lang.String)
     */
    @Override
    public Account getAccount(String acctName)
    {
        return new Account(acctName, SyncDb.BOX_ACCOUNT_TYPE);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#checkProviderAdd(android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void checkProviderAdd(SQLiteDatabase db) throws Exception
    {
        List<DbProvider> providers = SyncDb.getProviders(db);
        for (DbProvider provider: providers) {
            if (provider.itsType == ProviderType.BOX) {
                throw new Exception("Only one Box account allowed");
            }
        }
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#cleanupOnDelete(java.lang.String)
     */
    @Override
    public void cleanupOnDelete(String acctName)
    {
        unlinkAccount();
    }

    @Override
    protected String getAccountUserId()
    {
        return itsUserId;
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#requestSync(boolean)
     */
    @Override
    public void requestSync(boolean manual)
    {
        PasswdSafeUtil.dbginfo(TAG, "requestSync client: %b", itsClient);
        if (itsClient == null) {
            return;
        }
        doRequestSync(manual);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#sync(android.accounts.Account, com.jefftharris.passwdsafe.sync.lib.DbProvider, android.database.sqlite.SQLiteDatabase, boolean, com.jefftharris.passwdsafe.sync.lib.SyncLogRecord)
     */
    @Override
    public void sync(Account acct,
                     DbProvider provider,
                     SQLiteDatabase db,
                     boolean manual,
                     SyncLogRecord logrec) throws Exception
    {
        PasswdSafeUtil.dbginfo(TAG, "sync client: %b", itsClient);
        if (itsClient == null) {
            return;
        }
        new BoxSyncer(itsClient, provider, db, logrec, getContext()).sync();
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#insertLocalFile(long, java.lang.String, android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public long insertLocalFile(long providerId, String title, SQLiteDatabase db)
                                                                                 throws Exception
    {
        long id = SyncDb.addLocalFile(providerId, title,
                                      System.currentTimeMillis(), db);
        requestSync(false);
        return id;
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
        SyncDb.updateLocalFile(file.itsId, localFileName,
                               file.itsLocalTitle, file.itsLocalFolder,
                               localFile.lastModified(), db);
        requestSync(false);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#deleteLocalFile(com.jefftharris.passwdsafe.sync.lib.DbFile, android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void deleteLocalFile(DbFile file, SQLiteDatabase db)
            throws Exception
    {
        SyncDb.updateLocalFileDeleted(file.itsId, db);
        requestSync(false);
    }

    /** Get the current Box user */
    private synchronized BoxUser getCurrentUser()
    {
        if ((itsClient == null) || !itsClient.isAuthenticated()) {
            return null;
        }
        BoxUsersManager userMgr = itsClient.getUsersManager();
        BoxDefaultRequestObject req = new BoxDefaultRequestObject();
        try {
            return userMgr.getCurrentUser(req);
        } catch (BoxSDKException e) {
            Log.e(TAG, "Failed to get user", e);
        }
        return null;
    }

    /** Update the Box account client based on availability of authentication
     *  information. */
    private synchronized void updateBoxAcct()
    {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());

        String accessToken = prefs.getString(PREF_AUTH_ACCESS_TOKEN, null);
        PasswdSafeUtil.dbginfo(TAG, "updateBoxAcct token %b", accessToken);
        if (accessToken != null) {
            int expiresIn = prefs.getInt(PREF_AUTH_EXPIRES_IN, 0);
            String refreshToken = prefs.getString(PREF_AUTH_REFRESH_TOKEN, null);
            String tokenType = prefs.getString(PREF_AUTH_TOKEN_TYPE, null);

            BoxAndroidOAuthData authdata = new BoxAndroidOAuthData();
            authdata.put(BoxOAuthToken.FIELD_ACCESS_TOKEN, accessToken);
            authdata.put(BoxOAuthToken.FIELD_EXPIRES_IN, expiresIn);
            authdata.put(BoxOAuthToken.FIELD_REFRESH_TOKEN, refreshToken);
            authdata.put(BoxOAuthToken.FIELD_TOKEN_TYPE, tokenType);
            itsClient = new BoxAndroidClient(BOX_CLIENT_ID, BOX_CLIENT_SECRET,
                                             null, null);
            itsClient.addOAuthRefreshListener(new OAuthRefreshListener()
            {
                @Override
                public void onRefresh(IAuthData authdata)
                {
                    saveAuthData((BoxOAuthToken) authdata);
                }
            });

            itsClient.authenticate(authdata);

            itsUserId = prefs.getString(PREF_AUTH_USER_ID, null);
            if (itsUserId != null) {
                try {
                    updateProviderSyncFreq(itsUserId);
                    requestSync(false);
                } catch (Exception e) {
                    Log.e(TAG, "updateBoxAcct failure", e);
                }
            }

        } else {
            itsClient = null;
            setUserId(null);
            updateSyncFreq(null, 0);
        }
    }

    /** Save or clear the Box authentication data */
    private synchronized void saveAuthData(BoxOAuthToken authData)
    {
        PasswdSafeUtil.dbginfo(TAG, "saveAuthData: %b", authData);
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = prefs.edit();
        if (authData != null) {
            editor.putString(PREF_AUTH_ACCESS_TOKEN, authData.getAccessToken());
            editor.putInt(PREF_AUTH_EXPIRES_IN, authData.getExpiresIn());
            editor.putString(PREF_AUTH_REFRESH_TOKEN,
                             authData.getRefreshToken());
            editor.putString(PREF_AUTH_TOKEN_TYPE, authData.getTokenType());
        } else {
            editor.remove(PREF_AUTH_ACCESS_TOKEN);
            editor.remove(PREF_AUTH_EXPIRES_IN);
            editor.remove(PREF_AUTH_REFRESH_TOKEN);
            editor.remove(PREF_AUTH_TOKEN_TYPE);
        }
        editor.commit();
    }

    /** Update the account's user ID */
    private synchronized void setUserId(BoxUser user)
    {
        PasswdSafeUtil.dbginfo(TAG, "updateUserId: %s",
                               boxToString(user, itsClient));

        itsUserId = (user != null) ? user.getId() : null;

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_AUTH_USER_ID, itsUserId);
        editor.commit();
    }

    /** Convert a Box object to a string for debugging */
    public static String boxToString(BoxObject obj, BoxClient client)
    {
        if (obj == null) {
            return null;
        }
        try {
            return obj.toJSONString(client.getJSONParser());
        }
        catch (BoxJSONException e) {
            return null;
        }
    }
}
