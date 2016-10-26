/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib.view;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;

/**
 * Typeface utilities
 */
public class TypefaceUtils
{
    private static Typeface itsRobotoMonoFace;

    /**
     * Set a monospace font
     */
    public static void setMonospace(TextView tv, Context ctx)
    {
        tv.setTypeface(getRobotoMono(ctx));
    }

    /**
     * Set a monospace or default font
     */
    public static void enableMonospace(TextView tv, boolean mono, Context ctx)
    {
        tv.setTypeface(mono ? getRobotoMono(ctx) : Typeface.DEFAULT);
    }

    /**
     * Get the RobotoMono font
     */
    private static Typeface getRobotoMono(Context ctx)
    {
        if (itsRobotoMonoFace == null) {
            itsRobotoMonoFace = Typeface.createFromAsset(
                    ctx.getAssets(), "RobotoMono-Regular.ttf");
        }
        return itsRobotoMonoFace;
    }
}
