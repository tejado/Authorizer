/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.file;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pwsafe.lib.file.PwsRecord;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;

import net.tjado.passwdsafe.lib.Utils;

/** A filter for records */
public final class PasswdRecordFilter implements Parcelable
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
        ExpiryFilter(int expireRecordsIdx)
        {
            itsExpireRecordsIdx = expireRecordsIdx;
        }

        /** Get the filter value from its value index */
        public static ExpiryFilter fromIdx(int idx)
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
                                               net.tjado.passwdsafe.R.array.expire_filter_record :
                                               net.tjado.passwdsafe.R.array.expire_filter_records);
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

    /** Default options to match */
    public static final int OPTS_DEFAULT =          0;
    /** Record can not have an alias referencing it */
    public static final int OPTS_NO_ALIAS =         1 << 0;
    /** Record can not have a shortcut referencing it */
    public static final int OPTS_NO_SHORTCUT =      1 << 1;

    /** Filter type */
    private final Type itsType;

    /** Regex to match on various fields */
    private final Pattern itsSearchQuery;

    /** Expiration filter type */
    private final ExpiryFilter itsExpiryFilter;

    /** The expiration time to match on a record's expiration */
    private final long itsExpiryAtMillis;

    /** Filter options */
    private final int itsOptions;

    public static final String QUERY_MATCH = "";
    private String QUERY_MATCH_TITLE;
    private String QUERY_MATCH_USERNAME;
    private String QUERY_MATCH_URL;
    private String QUERY_MATCH_EMAIL;
    private String QUERY_MATCH_NOTES;

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
    public PasswdRecordFilter(ExpiryFilter filter, Date customDate, int opts)
    {
        itsType = Type.EXPIRATION;
        itsSearchQuery = null;
        itsExpiryFilter = filter;
        itsExpiryAtMillis = itsExpiryFilter.getExpiryFromNow(customDate);
        itsOptions = opts;
    }

    /** Serializable constructor for expiration */
    private PasswdRecordFilter(ExpiryFilter filter, long expiryMillis, int opts)
    {
        itsType = Type.EXPIRATION;
        itsSearchQuery = null;
        itsExpiryFilter = filter;
        itsExpiryAtMillis = expiryMillis;
        itsOptions = opts;
    }

    /* (non-Javadoc)
     * @see android.os.Parcelable#describeContents()
     */
    public int describeContents()
    {
        return 0;
    }

    /* (non-Javadoc)
     * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
     */
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(itsType.name());
        dest.writeInt(itsOptions);
        switch (itsType) {
        case QUERY: {
            dest.writeSerializable(itsSearchQuery);
            break;
        }
        case EXPIRATION: {
            dest.writeString(itsExpiryFilter.name());
            dest.writeLong(itsExpiryAtMillis);
            break;
        }
        }
    }

    public static final Parcelable.Creator<PasswdRecordFilter> CREATOR =
        new Parcelable.Creator<PasswdRecordFilter>()
        {
            public PasswdRecordFilter createFromParcel(Parcel source)
            {
                String typeStr = source.readString();
                Type type = Type.valueOf(typeStr);
                int options = source.readInt();
                switch (type) {
                case QUERY: {
                    Pattern query = (Pattern) source.readSerializable();
                    return new PasswdRecordFilter(query, options);
                }
                case EXPIRATION: {
                    ExpiryFilter expFilter =
                        ExpiryFilter.valueOf(source.readString());
                    long expMillis = source.readLong();
                    return new PasswdRecordFilter(expFilter, expMillis,
                                                  options);
                }
                }
                return null;
            }

            public PasswdRecordFilter[] newArray(int size)
            {
                return new PasswdRecordFilter[size];
            }
        };

    /**
     * Filter a record
     * @return A non-null string if the record matches the filter; null if it
     * does not
     */
    public final String filterRecord(PwsRecord rec,
                                     PasswdFileData fileData,
                                     Context ctx)
    {
        String queryMatch = null;
        switch (itsType) {
        case QUERY: {
            if (itsSearchQuery != null) {
                if (QUERY_MATCH_TITLE == null) {
                    QUERY_MATCH_TITLE = ctx.getString(net.tjado.passwdsafe.R.string.title);
                    QUERY_MATCH_USERNAME = ctx.getString(net.tjado.passwdsafe.R.string.username);
                    QUERY_MATCH_URL = ctx.getString(net.tjado.passwdsafe.R.string.url);
                    QUERY_MATCH_EMAIL = ctx.getString(net.tjado.passwdsafe.R.string.email);
                    QUERY_MATCH_NOTES = ctx.getString(net.tjado.passwdsafe.R.string.notes);
                }

                if (filterField(fileData.getTitle(rec))) {
                    queryMatch = QUERY_MATCH_TITLE;
                } else if (filterField(fileData.getUsername(rec))) {
                    queryMatch = QUERY_MATCH_USERNAME;
                } else if (filterField(fileData.getURL(rec))) {
                    queryMatch = QUERY_MATCH_URL;
                } else if (filterField(fileData.getEmail(rec))) {
                    queryMatch = QUERY_MATCH_EMAIL;
                } else if (filterField(fileData.getNotes(rec))) {
                    queryMatch = QUERY_MATCH_NOTES;
                }
            } else {
                queryMatch = QUERY_MATCH;
            }
            break;
        }
        case EXPIRATION: {
            PasswdExpiration expiry = fileData.getPasswdExpiry(rec);
            if (expiry == null) {
                break;
            }
            long expire = expiry.itsExpiration.getTime();
            if (expire < itsExpiryAtMillis) {
                queryMatch = Utils.formatDate(expire, ctx, true, true, true);
            }
            break;
        }
        }

        if ((queryMatch != null) &&
            (itsOptions != PasswdRecordFilter.OPTS_DEFAULT)) {
            PasswdRecord passwdRec = fileData.getPasswdRecord(rec);
            if (passwdRec != null) {
                for (PwsRecord ref: passwdRec.getRefsToRecord()) {
                    PasswdRecord passwdRef = fileData.getPasswdRecord(ref);
                    if (passwdRef == null) {
                        continue;
                    }
                    switch (passwdRef.getType()) {
                    case NORMAL: {
                        break;
                    }
                    case ALIAS: {
                        if (hasOptions(PasswdRecordFilter.OPTS_NO_ALIAS)) {
                            queryMatch = null;
                        }
                        break;
                    }
                    case SHORTCUT: {
                        if (hasOptions(PasswdRecordFilter.OPTS_NO_SHORTCUT)) {
                            queryMatch = null;
                        }
                        break;
                    }
                    }
                    if (queryMatch == null) {
                        break;
                    }
                }
            }
        }

        return queryMatch;
    }


    /**
     * Is the filter's type a query
     */
    public final boolean isQueryType()
    {
        switch (itsType) {
        case QUERY: {
            return true;
        }
        case EXPIRATION: {
            return false;
        }
        }
        return false;
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
                return ctx.getString(net.tjado.passwdsafe.R.string.password_expired);
            }
            case TODAY: {
                return ctx.getString(net.tjado.passwdsafe.R.string.password_expires_today);
            }
            case IN_A_WEEK:
            case IN_TWO_WEEKS:
            case IN_A_MONTH:
            case IN_A_YEAR:
            case CUSTOM: {
                return ctx.getString(
                        net.tjado.passwdsafe.R.string.password_expires_before,
                        Utils.formatDate(itsExpiryAtMillis, ctx,
                                     true, true, false));
            }
            case ANY: {
                return ctx.getString(net.tjado.passwdsafe.R.string.password_with_expiration);
            }
            }
        }
        }
        return "";
    }


    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public final boolean equals(Object o)
    {
        if (!(o instanceof PasswdRecordFilter)) {
            return false;
        }
        PasswdRecordFilter obj = (PasswdRecordFilter)o;
        if ((itsType != obj.itsType) ||
            (itsOptions != obj.itsOptions)) {
            return false;
        }
        switch (itsType) {
        case QUERY: {
            return
                itsSearchQuery.pattern().equals(obj.itsSearchQuery.pattern()) &&
                (itsSearchQuery.flags() == obj.itsSearchQuery.flags());
        }
        case EXPIRATION: {
            return
                ((itsExpiryFilter == obj.itsExpiryFilter) &&
                 (itsExpiryAtMillis == obj.itsExpiryAtMillis));
        }
        }
        return false;
    }


    /** Does the filter have the given options */
    private boolean hasOptions(int opts)
    {
        return (itsOptions & opts) != 0;
    }


    /** Match a field against the search query */
    private boolean filterField(String field)
    {
        if (field != null) {
            Matcher m = itsSearchQuery.matcher(field);
            return m.find();
        } else {
            return false;
        }
    }

}
