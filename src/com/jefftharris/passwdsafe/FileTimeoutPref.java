/*
 * Copyright (©) 2009-2011 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

public enum FileTimeoutPref
{
    // Values in their display order
    TO_NONE     (0,             "",     "None"),
    TO_30_SEC   (30 * 1000,     "30",   "30 seconds"),
    TO_2_MIN    (120 * 1000,    "120",  "2 minutes"),
    TO_5_MIN    (300 * 1000,    "300",  "5 minutes"),
    TO_15_MIN   (900 * 1000,    "900",  "15 minutes"),
    TO_1_HR     (3600 * 1000,   "3600", "1 hour");

    private final int itsTimeout;
    private final String itsValue;
    private final String itsDisplayName;

    private FileTimeoutPref(int timeout, String value, String displayName)
    {
        itsTimeout = timeout;
        itsValue = value;
        itsDisplayName = displayName;
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

    public final String getDisplayName()
    {
        return itsDisplayName;
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

    public static String[] getDisplayNames()
    {
        FileTimeoutPref[] prefs = values();
        String[] strs = new String[prefs.length];
        for (int i = 0; i < prefs.length; ++i) {
            strs[i] = prefs[i].getDisplayName();
        }
        return strs;
    }
}
