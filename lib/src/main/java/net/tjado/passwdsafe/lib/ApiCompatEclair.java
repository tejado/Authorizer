/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.os.Build;
import android.os.Bundle;

/**
 * ApiCompat class for Eclair (v5) and up
 */
public final class ApiCompatEclair
{
    /** Request a manual sync of a content provider */
    @TargetApi(Build.VERSION_CODES.ECLAIR)
    public static void requestManualSync(Account acct,
                                         String authority,
                                         Bundle extras)
    {
        Bundle options = new Bundle();
        if (extras != null) {
            options.putAll(extras);
        }
        options.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(acct, authority, options);
    }
}
