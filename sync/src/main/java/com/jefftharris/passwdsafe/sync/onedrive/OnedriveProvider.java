/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.onedrive;

import android.accounts.Account;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncTimerProvider;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.NewAccountTask;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;
import com.microsoft.authenticate.AuthClient;
import com.microsoft.authenticate.AuthException;
import com.microsoft.authenticate.AuthListener;
import com.microsoft.authenticate.AuthSession;
import com.microsoft.authenticate.AuthStatus;
import com.microsoft.onedriveaccess.IOneDriveService;
import com.microsoft.onedriveaccess.ODConnection;
import com.microsoft.onedriveaccess.OneDriveOAuthConfig;
import com.microsoft.onedriveaccess.model.Drive;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Implements a provider for the OneDrive service
 */
public class OnedriveProvider extends AbstractSyncTimerProvider
{
    private static final String CLIENT_ID = "00000000401589B9";
    private static final String PREF_USER_ID = "onedriveUserId";

    private static final String TAG = "OnedriveProvider";

    private AuthClient itsAuthClient;
    final private ReentrantLock itsServiceLock = new ReentrantLock();
    private String itsUserId = null;
    private boolean itsIsPendingAdd = false;

    /** Constructor */
    public OnedriveProvider(Context ctx)
    {
        super(ProviderType.ONEDRIVE, ctx, TAG);
    }

    @Override
    public void init()
    {
        super.init();
        itsAuthClient = new AuthClient(getContext(),
                                       OneDriveOAuthConfig.getInstance(),
                                       CLIENT_ID);
        updateOnedriveAcct(null);
    }

    /**
     * Start the process of linking to an account
     */
    @Override
    public void startAccountLink(final FragmentActivity activity,
                                 final int requestCode)
    {
        Runnable loginTask = new Runnable()
        {
            @Override
            public void run()
            {
                itsAuthClient.login(activity, null, new AuthListener()
                {
                    @Override
                    public void onAuthComplete(AuthStatus status,
                                               AuthSession session,
                                               Object userState)
                    {
                        PasswdSafeUtil.dbginfo(
                                TAG, "login ok status %s, sess [%s]",
                                status, session);

                        switch (status) {
                        case CONNECTED: {
                            Intent intent = new Intent();
                            PendingIntent pendIntent =
                                    activity.createPendingResult(
                                            requestCode, intent,
                                            PendingIntent.FLAG_ONE_SHOT);
                            try {
                                pendIntent.send(Activity.RESULT_OK);
                            } catch (PendingIntent.CanceledException e) {
                                Log.e(TAG, "login intent send failed", e);
                            }
                            break;
                        }
                        case NOT_CONNECTED:
                        case UNKNOWN: {
                            Log.e(TAG, "Auth complete, bad status: " + status);
                            break;
                        }
                        }
                    }

                    @Override
                    public void onAuthError(AuthException exception, Object userState)
                    {
                        Log.e(TAG, "Auth error", exception);
                    }
                });
            }
        };

        if (isAccountAuthorized()) {
            unlinkAccount(loginTask);
        } else {
            loginTask.run();
        }
    }

    /**
     * Finish the process of linking to an account
     */
    @Override
    public NewAccountTask finishAccountLink(int activityResult,
                                            Intent activityData,
                                            Uri providerAcctUri)
    {
        if (!isAccountAuthorized()) {
            Log.e(TAG, "finishAccountLink auth failed");
            return null;
        }

        return new NewAccountTask(providerAcctUri, null, ProviderType.ONEDRIVE,
                                  false, getContext())
        {
            @Override
            protected void doAccountUpdate(ContentResolver cr)
            {
                itsIsPendingAdd = true;
                try {
                    IOneDriveService service = acquireOnedriveService();
                    Drive drive = service.getDrive();
                    itsNewAcct = drive.Owner.User.Id;
                    setUserId(itsNewAcct);
                    super.doAccountUpdate(cr);
                } catch (Exception e) {
                    Log.e(TAG, "Error retrieving drive", e);
                } finally {
                    releaseOnedriveService();
                    itsIsPendingAdd = false;
                }
            }
        };
    }

    /**
     * Unlink an account
     */
    @Override
    public void unlinkAccount()
    {
        unlinkAccount(null);
    }

    /**
     * Is the account fully authorized
     */
    @Override
    public boolean isAccountAuthorized()
    {
        return itsAuthClient.hasRefreshToken();
    }

    /**
     * Get the account for the named provider
     */
    @Override
    public Account getAccount(String acctName)
    {
        return new Account(acctName, SyncDb.ONEDRIVE_ACCOUNT_TYPE);
    }

    /**
     * Check whether a provider can be added
     */
    @Override
    public void checkProviderAdd(SQLiteDatabase db) throws Exception
    {
        List<DbProvider> providers = SyncDb.getProviders(db);
        for (DbProvider provider: providers) {
            if (provider.itsType == ProviderType.ONEDRIVE) {
                throw new Exception("Only one OneDrive account allowed");
            }
        }
    }

    /**
     * Cleanup a provider when deleted
     */
    @Override
    public void cleanupOnDelete(String acctName) throws Exception
    {
        if (!itsIsPendingAdd) {
            unlinkAccount();
        }
    }

    /**
     * Request a sync
     */
    @Override
    public void requestSync(boolean manual)
    {
        boolean authorized = isAccountAuthorized();
        PasswdSafeUtil.dbginfo(TAG, "requestSync authorized: %b", authorized);
        if (authorized) {
            doRequestSync(manual);
        }
    }

    /**
     * Sync a provider
     */
    @Override
    public void sync(Account acct,
                     DbProvider provider,
                     SQLiteDatabase db,
                     SyncLogRecord logrec) throws Exception
    {
        boolean authorized = isAccountAuthorized();
        PasswdSafeUtil.dbginfo(TAG, "sync authorized: %b", authorized);
        if (authorized) {
            try {
                IOneDriveService service = acquireOnedriveService();
                new OnedriveSyncer(service, provider, db, logrec,
                                   getContext()).sync();
            } finally {
                releaseOnedriveService();
            }
        }
    }

    /**
     * Get a OneDrive service for the client
     */
    public IOneDriveService acquireOnedriveService()
            throws Exception
    {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new Exception("Can't invoke getOnedriveService in ui thread");
        }

        if (!itsServiceLock.tryLock(15, TimeUnit.MINUTES)) {
            throw new Exception("Timeout waiting for OneDrive service");
        }

        if (itsAuthClient.getSession().isExpired()) {
            PasswdSafeUtil.dbginfo(TAG, "sync refreshing auth token");
            itsAuthClient.getSession().refresh();
        }

        ODConnection conn = new ODConnection(itsAuthClient);
        conn.setVerboseLogcatOutput(PasswdSafeUtil.DEBUG);
        return conn.getService();
    }

    /**
     * Release the OneDrive service
     */
    public void releaseOnedriveService()
    {
        itsServiceLock.unlock();
    }

    /**
     * Get the account user identifier
     */
    @Override
    protected String getAccountUserId()
    {
        return itsUserId;
    }

    /**
     * Asynchronously unlink the account
     * @param completeCb The callback to run when the unlink is complete
     */
    private void unlinkAccount(final Runnable completeCb)
    {
        itsAuthClient.logout(new AuthListener()
        {
            @Override
            public void onAuthComplete(AuthStatus status,
                                       AuthSession session,
                                       Object userState)
            {
                setUserId(null);
                updateOnedriveAcct(completeCb);
            }

            @Override
            public void onAuthError(AuthException exception, Object userState)
            {
                Log.e(TAG, "logout auth error", exception);
                completeCb.run();
            }
        });
    }

    /**
     * Asynchronously update the OneDrive account client based on availability
     * of authentication information
     * @param completeCb The callback to run when the update is complete
     */
    private synchronized void updateOnedriveAcct(final Runnable completeCb)
    {
        if (isAccountAuthorized()) {
            SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(getContext());
            itsUserId = prefs.getString(PREF_USER_ID, null);
            if (itsUserId != null) {
                try {
                    updateProviderSyncFreq(itsUserId);
                } catch (Exception e) {
                    Log.e(TAG, "updateOnedriveAcct failure", e);
                }
            }
        }

        AuthListener authCb = new AuthListener()
        {
            @Override
            public void onAuthComplete(AuthStatus status,
                                       AuthSession session,
                                       Object userState)
            {
                PasswdSafeUtil.dbginfo(
                        TAG, "update acct complete: %s, session expire: %s",
                        status,
                        (session != null) ? session.getExpiresIn() : "(none)");
                switch (status) {
                case CONNECTED: {
                    requestSync(false);
                    break;
                }
                case NOT_CONNECTED:
                case UNKNOWN: {
                    itsUserId = null;
                    updateSyncFreq(null, 0);
                    break;
                }
                }

                if (completeCb != null) {
                    completeCb.run();
                }
            }

            @Override
            public void onAuthError(AuthException exception,
                                    Object userState)
            {
                Log.e(TAG, "update auth error", exception);
                if (completeCb != null) {
                    completeCb.run();
                }
            }
        };
        itsAuthClient.initialize(
                Arrays.asList("wl.signin", "wl.offline_access",
                              "onedrive.readwrite"),
                authCb, null, null);
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
}
