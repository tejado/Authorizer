/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.inputmethodservice.Keyboard;
import android.support.annotation.NonNull;
import android.view.inputmethod.EditorInfo;

import com.jefftharris.passwdsafe.lib.view.GuiUtils;

/**
 * The PasswdSafeIMEKeyboard class is a keyboard for PasswdSafe
 */
final class PasswdSafeIMEKeyboard extends Keyboard
{
    private Key itsEnterKey;

    /**
     * Constructor
     */
    public PasswdSafeIMEKeyboard(
            Context context,
            @SuppressWarnings("SameParameterValue") int xmlLayoutResId)
    {
        super(context, xmlLayoutResId);
    }

    /**
     * Set options on the keyboard
     */
    public void setOptions(EditorInfo info, Resources res)
    {
        if (itsEnterKey == null) {
            return;
        }

        int enterText = -1;
        int enterIcon = -1;
        int enterAction =
                info.imeOptions & (EditorInfo.IME_MASK_ACTION |
                                   EditorInfo.IME_FLAG_NO_ENTER_ACTION);
        switch (enterAction) {
        case EditorInfo.IME_ACTION_DONE: {
            enterText = R.string.done;
            break;
        }
        case EditorInfo.IME_ACTION_GO: {
            enterText = R.string.go;
            break;
        }
        case EditorInfo.IME_ACTION_NEXT: {
            enterText = R.string.next;
            break;
        }
        case EditorInfo.IME_ACTION_SEARCH: {
            enterIcon = R.drawable.ic_action_search;
            break;
        }
        case EditorInfo.IME_ACTION_SEND: {
            enterText = R.string.send;
            break;
        }
        default: {
            enterIcon = R.drawable.sym_keyboard_return;
            break;
        }
        }

        itsEnterKey.label =
                (enterText != -1) ? res.getString(enterText) : null;
        itsEnterKey.icon =
                (enterIcon != -1) ?
                GuiUtils.getDrawable(res, enterIcon) : null;
    }

    @Override
    protected Key createKeyFromXml(@NonNull Resources res,
                                   @NonNull Row parent,
                                   int x, int y, XmlResourceParser parser)
    {
        Key key = super.createKeyFromXml(res, parent, x, y, parser);
        if (key.codes[0] == PasswdSafeIME.ENTER_KEY) {
            itsEnterKey = key;
        }
        return key;
    }
}
