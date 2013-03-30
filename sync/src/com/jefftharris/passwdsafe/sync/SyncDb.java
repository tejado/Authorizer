/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

/**
 * The SyncDb encapsulates the synchronization database
 */
public class SyncDb
{
    public enum ProviderType
    {
        GDRIVE
    }

    private static final String TAG = "SyncDb";

    private static final String DB_TABLE_PROVIDERS = "providers";
    private static final String DB_COL_PROVIDERS_ID = BaseColumns._ID;
    private static final String DB_COL_PROVIDERS_TYPE = "type";
    private static final String DB_COL_PROVIDERS_ACCT = "acct";
    private static final String DB_COL_PROVIDERS_SYNC_CHANGE = "sync_change";
    private static final String DB_COL_PROVIDERS_SYNC_FREQ = "sync_freq";
    private static final String DB_MATCH_PROVIDERS_ID =
        DB_COL_PROVIDERS_ID + " = ?";
    private static final String DB_MATCH_PROVIDERS_TYPE =
        DB_COL_PROVIDERS_TYPE + " = ?";
    private static final String DB_MATCH_PROVIDERS_TYPE_ACCT =
        DB_COL_PROVIDERS_TYPE + " = ? AND " + DB_COL_PROVIDERS_ACCT + " = ?";

    private static final String DB_TABLE_FILES = "files";
    private static final String DB_COL_FILES_ID = BaseColumns._ID;
    private static final String DB_COL_FILES_PROVIDER = "provider";
    private static final String DB_COL_FILES_LOCAL_FILE = "local_file";
    private static final String DB_COL_FILES_LOCAL_MOD_DATE = "local_mod_date";
    private static final String DB_COL_FILES_LOCAL_DELETED = "local_deleted";
    private static final String DB_COL_FILES_REMOTE_ID = "remote_id";
    private static final String DB_COL_FILES_REMOTE_TITLE = "remote_title";
    private static final String DB_COL_FILES_REMOTE_MOD_DATE = "remote_mod_date";
    private static final String DB_COL_FILES_REMOTE_DELETED = "remote_deleted";
    private static final String DB_MATCH_FILES_ID =
        DB_COL_FILES_ID + " = ?";
    private static final String DB_MATCH_FILES_PROVIDER_ID =
        DB_COL_FILES_PROVIDER + " = ?";

    private DbHelper itsDbHelper;


    /** Entry in the files table */
    public static class DbFile
    {
        public final long itsId;
        public final String itsLocalFile;
        public final long itsLocalModDate;
        public final boolean itsIsLocalDeleted;
        public final String itsRemoteId;
        public final String itsRemoteTitle;
        public final long itsRemoteModDate;
        public final boolean itsIsRemoteDeleted;

        public static final String[] QUERY_FIELDS = {
            DB_COL_FILES_ID,
            DB_COL_FILES_LOCAL_FILE,
            DB_COL_FILES_LOCAL_MOD_DATE,
            DB_COL_FILES_LOCAL_DELETED,
            DB_COL_FILES_REMOTE_ID,
            DB_COL_FILES_REMOTE_TITLE,
            DB_COL_FILES_REMOTE_MOD_DATE,
            DB_COL_FILES_REMOTE_DELETED };

        public DbFile(Cursor cursor)
        {
            itsId = cursor.getLong(0);
            itsLocalFile = cursor.getString(1);
            itsLocalModDate = cursor.getLong(2);
            itsIsLocalDeleted = cursor.getInt(3) != 0;
            itsRemoteId = cursor.getString(4);
            itsRemoteTitle = cursor.getString(5);
            itsRemoteModDate = cursor.getLong(6);
            itsIsRemoteDeleted = cursor.getInt(7) != 0;
        }
    }


    /** Constructor */
    public SyncDb(Context ctx)
    {
        itsDbHelper = new DbHelper(ctx);
    }

    /** Close the DB */
    public void close()
    {
        itsDbHelper.close();
    }

    /** Get the database */
    public SQLiteDatabase getDb()
    {
        return itsDbHelper.getWritableDatabase();
    }

    /** Get the sync provider account */
    public String getProviderAccount()
    {
        try {
            SQLiteDatabase db = itsDbHelper.getReadableDatabase();
            String[] args = new String[] { ProviderType.GDRIVE.toString() };
            Cursor cursor = db.query(DB_TABLE_PROVIDERS,
                                     new String[] { DB_COL_PROVIDERS_ACCT },
                                     DB_MATCH_PROVIDERS_TYPE, args,
                                     null, null, null);
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        } catch (SQLException e) {
            Log.e(TAG, "DB error", e);
        }
        return "";
    }

    /** Add a provider */
    public void addProvider(String name)
        throws SQLException
    {
        PasswdSafeUtil.dbginfo(TAG, "Add provider %s", name);
        SQLiteDatabase db = itsDbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            ContentValues values = new ContentValues();
            values.put(DB_COL_PROVIDERS_TYPE, ProviderType.GDRIVE.toString());
            values.put(DB_COL_PROVIDERS_ACCT, name);
            values.put(DB_COL_PROVIDERS_SYNC_CHANGE, -1);
            values.put(DB_COL_PROVIDERS_SYNC_FREQ, 15 * 60);
            db.insertOrThrow(DB_TABLE_PROVIDERS, null, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Delete a provider */
    public void deleteProvider(String name)
        throws SQLException
    {
        PasswdSafeUtil.dbginfo(TAG, "Delete provider %s", name);
        long id = getProviderId(name);
        if (id == -1) {
            return;
        }
        SQLiteDatabase db = itsDbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            String[] idargs = new String[] { Long.toString(id) };
            db.delete(DB_TABLE_FILES, DB_MATCH_FILES_PROVIDER_ID, idargs);
            db.delete(DB_TABLE_PROVIDERS, DB_MATCH_PROVIDERS_ID, idargs);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Get the id for a provider */
    public long getProviderId(String name)
        throws SQLException
    {
        long id = -1;
        Cursor cursor = getProviderField(name, DB_COL_PROVIDERS_ID);
        if (cursor != null) {
            try {
                id = cursor.getLong(0);
            } finally {
                cursor.close();
            }
        }
        return id;
    }

    /** Get the id for a provider */
    public long getProviderId(String name, SQLiteDatabase db)
        throws SQLException
    {
        long id = -1;
        Cursor cursor = getProviderField(name, DB_COL_PROVIDERS_ID, db);
        if (cursor != null) {
            try {
                id = cursor.getLong(0);
            } finally {
                cursor.close();
            }
        }
        return id;
    }

    /** Get the sync frequency for a provider */
    public int getProviderSyncFreq(String name)
        throws SQLException
    {
        int freq = -1;
        Cursor cursor = getProviderField(name, DB_COL_PROVIDERS_SYNC_FREQ);
        if (cursor != null) {
            try {
                freq = cursor.getInt(0);
            } finally {
                cursor.close();
            }
        }
        return freq;
    }

    /** Get the sync change id for a provider */
    public long getProviderSyncChange(String name, SQLiteDatabase db)
        throws SQLException
    {
        long changeId = -1;
        Cursor cursor = getProviderField(name, DB_COL_PROVIDERS_SYNC_CHANGE,
                                         db);
        if (cursor != null) {
            try {
                changeId = cursor.getLong(0);
            } finally {
                cursor.close();
            }
        }
        return changeId;
    }


    /** Set the sync change identifier for a provider */
    public void setProviderSyncChange(String name, long changeId,
                                      SQLiteDatabase db)
    {
        PasswdSafeUtil.dbginfo(TAG, "Set provider sync change %s: %d",
                               name, changeId);
        ContentValues values = new ContentValues();
        values.put(DB_COL_PROVIDERS_SYNC_CHANGE, changeId);
        setProviderField(name, values, db);
    }


    /** Get all of the files for a provider */
    public List<DbFile> getFiles(String providerName, SQLiteDatabase db)
            throws SQLException
    {
        long providerId = getProviderId(providerName, db);

        List<DbFile> files = new ArrayList<DbFile>();
        Cursor cursor = db.query(DB_TABLE_FILES, DbFile.QUERY_FIELDS,
                                 DB_MATCH_FILES_PROVIDER_ID,
                                 new String[] { Long.toString(providerId) },
                                 null, null, null);
        try {
            for (boolean more = cursor.moveToFirst(); more;
                    more = cursor.moveToNext()) {
                files.add(new DbFile(cursor));
            }
        } finally {
            cursor.close();
        }

        return files;
    }


    /** Add a remote file for a provider */
    public void addRemoteFile(String providerName,
                              String remId, String remTitle, long remModDate,
                              SQLiteDatabase db)
        throws SQLException
    {
        long providerId = getProviderId(providerName, db);
        ContentValues values = new ContentValues();
        values.put(DB_COL_FILES_PROVIDER, providerId);
        values.put(DB_COL_FILES_LOCAL_MOD_DATE, -1);
        values.put(DB_COL_FILES_LOCAL_DELETED, false);
        values.put(DB_COL_FILES_REMOTE_ID, remId);
        values.put(DB_COL_FILES_REMOTE_TITLE, remTitle);
        values.put(DB_COL_FILES_REMOTE_MOD_DATE, remModDate);
        values.put(DB_COL_FILES_REMOTE_DELETED, false);
        db.insertOrThrow(DB_TABLE_FILES, null, values);
    }


    /** Update a remote file */
    public void updateRemoteFile(long fileId, String remTitle,
                                 long remModDate, SQLiteDatabase db)
            throws SQLException
    {
        ContentValues values = new ContentValues();
        values.put(DB_COL_FILES_REMOTE_TITLE, remTitle);
        values.put(DB_COL_FILES_REMOTE_MOD_DATE, remModDate);
        values.put(DB_COL_FILES_REMOTE_DELETED, false);
        db.update(DB_TABLE_FILES, values,
                  DB_MATCH_FILES_ID, new String[] { Long.toString(fileId) });
    }


    /** Update a remote file as deleted */
    public void updateRemoteFileDeleted(long fileId, SQLiteDatabase db)
            throws SQLException
    {
        ContentValues values = new ContentValues();
        values.put(DB_COL_FILES_REMOTE_DELETED, true);
        db.update(DB_TABLE_FILES, values,
                  DB_MATCH_FILES_ID, new String[] { Long.toString(fileId) });
    }


    /** Remove the file */
    public void removeFile(long fileId, SQLiteDatabase db)
        throws SQLException
    {
        db.delete(DB_TABLE_FILES, DB_MATCH_FILES_ID,
                  new String[] { Long.toString(fileId) });
    }


    /** Get a field for a provider */
    private Cursor getProviderField(String name, String column)
        throws SQLException
    {
        SQLiteDatabase db = itsDbHelper.getReadableDatabase();
        return getProviderField(name, column, db);
    }


    /** Get a field for a provider */
    private Cursor getProviderField(String name, String column,
                                    SQLiteDatabase db)
        throws SQLException
    {
        String[] args = new String[] { ProviderType.GDRIVE.toString(), name };
        Cursor cursor = db.query(DB_TABLE_PROVIDERS,
                                 new String[] { column },
                                 DB_MATCH_PROVIDERS_TYPE_ACCT, args,
                                 null, null, null);
        if (cursor.moveToFirst()) {
            return cursor;
        } else {
            cursor.close();
        }
        return null;
    }


    /** Set a field for a provider */
    private void setProviderField(String name, ContentValues values,
                                  SQLiteDatabase db)
        throws SQLException
    {
        String[] args = new String[] { ProviderType.GDRIVE.toString(), name };
        db.update(DB_TABLE_PROVIDERS, values,
                  DB_MATCH_PROVIDERS_TYPE_ACCT, args);
    }


    /** Database helper class to manage the tables */
    private static final class DbHelper extends SQLiteOpenHelper
    {
        private static final String DB_NAME = "sync.db";
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
            PasswdSafeUtil.dbginfo(TAG, "Create DB");
            enableForeignKey(db);
            db.execSQL("CREATE TABLE " + DB_TABLE_PROVIDERS + " (" +
                       DB_COL_PROVIDERS_ID + " INTEGER PRIMARY KEY," +
                       DB_COL_PROVIDERS_TYPE + " TEXT NOT NULL," +
                       DB_COL_PROVIDERS_ACCT + " TEXT NOT NULL," +
                       DB_COL_PROVIDERS_SYNC_CHANGE + " INTEGER NOT NULL," +
                       DB_COL_PROVIDERS_SYNC_FREQ + " INTEGER NOT NULL" +
                       ");");
            db.execSQL("CREATE TABLE " + DB_TABLE_FILES + " (" +
                       DB_COL_FILES_ID + " INTEGER PRIMARY KEY," +
                       DB_COL_FILES_PROVIDER + " INTEGER REFERENCES " +
                           DB_TABLE_PROVIDERS + "(" + DB_COL_PROVIDERS_ID +
                           ") NOT NULL," +
                       DB_COL_FILES_LOCAL_FILE + " TEXT," +
                       DB_COL_FILES_LOCAL_MOD_DATE + " INTEGER NOT NULL," +
                       DB_COL_FILES_LOCAL_DELETED + " INTEGER NOT NULL," +
                       DB_COL_FILES_REMOTE_ID + " TEXT," +
                       DB_COL_FILES_REMOTE_TITLE + " TEXT," +
                       DB_COL_FILES_REMOTE_MOD_DATE + " INTEGER NOT NULL," +
                       DB_COL_FILES_REMOTE_DELETED + " INTEGER NOT NULL" +
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
}
