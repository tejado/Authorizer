/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.view;

import java.util.Comparator;
import java.util.Date;

/**
 * Comparator for records
 */
public final class PasswdRecordListDataComparator
        implements Comparator<PasswdRecordListData>
{
    private final PasswdRecordDisplayOptions itsOptions;

    /**
     * Constructor
     */
    public PasswdRecordListDataComparator(PasswdRecordDisplayOptions options)
    {
        itsOptions = options;
    }

    @Override
    public int compare(PasswdRecordListData arg0, PasswdRecordListData arg1)
    {
        int rc;
        // Compare group order
        switch (itsOptions.itsGroupSortOrder) {
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

        rc = compareRecordField(arg0, arg1);
        if (!itsOptions.itsIsSortAscending) {
            rc = -rc;
        }
        return rc;
    }

    /**
     * Compare record field
     */
    private int compareRecordField(PasswdRecordListData arg0,
                                   PasswdRecordListData arg1)
    {
        int rc;
        switch (itsOptions.itsFieldSortOrder) {
        case TITLE: {
            break;
        }
        case CREATION_DATE: {
            rc = compareDateField(arg0.itsCreationTime, arg1.itsCreationTime);
            if (rc != 0) {
                return rc;
            }
            break;
        }
        case MOD_DATE: {
            Date date0 = (arg0.itsModTime != null) ?
                    arg0.itsModTime : arg0.itsCreationTime;
            Date date1 = (arg1.itsModTime != null) ?
                    arg1.itsModTime : arg1.itsCreationTime;
            rc = compareDateField(date0, date1);
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
            if (itsOptions.itsIsSortCaseSensitive) {
                return arg0.compareTo(arg1);
            } else {
                return arg0.compareToIgnoreCase(arg1);
            }
        }
    }

    /**
     * Compare two date fields
     */
    private int compareDateField(Date arg0, Date arg1)
    {
        if ((arg0 == null) && (arg1 == null)) {
            return 0;
        } else if ((arg0 != null) && (arg1 == null)) {
            return 1;
        } else if (arg0 == null) {
            return -1;
        } else {
            return arg0.compareTo(arg1);
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
