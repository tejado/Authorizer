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
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.sync.R;
import com.jefftharris.passwdsafe.sync.SyncLogsActivity;

/**
 * The SyncHelper class contains some helper methods for performing a sync.
 */
public class SyncHelper
{
    private static final String TAG = "SyncHelper";

    /** Get the filename for a local file */
    public static String getLocalFileName(long fileId)
    {
        return "syncfile-" + Long.toString(fileId);
    }


    /** Get the DB provider for an account */
    public static DbProvider getDbProviderForAcct(Account acct,
                                                  SQLiteDatabase db)
    {
        DbProvider provider = null;
        try {
            db.beginTransaction();
            ProviderType providerType;
            switch (acct.type) {
            case SyncDb.GDRIVE_ACCOUNT_TYPE: {
                providerType = ProviderType.GDRIVE;
                break;
            }
            case SyncDb.DROPBOX_ACCOUNT_TYPE: {
                providerType = ProviderType.DROPBOX;
                break;
            }
            case SyncDb.BOX_ACCOUNT_TYPE: {
                providerType = ProviderType.BOX;
                break;
            }
            case SyncDb.ONEDRIVE_ACCOUNT_TYPE: {
                providerType = ProviderType.ONEDRIVE;
                break;
            }
            case SyncDb.OWNCLOUD_ACCOUNT_TYPE: {
                providerType = ProviderType.OWNCLOUD;
                break;
            }
            default: {
                PasswdSafeUtil.dbginfo(TAG, "Unknown account type: ",
                                       acct.type);
                return null;
            }
            }
            provider = SyncDb.getProvider(acct.name, providerType, db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        if (provider == null) {
            PasswdSafeUtil.dbginfo(TAG, "No provider for %s", acct.name);
        }
        return provider;
    }

    /**
     * Perform a sync of a provider
     */
    public static void performSync(Account acct,
                                   DbProvider provider,
                                   Provider providerImpl,
                                   boolean manual,
                                   Context ctx)
    {
        SyncLogRecord logrec = beginSync(acct, provider, manual, ctx);
        SyncDb syncDb = null;
        try {
            SyncConnectivityResult connResult =
                    checkSyncConnectivity(acct, providerImpl, logrec, ctx);

            syncDb = SyncDb.acquire();
            performSync(acct, provider, providerImpl, connResult,
                        syncDb.getDb(), logrec);
        } finally {
            if (syncDb == null) {
                syncDb = SyncDb.acquire();
            }
            try {
                finishSync(acct, provider, logrec, syncDb.getDb(), ctx);
            } finally {
                syncDb.release();
            }
        }
    }

    /**
     * Begin a sync of a provider
     */
    private static SyncLogRecord beginSync(Account acct,
                                           DbProvider provider,
                                           boolean manual,
                                           Context ctx)
    {
        PasswdSafeUtil.dbginfo(TAG, "Performing sync %s (%s), manual %b",
                               acct.name, acct.type, manual);

        String displayName = TextUtils.isEmpty(provider.itsDisplayName) ?
                             provider.itsAcct : provider.itsDisplayName;

        return new SyncLogRecord(displayName,
                                 (provider.itsType != null)
                                 ? provider.itsType.getName(ctx) : null,
                                 manual);
    }

    /**
     * Check the connectivity of a provider before syncing
     */
    private static SyncConnectivityResult checkSyncConnectivity(
            Account acct,
            Provider providerImpl,
            SyncLogRecord logrec,
            Context ctx)
    {
        SyncConnectivityResult connResult = null;
        ConnectivityManager connMgr = (ConnectivityManager)
                ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
        boolean online = (netInfo != null) && netInfo.isConnected();
        if (online) {
            try {
                connResult = providerImpl.checkSyncConnectivity(acct);
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
    private static void performSync(Account acct,
                                    DbProvider provider,
                                    Provider providerImpl,
                                    SyncConnectivityResult connResult,
                                    SQLiteDatabase db,
                                    SyncLogRecord logrec)
    {
        try {
            if (!logrec.isNotConnected()) {
                providerImpl.sync(acct, provider, connResult, db, logrec);
            }
        } catch (Exception e) {
            Log.e(TAG, "Sync error", e);
            logrec.addFailure(e);
        }
    }

    /**
     * Finish the sync of a provider
     */
    private static void finishSync(Account acct,
                                   DbProvider provider,
                                   SyncLogRecord logrec,
                                   SQLiteDatabase db,
                                   Context ctx)
    {
        PasswdSafeUtil.dbginfo(TAG, "Sync finished for %s, online %b",
                               acct.name, !logrec.isNotConnected());
        logrec.setEndTime();

        try {
            db.beginTransaction();
            Log.i(TAG, logrec.toString(ctx));
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
            NotificationManager notifMgr =
                    (NotificationManager) ctx.getSystemService(
                            Context.NOTIFICATION_SERVICE);

            PendingIntent logsIntent = PendingIntent.getActivity(
                    ctx, 0,
                    new Intent(ctx, SyncLogsActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT);

            String title = ctx.getString(R.string.passwdsafe_sync_conflict);
            GuiUtils.showNotification(
                    notifMgr, ctx, R.drawable.ic_stat_app,
                    title, title, R.mipmap.ic_launcher_sync,
                    provider.getTypeAndDisplayName(ctx),
                    logrec.getConflictFiles(), logsIntent,
                    (int)provider.itsId, true);
        }
    }
}
