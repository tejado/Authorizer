/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.view;

import com.jefftharris.passwdsafe.lib.view.GuiUtils;

import android.content.Context;
import android.widget.Button;
import android.widget.TextView;

/**
 * The DialogUtils class provides utility dialogs
 */
public class DialogUtils
{
    /**
     * Setup the keyboard on a dialog. The initial field gets focus and shows
     * the keyboard. The final field clicks the Ok button when enter is pressed.
     */
    public static void setupDialogKeyboard(
            final android.support.v7.app.AlertDialog dialog,
            TextView initialField,
            TextView finalField,
            Context ctx)
    {
        GuiUtils.setShowKeyboardListener(dialog, initialField, ctx);
        GuiUtils.setupKeyboardEnter(finalField, new Runnable()
        {
            @Override
            public void run()
            {
                Button btn = dialog.getButton(
                        android.support.v7.app.AlertDialog.BUTTON_POSITIVE);
                if (btn.isEnabled()) {
                    btn.performClick();
                }
            }
        });
    }
}
