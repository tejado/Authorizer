/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

/**
 *  The ApiCompatHoneycomb class contains helper compatibility methods for
 *  Honeycomb and higher
 */
@TargetApi(11)
public final class ApiCompatHoneycomb
{
    /**
     * Recreate the activity
     */
    public static void recreateActivity(Activity act)
    {
        act.recreate();
    }


    /**
     * Copy text to the clipboard
     */
    public static void copyToClipboard(String str, Context ctx)
    {
        ClipboardManager clipMgr = (ClipboardManager)
                ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(null, str);
        clipMgr.setPrimaryClip(clip);
    }

    /**
     * Does the clipboard have text
     */
    public static boolean clipboardHasText(Context ctx)
    {
        ClipboardManager clipMgr = (ClipboardManager)
                ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        return clipMgr.hasPrimaryClip();
    }
}
