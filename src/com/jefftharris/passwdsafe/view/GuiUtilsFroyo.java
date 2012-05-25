/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.view;

import com.jefftharris.passwdsafe.GuiUtils;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;

/**
 * The GuiUtilsFroyo class contains helper GUI methods that are usable on Froyo
 * and higher
 */
public final class GuiUtilsFroyo
{
    /**
     * The KeyboardViewer class shows the soft keyboard when the dialog is
     * shown.
     */
    public static class KeyboardViewer implements DialogInterface.OnShowListener
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
            itsView.requestFocus();
            GuiUtils.setKeyboardVisible(itsView, itsContext, true);
        }
    }

    /**
     * Set a listener to show the keyboard when the dialog is shown
     */
    public static void setShowKeyboardListener(Dialog dialog, View view,
                                               Context ctx)
    {
        dialog.setOnShowListener(new KeyboardViewer(view, ctx));
    }
}
