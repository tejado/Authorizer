/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib;

import android.content.ContentResolver;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

/**
 * The PasswdSafeContract class is the contract for the PasswdSafe client and
 * sync providers
 */
public final class PasswdSafeContract
{
    /** The sync provider's authority */
    public static final String AUTHORITY =
        "com.jefftharris.passwdsafe.sync.provider";

    /** The base URI for the provider */
    public static final Uri CONTENT_URI =
        Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + AUTHORITY);

    public static final UriMatcher MATCHER;
    public static final int MATCH_PROVIDERS = 1;
    public static final int MATCH_PROVIDER = 2;
    public static final int MATCH_PROVIDER_FILES = 3;
    public static final int MATCH_PROVIDER_FILE = 4;
    public static final int MATCH_SYNC_LOGS = 5;
    public static final int MATCH_METHODS = 6;
    public static final int MATCH_PROVIDER_REMOTE_FILES = 7;
    public static final int MATCH_PROVIDER_REMOTE_FILE = 8;

    static {
        MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        MATCHER.addURI(PasswdSafeContract.AUTHORITY,
                       PasswdSafeContract.Providers.TABLE,
                       MATCH_PROVIDERS);
        MATCHER.addURI(PasswdSafeContract.AUTHORITY,
                       PasswdSafeContract.Providers.TABLE + "/#",
                       MATCH_PROVIDER);
        MATCHER.addURI(PasswdSafeContract.AUTHORITY,
                       PasswdSafeContract.Providers.TABLE + "/#/" +
                               PasswdSafeContract.Files.TABLE,
                       MATCH_PROVIDER_FILES);
        MATCHER.addURI(PasswdSafeContract.AUTHORITY,
                       PasswdSafeContract.Providers.TABLE + "/#/" +
                               PasswdSafeContract.Files.TABLE + "/#",
                       MATCH_PROVIDER_FILE);
        MATCHER.addURI(PasswdSafeContract.AUTHORITY,
                       PasswdSafeContract.SyncLogs.TABLE,
                       MATCH_SYNC_LOGS);
        MATCHER.addURI(PasswdSafeContract.AUTHORITY,
                       PasswdSafeContract.Methods.TABLE,
                       MATCH_METHODS);
        MATCHER.addURI(PasswdSafeContract.AUTHORITY,
                       PasswdSafeContract.Providers.TABLE + "/#/" +
                               PasswdSafeContract.RemoteFiles.TABLE,
                       MATCH_PROVIDER_REMOTE_FILES);
        MATCHER.addURI(PasswdSafeContract.AUTHORITY,
                       PasswdSafeContract.Providers.TABLE + "/#/" +
                               PasswdSafeContract.RemoteFiles.TABLE + "/#",
                       MATCH_PROVIDER_REMOTE_FILE);
    }

    /** The table of providers */
    public static final class Providers implements BaseColumns
    {
        public static final String TABLE = "providers";
        public static final Uri CONTENT_URI =
                Uri.withAppendedPath(PasswdSafeContract.CONTENT_URI, TABLE);
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd." + AUTHORITY + "." + TABLE;

        public static final String COL_TYPE = "type";
        public static final String COL_ACCT = "acct";
        public static final String COL_SYNC_FREQ = "sync_freq";
        public static final String COL_DISPLAY_NAME = "display_name";

        public static final String PROVIDER_SORT_ORDER =
                COL_TYPE + " ASC, " + COL_DISPLAY_NAME + " ASC";

        public static final String[] PROJECTION = {
            Providers._ID,
            Providers.COL_TYPE,
            Providers.COL_ACCT,
            Providers.COL_SYNC_FREQ,
            Providers.COL_DISPLAY_NAME
        };

        public static final int PROJECTION_IDX_ID = 0;
        public static final int PROJECTION_IDX_TYPE = 1;
        public static final int PROJECTION_IDX_ACCT = 2;
        public static final int PROJECTION_IDX_SYNC_FREQ = 3;
        public static final int PROJECTION_IDX_DISPLAY_NAME = 4;

        /** Get the provider's display name */
        public static String getDisplayName(Cursor cursor)
        {
            String displayName = null;
            if (cursor.getColumnCount() > PROJECTION_IDX_DISPLAY_NAME) {
                displayName = cursor.getString(PROJECTION_IDX_DISPLAY_NAME);
            }
            if (TextUtils.isEmpty(displayName)) {
                displayName = cursor.getString(PROJECTION_IDX_ACCT);
            }
            return displayName;
        }

        /** Get the provider id from the URI */
        public static long getId(Uri uri)
        {
            return Long.valueOf(getIdStr(uri));
        }

        /** Get the provider id string from the URI */
        public static String getIdStr(Uri uri)
        {
            return uri.getPathSegments().get(1);
        }
    }

    /** The table of files */
    public static final class Files implements BaseColumns
    {
        public static final String TABLE = "files";
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd." + AUTHORITY + "." + TABLE;

        public static final String COL_PROVIDER = "provider";
        public static final String COL_TITLE = "title";
        public static final String COL_MOD_DATE = "mod_date";
        public static final String COL_FILE = "file";
        public static final String COL_FOLDER = "folder";

        public static final String NOT_DELETED_SELECTION =
                "not local_deleted and not remote_deleted";
        public static final String TITLE_SORT_ORDER =
                COL_FOLDER + " ASC, " + COL_TITLE + " ASC";

        public static final String[] PROJECTION = {
            Files._ID,
            Files.COL_PROVIDER,
            Files.COL_TITLE,
            Files.COL_MOD_DATE,
            Files.COL_FILE,
            Files.COL_FOLDER
        };

        //public static final int PROJECTION_IDX_ID = 0;
        //public static final int PROJECTION_IDX_PROVIDER = 1;
        public static final int PROJECTION_IDX_TITLE = 2;
        public static final int PROJECTION_IDX_MOD_DATE = 3;
        //public static final int PROJECTION_IDX_FILE = 4;
        public static final int PROJECTION_IDX_FOLDER = 5;

        /** Get the file id from the URI */
        public static long getId(Uri uri)
        {
            return Long.valueOf(getIdStr(uri));
        }

        /** Get the file id string from the URI */
        public static String getIdStr(Uri uri)
        {
            return uri.getPathSegments().get(3);
        }
    }

    /** The table of remote files */
    public static final class RemoteFiles implements BaseColumns
    {
        public static final String TABLE = "remote_files";
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd." + AUTHORITY + "." + TABLE;

        public static final String COL_REMOTE_ID = "remote_id";

        public static final String NOT_DELETED_SELECTION =
                "not local_deleted and not remote_deleted";

        public static final String[] PROJECTION = {
            RemoteFiles._ID,
            RemoteFiles.COL_REMOTE_ID
        };

        public static final int PROJECTION_IDX_ID = 0;
        public static final int PROJECTION_IDX_REMOTE_ID = 1;

        /** Get the file id string from the URI */
        public static String getIdStr(Uri uri)
        {
            return uri.getPathSegments().get(3);
        }
    }

    /** The table of sync logs */
    public static final class SyncLogs implements BaseColumns
    {
        public static final String TABLE = "sync_logs";
        public static final Uri CONTENT_URI =
                Uri.withAppendedPath(PasswdSafeContract.CONTENT_URI, TABLE);
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + TABLE;
        //public static final String CONTENT_ITEM_TYPE =
        //"vnd.android.cursor.item/vnd." + AUTHORITY + "." + TABLE;

        public static final String COL_ACCT = "acct";
        public static final String COL_START = "start";
        public static final String COL_END = "end";
        public static final String COL_FLAGS = "flags";
        public static final String COL_LOG = "log";
        public static final String COL_STACK = "stack";

        public static final String START_SORT_ORDER = COL_START + " DESC";
        public static final String DEFAULT_SELECTION = COL_LOG + " != ''";

        public static final String[] PROJECTION = {
            SyncLogs._ID,
            SyncLogs.COL_ACCT,
            SyncLogs.COL_START,
            SyncLogs.COL_END,
            SyncLogs.COL_FLAGS,
            SyncLogs.COL_LOG,
            SyncLogs.COL_STACK
        };

        //public static final int PROJECTION_IDX_ID = 0;
        public static final int PROJECTION_IDX_ACCT = 1;
        public static final int PROJECTION_IDX_START = 2;
        public static final int PROJECTION_IDX_END = 3;
        public static final int PROJECTION_IDX_FLAGS = 4;
        public static final int PROJECTION_IDX_LOG = 5;
        public static final int PROJECTION_IDX_STACK = 6;

        //public static final int FLAGS_IS_FULL =                 1 << 0;
        public static final int FLAGS_IS_MANUAL =               1 << 1;
        public static final int FLAGS_IS_NOT_CONNECTED =        1 << 2;
    }

    /** The 'table' for methods */
    public static final class Methods
    {
        public static final String TABLE = "methods";
        public static final Uri CONTENT_URI =
                Uri.withAppendedPath(PasswdSafeContract.CONTENT_URI, TABLE);
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + TABLE;

        public static final String METHOD_SYNC = "sync";
    }

    /** The client provider's authority */
    public static final String CLIENT_AUTHORITY =
            "com.jefftharris.passwdsafe.client.provider";

    /** The base URI for the client provider */
    public static final Uri CLIENT_CONTENT_URI =
        Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + CLIENT_AUTHORITY);

    /** The client files */
    public static final class ClientFiles
    {
        public static final String TABLE = "files";
    }
}

