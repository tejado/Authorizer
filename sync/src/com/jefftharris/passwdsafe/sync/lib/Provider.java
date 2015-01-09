/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import java.io.File;

import android.accounts.Account;
import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/**
 * The Provider interface encapsulates a service that provides files which are
 * synchronized
 */
public interface Provider
{
    public static final String ACTION_SYNC_EXPIRATION_TIMEOUT =
            "com.jefftharris.passwdsafe.action.SYNC_EXPIRATION_TIMEOUT";
    public static final String SYNC_EXPIRATION_TIMEOUT_EXTRA_TYPE =
            "com.jefftharris.passwdsafe.extra.providerType";

    /** Initialize the provider */
    public void init();

    /** Finalize the provider */
    public void fini();

    /** Start the process of linking to an account */
    public void startAccountLink(Activity activity, int requestCode);

    /** Finish the process of linking to an account */
    public NewAccountTask finishAccountLink(int activityResult,
                                            Intent activityData,
                                            Uri providerAcctUri);

    /** Unlink an account */
    public void unlinkAccount();

    /** Is the account fully authorized */
    public boolean isAccountAuthorized();

    /** Get the account for the named provider */
    public Account getAccount(String acctName);

    /** Check whether a provider can be added */
    public void checkProviderAdd(SQLiteDatabase db)
            throws Exception;

    /** Cleanup a provider when deleted */
    public void cleanupOnDelete(String acctName)
            throws Exception;

    /** Update a provider's sync frequency */
    public void updateSyncFreq(Account acct, int freq);

    public void requestSync(boolean manual);

    /** Sync a provider */
    public void sync(Account acct,
                     DbProvider provider,
                     SQLiteDatabase db,
                     boolean manual,
                     boolean full,
                     SyncLogRecord logrec)
            throws Exception;

    /** Insert a local file */
    public long insertLocalFile(long providerId,
                                String title,
                                SQLiteDatabase db)
            throws Exception;

    /** Update a local file */
    public void updateLocalFile(DbFile file,
                                String localFileName,
                                File localFile,
                                SQLiteDatabase db)
            throws Exception;

    /** Delete a local file */
    public void deleteLocalFile(DbFile file, SQLiteDatabase db)
            throws Exception;
}
