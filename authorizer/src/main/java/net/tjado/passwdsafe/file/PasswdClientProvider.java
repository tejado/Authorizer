/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.file;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.tjado.passwdsafe.PasswdSafeFileDataFragment;
import net.tjado.passwdsafe.Preferences;
import net.tjado.passwdsafe.lib.PasswdSafeContract;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;

import org.pwsafe.lib.file.PwsRecord;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *  The PasswdClientProvider class is a content provider for the PasswdSafe
 *  client password files
 */
public class PasswdClientProvider extends ContentProvider
        implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final UriMatcher MATCHER;
    private static final int MATCH_FILES = 1;
    private static final int MATCH_SEARCH_SUGGESTIONS = 2;

    private static final String TAG = "PasswdClientProvider";

    private static PasswdClientProvider itsProvider = null;
    private static final Object itsProviderLock = new Object();
    private final Set<String> itsFiles = new HashSet<>();
    private int itsSearchFlags = 0;
    private MatchComparator itsSearchComp = new MatchComparator(true, false);

    static {
        MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        MATCHER.addURI(PasswdSafeContract.CLIENT_AUTHORITY,
                       PasswdSafeContract.ClientFiles.TABLE + "/*",
                       MATCH_FILES);
        MATCHER.addURI(PasswdSafeContract.CLIENT_AUTHORITY,
                       PasswdSafeContract.CLIENT_SEARCH_SUGGESTIONS,
                       MATCH_SEARCH_SUGGESTIONS);
    }

    /** Add a file to those provided and return the URI to access it */
    public static Uri addFile(File file)
    {
        String name = file.getAbsolutePath();
        Uri uri = PasswdSafeContract.CLIENT_CONTENT_URI.buildUpon()
                                                       .appendPath(PasswdSafeContract.ClientFiles.TABLE)
                                                       .appendPath(name)
                                                       .build();
        synchronized (itsProviderLock) {
            itsProvider.itsFiles.add(name);
        }
        return uri;
    }

    /** Remove a file from those provided */
    public static void removeFile(File file)
    {
        synchronized (itsProviderLock) {
            itsProvider.itsFiles.remove(file.getAbsolutePath());
        }
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#openFile(android.net.Uri, java.lang.String)
     */
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode)
            throws FileNotFoundException
    {
        if (!mode.equals("r")) {
            throw new IllegalArgumentException("Invalid mode: " + mode);
        }

        switch (MATCHER.match(uri)) {
        case MATCH_FILES: {
            String fileName = uri.getLastPathSegment();
            synchronized (this) {
                if ((fileName == null) || !itsFiles.contains(fileName)) {
                    throw new FileNotFoundException(fileName);
                }
                File file = new File(fileName);
                return ParcelFileDescriptor.open(
                        file, ParcelFileDescriptor.MODE_READ_ONLY);
            }
        }
        case MATCH_SEARCH_SUGGESTIONS: {
            break;
        }
        }
        return super.openFile(uri, mode);
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
     */
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs)
    {
        return 0;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#getType(android.net.Uri)
     */
    @Override
    public String getType(@NonNull Uri uri)
    {
        return null;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues)
     */
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values)
    {
        return null;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#onCreate()
     */
    @Override
    public boolean onCreate()
    {
        itsProvider = this;
        SharedPreferences prefs = Preferences.getSharedPrefs(getContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        updatePrefs(prefs);
        return true;
    }

    @Override
    public void shutdown()
    {
        super.shutdown();
        SharedPreferences prefs = Preferences.getSharedPrefs(getContext());
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
     */
    @Override
    public Cursor query(@NonNull Uri uri,
                        String[] projection,
                        String selection,
                        String[] selectionArgs,
                        String sortOrder)
    {
        PasswdSafeUtil.dbginfo(TAG, "query uri: %s", uri);
        switch (MATCHER.match(uri)) {
        case MATCH_FILES: {
            return null;
        }
        case MATCH_SEARCH_SUGGESTIONS: {
            if (selectionArgs.length != 1) {
                break;
            }
            String query = selectionArgs[0];
            if ((query == null) || (query.length() < 2)) {
                return null;
            }

            int limit = -1;
            try {
                String limitStr = uri.getQueryParameter("limit");
                if (!TextUtils.isEmpty(limitStr)) {
                    limit = Integer.parseInt(Objects.requireNonNull(limitStr));
                }
            } catch (NumberFormatException e) {
                // ignore
            }

            PasswdSafeUtil.dbginfo(TAG, "query suggestions: %s", query);
            Pattern queryPattern;
            MatchComparator comparator;
            synchronized (this) {
                queryPattern = Pattern.compile(query, itsSearchFlags);
                comparator = itsSearchComp;
            }
            // Groups are matched separately
            PasswdRecordFilter filter = new PasswdRecordFilter(
                    queryPattern, PasswdRecordFilter.OPTS_NO_GROUP);
            return PasswdSafeFileDataFragment.useOpenFileData(
                    new SuggestionsUser(filter, limit,
                                        comparator, getContext()));
        }
        }
        throw new IllegalArgumentException("query unknown: " + uri);
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[])
     */
    @Override
    public int update(@NonNull Uri uri,
                      ContentValues values,
                      String selection,
                      String[] selectionArgs)
    {
        return 0;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs,
                                          @Nullable String key)
    {
        if (key == null) {
            updatePrefs(prefs);
        } else {
            switch (key) {
            case Preferences.PREF_SORT_ASCENDING:
            case Preferences.PREF_SORT_CASE_SENSITIVE:
            case Preferences.PREF_SEARCH_CASE_SENSITIVE:
            case Preferences.PREF_SEARCH_REGEX: {
                updatePrefs(prefs);
                break;
            }
            }
        }
    }

    /**
     * Update search and sort preferences
     */
    private synchronized void updatePrefs(SharedPreferences prefs)
    {
        itsSearchFlags = 0;
        if (!Preferences.getSearchCaseSensitivePref(prefs)) {
            itsSearchFlags |= Pattern.CASE_INSENSITIVE;
        }
        if (!Preferences.getSearchRegexPref(prefs)) {
            itsSearchFlags |= Pattern.LITERAL;
        }

        itsSearchComp = new MatchComparator(
                Preferences.getSortAscendingPref(prefs),
                Preferences.getSortCaseSensitivePref(prefs));
    }

    /**
     * PasswdFileData user for search suggestions
     */
    private static class SuggestionsUser implements PasswdFileDataUser<Cursor>
    {
        private final PasswdRecordFilter itsFilter;
        private final int itsLimit;
        private final MatchComparator itsComparator;
        private final Context itsContext;

        /**
         * Constructor
         */
        protected SuggestionsUser(PasswdRecordFilter filter,
                                  int limit,
                                  MatchComparator comparator,
                                  Context ctx)
        {
            itsFilter = filter;
            itsLimit = limit;
            itsComparator = comparator;
            itsContext = ctx;
        }

        @Override
        public Cursor useFileData(@NonNull PasswdFileData fileData)
        {
            ArrayList<RecordMatch> recs = new ArrayList<>();
            Set<String> groups = new HashSet<>();
            for (PwsRecord rec: fileData.getRecords()) {
                String match = itsFilter.filterRecord(rec, fileData,
                                                      itsContext);
                if (match != null) {
                    recs.add(new RecordMatch(rec, match, fileData));
                }

                String matchGroup = itsFilter.matchGroup(rec, fileData);
                if ((matchGroup != null) && !groups.contains(matchGroup)) {
                    groups.add(matchGroup);
                    recs.add(new RecordMatch(matchGroup));
                }

                if (recs.size() >= itsLimit) {
                    break;
                }
            }
            Collections.sort(recs, itsComparator);

            MatrixCursor cursor = new MatrixCursor(
                    new String[]{BaseColumns._ID,
                                 SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA,
                                 SearchManager.SUGGEST_COLUMN_TEXT_1,
                                 SearchManager.SUGGEST_COLUMN_TEXT_2,
                                 SearchManager.SUGGEST_COLUMN_ICON_1 },
                    recs.size());

            Object[] row = new Object[5];
            row[4] = null;
            int id = 0;
            for (RecordMatch match: recs) {
                row[0] = id++;
                row[1] = match.itsData;
                row[2] = match.itsLabel;
                row[3] = match.itsMatch;
                cursor.addRow(row);
            }

            return cursor;
        }
    }

    /**
     * A matched password record
     */
    private static class RecordMatch
    {
        protected final String itsTitle;
        protected final String itsUser;
        protected final String itsData;
        protected final String itsLabel;
        protected final String itsMatch;

        /**
         * Constructor for a record
         */
        protected RecordMatch(PwsRecord rec,
                              String match,
                              PasswdFileData fileData)
        {
            itsTitle = fileData.getTitle(rec);
            itsUser = fileData.getUsername(rec);
            itsData = PasswdRecordFilter.SEARCH_VIEW_RECORD +
                      fileData.getUUID(rec);
            itsLabel = PasswdRecord.getRecordId(null, itsTitle, itsUser);
            itsMatch = match;
        }

        /**
         * Constructor for a group
         */
        protected RecordMatch(String group)
        {
            itsTitle = group;
            itsUser = null;
            itsData = PasswdRecordFilter.SEARCH_VIEW_GROUP + itsTitle;
            itsLabel = itsTitle;
            itsMatch = PasswdRecordFilter.QUERY_MATCH_GROUP;
        }
    }

    /**
     * Comparator for RecordMatch objects
     */
    private static class MatchComparator implements Comparator<RecordMatch>
    {
        private final boolean itsIsAscending;
        private final boolean itsIsCaseSensitive;

        /**
         * Constructor
         */
        protected MatchComparator(boolean ascending, boolean caseSensitive)
        {
            itsIsAscending = ascending;
            itsIsCaseSensitive = caseSensitive;
        }

        @Override
        public int compare(RecordMatch m1, RecordMatch m2)
        {
            int rc = compareField(m1.itsTitle, m2.itsTitle);
            if (rc == 0) {
                rc = compareField(m1.itsUser, m2.itsUser);
            }

            if (!itsIsAscending) {
                rc = -rc;
            }

            return rc;
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
                if (itsIsCaseSensitive) {
                    return arg0.compareTo(arg1);
                } else {
                    return arg0.compareToIgnoreCase(arg1);
                }
            }
        }
    }
}
