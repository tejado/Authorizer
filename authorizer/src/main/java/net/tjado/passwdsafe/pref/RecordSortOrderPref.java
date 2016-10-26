/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.pref;

import android.content.res.Resources;

import net.tjado.passwdsafe.R;

/**
 * Preference for how to sort records and groups
 */
public enum RecordSortOrderPref
{
    // Values in display order
    GROUP_FIRST("GROUP_FIRST"),
    GROUP_INLINE("GROUP_INLINE"),
    GROUP_LAST("GROUP_LAST");

    private final String itsValue;

    /** Constructor */
    RecordSortOrderPref(String value)
    {
        itsValue = value;
    }

    /** Get the display name of the preference */
    public final String getDisplayName(Resources res)
    {
        return getDisplayNamesArray(res)[ordinal()];
    }

    /** Get all of the preference values */
    public static String[] getValues()
    {
        RecordSortOrderPref[] prefs = values();
        String[] strs = new String[prefs.length];
        for (int i = 0; i < prefs.length; ++i) {
            strs[i] = prefs[i].itsValue;
        }
        return strs;
    }

    /** Get all of the display names of the preferences */
    public static String[] getDisplayNames(Resources res)
    {
        String[] displayNames = getDisplayNamesArray(res);
        RecordSortOrderPref[] prefs = values();
        String[] strs = new String[prefs.length];
        for (int i = 0; i < prefs.length; ++i) {
            strs[i] = displayNames[prefs[i].ordinal()];
        }
        return strs;
    }

    /** Get the display names array for the preferences */
    private static String[] getDisplayNamesArray(Resources res)
    {
        return res.getStringArray(R.array.record_sort_order_pref);
    }
}
