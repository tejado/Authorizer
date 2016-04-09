/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import java.io.File;

import android.accounts.Account;
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;

/**
 * The Provider interface encapsulates a service that provides files which are
 * synchronized
 */
public interface Provider
{
    String ACTION_SYNC_EXPIRATION_TIMEOUT =
            "com.jefftharris.passwdsafe.action.SYNC_EXPIRATION_TIMEOUT";
    String SYNC_EXPIRATION_TIMEOUT_EXTRA_TYPE =
            "com.jefftharris.passwdsafe.extra.providerType";

    /** Initialize the provider */
    void init();

    /** Finalize the provider */
    void fini();

    /** Start the process of linking to an account */
    void startAccountLink(FragmentActivity activity, int requestCode);

    /** Finish the process of linking to an account */
    NewAccountTask finishAccountLink(int activityResult,
                                     Intent activityData,
                                     Uri providerAcctUri);

    /** Unlink an account */
    void unlinkAccount();

    /** Is the account fully authorized */
    boolean isAccountAuthorized();

    /** Get the account for the named provider */
    Account getAccount(String acctName);

    /** Check whether a provider can be added */
    void checkProviderAdd(SQLiteDatabase db)
            throws Exception;

    /** Cleanup a provider when deleted */
    void cleanupOnDelete(String acctName)
            throws Exception;

    /** Update a provider's sync frequency */
    void updateSyncFreq(Account acct, int freq);

    /** Request a sync */
    void requestSync(boolean manual);

    /** Sync a provider */
    void sync(Account acct,
              DbProvider provider,
              SQLiteDatabase db,
              SyncLogRecord logrec)
            throws Exception;

    /** Insert a local file */
    long insertLocalFile(long providerId, String title, SQLiteDatabase db)
            throws SQLException;

    /** Update a local file */
    void updateLocalFile(DbFile file,
                         String localFileName,
                         File localFile,
                         SQLiteDatabase db);

    /** Delete a local file */
    void deleteLocalFile(DbFile file, SQLiteDatabase db);
}
