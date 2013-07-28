/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import android.accounts.Account;
import android.database.sqlite.SQLiteDatabase;

/**
 * The Provider interface encapsulates a service that provides files which are
 * synchronized
 */
public interface Provider
{
    /** Get the account for the named provider */
    Account getAccount(String acctName);

    /** Cleanup a provider when deleted */
    void cleanupOnDelete(String acctName);

    /** Sync a provider */
    void sync(Account acct, SyncDb.DbProvider provider,
              SQLiteDatabase db,
              boolean manual, SyncLogRecord logrec) throws Exception;
}
