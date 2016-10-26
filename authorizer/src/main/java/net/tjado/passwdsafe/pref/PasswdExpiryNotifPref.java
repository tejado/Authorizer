/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.pref;

import net.tjado.passwdsafe.file.PasswdRecordFilter;

import android.content.res.Resources;

/**
 * Preference for notification of expired passwords
 */
public enum PasswdExpiryNotifPref
{
    // Values in display order
    NONE            ("",
                     null),
    EXPIRED         ("EXPIRED",
                     PasswdRecordFilter.ExpiryFilter.EXPIRED),
    TODAY           ("TODAY",
                     PasswdRecordFilter.ExpiryFilter.TODAY),
    IN_A_WEEK       ("IN_A_WEEK",
                     PasswdRecordFilter.ExpiryFilter.IN_A_WEEK),
    IN_TWO_WEEKS    ("IN_TWO_WEEKS",
                     PasswdRecordFilter.ExpiryFilter.IN_TWO_WEEKS),
    IN_A_MONTH      ("IN_A_MONTH",
                     PasswdRecordFilter.ExpiryFilter.IN_A_MONTH),
    IN_A_YEAR       ("IN_A_YEAR",
                     PasswdRecordFilter.ExpiryFilter.IN_A_YEAR);

    private final String itsValue;
    private final PasswdRecordFilter.ExpiryFilter itsFilter;

    /** Constructor */
    PasswdExpiryNotifPref(String value, PasswdRecordFilter.ExpiryFilter filter)
    {
        itsValue = value;
        itsFilter = filter;
    }

    /** Get the value of the preference */
    public final String getValue()
    {
        return itsValue;
    }

    /** Get the expiration filter from the preference */
    public final PasswdRecordFilter.ExpiryFilter getFilter()
    {
        return itsFilter;
    }

    /** Get the display name of the preference */
    public final String getDisplayName(Resources res)
    {
        return getDisplayNamesArray(res)[ordinal()];
    }

    /** Get the preference from its value */
    public static PasswdExpiryNotifPref prefValueOf(String str)
    {
        for (PasswdExpiryNotifPref pref : values()) {
            if (pref.getValue().equals(str)) {
                return pref;
            }
        }
        throw new IllegalArgumentException(str);
    }

    /** Get all of the preference values */
    public static String[] getValues()
    {
        PasswdExpiryNotifPref[] prefs = values();
        String[] strs = new String[prefs.length];
        for (int i = 0; i < prefs.length; ++i) {
            strs[i] = prefs[i].getValue();
        }
        return strs;
    }

    /** Get all of the display names of the preferences */
    public static String[] getDisplayNames(Resources res)
    {
        String[] displayNames = getDisplayNamesArray(res);
        PasswdExpiryNotifPref[] prefs = values();
        String[] strs = new String[prefs.length];
        for (int i = 0; i < prefs.length; ++i) {
            strs[i] = displayNames[prefs[i].ordinal()];
        }
        return strs;
    }

    /** Get the display names array for the preferences */
    private static String[] getDisplayNamesArray(Resources res)
    {
        return res.getStringArray(net.tjado.passwdsafe.R.array.passwd_expiry_notif_pref);
    }
}
