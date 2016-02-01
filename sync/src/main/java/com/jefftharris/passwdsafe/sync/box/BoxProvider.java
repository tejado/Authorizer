/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.box;

import java.util.List;

import android.accounts.Account;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;

import com.box.androidsdk.content.BoxConfig;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.auth.BoxAuthentication;
import com.box.androidsdk.content.models.BoxJsonObject;
import com.box.androidsdk.content.models.BoxSession;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncTimerProvider;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.NewAccountTask;
import com.jefftharris.passwdsafe.sync.lib.NotifUtils;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;

/**
 * Implements a provider for the Box.com service
 */
public class BoxProvider extends AbstractSyncTimerProvider
        implements BoxAuthentication.AuthListener
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

    private BoxSession itsClient;
    private PendingIntent itsAcctLinkIntent = null;
    private boolean itsIsPendingAdd = false;

    /** Constructor */
    public BoxProvider(Context ctx)
    {
        super(ProviderType.BOX, ctx, TAG);
    }

    @Override
    public void init()
    {
        super.init();
        BoxConfig.CLIENT_ID = BOX_CLIENT_ID;
        BoxConfig.CLIENT_SECRET = BOX_CLIENT_SECRET;
        BoxConfig.IS_LOG_ENABLED = false;
        itsClient = new BoxSession(getContext());
        itsClient.setSessionAuthListener(this);
        updateBoxAcct();

        // Check for migration
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());
        if (prefs.contains(PREF_AUTH_REFRESH_TOKEN)) {
            NotifUtils.showNotif(NotifUtils.Type.BOX_MIGRATGED, getContext());
        }
        if (prefs.contains(PREF_AUTH_USER_ID)) {
            prefs.edit().remove(PREF_AUTH_USER_ID).apply();
        }
    }

    @Override
    public void startAccountLink(FragmentActivity activity, int requestCode)
    {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());
        if (prefs.contains(PREF_AUTH_USER_ID) ||
            prefs.contains(PREF_AUTH_REFRESH_TOKEN)) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.remove(PREF_AUTH_USER_ID);
            edit.remove(PREF_AUTH_ACCESS_TOKEN);
            edit.remove(PREF_AUTH_EXPIRES_IN);
            edit.remove(PREF_AUTH_REFRESH_TOKEN);
            edit.remove(PREF_AUTH_TOKEN_TYPE);
            edit.apply();
        }

        if (isAccountAuthorized()) {
            unlinkAccount();
        }

        Intent intent = new Intent();
        itsAcctLinkIntent = activity.createPendingResult(
                requestCode, intent, PendingIntent.FLAG_ONE_SHOT);
        itsClient.authenticate();
    }

    @Override
    public NewAccountTask finishAccountLink(int activityResult,
                                            Intent activityData,
                                            Uri acctProviderUri)
    {
        if (!isAccountAuthorized()) {
            Log.e(TAG, "finishAccountLink auth failed");
            return null;
        }
        updateBoxAcct();

        return new NewAccountTask(acctProviderUri, null, ProviderType.BOX,
                                  false, getContext())
        {
            @Override
            protected void doAccountUpdate(ContentResolver cr)
            {
                itsIsPendingAdd = true;
                try {
                    itsNewAcct = itsClient.getUserId();
                    super.doAccountUpdate(cr);
                } finally {
                    itsIsPendingAdd = false;
                }
            }
        };
    }

    @Override
    public void unlinkAccount()
    {
        if (itsAcctLinkIntent != null) {
            itsAcctLinkIntent.cancel();
            itsAcctLinkIntent = null;
        }
        itsClient.logout();
        updateBoxAcct();
    }

    @Override
    public synchronized boolean isAccountAuthorized()
    {
        BoxAuthentication.BoxAuthenticationInfo authInfo =
                itsClient.getAuthInfo();
        return ((authInfo != null) &&
                !TextUtils.isEmpty(itsClient.getUserId()) &&
                !TextUtils.isEmpty(authInfo.refreshToken()));
    }

    @Override
    public Account getAccount(String acctName)
    {
        return new Account(acctName, SyncDb.BOX_ACCOUNT_TYPE);
    }

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

    @Override
    public void cleanupOnDelete(String acctName)
    {
        if (!itsIsPendingAdd) {
            unlinkAccount();
        }
    }

    @Override
    protected String getAccountUserId()
    {
        return itsClient.getUserId();
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
                new BoxSyncer(itsClient, provider, db,
                              logrec, getContext()).sync();
            }
        } catch (Exception e) {
            Throwable t = e.getCause();
            if (t instanceof BoxException.RefreshFailure) {
                if (((BoxException.RefreshFailure)t).isErrorFatal()) {
                    Log.e(TAG, "sync: fatal refresh", t);
                    unlinkAccount();
                }
            }
            throw e;
        }
    }

    @Override
    public void onAuthCreated(BoxAuthentication.BoxAuthenticationInfo info)
    {
        PasswdSafeUtil.dbginfo(TAG, "onAuthCreated: %s", boxToString(info));
        if (itsAcctLinkIntent != null) {
            try {
                itsAcctLinkIntent.send(Activity.RESULT_OK);
                itsAcctLinkIntent = null;
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, "login intent send failed", e);
            }
        }
    }

    @Override
    public void onRefreshed(BoxAuthentication.BoxAuthenticationInfo info)
    {
        PasswdSafeUtil.dbginfo(TAG, "onRefreshed: %s", boxToString(info));
    }

    @Override
    public void onAuthFailure(BoxAuthentication.BoxAuthenticationInfo info,
                              Exception ex)
    {
        PasswdSafeUtil.dbginfo(TAG, "onAuthFailure: %s: %s",
                               boxToString(info), ex);
        if (itsAcctLinkIntent != null) {
            itsAcctLinkIntent.cancel();
            itsAcctLinkIntent = null;
        }
    }

    @Override
    public void onLoggedOut(BoxAuthentication.BoxAuthenticationInfo info,
                            Exception ex)
    {
        PasswdSafeUtil.dbginfo(TAG, "onLoggedOut: %s: %s",
                               boxToString(info), ex);
    }

    /** Update the Box account client based on availability of authentication
     *  information. */
    private synchronized void updateBoxAcct()
    {
        boolean isAuthorized = isAccountAuthorized();
        PasswdSafeUtil.dbginfo(TAG, "updateBoxAcct isAuth %b", isAuthorized);
        if (isAuthorized) {
            String userId = itsClient.getUserId();
            if (userId != null) {
                try {
                    updateProviderSyncFreq(userId);
                } catch (Exception e) {
                    Log.e(TAG, "updateBoxAcct failure", e);
                }
            }
            requestSync(false);
        } else {
            updateSyncFreq(null, 0);
        }
    }

    /** Convert a Box object to a string for debugging */
    public static String boxToString(BoxJsonObject obj)
    {
        return (obj != null) ? obj.toJson() : null;
    }
}
