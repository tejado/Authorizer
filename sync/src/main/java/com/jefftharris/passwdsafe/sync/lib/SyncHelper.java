/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import android.accounts.Account;
import android.database.sqlite.SQLiteDatabase;

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
}
