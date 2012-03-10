/*
 * Copyright (©) 2009-2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

public enum FontSizePref
{
    // Values in their display order
    NORMAL  ("Normal"),
    SMALL   ("Small");

    private final String itsDisplayName;

    private FontSizePref(String displayName)
    {
        itsDisplayName = displayName;
    }

    public final String getDisplayName()
    {
        return itsDisplayName;
    }

    public static String[] getValues()
    {
        FontSizePref[] prefs = values();
        String[] strs = new String[prefs.length];
        for (int i = 0; i < prefs.length; ++i) {
            strs[i] = prefs[i].toString();
        }
        return strs;
    }

    public static String[] getDisplayNames()
    {
        FontSizePref[] prefs = values();
        String[] strs = new String[prefs.length];
        for (int i = 0; i < prefs.length; ++i) {
            strs[i] = prefs[i].getDisplayName();
        }
        return strs;
    }
}