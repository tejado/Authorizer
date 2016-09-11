/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import android.accounts.Account;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.R;

import java.util.ArrayList;
import java.util.List;

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
    private final String itsNotifTag;


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
        itsNotifTag = Long.toString(itsProvider.itsId);
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
        showProgressNotif();
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
            SyncDb.addSyncLog(logrec, db, itsContext);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Sync write log error", e);
        } finally {
            db.endTransaction();
        }

        NotifUtils.cancelNotif(NotifUtils.Type.SYNC_PROGRESS,
                               itsNotifTag, itsContext);
        showResultNotifs(logrec);
    }

    /**
     * Show the sync progress notification
     */
    private void showProgressNotif()
    {
        String title = NotifUtils.getTitle(NotifUtils.Type.SYNC_PROGRESS,
                                           itsContext);
        String content = itsProvider.getTypeAndDisplayName(itsContext);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(itsContext)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setTicker(title)
                        .setAutoCancel(true)
                        .setProgress(100, 0, true)
                        .setCategory(NotificationCompat.CATEGORY_PROGRESS);
        NotifUtils.showNotif(builder, NotifUtils.Type.SYNC_PROGRESS,
                             itsNotifTag, itsContext);
    }

    /**
     * Show any sync result notifications
     */
    private void showResultNotifs(SyncLogRecord logrec)
    {
        List<String> results = new ArrayList<>();
        boolean success = true;
        for (Exception failure: logrec.getFailures()) {
            results.add(itsContext.getString(R.string.error_fmt,
                                             failure.getLocalizedMessage()));
            success = false;
        }
        results.addAll(logrec.getEntries());
        if (!results.isEmpty()) {
            showResultNotif(NotifUtils.Type.SYNC_RESULTS, success, results);
        }

        results = logrec.getConflictFiles();
        if (!results.isEmpty()) {
            showResultNotif(NotifUtils.Type.SYNC_CONFLICT, false, results);
        }
    }

    /**
     * Show a sync result notification
     */
    private void showResultNotif(NotifUtils.Type type,
                                 boolean success,
                                 List<String> results)
    {
        String title = NotifUtils.getTitle(type, itsContext);
        String content = itsProvider.getTypeAndDisplayName(itsContext);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(itsContext)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setTicker(title)
                        .setAutoCancel(true);
        if (success) {
            builder.setCategory(NotificationCompat.CATEGORY_STATUS);
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
            builder.setCategory(NotificationCompat.CATEGORY_ERROR);
        }

        NotificationCompat.InboxStyle style =
                new NotificationCompat.InboxStyle(builder)
                        .setBigContentTitle(title)
                        .setSummaryText(content);
        int numLines = Math.min(results.size(), 5);
        for (int i = 0; i < numLines; ++i) {
            style.addLine(results.get(i));
        }
        if (numLines < results.size()) {
            style.addLine("…");
            builder.setNumber(results.size());
        }

        NotifUtils.showNotif(builder, type, itsNotifTag, itsContext);
    }
}
