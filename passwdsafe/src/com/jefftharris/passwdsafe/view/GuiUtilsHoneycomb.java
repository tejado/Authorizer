/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.view;

import java.lang.reflect.Method;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.IBinder;
import android.view.inputmethod.InputMethodManager;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;


/**
 * The GuiUtilsHoneycomb class contains helper GUI methods that are usable on
 * Honeycomb and higher
 */
@TargetApi(11)
public final class GuiUtilsHoneycomb
{
    private static Method itsInvalidateOptionsMenuMeth;
    private static Method itsSwitchLastIMMeth;

    static {
        try {
            itsInvalidateOptionsMenuMeth =
                Activity.class.getMethod("invalidateOptionsMenu");
            itsSwitchLastIMMeth =
                InputMethodManager.class.getMethod("switchToLastInputMethod",
                                                   IBinder.class);
        } catch (NoSuchMethodException e) {
            // No method
        }
    }

    /**
     * Invalidate the options menu on an activity
     */
    public static void invalidateOptionsMenu(Activity act)
    {
        try {
            itsInvalidateOptionsMenuMeth.invoke(act);
        }
        catch (Exception e) {
            PasswdSafeUtil.showFatalMsg(e, act);
        }
    }

    /** Try to switch to the previous input method */
    public static void switchToLastInputMethod(InputMethodManager mgr,
                                               IBinder token)
    {
        try {
            itsSwitchLastIMMeth.invoke(mgr, token);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
