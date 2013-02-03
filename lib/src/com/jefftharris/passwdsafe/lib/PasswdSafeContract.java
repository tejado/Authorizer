/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.lib;

import android.content.ContentResolver;
import android.net.Uri;

/**
 * The PasswdSafeContract class is the contract for the PasswdSafe sync provider
 */
public final class PasswdSafeContract
{
    /** The provider's authority */
    public static final String AUTHORITY =
        "com.jefftharris.passwdsafe.sync.provider";

    /** The base URI for the provider */
    public static final Uri CONTENT_URI =
        Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + AUTHORITY);
}
