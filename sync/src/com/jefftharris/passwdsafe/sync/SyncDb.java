/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;

/**
 * The SyncDb encapsulates the synchronization database
 */
public class SyncDb
{
    private static final String TAG = "SyncDb";

    public static final String DB_TABLE_PROVIDERS = "providers";
    public static final String DB_COL_PROVIDERS_ID = BaseColumns._ID;
    public static final String DB_COL_PROVIDERS_TYPE = "type";
    public static final String DB_COL_PROVIDERS_ACCT = "acct";
    public static final String DB_COL_PROVIDERS_SYNC_CHANGE = "sync_change";
    public static final String DB_COL_PROVIDERS_SYNC_FREQ = "sync_freq";
    public static final String DB_MATCH_PROVIDERS_ID =
        DB_COL_PROVIDERS_ID + " = ?";
    private static final String DB_MATCH_PROVIDERS_TYPE_ACCT =
        DB_COL_PROVIDERS_TYPE + " = ? AND " + DB_COL_PROVIDERS_ACCT + " = ?";

    public static final String DB_TABLE_FILES = "files";
    public static final String DB_COL_FILES_ID = BaseColumns._ID;
    public static final String DB_COL_FILES_PROVIDER = "provider";
    public static final String DB_COL_FILES_LOCAL_FILE = "local_file";
    public static final String DB_COL_FILES_LOCAL_TITLE = "local_title";
    public static final String DB_COL_FILES_LOCAL_MOD_DATE = "local_mod_date";
    public static final String DB_COL_FILES_LOCAL_DELETED = "local_deleted";
    public static final String DB_COL_FILES_REMOTE_ID = "remote_id";
    public static final String DB_COL_FILES_REMOTE_TITLE = "remote_title";
    public static final String DB_COL_FILES_REMOTE_MOD_DATE = "remote_mod_date";
    public static final String DB_COL_FILES_REMOTE_DELETED = "remote_deleted";
    public static final String DB_MATCH_FILES_ID =
        DB_COL_FILES_ID + " = ?";
    public static final String DB_MATCH_FILES_PROVIDER_ID =
        DB_COL_FILES_PROVIDER + " = ?";

    public static final String DB_TABLE_SYNC_LOGS = "sync_logs";
    public static final String DB_COL_SYNC_LOGS_ID = BaseColumns._ID;
    public static final String DB_COL_SYNC_LOGS_ACCT = "acct";
    public static final String DB_COL_SYNC_LOGS_START = "start";
    public static final String DB_COL_SYNC_LOGS_END = "end";
    public static final String DB_COL_SYNC_LOGS_FLAGS = "flags";
    public static final String DB_COL_SYNC_LOGS_LOG = "log";
    public static final String DB_MATCH_SYNC_LOGS_START_BEFORE =
            DB_COL_SYNC_LOGS_START + " < ?";

    private DbHelper itsDbHelper;

    /** Entry in the providers table */
    public static class DbProvider
    {
        public final long itsId;
        public final String itsAcct;
        public final long itsSyncChange;
        public final int itsSyncFreq;

        public static final String[] QUERY_FIELDS = {
            DB_COL_PROVIDERS_ID,
            DB_COL_PROVIDERS_ACCT,
            DB_COL_PROVIDERS_SYNC_CHANGE,
            DB_COL_PROVIDERS_SYNC_FREQ };

        public DbProvider(Cursor cursor)
        {
            itsId = cursor.getLong(0);
            itsAcct = cursor.getString(1);
            itsSyncChange = cursor.getLong(2);
            itsSyncFreq = cursor.getInt(3);
        }

        @Override
        public String toString()
        {
            return String.format(
                    Locale.US,
                    "{id:%d, acct:%s, syncChange:%d, syncFreq:%d}",
                    itsId, itsAcct, itsSyncChange, itsSyncFreq);
        }
    }

    /** Entry in the files table */
    public static class DbFile
    {
        public final long itsId;
        public final String itsLocalFile;
        public final String itsLocalTitle;
        public final long itsLocalModDate;
        public final boolean itsIsLocalDeleted;
        public final String itsRemoteId;
        public final String itsRemoteTitle;
        public final long itsRemoteModDate;
        public final boolean itsIsRemoteDeleted;

        public static final String[] QUERY_FIELDS = {
            DB_COL_FILES_ID,
            DB_COL_FILES_LOCAL_FILE,
            DB_COL_FILES_LOCAL_TITLE,
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
            itsLocalTitle = cursor.getString(2);
            itsLocalModDate = cursor.getLong(3);
            itsIsLocalDeleted = cursor.getInt(4) != 0;
            itsRemoteId = cursor.getString(5);
            itsRemoteTitle = cursor.getString(6);
            itsRemoteModDate = cursor.getLong(7);
            itsIsRemoteDeleted = cursor.getInt(8) != 0;
        }

        @Override
        public String toString()
        {
            return String.format(Locale.US,
                    "{id:%d, local:{title:%s, file:%s, mod:%d, del:%b}, " +
                    "remote:{id:%s, title:'%s', mod:%d, del:%b}}",
                    itsId, itsLocalTitle, itsLocalFile, itsLocalModDate,
                    itsIsLocalDeleted, itsRemoteId, itsRemoteTitle,
                    itsRemoteModDate, itsIsRemoteDeleted);
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

    /** Add a provider */
    public static long addProvider(String name, int freq, SQLiteDatabase db)
        throws SQLException
    {
        ContentValues values = new ContentValues();
        values.put(DB_COL_PROVIDERS_TYPE, ProviderType.GDRIVE.toString());
        values.put(DB_COL_PROVIDERS_ACCT, name);
        values.put(DB_COL_PROVIDERS_SYNC_CHANGE, -1);
        values.put(DB_COL_PROVIDERS_SYNC_FREQ, freq);
        return db.insertOrThrow(DB_TABLE_PROVIDERS, null, values);
    }

    /** Delete a provider */
    public static void deleteProvider(long id, SQLiteDatabase db)
        throws SQLException
    {
        String[] idargs = new String[] { Long.toString(id) };
        db.delete(DB_TABLE_FILES, DB_MATCH_FILES_PROVIDER_ID, idargs);
        db.delete(DB_TABLE_PROVIDERS, DB_MATCH_PROVIDERS_ID, idargs);
    }

    /** Update a provider sync frequency */
    public static void updateProviderSyncFreq(long id, int freq,
                                              SQLiteDatabase db)
           throws SQLException
    {
        ContentValues values = new ContentValues();
        values.put(DB_COL_PROVIDERS_SYNC_FREQ, freq);
        updateProviderFields(id, values, db);
    }

    /** Update a provider sync change */
    public static void updateProviderSyncChange(DbProvider provider,
                                                long change,
                                                SQLiteDatabase db)
           throws SQLException
    {
        PasswdSafeUtil.dbginfo(TAG, "Set provider sync change %s: %d",
                               provider.itsAcct, change);
        ContentValues values = new ContentValues();
        values.put(DB_COL_PROVIDERS_SYNC_CHANGE, change);
        updateProviderFields(provider.itsId, values, db);
    }

    /** Get a provider by id */
    public static DbProvider getProvider(long id, SQLiteDatabase db)
            throws SQLException
    {
        return getProvider(DB_MATCH_PROVIDERS_ID,
                           new String[] { Long.toString(id) }, db);
    }

    /** Get a provider by name */
    public static DbProvider getProvider(String acctName, SQLiteDatabase db)
            throws SQLException
    {
        return getProvider(
                DB_MATCH_PROVIDERS_TYPE_ACCT,
                new String[] { ProviderType.GDRIVE.toString(), acctName },
                db);
    }

    /** Get the providers */
    public static List<DbProvider> getProviders(SQLiteDatabase db)
            throws SQLException
    {
        List<DbProvider> providers = new ArrayList<DbProvider>();
        Cursor cursor = db.query(DB_TABLE_PROVIDERS, DbProvider.QUERY_FIELDS,
                                 null, null, null, null, null);
        try {
            for (boolean more = cursor.moveToFirst(); more;
                    more = cursor.moveToNext()) {
                providers.add(new DbProvider(cursor));
            }
        } finally {
            cursor.close();
        }
        return providers;
    }

    /** Get a file */
    public DbFile getFile(long id)
            throws SQLException
    {
        SQLiteDatabase db = itsDbHelper.getReadableDatabase();
        return getFile(id, db);
    }


    /** Get a file */
    public static DbFile getFile(long id, SQLiteDatabase db)
            throws SQLException
    {
        Cursor cursor = db.query(DB_TABLE_FILES, DbFile.QUERY_FIELDS,
                                 DB_MATCH_FILES_ID,
                                 new String[] { Long.toString(id) },
                                 null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return new DbFile(cursor);
            }
        } finally {
            cursor.close();
        }

        return null;
    }


    /** Get all of the files for a provider by id */
    public static List<DbFile> getFiles(long providerId, SQLiteDatabase db)
            throws SQLException
    {
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


    /** Add a local file for a provider */
    public static long addLocalFile(long providerId, String title,
                                    long modDate, SQLiteDatabase db)
            throws SQLException
    {
        ContentValues values = new ContentValues();
        values.put(DB_COL_FILES_PROVIDER, providerId);
        values.put(DB_COL_FILES_LOCAL_TITLE, title);
        values.put(DB_COL_FILES_LOCAL_MOD_DATE, modDate);
        values.put(DB_COL_FILES_LOCAL_DELETED, false);
        values.put(DB_COL_FILES_REMOTE_MOD_DATE, -1);
        values.put(DB_COL_FILES_REMOTE_DELETED, false);
        return db.insertOrThrow(DB_TABLE_FILES, null, values);
    }


    /** Add a remote file for a provider */
    public static void addRemoteFile(long providerId,
                                     String remId, String remTitle,
                                     long remModDate, SQLiteDatabase db)
        throws SQLException
    {
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


    /** Update a local file */
    public static void updateLocalFile(long fileId, String locFile,
                                       String locTitle, long locModDate,
                                       SQLiteDatabase db)
            throws SQLException
    {
        ContentValues values = new ContentValues();
        values.put(DB_COL_FILES_LOCAL_FILE, locFile);
        values.put(DB_COL_FILES_LOCAL_TITLE, locTitle);
        values.put(DB_COL_FILES_LOCAL_MOD_DATE, locModDate);
        values.put(DB_COL_FILES_LOCAL_DELETED, false);
        db.update(DB_TABLE_FILES, values,
                  DB_MATCH_FILES_ID, new String[] { Long.toString(fileId) });
    }

    /** Update a remote file */
    public static void updateRemoteFile(long fileId, String remId,
                                        String remTitle, long remModDate,
                                        SQLiteDatabase db)
            throws SQLException
    {
        ContentValues values = new ContentValues();
        values.put(DB_COL_FILES_REMOTE_ID, remId);
        values.put(DB_COL_FILES_REMOTE_TITLE, remTitle);
        values.put(DB_COL_FILES_REMOTE_MOD_DATE, remModDate);
        values.put(DB_COL_FILES_REMOTE_DELETED, false);
        db.update(DB_TABLE_FILES, values,
                  DB_MATCH_FILES_ID, new String[] { Long.toString(fileId) });
    }


    /** Update a remote file as deleted */
    public static void updateRemoteFileDeleted(long fileId, SQLiteDatabase db)
            throws SQLException
    {
        ContentValues values = new ContentValues();
        values.put(DB_COL_FILES_REMOTE_DELETED, true);
        db.update(DB_TABLE_FILES, values,
                  DB_MATCH_FILES_ID, new String[] { Long.toString(fileId) });
    }


    /** Update a local file as deleted */
    public static void updateLocalFileDeleted(long fileId, SQLiteDatabase db)
            throws SQLException
    {
        ContentValues values = new ContentValues();
        values.put(DB_COL_FILES_LOCAL_DELETED, true);
        db.update(DB_TABLE_FILES, values,
                  DB_MATCH_FILES_ID, new String[] { Long.toString(fileId) });
    }


    /** Remove the file */
    public static void removeFile(long fileId, SQLiteDatabase db)
        throws SQLException
    {
        db.delete(DB_TABLE_FILES, DB_MATCH_FILES_ID,
                  new String[] { Long.toString(fileId) });
    }


    /** Add a sync log */
    public static void addSyncLog(SyncLogRecord logrec, SQLiteDatabase db)
        throws SQLException
    {
        ContentValues values = new ContentValues();
        values.put(DB_COL_SYNC_LOGS_ACCT, logrec.getAccount());
        values.put(DB_COL_SYNC_LOGS_START, logrec.getStartTime());
        values.put(DB_COL_SYNC_LOGS_END, logrec.getEndTime());
        values.put(DB_COL_SYNC_LOGS_LOG, logrec.getActions());

        int flags = 0;
        if (logrec.isFullSync()) {
            flags |= PasswdSafeContract.SyncLogs.FLAGS_IS_FULL;
        }
        if (logrec.isManualSync()) {
            flags |= PasswdSafeContract.SyncLogs.FLAGS_IS_MANUAL;
        }
        values.put(DB_COL_SYNC_LOGS_FLAGS, flags);

        db.insertOrThrow(DB_TABLE_SYNC_LOGS, null, values);
    }


    /** Delete old logs */
    public static void deleteSyncLogs(long removeBefore, SQLiteDatabase db)
        throws SQLException
    {
        db.delete(DB_TABLE_SYNC_LOGS, DB_MATCH_SYNC_LOGS_START_BEFORE,
                  new String[] { Long.toString(removeBefore) });
    }


    /** Get a provider */
    private static DbProvider getProvider(String match, String[] matchArgs,
                                          SQLiteDatabase db)
            throws SQLException
    {
        Cursor cursor = db.query(DB_TABLE_PROVIDERS, DbProvider.QUERY_FIELDS,
                                 match, matchArgs, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return new DbProvider(cursor);
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    /** Update fields for a provider */
    private static void updateProviderFields(long providerId,
                                             ContentValues values,
                                             SQLiteDatabase db)
        throws SQLException
    {
        String[] idargs = new String[] { Long.toString(providerId) };
        db.update(DB_TABLE_PROVIDERS, values,
                  DB_MATCH_PROVIDERS_ID, idargs);
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
                       DB_COL_PROVIDERS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                       DB_COL_PROVIDERS_TYPE + " TEXT NOT NULL," +
                       DB_COL_PROVIDERS_ACCT + " TEXT NOT NULL," +
                       DB_COL_PROVIDERS_SYNC_CHANGE + " INTEGER NOT NULL," +
                       DB_COL_PROVIDERS_SYNC_FREQ + " INTEGER NOT NULL" +
                       ");");
            db.execSQL("CREATE TABLE " + DB_TABLE_FILES + " (" +
                       DB_COL_FILES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                       DB_COL_FILES_PROVIDER + " INTEGER REFERENCES " +
                           DB_TABLE_PROVIDERS + "(" + DB_COL_PROVIDERS_ID +
                           ") NOT NULL," +
                       DB_COL_FILES_LOCAL_FILE + " TEXT," +
                       DB_COL_FILES_LOCAL_TITLE + " TEXT," +
                       DB_COL_FILES_LOCAL_MOD_DATE + " INTEGER NOT NULL," +
                       DB_COL_FILES_LOCAL_DELETED + " INTEGER NOT NULL," +
                       DB_COL_FILES_REMOTE_ID + " TEXT," +
                       DB_COL_FILES_REMOTE_TITLE + " TEXT," +
                       DB_COL_FILES_REMOTE_MOD_DATE + " INTEGER NOT NULL," +
                       DB_COL_FILES_REMOTE_DELETED + " INTEGER NOT NULL" +
                       ");");
            db.execSQL("CREATE TABLE " + DB_TABLE_SYNC_LOGS + " (" +
                       DB_COL_SYNC_LOGS_ID + " INTEGER PRIMARY KEY," +
                       DB_COL_SYNC_LOGS_ACCT + " TEXT NOT NULL," +
                       DB_COL_SYNC_LOGS_START + " INTEGER NOT NULL," +
                       DB_COL_SYNC_LOGS_END + " INTEGER NOT NULL," +
                       DB_COL_SYNC_LOGS_FLAGS + " INTEGER NOT NULL," +
                       DB_COL_SYNC_LOGS_LOG + " TEXT" +
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
