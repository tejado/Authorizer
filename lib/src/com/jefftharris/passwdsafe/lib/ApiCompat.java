/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.lib;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

/**
 * The ApiCompat class provides a compatibility interface for different Android
 * versions
 */
public final class ApiCompat
{
    public static final int SDK_CUPCAKE =
        android.os.Build.VERSION_CODES.CUPCAKE;
    public static final int SDK_ECLAIR =
            android.os.Build.VERSION_CODES.ECLAIR;
    public static final int SDK_FROYO =
        android.os.Build.VERSION_CODES.FROYO;
    public static final int SDK_HONEYCOMB = 11;
    public static final int SDK_KITKAT = 19;

    /** Copy of Intent.FLAG_ACTIVITY_CLEAR_TASK available on API 11 */
    public static final int INTENT_FLAG_ACTIVITY_CLEAR_TASK = 0x00008000;

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

    /** Request a manual sync of a content provider */
    public static void requestManualSync(Account acct,
                                         Uri uri,
                                         Context ctx,
                                         Bundle extras)
    {
        if (SDK_VERSION >= SDK_ECLAIR) {
            ApiCompatEclair.requestManualSync(acct, uri.getAuthority(), extras);
        } else {
            ApiCompatCupcake.requestManualSync(uri, ctx);
        }
    }


    /** Set whether the window is visible in the recent apps list */
    public static void setRecentAppsVisible(Window w, boolean visible)
    {
        /* The screen appears garbled before honeycomb, and the screenshot
         * feature started with honeycomb */
        if (SDK_VERSION >= SDK_HONEYCOMB) {
            if (visible) {
                w.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            } else {
                w.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }
        }
    }


    /** API compatible call for ContentResolver.takePersistableUriPermission */
    public static void takePersistableUriPermission(ContentResolver cr,
                                                    Uri uri,
                                                    int flags)
    {
        if (SDK_VERSION >= SDK_KITKAT) {
            ApiCompatKitkat.takePersistableUriPermission(cr, uri, flags);
        }
    }
}
