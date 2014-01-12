/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.ProviderType;

/**
 * Task to complete the addition of a new account
 */
public class NewAccountTask extends AccountUpdateTask
{
    private String itsNewAcct;
    private ProviderType itsAcctType;

    /** Constructor */
    public NewAccountTask(Uri currAcctUri,
                          String newAcct,
                          ProviderType acctType,
                          Context ctx)
    {
        super(currAcctUri, ctx.getString(R.string.adding_account));
        itsNewAcct = newAcct;
        itsAcctType = acctType;
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AccountUpdateTask#doAccountUpdate(android.content.ContentResolver)
     */
    @Override
    protected void doAccountUpdate(ContentResolver cr)
    {
        // Stop syncing for the previously selected account.
        if (itsAccountUri != null) {
            cr.delete(itsAccountUri, null, null);
        }

        if (itsNewAcct != null) {
            ContentValues values = new ContentValues();
            values.put(PasswdSafeContract.Providers.COL_ACCT,
                       itsNewAcct);
            values.put(PasswdSafeContract.Providers.COL_TYPE,
                       itsAcctType.name());
            cr.insert(PasswdSafeContract.Providers.CONTENT_URI, values);
        }
    }
}
