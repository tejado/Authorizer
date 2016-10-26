/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.OpenableColumns;

import net.tjado.passwdsafe.lib.ApiCompat;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;


/**
 * Database helper class to manage the recent files list
 */
@SuppressWarnings({"WeakerAccess", "TryFinallyCanBeTryWithResources"})
public final class RecentFilesDb extends SQLiteOpenHelper
{
    public static final String DB_TABLE_FILES = "files";
    public static final String DB_COL_FILES_ID = BaseColumns._ID;
    public static final String DB_COL_FILES_TITLE = "title";
    public static final String DB_COL_FILES_URI = "uri";
    public static final String DB_COL_FILES_DATE = "date";

    private static final String[] QUERY_COLUMNS = new String[] {
            DB_COL_FILES_ID, DB_COL_FILES_TITLE,
            DB_COL_FILES_URI, DB_COL_FILES_DATE };

    public static final int QUERY_COL_ID = 0;
    public static final int QUERY_COL_TITLE = 1;
    public static final int QUERY_COL_URI = 2;
    public static final int QUERY_COL_DATE = 3;

    private static final int NUM_RECENT_FILES = 10;

    private static final String TAG = "RecentFilesDb";

    private static final String WHERE_BY_ID = DB_COL_FILES_ID + " = ?";
    private static final String WHERE_BY_URI = DB_COL_FILES_URI + " = ?";
    private static final String ORDER_BY_DATE = DB_COL_FILES_DATE + " DESC";

    private static final String DB_NAME = "recent_files.db";
    private static final int DB_VERSION = 1;

    /** Constructor */
    public RecentFilesDb(Context context)
    {
        super(context, DB_NAME, null, DB_VERSION);
    }


    /** Query files */
    public Cursor queryFiles() throws SQLException
    {
        SQLiteDatabase db = getReadableDatabase();
        PasswdSafeUtil.dbginfo(TAG, "load recent files");
        return db.query(DB_TABLE_FILES, QUERY_COLUMNS,
                        null, null, null, null, ORDER_BY_DATE);
    }


    /** Insert or update the entry for the file */
    public void insertOrUpdateFile(Uri uri, String title)
            throws SQLException
    {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.beginTransaction();
            String uristr = uri.toString();
            long uriId = -1;
            {
                Cursor cursor = db.query(DB_TABLE_FILES, QUERY_COLUMNS,
                                         WHERE_BY_URI, new String[]{ uristr },
                                         null, null, null);
                try {
                    if (cursor.moveToFirst()) {
                        uriId = cursor.getLong(QUERY_COL_ID);
                    }
                } finally {
                    cursor.close();
                }
            }
            ContentValues values = new ContentValues();
            values.put(DB_COL_FILES_DATE, System.currentTimeMillis());
            if (uriId != -1) {
                db.update(DB_TABLE_FILES, values, WHERE_BY_ID,
                          new String[] { Long.toString(uriId) });
            } else {
                values.put(DB_COL_FILES_TITLE, title);
                values.put(DB_COL_FILES_URI, uristr);
                db.insertOrThrow(DB_TABLE_FILES, null, values);
            }

            Cursor delCursor = db.query(DB_TABLE_FILES, QUERY_COLUMNS,
                                        null, null, null, null, ORDER_BY_DATE);
            try {
                if (delCursor.move(NUM_RECENT_FILES)) {
                    while (delCursor.moveToNext()) {
                        long id = delCursor.getLong(QUERY_COL_ID);
                        db.delete(DB_TABLE_FILES, WHERE_BY_ID,
                                  new String[] { Long.toString(id) });
                    }
                }
            } finally {
                delCursor.close();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }


    /** Delete a recent file with the given uri */
    public void removeUri(Uri permUri) throws SQLException
    {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.beginTransaction();
            db.delete(DB_TABLE_FILES, WHERE_BY_URI,
                      new String[]{ permUri.toString() });
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }


    /** Clear the recent files */
    public void clear() throws SQLException
    {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.beginTransaction();
            db.delete(DB_TABLE_FILES, null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }


    /** Update an opened storage access file */
    public static void updateOpenedSafFile(Uri uri, int flags, Context ctx)
    {
        ContentResolver cr = ctx.getContentResolver();
        ApiCompat.takePersistableUriPermission(cr, uri, flags);
    }

    /** Get the display name of a storage access file */
    public static String getSafDisplayName(Uri uri, Context ctx)
    {
        ContentResolver cr = ctx.getContentResolver();
        Cursor cursor = cr.query(uri, null, null, null, null);
        try {
            if ((cursor != null) && (cursor.moveToFirst())) {
                return cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        PasswdSafeUtil.dbginfo(TAG, "Create DB");
        db.execSQL("CREATE TABLE " + DB_TABLE_FILES + " (" +
                   DB_COL_FILES_ID + " INTEGER PRIMARY KEY," +
                   DB_COL_FILES_TITLE + " TEXT NOT NULL, " +
                   DB_COL_FILES_URI + " TEXT NOT NULL, " +
                   DB_COL_FILES_DATE + " INTEGER NOT NULL" +
                   ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
    }
}
