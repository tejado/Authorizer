/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * The NotificationMgr class encapsulates the notifications provided by the app
 */
public class NotificationMgr
{
    private static final String TAG = "NotificationMgr";

    private static final String DB_TABLE_URIS = "uris";
    private static final String DB_COL_URIS_ID = BaseColumns._ID;
    private static final String DB_COL_URIS_URI = "uri";

    private static final String DB_MATCH_URIS_URI = DB_COL_URIS_URI + " = ?";

    DbHelper itsDbHelper;

    /** Constructor */
    public NotificationMgr(Context ctx)
    {
        itsDbHelper = new DbHelper(ctx);

        // Simple query to create the database on startup
        SQLiteDatabase db = itsDbHelper.getReadableDatabase();
        DatabaseUtils.queryNumEntries(db, DB_TABLE_URIS);
    }


    /** Are notifications enabled for a URI */
    public boolean hasPasswdExpiryNotif(Uri uri)
    {
        try {
            if (uri == null) {
                return false;
            }
            SQLiteDatabase db = itsDbHelper.getReadableDatabase();
            return doHasPasswdExpiryNotif(uri.toString(), db);
            // TODO: cache flag in memory?
        } catch (SQLiteException e) {
            Log.e(TAG, "Database error", e);
            return false;
        }
    }


    /** Toggle whether notifications are enabled for a URI */
    public void togglePasswdExpiryNotif(Uri uri, Activity act)
    {
        try {
            String uristr = uri.toString();
            SQLiteDatabase db = itsDbHelper.getWritableDatabase();
            if (doHasPasswdExpiryNotif(uristr, db)) {
                db.delete(DB_TABLE_URIS, DB_MATCH_URIS_URI,
                          new String[] { uristr });
            } else {
                ContentValues values = new ContentValues(1);
                values.put(DB_COL_URIS_URI, uristr);
                db.insert(DB_TABLE_URIS, null, values);
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Database error", e);
        }
    }


    /** Implementation to check whether notifications are enabled for a URI */
    private boolean doHasPasswdExpiryNotif(String uristr, SQLiteDatabase db)
    {
        String query = SQLiteQueryBuilder.buildQueryString(
            true, DB_TABLE_URIS, new String[] { "count(*)" },
            DB_MATCH_URIS_URI, null, null, null, null);
        long count = DatabaseUtils.longForQuery(
            db, query, new String[] { uristr });
        return count != 0;
    }

    // TODO: not all URIs should support notifications

    /** Database helper class to manage the database tables */
    private static class DbHelper extends SQLiteOpenHelper
    {
        private static final String DB_NAME = "notifications.db";
        private static final int DB_VERSION = 1;

        /** Constructor */
        public DbHelper(Context context)
        {
            super(context, DB_NAME, null, DB_VERSION);
        }

        /* (non-Javadoc)
         * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
         */
        @Override
        public void onCreate(SQLiteDatabase db)
        {
            db.execSQL("CREATE TABLE " + DB_TABLE_URIS + " (" +
                       DB_COL_URIS_ID + " INTEGER PRIMARY KEY," +
                       DB_COL_URIS_URI + " TEXT NOT NULL" +
                       ");");
        }

        /* (non-Javadoc)
         * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
        }
    }
}
