/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.TreeSet;

import org.pwsafe.lib.file.PwsRecord;

import com.jefftharris.passwdsafe.file.PasswdExpiration;
import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdFileDataObserver;
import com.jefftharris.passwdsafe.file.PasswdRecord;
import com.jefftharris.passwdsafe.view.AbstractDialogClickListener;
import com.jefftharris.passwdsafe.view.DialogUtils;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * The NotificationMgr class encapsulates the notifications provided by the app
 */
public class NotificationMgr implements PasswdFileDataObserver
{
    private static final String TAG = "NotificationMgr";

    private static final String DB_TABLE_URIS = "uris";
    private static final String DB_COL_URIS_ID = BaseColumns._ID;
    private static final String DB_COL_URIS_URI = "uri";
    private static final String DB_MATCH_URIS_ID = DB_COL_URIS_ID + " = ?";
    private static final String DB_MATCH_URIS_URI = DB_COL_URIS_URI + " = ?";

    private static final String DB_TABLE_EXPIRYS = "expirations";
    private static final String DB_COL_EXPIRYS_ID = BaseColumns._ID;
    private static final String DB_COL_EXPIRYS_URI = "uri";
    private static final String DB_COL_EXPIRYS_UUID = "rec_uuid";
    private static final String DB_COL_EXPIRYS_TITLE = "rec_title";
    private static final String DB_COL_EXPIRYS_GROUP = "rec_group";
    private static final String DB_COL_EXPIRYS_USER = "rec_username";
    private static final String DB_COL_EXPIRYS_EXPIRE = "rec_expire";
    private static final String DB_MATCH_EXPIRYS_URI =
        DB_COL_EXPIRYS_URI + " = ?";

    private static final DateFormat SQL_DATE_FMT =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    static {
        SQL_DATE_FMT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private DbHelper itsDbHelper;
    private TreeSet<ExpiryEntry> itsEntries = new TreeSet<ExpiryEntry>();

    /** Constructor */
    public NotificationMgr(Context ctx)
    {
        itsDbHelper = new DbHelper(ctx);
        PasswdFileData.addObserver(this);

        // Simple query to create the database on startup
        try {
            SQLiteDatabase db = itsDbHelper.getReadableDatabase();
            loadEntries(db);
        } catch (SQLException e) {
            Log.e(TAG, "Database error", e);
        }
    }


    /** Are notifications enabled for a URI */
    public boolean hasPasswdExpiryNotif(Uri uri)
    {
        try {
            if (uri == null) {
                return false;
            }
            SQLiteDatabase db = itsDbHelper.getReadableDatabase();
            Long uriId = getDbUriId(uri.toString(), db);
            return uriId != null;
            // TODO: cache flag in memory?
        } catch (SQLException e) {
            Log.e(TAG, "Database error", e);
            return false;
        }
    }


    /** Toggle whether notifications are enabled for a password file */
    public void togglePasswdExpiryNotif(final PasswdFileData fileData,
                                        Activity act)
    {
        try {
            if (fileData == null) {
                return;
            }

            String uristr = fileData.getUri().toString();
            SQLiteDatabase db = itsDbHelper.getWritableDatabase();
            Long uriId = getDbUriId(uristr, db);
            if (uriId != null) {
                String[] idarg = new String[] { uriId.toString() };
                db.delete(DB_TABLE_EXPIRYS, DB_MATCH_EXPIRYS_URI, idarg);
                db.delete(DB_TABLE_URIS, DB_MATCH_URIS_ID, idarg);
            } else {
                DialogUtils.DialogData dlgData =
                    DialogUtils.createConfirmPrompt(
                        act,
                        new AbstractDialogClickListener()
                        {
                            @Override
                            public void onOkClicked(DialogInterface dialog)
                            {
                                enablePasswdExpiryNotif(fileData);
                            }
                        },
                        act.getString(R.string.expiration_notifications),
                        act.getString(R.string.expiration_notifications_warning));
                dlgData.itsDialog.show();
                dlgData.itsValidator.validate();
            }
        } catch (SQLException e) {
            Log.e(TAG, "Database error", e);
        }
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.file.PasswdFileDataObserver#passwdFileDataChanged(com.jefftharris.passwdsafe.file.PasswdFileData)
     */
    public void passwdFileDataChanged(PasswdFileData fileData)
    {
        // TODO: only update if necessary
        try {
            SQLiteDatabase db = itsDbHelper.getWritableDatabase();
            try {
                db.beginTransaction();
                Long id = getDbUriId(fileData.getUri().toString(), db);
                if (id != null) {
                    doUpdatePasswdFileData(id, fileData, db);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (SQLException e) {
            Log.e(TAG, "Database error", e);
        }
    }


    /** Enable notifications for the password file */
    private void enablePasswdExpiryNotif(PasswdFileData fileData)
    {
        try {
            SQLiteDatabase db = itsDbHelper.getWritableDatabase();
            try {
                db.beginTransaction();
                ContentValues values = new ContentValues(1);
                values.put(DB_COL_URIS_URI, fileData.getUri().toString());
                long id = db.insertOrThrow(DB_TABLE_URIS, null, values);
                doUpdatePasswdFileData(id, fileData, db);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (SQLException e) {
            Log.e(TAG, "Database error", e);
        }
    }


    /** Update the notification expirations for a password file */
    private void doUpdatePasswdFileData(long uriId,
                                        PasswdFileData fileData,
                                        SQLiteDatabase db)
        throws SQLException
    {
        PasswdSafeApp.dbginfo(TAG, "Update " + fileData.getUri() + ", id: " +
                              uriId);

        db.delete(DB_TABLE_EXPIRYS, DB_MATCH_EXPIRYS_URI,
                  new String[] { Long.toString(uriId) });
        ContentValues values = new ContentValues();
        values.put(DB_COL_EXPIRYS_URI, uriId);
        for (PasswdRecord rec: fileData.getPasswdRecords()) {
            PasswdExpiration expiry = rec.getPasswdExpiry();
            if (expiry != null) {
                PwsRecord pwsrec = rec.getRecord();
                values.put(DB_COL_EXPIRYS_UUID, rec.getUUID());
                values.put(DB_COL_EXPIRYS_TITLE, fileData.getTitle(pwsrec));
                values.put(DB_COL_EXPIRYS_GROUP, fileData.getGroup(pwsrec));
                values.put(DB_COL_EXPIRYS_USER, fileData.getUsername(pwsrec));
                values.put(DB_COL_EXPIRYS_EXPIRE,
                           dateToSqlDate(expiry.itsExpiration));
                db.insertOrThrow(DB_TABLE_EXPIRYS, null, values);
            }
        }
    }


    /** Load the expiration entries from the database */
    private void loadEntries(SQLiteDatabase db)
    {
        String uriCol = DB_TABLE_URIS + "." + DB_COL_URIS_URI;
        String uuidCol = DB_TABLE_EXPIRYS + "." + DB_COL_EXPIRYS_UUID;
        String titleCol = DB_TABLE_EXPIRYS + "." + DB_COL_EXPIRYS_TITLE;
        String groupCol = DB_TABLE_EXPIRYS + "." + DB_COL_EXPIRYS_GROUP;
        String userCol = DB_TABLE_EXPIRYS + "." + DB_COL_EXPIRYS_USER;
        String expireCol = DB_TABLE_EXPIRYS + "." + DB_COL_EXPIRYS_EXPIRE;
        Cursor cursor =
            db.query(DB_TABLE_URIS + "," + DB_TABLE_EXPIRYS +
                     " ON (" + DB_TABLE_URIS + "." + DB_COL_URIS_ID + "=" +
                     DB_TABLE_EXPIRYS + "." + DB_COL_EXPIRYS_URI + ")",
                     new String[] { uriCol, uuidCol, titleCol, groupCol,
                                    userCol, expireCol },
                     null, null, null, null, null);
        //Log.e(TAG, "entries: " + DatabaseUtils.dumpCursorToString(cursor));
        while (!cursor.moveToNext()) {
            ExpiryEntry entry =
                new ExpiryEntry(cursor.getString(2) + "/" +
                                cursor.getString(3) + "/" +
                                cursor.getString(4),
                                cursor.getString(0),
                                cursor.getString(1),
                                sqlDateToDate(cursor.getString(5)));
            itsEntries.add(entry);
        }
    }


    /** Get the id for a URI or null if not found */
    private Long getDbUriId(String uristr, SQLiteDatabase db)
        throws SQLException
    {
        Cursor cursor = db.query(DB_TABLE_URIS, new String[] { DB_COL_URIS_ID },
                                 DB_MATCH_URIS_URI, new String[] { uristr },
                                 null, null, null);
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return cursor.getLong(0);
        } finally {
            cursor.close();
        }
    }


    /** Convert a Date to a string for the database */
    private static synchronized String dateToSqlDate(Date date)
    {
        return SQL_DATE_FMT.format(date);
    }


    /** Convert a date string in the database to a Date */
    private static synchronized Date sqlDateToDate(String str)
    {
        try {
            return SQL_DATE_FMT.parse(str);
        } catch (ParseException e) {
            return null;
        }
    }
    // TODO: not all URIs should support notifications

    /** Database helper class to manage the database tables */
    private static final class DbHelper extends SQLiteOpenHelper
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
            enableForeignKey(db);
            db.execSQL("CREATE TABLE " + DB_TABLE_URIS + " (" +
                       DB_COL_URIS_ID + " INTEGER PRIMARY KEY," +
                       DB_COL_URIS_URI + " TEXT NOT NULL" +
                       ");");
            db.execSQL("CREATE TABLE " + DB_TABLE_EXPIRYS + " (" +
                       DB_COL_EXPIRYS_ID + " INTEGER PRIMARY KEY, " +
                       DB_COL_EXPIRYS_URI + " INTEGER REFERENCES " +
                           DB_TABLE_URIS + "(" + DB_COL_URIS_ID +") NOT NULL, " +
                       DB_COL_EXPIRYS_UUID + " TEXT NOT NULL, " +
                       DB_COL_EXPIRYS_TITLE + " TEXT NOT NULL, " +
                       DB_COL_EXPIRYS_GROUP + " TEXT, " +
                       DB_COL_EXPIRYS_USER + " TEXT, " +
                       DB_COL_EXPIRYS_EXPIRE + " TEXT NOT NULL" +
                       ");");
        }


        /* (non-Javadoc)
         * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            enableForeignKey(db);
        }


        /* (non-Javadoc)
         * @see android.database.sqlite.SQLiteOpenHelper#onOpen(android.database.sqlite.SQLiteDatabase)
         */
        @Override
        public void onOpen(SQLiteDatabase db)
        {
            enableForeignKey(db);
            super.onOpen(db);
        }


        /** Enable support for foreign keys on the open database connection */
        private void enableForeignKey(SQLiteDatabase db)
            throws SQLException
        {
            if (!db.isReadOnly()) {
                db.execSQL("PRAGMA foreign_keys = ON;");
            }
        }
    }


    /** The ExpiryEntry class represents an expiration entry for notifications */
    private static final class ExpiryEntry implements Comparable<ExpiryEntry>
    {
        public final String itsName;
        public final String itsUri;
        public final String itsUuid;
        public final Date itsExpiry;

        /** Constructor */
        public ExpiryEntry(String name, String uri, String uuid, Date expiry)
        {
            itsName = name;
            itsUri = uri;
            itsUuid = uuid;
            itsExpiry = expiry;
        }


        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(ExpiryEntry another)
        {
            int rc = itsExpiry.compareTo(another.itsExpiry);
            if (rc == 0) {
                rc = itsName.compareTo(another.itsName);
                if (rc == 0) {
                    rc = itsUuid.compareTo(another.itsUuid);
                    if (rc == 0) {
                        rc = itsUri.compareTo(another.itsUri);
                    }
                }
            }
            return rc;
        }
    }
}
