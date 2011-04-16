/*
 * Copyright (©) 2010-2011 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;

public abstract class AbstractDialogClickListener
    implements OnClickListener, OnCancelListener
{
    public final void onClick(DialogInterface dialog, int which)
    {
        switch (which) {
        case DialogInterface.BUTTON_POSITIVE:
            onOkClicked(dialog);
            break;
        case DialogInterface.BUTTON_NEGATIVE:
            onCancelClicked(dialog);
            break;
        }
    }

    public final void onCancel(DialogInterface dialog)
    {
        onCancelClicked(dialog);
    }

    public abstract void onOkClicked(DialogInterface dialog);

    public void onCancelClicked(DialogInterface dialog)
    {
    }
}
