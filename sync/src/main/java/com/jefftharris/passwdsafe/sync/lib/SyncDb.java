/*
 * Copyright (©) 2013-2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;

/**
 * The SyncDb encapsulates the synchronization database
 */
@SuppressWarnings("TryFinallyCanBeTryWithResources")
public class SyncDb
{
    private static final String TAG = "SyncDb";

    public static final String GDRIVE_ACCOUNT_TYPE = "com.google";
    public static final String DROPBOX_ACCOUNT_TYPE = "com.jefftharris.dropbox";
    public static final String BOX_ACCOUNT_TYPE = "com.jefftharris.box";
    public static final String ONEDRIVE_ACCOUNT_TYPE =
            "com.jefftharris.onedrive";
    public static final String OWNCLOUD_ACCOUNT_TYPE = "owncloud";

    public static final String DB_TABLE_PROVIDERS = "providers";
    public static final String DB_COL_PROVIDERS_ID = BaseColumns._ID;
    public static final String DB_COL_PROVIDERS_TYPE = "type";
    public static final String DB_COL_PROVIDERS_ACCT = "acct";
    public static final String DB_COL_PROVIDERS_SYNC_CHANGE = "sync_change";
    public static final String DB_COL_PROVIDERS_SYNC_FREQ = "sync_freq";
    public static final String DB_COL_PROVIDERS_DISPLAY_NAME = "display_name";
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
    public static final String DB_COL_FILES_LOCAL_FOLDER = "local_folder";
    public static final String DB_COL_FILES_LOCAL_CHANGE = "local_change";
    public static final String DB_COL_FILES_REMOTE_ID = "remote_id";
    public static final String DB_COL_FILES_REMOTE_TITLE = "remote_title";
    public static final String DB_COL_FILES_REMOTE_MOD_DATE = "remote_mod_date";
    public static final String DB_COL_FILES_REMOTE_DELETED = "remote_deleted";
    public static final String DB_COL_FILES_REMOTE_FOLDER = "remote_folder";
    public static final String DB_COL_FILES_REMOTE_CHANGE = "remote_change";
    public static final String DB_COL_FILES_REMOTE_HASH = "remote_hash";
    public static final String DB_MATCH_FILES_ID =
        DB_COL_FILES_ID + " = ?";
    public static final String DB_MATCH_FILES_PROVIDER_ID =
        DB_COL_FILES_PROVIDER + " = ?";
    private static final String DB_MATCH_FILES_PROVIDER_REMOTE_ID =
        DB_COL_FILES_PROVIDER + " = ? AND " +
        DB_COL_FILES_REMOTE_ID + " = ?";

    public static final String DB_TABLE_SYNC_LOGS = "sync_logs";
    public static final String DB_COL_SYNC_LOGS_ID = BaseColumns._ID;
    public static final String DB_COL_SYNC_LOGS_ACCT = "acct";
    public static final String DB_COL_SYNC_LOGS_START = "start";
    public static final String DB_COL_SYNC_LOGS_END = "end";
    public static final String DB_COL_SYNC_LOGS_FLAGS = "flags";
    public static final String DB_COL_SYNC_LOGS_LOG = "log";
    private static final String DB_MATCH_SYNC_LOGS_START_BEFORE =
            DB_COL_SYNC_LOGS_START + " < ?";

    private static SyncDb itsDb = null;

    private final DbHelper itsDbHelper;
    private final ReentrantLock itsMutex = new ReentrantLock();

    /** Initialize the single SyncDb instance */
    public static void initializeDb(Context ctx)
    {
        itsDb = new SyncDb(ctx);
    }

    /** Finalize the single SyncDb instance */
    public static void finalizeDb()
    {
        itsDb.close();
        itsDb = null;
    }

    /** Acquire the single SyncDb instance */
    public static SyncDb acquire()
    {
        itsDb.doAcquire();
        return itsDb;
    }

    /** Constructor */
    private SyncDb(Context ctx)
    {
        itsDbHelper = new DbHelper(ctx);
    }

    /** Close the DB */
    private void close()
    {
        itsDbHelper.close();
    }

    /** Acquire a lock on the database */
    private void doAcquire()
    {
        itsMutex.lock();
    }

    /** Release the lock on the database */
    public void release()
    {
        itsMutex.unlock();
    }

    /** Get the database */
    public SQLiteDatabase getDb()
    {
        return itsDbHelper.getWritableDatabase();
    }

    /** Begin a transaction */
    public SQLiteDatabase beginTransaction()
        throws SQLException
    {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        return db;
    }

    /** End a transaction and release the database */
    public void endTransactionAndRelease()
        throws SQLException
    {
        try {
            getDb().endTransaction();
        } finally {
            release();
        }
    }

    /** Add a provider */
    public static long addProvider(String name, ProviderType type,
                                   int freq, SQLiteDatabase db)
        throws SQLException
    {
        ContentValues values = new ContentValues();
        values.put(DB_COL_PROVIDERS_TYPE, type.toString());
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

    /** Update a provider display name */
    public static void updateProviderDisplayName(long id, String displayName,
                                                 SQLiteDatabase db)
           throws SQLException
    {
        ContentValues values = new ContentValues();
        values.put(DB_COL_PROVIDERS_DISPLAY_NAME, displayName);
        updateProviderFields(id, values, db);
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

    /** Get a provider by id */
    public static DbProvider getProvider(long id, SQLiteDatabase db)
            throws SQLException
    {
        return getProvider(DB_MATCH_PROVIDERS_ID,
                           new String[] { Long.toString(id) }, db);
    }

    /** Get a provider by name and type */
    public static DbProvider getProvider(String acctName,
                                         ProviderType type,
                                         SQLiteDatabase db)
            throws SQLException
    {
        return getProvider(
                DB_MATCH_PROVIDERS_TYPE_ACCT,
                new String[] { type.name(), acctName },
                db);
    }

    /** Get the providers */
    public static List<DbProvider> getProviders(SQLiteDatabase db)
            throws SQLException
    {
        List<DbProvider> providers = new ArrayList<>();
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

    /** Get a file by id */
    public static DbFile getFile(long id, SQLiteDatabase db)
            throws SQLException
    {
        return getFile(DB_MATCH_FILES_ID, new String[] { Long.toString(id) },
                       db);
    }


    /** Get a file by provider and remote file id */
    public static DbFile getFileByRemoteId(long provider,
                                           String remoteId,
                                           SQLiteDatabase db)
            throws SQLException
    {
        return getFile(DB_MATCH_FILES_PROVIDER_REMOTE_ID,
                       new String[] { Long.toString(provider),
                                      remoteId },
                       db);
    }


    /** Get all of the files for a provider by id */
    public static List<DbFile> getFiles(long providerId, SQLiteDatabase db)
            throws SQLException
    {
        List<DbFile> files = new ArrayList<>();
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
        values.put(DB_COL_FILES_LOCAL_CHANGE,
                   DbFile.FileChange.toDbStr(DbFile.FileChange.ADDED));
        values.put(DB_COL_FILES_REMOTE_MOD_DATE, -1);
        values.put(DB_COL_FILES_REMOTE_DELETED, false);
        return db.insertOrThrow(DB_TABLE_FILES, null, values);
    }


    /** Add a remote file for a provider */
    public static long addRemoteFile(long providerId,
                                     String remId, String remTitle,
                                     String remFolder, long remModDate,
                                     String remHash, SQLiteDatabase db)
        throws SQLException
    {
        ContentValues values = new ContentValues();
        values.put(DB_COL_FILES_PROVIDER, providerId);
        values.put(DB_COL_FILES_LOCAL_MOD_DATE, -1);
        values.put(DB_COL_FILES_LOCAL_DELETED, false);
        values.put(DB_COL_FILES_REMOTE_ID, remId);
        values.put(DB_COL_FILES_REMOTE_TITLE, remTitle);
        values.put(DB_COL_FILES_REMOTE_MOD_DATE, remModDate);
        values.put(DB_COL_FILES_REMOTE_HASH, remHash);
        values.put(DB_COL_FILES_REMOTE_DELETED, false);
        values.put(DB_COL_FILES_REMOTE_FOLDER, remFolder);
        values.put(DB_COL_FILES_REMOTE_CHANGE,
                   DbFile.FileChange.toDbStr(DbFile.FileChange.ADDED));
        return db.insertOrThrow(DB_TABLE_FILES, null, values);
    }


    /** Update a local file */
    public static void updateLocalFile(long fileId, String locFile,
                                       String locTitle, String locFolder,
                                       long locModDate, SQLiteDatabase db)
            throws SQLException
    {
        ContentValues values = new ContentValues();
        values.put(DB_COL_FILES_LOCAL_FILE, locFile);
        values.put(DB_COL_FILES_LOCAL_TITLE, locTitle);
        values.put(DB_COL_FILES_LOCAL_MOD_DATE, locModDate);
        values.put(DB_COL_FILES_LOCAL_DELETED, false);
        values.put(DB_COL_FILES_LOCAL_FOLDER, locFolder);
        db.update(DB_TABLE_FILES, values,
                  DB_MATCH_FILES_ID, new String[] { Long.toString(fileId) });
    }


    /** Update the change for a local file */
    public static void updateLocalFileChange(long fileId,
                                             DbFile.FileChange change,
                                             SQLiteDatabase db)
            throws SQLException
    {
        ContentValues values = new ContentValues();
        values.put(DB_COL_FILES_LOCAL_CHANGE,
                   DbFile.FileChange.toDbStr(change));
        db.update(DB_TABLE_FILES, values,
                  DB_MATCH_FILES_ID, new String[] { Long.toString(fileId) });
    }


    /** Update a remote file */
    public static void updateRemoteFile(long fileId, String remId,
                                        String remTitle, String remFolder,
                                        long remModDate, String remHash,
                                        SQLiteDatabase db)
            throws SQLException
    {
        ContentValues values = new ContentValues();
        values.put(DB_COL_FILES_REMOTE_ID, remId);
        values.put(DB_COL_FILES_REMOTE_TITLE, remTitle);
        values.put(DB_COL_FILES_REMOTE_MOD_DATE, remModDate);
        values.put(DB_COL_FILES_REMOTE_HASH, remHash);
        values.put(DB_COL_FILES_REMOTE_DELETED, false);
        values.put(DB_COL_FILES_REMOTE_FOLDER, remFolder);
        db.update(DB_TABLE_FILES, values,
                  DB_MATCH_FILES_ID, new String[] { Long.toString(fileId) });
    }


    /** Update the change for a remote file */
    public static void updateRemoteFileChange(long fileId,
                                              DbFile.FileChange change,
                                              SQLiteDatabase db)
            throws SQLException
    {
        ContentValues values = new ContentValues();
        values.put(DB_COL_FILES_REMOTE_CHANGE,
                   DbFile.FileChange.toDbStr(change));
        switch (change) {
        case ADDED:
        case MODIFIED: {
            values.put(DB_COL_FILES_REMOTE_DELETED, false);
            break;
        }
        case REMOVED: {
            values.put(DB_COL_FILES_REMOTE_DELETED, true);
            break;
        }
        case NO_CHANGE: {
            break;
        }
        }
        db.update(DB_TABLE_FILES, values,
                  DB_MATCH_FILES_ID, new String[] { Long.toString(fileId) });
    }


    /** Update a remote file as deleted */
    public static void updateRemoteFileDeleted(long fileId, SQLiteDatabase db)
            throws SQLException
    {
        ContentValues values = new ContentValues();
        values.put(DB_COL_FILES_REMOTE_DELETED, true);
        values.put(DB_COL_FILES_REMOTE_CHANGE,
                DbFile.FileChange.toDbStr(DbFile.FileChange.REMOVED));
        db.update(DB_TABLE_FILES, values,
                  DB_MATCH_FILES_ID, new String[] { Long.toString(fileId) });
    }


    /** Update a local file as deleted */
    public static void updateLocalFileDeleted(long fileId, SQLiteDatabase db)
            throws SQLException
    {
        ContentValues values = new ContentValues();
        values.put(DB_COL_FILES_LOCAL_DELETED, true);
        values.put(DB_COL_FILES_LOCAL_CHANGE,
                   DbFile.FileChange.toDbStr(DbFile.FileChange.REMOVED));
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
        if (logrec.isManualSync()) {
            flags |= PasswdSafeContract.SyncLogs.FLAGS_IS_MANUAL;
        }
        if (logrec.isNotConnected()) {
            flags |= PasswdSafeContract.SyncLogs.FLAGS_IS_NOT_CONNECTED;
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


    /** Get a file */
    private static DbFile getFile(String match, String[] matchArgs,
                                  SQLiteDatabase db)
            throws SQLException
    {
        Cursor cursor = db.query(DB_TABLE_FILES, DbFile.QUERY_FIELDS,
                                 match, matchArgs, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return new DbFile(cursor);
            }
        } finally {
            cursor.close();
        }

        return null;
    }


    /** Database helper class to manage the tables */
    private static final class DbHelper extends SQLiteOpenHelper
    {
        private static final String DB_NAME = "sync.db";
        private static final int DB_VERSION = 4;

        private final Context itsContext;

        /** Constructor */
        public DbHelper(Context context)
        {
            super(context, DB_NAME, null, DB_VERSION);
            itsContext = context;
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

            onUpgrade(db, 1, DB_VERSION);
        }

        /* (non-Javadoc)
         * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            enableForeignKey(db);

            if (oldVersion < 2) {
                PasswdSafeUtil.dbginfo(TAG, "Upgrade to v2");
                db.execSQL("ALTER TABLE " + DB_TABLE_PROVIDERS +
                           " ADD COLUMN " + DB_COL_PROVIDERS_DISPLAY_NAME +
                           " TEXT;");
            }

            if (oldVersion < 3) {
                PasswdSafeUtil.dbginfo(TAG, "Upgrade to v3");
                db.execSQL("ALTER TABLE " + DB_TABLE_FILES +
                           " ADD COLUMN " + DB_COL_FILES_LOCAL_FOLDER +
                           " TEXT;");
                db.execSQL("ALTER TABLE " + DB_TABLE_FILES +
                           " ADD COLUMN " + DB_COL_FILES_REMOTE_FOLDER +
                           " TEXT;");
            }

            if (oldVersion < 4) {
                PasswdSafeUtil.dbginfo(TAG, "Upgrade to v4");
                db.execSQL("ALTER TABLE " + DB_TABLE_FILES +
                           " ADD COLUMN " + DB_COL_FILES_LOCAL_CHANGE +
                           " TEXT;");
                db.execSQL("ALTER TABLE " + DB_TABLE_FILES +
                           " ADD COLUMN " + DB_COL_FILES_REMOTE_CHANGE +
                           " TEXT;");
                db.execSQL("ALTER TABLE " + DB_TABLE_FILES +
                           " ADD COLUMN " + DB_COL_FILES_REMOTE_HASH +
                           " TEXT;");

                for (DbProvider provider: getProviders(db)) {
                    for (DbFile file: getFiles(provider.itsId, db)) {
                        onUpgradeV4File(file, db);
                    }
                }
            }
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

        /** Upgrade a file for the v4 schema */
        private void onUpgradeV4File(DbFile file, SQLiteDatabase db)
                throws SQLException
        {
            DbFile.FileChange local = DbFile.FileChange.NO_CHANGE;
            DbFile.FileChange remote = DbFile.FileChange.NO_CHANGE;
            if (file.itsIsRemoteDeleted) {
                remote = DbFile.FileChange.REMOVED;
            } else if (TextUtils.isEmpty(file.itsLocalFile) ||
                    (file.itsLocalModDate == -1) ||
                    !itsContext.getFileStreamPath(file.itsLocalFile).exists()) {
                remote = DbFile.FileChange.ADDED;
            } else if (!TextUtils.equals(file.itsLocalFolder,
                                         file.itsRemoteFolder) ||
                    (file.itsRemoteModDate > file.itsLocalModDate)) {
                remote = DbFile.FileChange.MODIFIED;
            }

            if (file.itsIsLocalDeleted) {
                local = DbFile.FileChange.REMOVED;
            } else if (TextUtils.isEmpty(file.itsRemoteId)) {
                local = DbFile.FileChange.ADDED;
            } else if (file.itsLocalModDate > file.itsRemoteModDate) {
                local = DbFile.FileChange.MODIFIED;
            }

            if (local != DbFile.FileChange.NO_CHANGE) {
                updateLocalFileChange(file.itsId, local, db);
            }
            if (remote != DbFile.FileChange.NO_CHANGE) {
                updateRemoteFileChange(file.itsId, remote, db);
            }
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
