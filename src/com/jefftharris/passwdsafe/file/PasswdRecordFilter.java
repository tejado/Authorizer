/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.file;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import android.content.Context;

import com.jefftharris.passwdsafe.R;
import com.jefftharris.passwdsafe.util.Utils;

/** A filter for records */
public final class PasswdRecordFilter
{
    /** Type of filter */
    public enum Type
    {
        QUERY,
        EXPIRATION
    }

    /** Expiration filter type */
    public enum ExpiryFilter
    {
        // Order must match expire_filters string array
        EXPIRED,
        TODAY,
        IN_A_WEEK,
        IN_A_MONTH,
        IN_A_YEAR,
        ANY,
        CUSTOM;

        /** Get the filter value from its value index */
        public static PasswdRecordFilter.ExpiryFilter fromIdx(int idx)
        {
            if ((idx >= 0) && (idx < values().length)) {
                return values()[idx];
            }
            return ANY;
        }
    }

    /** Default options to match */
    public static final int OPTS_DEFAULT =          0;
    /** Record can not have an alias referencing it */
    public static final int OPTS_NO_ALIAS =         1 << 0;
    /** Record can not have a shortcut referencing it */
    public static final int OPTS_NO_SHORTCUT =      1 << 1;

    /** Filter type */
    public final PasswdRecordFilter.Type itsType;

    /** Regex to match on various fields */
    public final Pattern itsSearchQuery;

    /** Expiration filter type */
    public final PasswdRecordFilter.ExpiryFilter itsExpiryFilter;

    /** The expiration time to match on a record's expiration */
    public final long itsExpiryAtMillis;

    /** Filter options */
    public final int itsOptions;

    /** Constructor for a query */
    public PasswdRecordFilter(Pattern query, int opts)
    {
        itsType = Type.QUERY;
        itsSearchQuery = query;
        itsExpiryFilter = ExpiryFilter.ANY;
        itsExpiryAtMillis = 0;
        itsOptions = opts;
    }

    /** Constructor for expiration */
    public PasswdRecordFilter(PasswdRecordFilter.ExpiryFilter filter, Date customDate,
                              int opts)
    {
        itsType = Type.EXPIRATION;
        itsSearchQuery = null;
        itsExpiryFilter = filter;
        Calendar expiry = Calendar.getInstance();
        switch (itsExpiryFilter) {
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
            expiry.setTime(customDate);
            break;
        }
        }

        itsExpiryAtMillis = expiry.getTimeInMillis();
        itsOptions = opts;
    }

    /** Does the filter have the given options */
    public final boolean hasOptions(int opts)
    {
        return (itsOptions & opts) != 0;
    }

    /** Convert the filter to a string */
    public final String toString(Context ctx)
    {
        switch (itsType) {
        case QUERY: {
            if (itsSearchQuery != null) {
                return itsSearchQuery.pattern();
            }
            break;
        }
        case EXPIRATION: {
            switch (itsExpiryFilter) {
            case EXPIRED: {
                return ctx.getString(R.string.password_expired);
            }
            case TODAY: {
                return ctx.getString(R.string.password_expires_today);
            }
            case IN_A_WEEK:
            case IN_A_MONTH:
            case IN_A_YEAR:
            case CUSTOM: {
                return ctx.getString(
                    R.string.password_expires_before,
                    Utils.formatDate(itsExpiryAtMillis, ctx, true, true));
            }
            case ANY: {
                return ctx.getString(R.string.password_with_expiration);
            }
            }
        }
        }
        return "";
    }
}