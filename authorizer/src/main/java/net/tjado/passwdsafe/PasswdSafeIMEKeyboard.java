/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.inputmethodservice.Keyboard;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;

import net.tjado.passwdsafe.lib.view.GuiUtils;

/**
 * The PasswdSafeIMEKeyboard class is a keyboard for PasswdSafe
 */
public final class PasswdSafeIMEKeyboard extends Keyboard
{
    private Key itsEnterKey;
    private PasswdSafeKey itsPasswordKey;

    /**
     * Constructor
     */
    public PasswdSafeIMEKeyboard(Context context, int xmlLayoutResId)
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

    /**
     * Set whether a previous password should be enabled
     */
    public void setHasPreviousPassword(boolean hasPreviousPassword)
    {
        itsPasswordKey.setHasLongPress(hasPreviousPassword,
                                       R.xml.keyboard_popup_password);
    }

    @Override
    protected Key createKeyFromXml(@NonNull Resources res,
                                   @NonNull Row parent,
                                   int x, int y, XmlResourceParser parser)
    {
        PasswdSafeKey key = new PasswdSafeKey(res, parent, x, y, parser);
        switch (key.codes[0]) {
        case PasswdSafeIME.ENTER_KEY: {
            itsEnterKey = key;
            break;
        }
        case PasswdSafeIME.PASSWORD_KEY: {
            itsPasswordKey = key;
            break;
        }
        }
        return key;
    }

    /**
     * Key class to use a custom drawable state
     */
    private static final class PasswdSafeKey extends Key
    {
        private static final int[] ACTION_KEY_NORMAL = {
                android.R.attr.state_single };
        private static final int[] ACTION_KEY_PRESSED = {
                android.R.attr.state_single, android.R.attr.state_pressed };

        private final CharSequence itsLabel;

        /**
         * Constructor
         */
        private PasswdSafeKey(Resources res,
                              Row parent,
                              int x,
                              int y,
                              XmlResourceParser parser)
        {
            super(res, parent, x, y, parser);
            itsLabel = label;
        }

        /**
         * Set whether the key has a popup for long-press behavior
         */
        private void setHasLongPress(boolean hasLongPress,
                                     @SuppressWarnings("SameParameterValue") int longPressPopup)
        {
            if (hasLongPress) {
                label = itsLabel + " …";
                popupResId = longPressPopup;
            } else {
                label = itsLabel;
                popupResId = 0;
            }
        }

        @Override
        public int[] getCurrentDrawableState()
        {
            switch (codes[0]) {
            case Keyboard.KEYCODE_DELETE:
            case Keyboard.KEYCODE_MODE_CHANGE:
            case PasswdSafeIME.ENTER_KEY:
            case PasswdSafeIME.KEYBOARD_CHOOSE_KEY:
            case PasswdSafeIME.KEYBOARD_NEXT_KEY:
            case PasswdSafeIME.PASSWDSAFE_KEY: {
                if (pressed) {
                    return ACTION_KEY_PRESSED;
                }
                return ACTION_KEY_NORMAL;
            }
            default: {
                return super.getCurrentDrawableState();
            }
            }
        }
    }
}
