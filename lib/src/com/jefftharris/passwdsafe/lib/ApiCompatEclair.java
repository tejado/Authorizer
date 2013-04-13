/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.lib;

import android.content.ContentResolver;
import android.os.Bundle;

/**
 * ApiCompat class for Eclair (v5) and up
 */
public final class ApiCompatEclair
{
    /** Request a sync of a content provider */
    public static void requestProviderSync(String authority)
    {
        Bundle options = new Bundle();
        options.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(null, authority, options);
    }
}
