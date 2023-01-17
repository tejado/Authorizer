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
 * Preference for how to sort record fields
 */
public enum RecordFieldSortPref
{
    // Values in display order
    TITLE("TITLE"),
    CREATION_DATE("CREATION_DATE"),
    MOD_DATE("MOD_DATE");

    private final String itsValue;

    /**
     * Constructor
     */
    RecordFieldSortPref(String value)
    {
        itsValue = value;
    }

    /**
     * Get the display name of the preference
     */
    public final String getDisplayName(Resources res)
    {
        return getDisplayNamesArray(res)[ordinal()];
    }

    /** Get all of the preference values */
    public static String[] getValues()
    {
        RecordFieldSortPref[] prefs = values();
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
        RecordFieldSortPref[] prefs = values();
        String[] strs = new String[prefs.length];
        for (int i = 0; i < prefs.length; ++i) {
            strs[i] = displayNames[prefs[i].ordinal()];
        }
        return strs;
    }

    /** Get the display names array for the preferences */
    private static String[] getDisplayNamesArray(Resources res)
    {
        return res.getStringArray(R.array.record_field_sort_pref);
    }
}
