/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib;

import android.accounts.Account;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.ClipboardManager;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import java.util.Collections;
import java.util.List;

/**
 * The ApiCompat class provides a compatibility interface for different Android
 * versions
 */
public final class ApiCompat
{
    private static final int SDK_ECLAIR =
            android.os.Build.VERSION_CODES.ECLAIR;
    public static final int SDK_HONEYCOMB = 11;
    public static final int SDK_KITKAT = 19;
    public static final int SDK_LOLLIPOP = 21;

    public static final int SDK_VERSION = Build.VERSION.SDK_INT;

    /** Request a manual sync of a content provider */
    @SuppressWarnings("SameParameterValue")
    public static void requestManualSync(Account acct,
                                         Uri uri,
                                         Bundle extras)
    {
        if (SDK_VERSION >= SDK_ECLAIR) {
            ApiCompatEclair.requestManualSync(acct, uri.getAuthority(), extras);
        }
    }


    /** Set whether the window is visible in the recent apps list */
    public static void setRecentAppsVisible(
            Window w, @SuppressWarnings("SameParameterValue") boolean visible)
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


    /**
     * Recreate the activity
     */
    public static void recreateActivity(Activity act)
    {
        if (SDK_VERSION >= SDK_HONEYCOMB) {
            ApiCompatHoneycomb.recreateActivity(act);
        } else {
            Intent startIntent = act.getIntent();
            act.finish();
            act.startActivity(startIntent);
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


    /** API compatible call for
     * ContentResolver.releasePersistableUriPermission */
    public static void releasePersistableUriPermission(ContentResolver cr,
                                                       Uri uri,
                                                       int flags)
    {
        if (SDK_VERSION >= SDK_KITKAT) {
            ApiCompatKitkat.releasePersistableUriPermission(cr, uri, flags);
        }
    }


    /** API compatible call for ContentResolver.getPersistedUriPermissions */
    public static List<Uri> getPersistedUriPermissions(ContentResolver cr)
    {
        if (SDK_VERSION >= SDK_KITKAT) {
            return ApiCompatKitkat.getPersistedUriPermissions(cr);
        }
        return Collections.emptyList();
    }


    /** Can the account manager get an auth token with showing a dialog */
    public static boolean canAccountMgrGetAuthTokenWithDialog()
    {
        return SDK_VERSION < SDK_KITKAT;
    }


    /** API compatible call for DocumentsContract.deleteDocument */
    public static boolean documentsContractDeleteDocument(ContentResolver cr,
                                                          Uri uri)
    {
        return (SDK_VERSION >= SDK_KITKAT) &&
                ApiCompatKitkat.documentsContractDeleteDocument(cr, uri);
    }

    /**
     * Copy text to the clipboard
     */
    public static void copyToClipboard(String str, Context ctx)
    {
        if (SDK_VERSION >= SDK_HONEYCOMB) {
            ApiCompatHoneycomb.copyToClipboard(str, ctx);
        } else {
            @SuppressWarnings("deprecation")
            ClipboardManager clipMgr = (ClipboardManager)
                    ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            clipMgr.setText(str);
        }
    }

    /**
     * Does the clipboard have text
     */
    public static boolean clipboardHasText(Context ctx)
    {
        if (SDK_VERSION >= SDK_HONEYCOMB) {
            return ApiCompatHoneycomb.clipboardHasText(ctx);
        } else {
            @SuppressWarnings("deprecation")
            ClipboardManager clipMgr = (ClipboardManager)
                    ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            return clipMgr.hasText();
        }
    }

    /**
     * API compatible call for
     * InputMethodManager.shouldOfferSwitchingToNextInputMethod
     */
    public static boolean shouldOfferSwitchingToNextInputMethod(
            InputMethodManager imm,
            IBinder imeToken)
    {
        return (SDK_VERSION >= SDK_KITKAT) &&
               ApiCompatKitkat.shouldOfferSwitchingToNextInputMethod(imm,
                                                                     imeToken);
    }

    /**
     * API compatible call for
     * InputMethodManager.switchToNextInputMethod
     */
    @SuppressWarnings("SameParameterValue")
    public static boolean switchToNextInputMethod(InputMethodManager imm,
                                                  IBinder imeToken,
                                                  boolean onlyCurrentIme)
    {
        return (SDK_VERSION >= SDK_KITKAT) &&
               ApiCompatKitkat.switchToNextInputMethod(imm, imeToken,
                                                       onlyCurrentIme);
    }
}
