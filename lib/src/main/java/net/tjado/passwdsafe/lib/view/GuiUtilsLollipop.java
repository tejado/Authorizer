/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib.view;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;

/**
 * The GuiUtilsLollipop class contains helper GUI methods that are usable on
 * Lollipop and higher
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class GuiUtilsLollipop
{
    /**
     * Get a drawable resource
     */
    public static Drawable getDrawable(Resources res, int id)
    {
        return res.getDrawable(id, null);
    }
}
