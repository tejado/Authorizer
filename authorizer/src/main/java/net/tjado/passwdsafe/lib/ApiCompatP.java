/*
 * Copyright (©) 2022 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib;

import android.annotation.TargetApi;
import android.content.ClipboardManager;
import android.os.Build;
import androidx.annotation.NonNull;

/**
 * The ApiCompatP class contains helper methods that are usable on
 * P and higher
 */
@TargetApi(Build.VERSION_CODES.P)
public final class ApiCompatP
{
    /**
     * Clear the clipboard
     */
    public static void clearClipboard(@NonNull ClipboardManager clipMgr)
    {
        clipMgr.clearPrimaryClip();
    }
}
