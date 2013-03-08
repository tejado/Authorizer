/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import java.util.Collection;

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
    private static final String DB_COL_FILES_FILE_ID = "file_id";
    private static final String DB_COL_FILES_FILE_TITLE = "file_title";
    private static final String DB_COL_FILES_FILE_MOD_DATE = "file_mod_date";
    private static final String DB_MATCH_FILES_ID =
        DB_COL_FILES_ID + " = ?";
    private static final String DB_MATCH_FILES_PROVIDER_ID =
        DB_COL_FILES_PROVIDER + " = ?";

    private DbHelper itsDbHelper;

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
    public long getProviderSyncChange(String name)
        throws SQLException
    {
        long changeId = -1;
        Cursor cursor = getProviderField(name, DB_COL_PROVIDERS_SYNC_CHANGE);
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
    public void setProviderSyncChange(String name, long changeId)
    {
        PasswdSafeUtil.dbginfo(TAG, "Set provider sync change %s: %d",
                               name, changeId);
        ContentValues values = new ContentValues();
        values.put(DB_COL_PROVIDERS_SYNC_CHANGE, changeId);
        setProviderField(name, values);
    }


    /** Get a cursor for all of the files for a provider */
    public Cursor getFiles(String providerName)
        throws SQLException
    {
        long providerId = getProviderId(providerName);
        SQLiteDatabase db = itsDbHelper.getReadableDatabase();
        return db.query(DB_TABLE_FILES,
                        new String[] { DB_COL_FILES_ID,
                                       DB_COL_FILES_FILE_ID,
                                       DB_COL_FILES_FILE_TITLE,
                                       DB_COL_FILES_FILE_MOD_DATE,
                                       DB_COL_FILES_LOCAL_FILE },
                        DB_MATCH_FILES_PROVIDER_ID,
                        new String[] { Long.toString(providerId) },
                        null, null, null);
    }


    /** Add a file for a provider */
    public void addFile(String providerName,
                        String localFile,
                        String fileId,
                        String fileTitle,
                        long fileModDate)
        throws SQLException
    {
        long providerId = getProviderId(providerName);
        SQLiteDatabase db = itsDbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            ContentValues values = new ContentValues();
            values.put(DB_COL_FILES_PROVIDER, providerId);
            values.put(DB_COL_FILES_LOCAL_FILE, localFile);
            values.put(DB_COL_FILES_FILE_ID, fileId);
            values.put(DB_COL_FILES_FILE_TITLE, fileTitle);
            values.put(DB_COL_FILES_FILE_MOD_DATE, fileModDate);
            db.insertOrThrow(DB_TABLE_FILES, null, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }


    /** Update a file */
    public void updateFile(long fileId, String localFile,
                           String fileTitle, long fileModDate)
        throws SQLException
    {
        SQLiteDatabase db = itsDbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            ContentValues values = new ContentValues();
            if (localFile != null) {
                values.put(DB_COL_FILES_LOCAL_FILE, localFile);
            }
            values.put(DB_COL_FILES_FILE_TITLE, fileTitle);
            values.put(DB_COL_FILES_FILE_MOD_DATE, fileModDate);
            db.update(DB_TABLE_FILES, values,
                      DB_MATCH_FILES_ID,
                      new String[] { Long.toString(fileId) });
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }


    /** Remove the files */
    public void removeFiles(Collection<Long> fileIds)
        throws SQLException
    {
        SQLiteDatabase db = itsDbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            String[] args = new String[1];
            for (Long id: fileIds) {
                args[0] = id.toString();
                db.delete(DB_TABLE_FILES, DB_MATCH_FILES_ID, args);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }


    /** Get a field for a provider */
    private Cursor getProviderField(String name, String column)
        throws SQLException
    {
        SQLiteDatabase db = itsDbHelper.getReadableDatabase();
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
    private void setProviderField(String name, ContentValues values)
        throws SQLException
    {
        SQLiteDatabase db = itsDbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            String[] args = new String[] { ProviderType.GDRIVE.toString(),
                                           name };
            db.update(DB_TABLE_PROVIDERS, values,
                      DB_MATCH_PROVIDERS_TYPE_ACCT, args);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /*
    public void setProviderSyncFreq(String name, int freq)
        throws SQLException
    {
        SQLiteDatabase db = itsDbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            String[] args = new String[] { ProviderType.GDRIVE.toString(),
                                           name };
            ContentValues values = new ContentValues();
            values.put(DB_COL_PROVIDERS_SYNC_FREQ, freq);
            db.update(DB_TABLE_PROVIDERS, values,
                      DB_MATCH_PROVIDERS_TYPE_ACCT, args);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

    }
    */

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
                       DB_COL_FILES_FILE_ID + " TEXT NOT NULL," +
                       DB_COL_FILES_FILE_TITLE + " TEXT NOT NULL," +
                       DB_COL_FILES_FILE_MOD_DATE + " INTEGER NOT NULL" +
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
