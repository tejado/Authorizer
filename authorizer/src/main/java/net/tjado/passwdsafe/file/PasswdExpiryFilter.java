/*
 * Copyright (©) 2022 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.file;

import android.content.res.Resources;

import net.tjado.passwdsafe.R;

import java.util.Calendar;
import java.util.Date;

/**
 * Password expiration filter type
 */
public enum PasswdExpiryFilter
{
    // Order must match expire_filters string array
    EXPIRED         (0),
    TODAY           (1),
    IN_A_WEEK       (2),
    IN_TWO_WEEKS    (3),
    IN_A_MONTH      (4),
    IN_A_YEAR       (5),
    ANY             (-1),
    CUSTOM          (-1);

    private final int itsExpireRecordsIdx;

    /** Constructor */
    PasswdExpiryFilter(int expireRecordsIdx)
    {
        itsExpireRecordsIdx = expireRecordsIdx;
    }

    /** Get the filter value from its value index */
    public static PasswdExpiryFilter fromIdx(int idx)
    {
        if ((idx >= 0) && (idx < values().length)) {
            return values()[idx];
        }
        return ANY;
    }

    /**
     * Get a string indicating how many records expire based on the filter
     * type
     */
    public String getRecordsExpireStr(int numRecords, Resources res)
    {
        if (itsExpireRecordsIdx == -1) {
            throw new IllegalArgumentException("No str");
        }
        String[] strs = res.getStringArray((numRecords == 1) ?
                                           R.array.expire_filter_record :
                                           R.array.expire_filter_records);
        return String.format(strs[itsExpireRecordsIdx], numRecords);
    }

    /** Get the expiration date from now based on the filter type */
    public long getExpiryFromNow(Date customDate)
    {
        Calendar expiry = Calendar.getInstance();
        switch (this) {
        case EXPIRED: {
            break;
        }
        case TODAY: {
            expiry.add(Calendar.DAY_OF_MONTH, 1);
            expiry.set(Calendar.HOUR_OF_DAY, 0);
            expiry.set(Calendar.MINUTE, 0);
            expiry.set(Calendar.SECOND, 0);
            expiry.set(Calendar.MILLISECOND, 0);
            break;
        }
        case IN_A_WEEK: {
            expiry.add(Calendar.WEEK_OF_YEAR, 1);
            break;
        }
        case IN_TWO_WEEKS: {
            expiry.add(Calendar.WEEK_OF_YEAR, 2);
            break;
        }
        case IN_A_MONTH: {
            expiry.add(Calendar.MONTH, 1);
            break;
        }
        case IN_A_YEAR: {
            expiry.add(Calendar.YEAR, 1);
            break;
        }
        case ANY: {
            expiry.setTimeInMillis(Long.MAX_VALUE);
            break;
        }
        case CUSTOM: {
            if (customDate != null) {
                expiry.setTime(customDate);
            }
            break;
        }
        }
        return expiry.getTimeInMillis();
    }
}
