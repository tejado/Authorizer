/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.jefftharris.passwdsafe.lib.ProviderType;

/**
 *  Entry in the providers table
 */
public class DbProvider
{
    public final long itsId;
    public final ProviderType itsType;
    public final String itsAcct;
    public final int itsSyncFreq;
    public final String itsDisplayName;

    public static final String[] QUERY_FIELDS = {
        SyncDb.DB_COL_PROVIDERS_ID,
        SyncDb.DB_COL_PROVIDERS_TYPE,
        SyncDb.DB_COL_PROVIDERS_ACCT,
        SyncDb.DB_COL_PROVIDERS_SYNC_FREQ,
        SyncDb.DB_COL_PROVIDERS_DISPLAY_NAME };

    /** Constructor */
    public DbProvider(Cursor cursor)
    {
        itsId = cursor.getLong(0);
        itsType = ProviderType.fromString(cursor.getString(1));
        itsAcct = cursor.getString(2);
        itsSyncFreq = cursor.getInt(3);
        itsDisplayName = cursor.getString(4);
    }

    /** Get the type and display name */
    public String getTypeAndDisplayName(Context ctx)
    {
        StringBuilder str = new StringBuilder();
        if (itsType != null) {
            str.append(itsType.getName(ctx));
        }
        str.append(" - ");
        if (!TextUtils.isEmpty(itsDisplayName)) {
            str.append(itsDisplayName);
        } else {
            str.append(itsAcct);
        }
        return str.toString();
    }

    @Override
    public String toString()
    {
        return String.format(
                Locale.US,
                "{id:%d, type: %s, acct:%s, syncFreq:%d, dispName:%s}",
                itsId, itsType, itsAcct, itsSyncFreq, itsDisplayName);
    }
}
