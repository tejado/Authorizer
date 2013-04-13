/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.lib;

/**
 * The ApiCompat class provides a compatibility interface for different Android
 * versions
 */
public final class ApiCompat
{
    public static final int SDK_CUPCAKE =
        android.os.Build.VERSION_CODES.CUPCAKE;
    public static final int SDK_FROYO =
        android.os.Build.VERSION_CODES.FROYO;
    public static final int SDK_HONEYCOMB = 11;

    public static final int SDK_VERSION;
    static {
        int sdk;
        try {
            sdk = Integer.parseInt(android.os.Build.VERSION.SDK);
        } catch (NumberFormatException e) {
            // Default back to android 1.5
            sdk = SDK_CUPCAKE;
        }
        SDK_VERSION = sdk;
    }
}
