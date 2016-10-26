/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;

/**
 * The keyboard view for the PasswdSafe IME
 */
public class PasswdSafeIMEKeyboardView extends KeyboardView
{
    /**
     * Constructor
     */
    public PasswdSafeIMEKeyboardView(Context context,
                                     AttributeSet attrs)
    {
        super(context, attrs);
    }

    /**
     * Constructor
     */
    public PasswdSafeIMEKeyboardView(Context context, AttributeSet attrs,
                                     int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected boolean onLongPress(Keyboard.Key key)
    {
        switch (key.codes[0]) {
        case ' ':
        case PasswdSafeIME.KEYBOARD_NEXT_KEY:
        case PasswdSafeIME.KEYBOARD_CHOOSE_KEY: {
            getOnKeyboardActionListener().onKey(
                    PasswdSafeIME.KEYBOARD_CHOOSE_KEY, null);
            return true;
        }
        default: {
            return super.onLongPress(key);
        }
        }
    }
}
