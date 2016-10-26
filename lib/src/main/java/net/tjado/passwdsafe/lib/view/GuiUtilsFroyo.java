/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib.view;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * The GuiUtilsFroyo class contains helper GUI methods that are usable on Froyo
 * and higher
 */
@TargetApi(8)
public final class GuiUtilsFroyo
{
    /**
     * The KeyboardViewer class shows the soft keyboard when run or the dialog
     * is shown.
     */
    public static class KeyboardViewer implements DialogInterface.OnShowListener,
                                                  Runnable
    {
        private final View itsView;
        private final Context itsContext;

        /**
         * Constructor
         */
        public KeyboardViewer(View view, Context ctx)
        {
            itsView = view;
            itsContext = ctx;
        }

        /* (non-Javadoc)
         * @see android.content.DialogInterface.OnShowListener#onShow(android.content.DialogInterface)
         */
        public void onShow(DialogInterface dialog)
        {
            run();
        }

        @Override
        public void run()
        {
            itsView.requestFocus();
            GuiUtils.setKeyboardVisible(itsView, itsContext, true);
        }
    }

    /**
     * Cause the keyboard to be shown for the view
     */
    public static void showKeyboard(View view, Context ctx)
    {
        view.post(new KeyboardViewer(view, ctx));
    }

    /**
     * Get a drawable resource
     */
    @SuppressWarnings("deprecation")
    public static Drawable getDrawable(Resources res, int id)
    {
        return res.getDrawable(id);
    }
}
