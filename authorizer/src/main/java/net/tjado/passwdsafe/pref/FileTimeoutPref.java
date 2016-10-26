/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.pref;

import android.content.res.Resources;

public enum FileTimeoutPref
{
    // Values in their display order
    TO_NONE     (0,             "",     0),
    TO_30_SEC   (30 * 1000,     "30",   1),
    TO_2_MIN    (120 * 1000,    "120",  2),
    TO_5_MIN    (300 * 1000,    "300",  3),
    TO_15_MIN   (900 * 1000,    "900",  4),
    TO_1_HR     (3600 * 1000,   "3600", 5);

    private final int itsTimeout;
    private final String itsValue;
    private final int itsDisplayNameIdx;

    FileTimeoutPref(int timeout, String value, int displayNameIdx)
    {
        itsTimeout = timeout;
        itsValue = value;
        itsDisplayNameIdx = displayNameIdx;
    }

    /// Get timeout in milliseconds
    public final int getTimeout()
    {
        return itsTimeout;
    }

    public final String getValue()
    {
        return itsValue;
    }

    private int getDisplayNameIdx()
    {
        return itsDisplayNameIdx;
    }

    public final String getDisplayName(Resources res)
    {
        return getDisplayNamesArray(res)[itsDisplayNameIdx];
    }

    public static FileTimeoutPref prefValueOf(String str)
    {
        for (FileTimeoutPref pref : FileTimeoutPref.values()) {
            if (pref.getValue().equals(str)) {
                return pref;
            }
        }
        throw new IllegalArgumentException(str);
    }

    public static String[] getValues()
    {
        FileTimeoutPref[] prefs = values();
        String[] strs = new String[prefs.length];
        for (int i = 0; i < prefs.length; ++i) {
            strs[i] = prefs[i].getValue();
        }
        return strs;
    }

    public static String[] getDisplayNames(Resources res)
    {
        String[] displayNames = getDisplayNamesArray(res);
        FileTimeoutPref[] prefs = values();
        String[] strs = new String[prefs.length];
        for (int i = 0; i < prefs.length; ++i) {
            strs[i] = displayNames[prefs[i].getDisplayNameIdx()];
        }
        return strs;
    }

    private static String[] getDisplayNamesArray(Resources res)
    {
        return res.getStringArray(net.tjado.passwdsafe.R.array.file_timeout_pref);
    }
}
