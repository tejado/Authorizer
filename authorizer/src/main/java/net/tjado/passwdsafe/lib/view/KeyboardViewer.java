/*
 * Copyright (©) 2019 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib.view;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;

/**
 * The KeyboardViewer class shows the soft keyboard when run or the dialog
 * is shown.
 */
public class KeyboardViewer implements DialogInterface.OnShowListener,
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

    @Override
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
