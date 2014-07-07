/*
 * Copyright (©) 2013-2014 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.lib;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

/**
 * The ApiCompat class provides a compatibility interface for different Android
 * versions
 */
public final class ApiCompat
{
    public static final int SDK_FROYO =
        android.os.Build.VERSION_CODES.FROYO;
    public static final int SDK_HONEYCOMB = 11;
    public static final int SDK_KITKAT = 19;

    /** Copy of Intent.FLAG_ACTIVITY_CLEAR_TASK available on API 11 */
    public static final int INTENT_FLAG_ACTIVITY_CLEAR_TASK = 0x00008000;

    public static final int SDK_VERSION = android.os.Build.VERSION.SDK_INT;

    /** Request a manual sync of a content provider */
    public static void requestManualSync(Account acct, Uri uri, Context ctx)
    {
        Bundle options = new Bundle();
        options.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(acct, uri.getAuthority(), options);
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


    /** Get a list item 1 layout which may support the activated state */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static int getOptionalActivatedListItem1()
    {
        if (SDK_VERSION >= SDK_HONEYCOMB) {
            return android.R.layout.simple_list_item_activated_1;
        } else {
            return android.R.layout.simple_list_item_1;
        }
    }
}
