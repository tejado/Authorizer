/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.view;

import android.content.SharedPreferences;

import net.tjado.passwdsafe.Preferences;
import net.tjado.passwdsafe.pref.RecordFieldSortPref;
import net.tjado.passwdsafe.pref.RecordSortOrderPref;

/**
 * Record display options
 */
public final class PasswdRecordDisplayOptions
{
    public final boolean itsIsSortCaseSensitive;
    public final boolean itsIsSortAscending;
    public final boolean itsIsGroupRecords;
    public final RecordSortOrderPref itsGroupSortOrder;
    public final RecordFieldSortPref itsFieldSortOrder;

    /**
     * Default constructor
     */
    public PasswdRecordDisplayOptions()
    {
        itsIsSortCaseSensitive = Preferences.PREF_SORT_CASE_SENSITIVE_DEF;
        itsIsSortAscending = Preferences.PREF_SORT_ASCENDING_DEF;
        itsIsGroupRecords = Preferences.PREF_GROUP_RECORDS_DEF;
        itsGroupSortOrder = Preferences.PREF_RECORD_SORT_ORDER_DEF;
        itsFieldSortOrder = Preferences.PREF_RECORD_FIELD_SORT_DEF;
    }

    /**
     * Constructor
     */
    public PasswdRecordDisplayOptions(SharedPreferences prefs)
    {
        itsIsSortCaseSensitive =
                Preferences.getSortCaseSensitivePref(prefs);
        itsIsSortAscending = Preferences.getSortAscendingPref(prefs);
        itsIsGroupRecords = Preferences.getGroupRecordsPref(prefs);
        itsGroupSortOrder = Preferences.getRecordSortOrderPref(prefs);
        itsFieldSortOrder = Preferences.getRecordFieldSortPref(prefs);
    }
}
