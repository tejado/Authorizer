/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.view;

import net.tjado.passwdsafe.pref.RecordSortOrderPref;

import java.util.Comparator;

/**
 * Comparator for records
 */
public final class PasswdRecordListDataComparator
        implements Comparator<PasswdRecordListData>
{
    private final boolean itsIsSortCaseSensitive;
    private final RecordSortOrderPref itsSortOrder;

    /**
     * Constructor
     */
    public PasswdRecordListDataComparator(boolean sortCaseSensitive,
                                          RecordSortOrderPref sortOrder)
    {
        itsIsSortCaseSensitive = sortCaseSensitive;
        itsSortOrder = sortOrder;
    }

    @Override
    public int compare(PasswdRecordListData arg0,
                       PasswdRecordListData arg1)
    {
        int rc;
        // Compare group order
        switch (itsSortOrder) {
        case GROUP_FIRST: {
            rc = compareIsGroup(arg0, arg1);
            if (rc != 0) {
                return rc;
            }
            break;
        }
        case GROUP_INLINE: {
            break;
        }
        case GROUP_LAST: {
            rc = -compareIsGroup(arg0, arg1);
            if (rc != 0) {
                return rc;
            }
            break;
        }
        }

        rc = compareField(arg0.itsTitle, arg1.itsTitle);
        if (rc != 0) {
            return rc;
        }
        return compareField(arg0.itsUser, arg1.itsUser);
    }

    /**
     * Compare two string fields
     */
    private int compareField(String arg0, String arg1)
    {
        if ((arg0 == null) && (arg1 == null)) {
            return 0;
        } else if (arg0 == null) {
            return -1;
        } else if (arg1 == null) {
            return 1;
        } else {
            if (itsIsSortCaseSensitive) {
                return arg0.compareTo(arg1);
            } else {
                return arg0.compareToIgnoreCase(arg1);
            }
        }
    }

    /**
     * Compare whether the item is a group or not
     */
    private int compareIsGroup(PasswdRecordListData arg0,
                               PasswdRecordListData arg1)
    {
        if (!arg0.itsIsRecord && arg1.itsIsRecord) {
            return -1;
        } else if (arg0.itsIsRecord && !arg1.itsIsRecord) {
            return 1;
        }
        return 0;
    }
}
