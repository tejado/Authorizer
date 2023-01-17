/*
 * Copyright (©) 2017 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.pref;

import android.content.res.Resources;

import net.tjado.passwdsafe.R;

import java.util.concurrent.TimeUnit;

/**
 *  Preference for timeout for showing a password
 */
public enum PasswdTimeoutPref
{
    // Values in their display order.  Names must be preserved.
    TO_NONE(0),
    TO_15_SEC(15),
    TO_30_SEC(30),
    TO_1_MIN(1*60),
    TO_5_MIN(5*60);

    private final long itsTimeout;

    /**
     * Constructor
     */
    PasswdTimeoutPref(int timeout)
    {
        itsTimeout = TimeUnit.SECONDS.toMillis(timeout);
    }

    /**
     * Get the timeout in milliseconds
     */
    public final long getTimeout()
    {
        return itsTimeout;
    }

    /**
     * Get the display name of the preference
     */
    public final String getDisplayName(Resources res)
    {
        return getDisplayNamesArray(res)[ordinal()];
    }

    /**
     * Get the preference from the saved value
     */
    public static PasswdTimeoutPref prefValueOf(String str)
    {
        return valueOf(str);
    }

    /**
     * Get the values of the preference values
     */
    public static String[] getValues()
    {
        PasswdTimeoutPref[] prefs = values();
        String[] strs = new String[prefs.length];
        for (int i = 0; i < prefs.length; ++i) {
            strs[i] = prefs[i].name();
        }
        return strs;
    }

    /**
     * Get the display names of the preference values
     */
    public static String[] getDisplayNames(Resources res)
    {
        String[] displayNames = getDisplayNamesArray(res);
        PasswdTimeoutPref[] prefs = values();
        String[] strs = new String[prefs.length];
        for (int i = 0; i < prefs.length; ++i) {
            strs[i] = displayNames[prefs[i].ordinal()];
        }
        return strs;
    }

    /**
     * Get the array of display names
     */
    private static String[] getDisplayNamesArray(Resources res)
    {
        return res.getStringArray(R.array.passwd_timeout_pref);
    }
}
