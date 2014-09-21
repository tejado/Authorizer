/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import android.accounts.Account;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;

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
            ProviderType providerType = null;
            if (acct.type.equals(SyncDb.GDRIVE_ACCOUNT_TYPE)) {
                providerType = ProviderType.GDRIVE;
            } else if (acct.type.equals(SyncDb.DROPBOX_ACCOUNT_TYPE)) {
                providerType = ProviderType.DROPBOX;
            } else if (acct.type.equals(SyncDb.BOX_ACCOUNT_TYPE)) {
                providerType = ProviderType.BOX;
            } else {
                PasswdSafeUtil.dbginfo(TAG, "Unknown account type: ",
                                       acct.type);
                return null;
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


    /** Perform a sync operation */
    public static void performSync(Account acct,
                                   DbProvider provider,
                                   Provider providerImpl,
                                   boolean manual,
                                   SQLiteDatabase db,
                                   Context ctx)
    {
        ConnectivityManager connMgr = (ConnectivityManager)
                ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
        boolean online = (netInfo != null) && netInfo.isConnected();

        PasswdSafeUtil.dbginfo(TAG,
                               "Performing sync %s (%s), manual %b, online %b",
                               acct.name, acct.type, manual, online);

        String displayName = TextUtils.isEmpty(provider.itsDisplayName) ?
                provider.itsAcct : provider.itsDisplayName;
        SyncLogRecord logrec =
                new SyncLogRecord(displayName,
                                  provider.itsType.getName(ctx), manual);
        try {
            logrec.setNotConnected(!online);
            if (online) {
                providerImpl.sync(acct, provider, db, manual, logrec);
            }
        } catch (Exception e) {
            Log.e(TAG, "Sync error", e);
            logrec.addFailure(e);
        }
        PasswdSafeUtil.dbginfo(TAG, "Sync finished for %s", acct.name);
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
    }
}
