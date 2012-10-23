/*
 * Copyright (©) 2009-2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.content.res.Resources;

public enum FontSizePref
{
    // Values in their display order
    NORMAL  (0),
    SMALL   (1);

    private final int itsDisplayNameIdx;

    private FontSizePref(int displayNameIdx)
    {
        itsDisplayNameIdx = displayNameIdx;
    }

    public final int getDisplayNameIdx()
    {
        return itsDisplayNameIdx;
    }

    public final String getDisplayName(Resources res)
    {
        return getDisplayNamesArray(res)[itsDisplayNameIdx];
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

    public static String[] getDisplayNames(Resources res)
    {
        String[] displayNames = getDisplayNamesArray(res);
        FontSizePref[] prefs = values();
        String[] strs = new String[prefs.length];
        for (int i = 0; i < prefs.length; ++i) {
            strs[i] = displayNames[prefs[i].getDisplayNameIdx()];
        }
        return strs;
    }

    private static final String[] getDisplayNamesArray(Resources res)
    {
        return res.getStringArray(R.array.font_size_pref);
    }
}