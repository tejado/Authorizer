/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.file;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pwsafe.lib.file.PwsRecord;

import android.content.Context;

import net.tjado.passwdsafe.R;
import net.tjado.passwdsafe.lib.Utils;

/** A filter for records */
public final class PasswdRecordFilter
{
    /** Type of filter */
    public enum Type
    {
        QUERY,
        EXPIRATION
    }

    /** Default options to match */
    public static final int OPTS_DEFAULT =          0;
    /** Record can not have an alias referencing it */
    public static final int OPTS_NO_ALIAS =         1 << 0;
    /** Record can not have a shortcut referencing it */
    public static final int OPTS_NO_SHORTCUT =      1 << 1;
    /** Record group not matched */
    public static final int OPTS_NO_GROUP =         1 << 2;

    /** Record can not have an alias or shortcut referencing it */
    private static final int OPTS_NO_ALIASSHORT =
            (OPTS_NO_ALIAS | OPTS_NO_SHORTCUT);

    /** Filter type */
    private final Type itsType;

    /** Regex to match on various fields */
    private final Pattern itsSearchQuery;

    /** Expiration filter type */
    private final PasswdExpiryFilter itsExpiryFilter;

    /** The expiration time to match on a record's expiration */
    private final long itsExpiryAtMillis;

    /** Filter options */
    private final int itsOptions;

    public static final String QUERY_MATCH = "";
    private static String QUERY_MATCH_TITLE;
    private static String QUERY_MATCH_USERNAME;
    private static String QUERY_MATCH_PASSWORD;
    private static String QUERY_MATCH_URL;
    private static String QUERY_MATCH_EMAIL;
    private static String QUERY_MATCH_NOTES;
    public static String QUERY_MATCH_GROUP;

    /** Search view result data prefix for a record */
    public static final String SEARCH_VIEW_RECORD = "REC:";
    /** Search view result data prefix for a group */
    public static final String SEARCH_VIEW_GROUP = "GRP:";

    /** Constructor for a query */
    public PasswdRecordFilter(Pattern query, int opts)
    {
        itsType = Type.QUERY;
        itsSearchQuery = query;
        itsExpiryFilter = PasswdExpiryFilter.ANY;
        itsExpiryAtMillis = 0;
        itsOptions = opts;
    }

    /** Constructor for expiration */
    public PasswdRecordFilter(PasswdExpiryFilter filter, Date customDate)
    {
        itsType = Type.EXPIRATION;
        itsSearchQuery = null;
        itsExpiryFilter = filter;
        itsExpiryAtMillis = itsExpiryFilter.getExpiryFromNow(customDate);
        itsOptions = OPTS_DEFAULT;
    }

    /**
     * Initialize the query matches
     */
    public static void initMatches(Context ctx)
    {
        if (QUERY_MATCH_TITLE == null) {
            QUERY_MATCH_TITLE = ctx.getString(R.string.title);
            QUERY_MATCH_USERNAME = ctx.getString(R.string.username);
            QUERY_MATCH_PASSWORD = ctx.getString(R.string.password);
            QUERY_MATCH_URL = ctx.getString(R.string.url);
            QUERY_MATCH_EMAIL = ctx.getString(R.string.email);
            QUERY_MATCH_NOTES = ctx.getString(R.string.notes);
            QUERY_MATCH_GROUP = ctx.getString(R.string.group);
        }
    }

    /**
     * Filter a record
     * @return A non-null string if the record matches the filter; null if it
     * does not
     */
    public String filterRecord(PwsRecord rec,
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
                } else if (filterField(fileData.getURL(rec, PasswdFileData.UrlStyle.FULL))) {
                    queryMatch = QUERY_MATCH_URL;
                } else if (filterField(fileData.getEmail(rec, PasswdFileData.EmailStyle.FULL))) {
                    queryMatch = QUERY_MATCH_EMAIL;
                } else if (filterField(fileData.getNotes(rec, ctx).getNotes())) {
                    queryMatch = QUERY_MATCH_NOTES;
                } else if (!hasOptions(OPTS_NO_GROUP) &&
                           filterField(fileData.getGroup(rec))) {
                    queryMatch = QUERY_MATCH_GROUP;
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

        if ((queryMatch != null) && hasOptions(PasswdRecordFilter.OPTS_NO_ALIASSHORT)) {
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
     * Match a record's group against the filter
     * @return The group if matched; null otherwise
     */
    public String matchGroup(PwsRecord rec, PasswdFileData fileData)
    {
        switch (itsType) {
        case QUERY: {
            if (itsSearchQuery != null) {
                String group = fileData.getGroup(rec);
                if (filterField(group)) {
                    return group;
                }
            }
            break;
        }
        case EXPIRATION: {
            break;
        }
        }

        return null;
    }


    /**
     * Is the filter's type a query
     */
    public boolean isQueryType()
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
    public String toString(Context ctx)
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
