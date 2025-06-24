/*
 * Copyright (©) 2022 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib.view;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;

/**
 * The GuiUtilsQ class contains helper GUI methods that are usable on
 * Q and higher
 */

@TargetApi(Build.VERSION_CODES.Q)
public final class GuiUtilsQ
{
    /**
     * Disable force-dark mode
     */
    public static void disableForceDark(View view)
    {
        view.setForceDarkAllowed(false);
    }
}
