/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import android.accounts.Account;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.sync.R;
import com.jefftharris.passwdsafe.sync.SyncLogsActivity;

/**
 * Encapsulation of a sync operation for a provider
 */
public class ProviderSync
{
    private static final String TAG = "ProviderSync";

    private final Account itsAccount;
    private final DbProvider itsProvider;
    private final Provider itsProviderImpl;
    private final Context itsContext;


    /**
     * Constructor
     */
    public ProviderSync(Account acct,
                        DbProvider provider,
                        Provider providerImpl,
                        Context ctx)
    {
        itsAccount = acct;
        itsProvider = provider;
        itsProviderImpl = providerImpl;
        itsContext = ctx;
    }

    /**
     * Perform a sync
     */
    public void sync(boolean manual)
    {
        SyncLogRecord logrec = begin(manual);
        SyncDb syncDb = null;
        try {
            SyncConnectivityResult connResult = checkSyncConnectivity(logrec);
            syncDb = SyncDb.acquire();
            performSync(connResult, syncDb.getDb(), logrec);
        } finally {
            if (syncDb == null) {
                syncDb = SyncDb.acquire();
            }
            try {
                finish(logrec, syncDb.getDb());
            } finally {
                syncDb.release();
            }
        }
    }

    /**
     * Begin a sync
     */
    private SyncLogRecord begin(boolean manual)
    {
        PasswdSafeUtil.dbginfo(TAG, "Performing sync %s (%s), manual %b",
                               itsAccount.name, itsAccount.type, manual);
        String displayName = TextUtils.isEmpty(itsProvider.itsDisplayName) ?
                             itsProvider.itsAcct : itsProvider.itsDisplayName;
        return new SyncLogRecord(
                displayName,
                ((itsProvider.itsType != null) ?
                 itsProvider.itsType.getName(itsContext) : null),
                manual);
    }

    /**
     * Check the connectivity of a provider before syncing
     */
    private SyncConnectivityResult checkSyncConnectivity(SyncLogRecord logrec)
    {
        SyncConnectivityResult connResult = null;
        ConnectivityManager connMgr = (ConnectivityManager)
                itsContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
        boolean online = (netInfo != null) && netInfo.isConnected();
        if (online) {
            try {
                connResult = itsProviderImpl.checkSyncConnectivity(itsAccount);
                online = (connResult != null);
            } catch (Exception e) {
                Log.e(TAG, "checkSyncConnectivity error", e);
                online = false;
                logrec.addFailure(e);
            }
        }
        logrec.setNotConnected(!online);
        return connResult;
    }

    /**
     * Perform the sync of a provider
     */
    private void performSync(SyncConnectivityResult connResult,
                             SQLiteDatabase db,
                             SyncLogRecord logrec)
    {
        try {
            if (!logrec.isNotConnected()) {
                itsProviderImpl.sync(itsAccount, itsProvider,
                                     connResult, db, logrec);
            }
        } catch (Exception e) {
            Log.e(TAG, "Sync error", e);
            logrec.addFailure(e);
        }
    }

    /**
     * Finish the sync of a provider
     */
    private void finish(SyncLogRecord logrec, SQLiteDatabase db)
    {
        PasswdSafeUtil.dbginfo(TAG, "Sync finished for %s, online %b",
                               itsAccount.name, !logrec.isNotConnected());
        logrec.setEndTime();

        try {
            db.beginTransaction();
            Log.i(TAG, logrec.toString(itsContext));
            SyncDb.deleteSyncLogs(
                    System.currentTimeMillis() - 2 * DateUtils.WEEK_IN_MILLIS,
                    db);
            SyncDb.addSyncLog(logrec, db);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Sync write log error", e);
        } finally {
            db.endTransaction();
        }

        if (!logrec.getConflictFiles().isEmpty()) {
            NotificationManager notifMgr = (NotificationManager)
                    itsContext.getSystemService(Context.NOTIFICATION_SERVICE);

            PendingIntent logsIntent = PendingIntent.getActivity(
                    itsContext, 0,
                    new Intent(itsContext, SyncLogsActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT);

            String title =
                    itsContext.getString(R.string.passwdsafe_sync_conflict);
            GuiUtils.showNotification(
                    notifMgr, itsContext, R.drawable.ic_stat_app,
                    title, title, R.mipmap.ic_launcher_sync,
                    itsProvider.getTypeAndDisplayName(itsContext),
                    logrec.getConflictFiles(), logsIntent,
                    (int)itsProvider.itsId, true);
        }
    }
}
