/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

/**
 *  The PasswdSafeProvider class is a content provider for synced
 *  password files
 */
public class PasswdSafeProvider extends ContentProvider
{
    private static final String TAG = "PasswdSafeProvider";

    private static final UriMatcher MATCHER;
    private static final int MATCH_PROVIDERS = 1;
    private static final int MATCH_PROVIDER = 2;
    private static final int MATCH_PROVIDER_FILES = 3;
    private static final int MATCH_PROVIDER_FILE = 4;
    private static final int MATCH_PROVIDER_SYNC_LOGS = 5;

    private static final HashMap<String, String> PROVIDERS_MAP;
    private static final HashMap<String, String> FILES_MAP;
    private static final HashMap<String, String> SYNC_LOGS_MAP;

    private SyncDb itsDb;
    private OnAccountsUpdateListener itsListener;

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
                       MATCH_PROVIDER_SYNC_LOGS);

        PROVIDERS_MAP = new HashMap<String, String>();
        PROVIDERS_MAP.put(PasswdSafeContract.Providers._ID,
                          SyncDb.DB_COL_PROVIDERS_ID);
        PROVIDERS_MAP.put(PasswdSafeContract.Providers.COL_TYPE,
                          SyncDb.DB_COL_PROVIDERS_TYPE);
        PROVIDERS_MAP.put(PasswdSafeContract.Providers.COL_ACCT,
                          SyncDb.DB_COL_PROVIDERS_ACCT);
        PROVIDERS_MAP.put(PasswdSafeContract.Providers.COL_SYNC_FREQ,
                          SyncDb.DB_COL_PROVIDERS_SYNC_FREQ);

        FILES_MAP = new HashMap<String, String>();
        FILES_MAP.put(PasswdSafeContract.Files._ID,
                      SyncDb.DB_COL_FILES_ID);
        FILES_MAP.put(PasswdSafeContract.Files.COL_PROVIDER,
                      SyncDb.DB_COL_FILES_PROVIDER + " AS " +
                              PasswdSafeContract.Files.COL_PROVIDER);
        FILES_MAP.put(PasswdSafeContract.Files.COL_TITLE,
                      SyncDb.DB_COL_FILES_LOCAL_TITLE + " AS " +
                              PasswdSafeContract.Files.COL_TITLE);
        FILES_MAP.put(PasswdSafeContract.Files.COL_MOD_DATE,
                      SyncDb.DB_COL_FILES_LOCAL_MOD_DATE + " AS " +
                              PasswdSafeContract.Files.COL_MOD_DATE);
        FILES_MAP.put(PasswdSafeContract.Files.COL_FILE,
                      SyncDb.DB_COL_FILES_LOCAL_FILE + " AS " +
                              PasswdSafeContract.Files.COL_FILE);

        SYNC_LOGS_MAP = new HashMap<String, String>();
        SYNC_LOGS_MAP.put(PasswdSafeContract.SyncLogs._ID,
                          SyncDb.DB_COL_SYNC_LOGS_ID);
        SYNC_LOGS_MAP.put(PasswdSafeContract.SyncLogs.COL_DATE,
                          SyncDb.DB_COL_SYNC_LOGS_DATE);
        SYNC_LOGS_MAP.put(PasswdSafeContract.SyncLogs.COL_LOG,
                          SyncDb.DB_COL_SYNC_LOGS_LOG);
    }


    /* (non-Javadoc)
     * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs)
    {
        if (selection != null) {
            throw new IllegalArgumentException("selection not supported");
        }
        if (selectionArgs != null) {
            throw new IllegalArgumentException("selectionArgs not supported");
        }

        switch (MATCHER.match(uri)) {
        case MATCH_PROVIDER: {
            PasswdSafeUtil.dbginfo(TAG, "Delete provider: %s", uri);
            Long id = Long.valueOf(uri.getPathSegments().get(1));
            SQLiteDatabase db = itsDb.getDb();
            try {
                db.beginTransaction();
                SyncDb.DbProvider provider = SyncDb.getProvider(id, db);
                if (provider == null) {
                    return 0;
                }

                GDriveSyncer.deleteProvider(provider, db, getContext(), null);
                db.setTransactionSuccessful();
                return 1;
            } catch (Exception e) {
                String msg = "Error deleting provier: " + uri;
                Log.e(TAG, msg, e);
                throw new RuntimeException(msg, e);
            } finally {
                db.endTransaction();
            }
        }
        case MATCH_PROVIDER_FILE: {
            PasswdSafeUtil.dbginfo(TAG, "Delete file: %s", uri);
            SQLiteDatabase db = itsDb.getDb();
            try {
                db.beginTransaction();
                Long id = Long.valueOf(uri.getPathSegments().get(3));
                SyncDb.DbFile file = SyncDb.getFile(id, db);
                if (file == null) {
                    return 0;
                }
                SyncDb.updateLocalFileDeleted(id, db);
                db.setTransactionSuccessful();

                getContext().getContentResolver().notifyChange(uri, null);
                return 1;
            } catch (Exception e) {
                String msg = "Error deleting file: " + uri;
                Log.e(TAG, msg, e);
                throw new RuntimeException(msg, e);
            } finally {
                db.endTransaction();
            }
        }
        default: {
            throw new IllegalArgumentException(
                    "delete unknown match for uri: " + uri);
        }
        }
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#getType(android.net.Uri)
     */
    @Override
    public String getType(Uri uri)
    {
        switch (MATCHER.match(uri)) {
        case MATCH_PROVIDERS: {
            return PasswdSafeContract.Providers.CONTENT_TYPE;
        }
        case MATCH_PROVIDER: {
            return PasswdSafeContract.Providers.CONTENT_ITEM_TYPE;
        }
        case MATCH_PROVIDER_FILES: {
            return PasswdSafeContract.Files.CONTENT_TYPE;
        }
        case MATCH_PROVIDER_FILE: {
            return PasswdSafeContract.Files.CONTENT_ITEM_TYPE;
        }
        default: {
            throw new IllegalArgumentException(
                    "type unknown match for uri: " + uri);
        }
        }
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues)
     */
    @Override
    public Uri insert(Uri uri, ContentValues values)
    {
        switch (MATCHER.match(uri)) {
        case MATCH_PROVIDERS: {
            String acct = values.getAsString(
                    PasswdSafeContract.Providers.COL_ACCT);
            if (acct == null) {
                throw new IllegalArgumentException("No acct for provider");
            }
            PasswdSafeUtil.dbginfo(TAG, "Insert provider: %s", acct);
            SQLiteDatabase db = itsDb.getDb();
            try {
                db.beginTransaction();
                long id = GDriveSyncer.addProvider(acct, db, getContext());
                db.setTransactionSuccessful();

                return ContentUris.withAppendedId(
                        PasswdSafeContract.Providers.CONTENT_URI, id);
            } catch (Exception e) {
                String msg = "Error adding provider: " + acct;
                Log.e(TAG, msg, e);
                throw new RuntimeException(msg, e);
            } finally {
                db.endTransaction();
            }
        }
        case MATCH_PROVIDER_FILES: {
            String title = values.getAsString(
                    PasswdSafeContract.Files.COL_TITLE);
            if (title == null) {
                throw new IllegalArgumentException("No title for file");
            }
            PasswdSafeUtil.dbginfo(TAG, "Insert file \"%s\" for %s",
                                   title, uri);
            SQLiteDatabase db = itsDb.getDb();
            try {
                db.beginTransaction();
                Long providerId = Long.valueOf(uri.getPathSegments().get(1));
                SyncDb.DbProvider provider = SyncDb.getProvider(providerId, db);
                if (provider == null) {
                    throw new Exception("No provider for " + providerId);
                }
                long id = SyncDb.addLocalFile(providerId, title,
                                              System.currentTimeMillis(), db);
                db.setTransactionSuccessful();

                ContentResolver cr = getContext().getContentResolver();
                cr.notifyChange(uri, null);
                return ContentUris.withAppendedId(uri, id);
            } catch (Exception e) {
                String msg = "Error adding file: " + title;
                Log.e(TAG, msg, e);
                throw new RuntimeException(msg, e);
            } finally {
                db.endTransaction();
            }
        }
        default: {
            throw new IllegalArgumentException(
                    "insert unknown match for uri: " + uri);
        }
        }
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#onCreate()
     */
    @Override
    public boolean onCreate()
    {
        PasswdSafeUtil.dbginfo(TAG, "onCreate");
        itsDb = new SyncDb(getContext());
        itsListener = new OnAccountsUpdateListener()
        {
            @Override
            public void onAccountsUpdated(Account[] accounts)
            {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params)
                    {
                        SQLiteDatabase db = itsDb.getDb();
                        try {
                            db.beginTransaction();
                            GDriveSyncer.validateAccounts(db, getContext());
                            db.setTransactionSuccessful();
                        } catch (Exception e) {
                            Log.e(TAG, "Error validating accounts", e);
                        } finally {
                            db.endTransaction();
                        }
                        return null;
                    }
                }.execute();
            }
        };
        AccountManager mgr = AccountManager.get(getContext());
        mgr.addOnAccountsUpdatedListener(itsListener, null, false);

        return true;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
     */
    @Override
    public Cursor query(Uri uri,
                        String[] projection,
                        String selection,
                        String[] selectionArgs,
                        String sortOrder)
    {
        PasswdSafeUtil.dbginfo(TAG, "query uri: %s", uri);

        boolean selectionValid = (selection == null);
        if (selectionArgs != null) {
            throw new IllegalArgumentException("selectionArgs not supported");
        }
        boolean sortOrderValid = (sortOrder == null);

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (MATCHER.match(uri)) {
        case MATCH_PROVIDERS: {
            qb.setTables(SyncDb.DB_TABLE_PROVIDERS);
            qb.setProjectionMap(PROVIDERS_MAP);
            break;
        }
        case MATCH_PROVIDER: {
            qb.setTables(SyncDb.DB_TABLE_PROVIDERS);
            qb.setProjectionMap(PROVIDERS_MAP);
            selection = SyncDb.DB_MATCH_PROVIDERS_ID;
            selectionArgs = new String[] { uri.getPathSegments().get(1) };
            break;
        }
        case MATCH_PROVIDER_FILES: {
            qb.setTables(SyncDb.DB_TABLE_FILES);
            qb.setProjectionMap(FILES_MAP);

            StringBuffer fullSelection =
                    new StringBuffer(SyncDb.DB_MATCH_FILES_PROVIDER_ID);
            if (PasswdSafeContract.Files.NOT_DELETED_SELECTION.equals(
                        selection)) {
                selectionValid = true;
                fullSelection.append(" and ");
                fullSelection.append(selection);
            }
            selection = fullSelection.toString();

            selectionArgs = new String[] { uri.getPathSegments().get(1) };
            if (PasswdSafeContract.Files.TITLE_SORT_ORDER.equals(sortOrder)) {
                sortOrderValid = true;
            }
            break;
        }
        case MATCH_PROVIDER_FILE: {
            qb.setTables(SyncDb.DB_TABLE_FILES);
            qb.setProjectionMap(FILES_MAP);
            selection = SyncDb.DB_MATCH_FILES_ID;
            selectionArgs = new String[] { uri.getPathSegments().get(3) };
            break;
        }
        case MATCH_PROVIDER_SYNC_LOGS: {
            qb.setTables(SyncDb.DB_TABLE_SYNC_LOGS);
            qb.setProjectionMap(SYNC_LOGS_MAP);
            if (PasswdSafeContract.SyncLogs.DATE_SORT_ORDER.equals(sortOrder)) {
                sortOrderValid = true;
            }
            break;
        }
        default: {
            throw new IllegalArgumentException(
                    "query unknown match for uri: " + uri);
        }
        }

        if (!selectionValid) {
            throw new IllegalArgumentException("selection not supported");
        }
        if (!sortOrderValid) {
            throw new IllegalArgumentException("sortOrder not supported");
        }

        SQLiteDatabase db = itsDb.getDb();
        Cursor c = qb.query(db, projection, selection, selectionArgs,
                            null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(),
                             PasswdSafeContract.CONTENT_URI);
        return c;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[])
     */
    @Override
    public int update(Uri uri,
                      ContentValues values,
                      String selection,
                      String[] selectionArgs)
    {
        switch (MATCHER.match(uri)) {
        case MATCH_PROVIDER: {
            PasswdSafeUtil.dbginfo(TAG, "Update provider: %s", uri);
            Long id = Long.valueOf(uri.getPathSegments().get(1));
            SQLiteDatabase db = itsDb.getDb();
            try {
                db.beginTransaction();
                SyncDb.DbProvider provider = SyncDb.getProvider(id, db);
                if (provider == null) {
                    return 0;
                }

                Integer syncFreq = values.getAsInteger(
                        PasswdSafeContract.Providers.COL_SYNC_FREQ);
                if ((syncFreq != null) && (provider.itsSyncFreq != syncFreq)) {
                    PasswdSafeUtil.dbginfo(TAG, "Update sync freq %d",
                                           syncFreq);
                    GDriveSyncer.updateSyncFreq(provider, syncFreq,
                                                db, getContext());
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                String msg = "Error deleting provier: " + uri;
                Log.e(TAG, msg, e);
                throw new RuntimeException(msg, e);
            } finally {
                db.endTransaction();
            }
            return 1;
        }
        case MATCH_PROVIDER_FILE: {
            long id = Long.valueOf(uri.getPathSegments().get(3));
            String updateUri =
                    values.getAsString(PasswdSafeContract.Files.COL_FILE);
            if (updateUri == null) {
                throw new IllegalArgumentException("File missing");
            }

            SyncDb.DbFile file = itsDb.getFile(id);
            if (file == null) {
                throw new IllegalArgumentException(
                        "File not found: " + uri);
            }

            String localFileName = (file.itsLocalFile != null) ?
                file.itsLocalFile : GDriveSyncer.getLocalFileName(id);
            File tmpFile = null;
            try {
                Context ctx = getContext();
                tmpFile = File.createTempFile("passwd", ".tmp",
                                              ctx.getFilesDir());

                ContentResolver cr = getContext().getContentResolver();
                writeToFile(cr.openInputStream(Uri.parse(updateUri)),
                            updateUri, tmpFile);

                SQLiteDatabase db = itsDb.getDb();
                try {
                    db.beginTransaction();

                    File localFile = ctx.getFileStreamPath(localFileName);
                    if (!tmpFile.renameTo(localFile)) {
                        throw new IOException(
                                 "Error renaming " + tmpFile.getAbsolutePath() +
                                 " to " + localFile.getAbsolutePath());
                    }
                    tmpFile = null;
                    SyncDb.updateLocalFile(file.itsId, localFileName,
                                           file.itsLocalTitle,
                                           localFile.lastModified(), db);
                    db.setTransactionSuccessful();

                    cr.notifyChange(uri, null);
                } finally {
                    db.endTransaction();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error updating " + uri, e);
                return 0;
            } finally {
                if (tmpFile != null) {
                    if (!tmpFile.delete()) {
                        Log.e(TAG, "Error deleting tmp file " +
                                tmpFile.getAbsolutePath());
                    }
                }
            }
            return 1;
        }
        default: {
            throw new IllegalArgumentException(
                    "Update not supported for uri: " + uri);
        }
        }
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#openFile(android.net.Uri, java.lang.String)
     */
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException
    {
        if (!mode.equals("r")) {
            throw new IllegalArgumentException("Invalid mode: " + mode);
        }

        switch (MATCHER.match(uri)) {
        case MATCH_PROVIDER_FILE: {
            long id = Long.valueOf(uri.getPathSegments().get(3));
            SyncDb.DbFile file = itsDb.getFile(id);
            if ((file == null) || (file.itsLocalFile == null)) {
                throw new FileNotFoundException(uri.toString());
            }
            File localFile = getContext().getFileStreamPath(file.itsLocalFile);
            PasswdSafeUtil.dbginfo(TAG, "openFile uri %s, file %s",
                                   uri, localFile);
            ParcelFileDescriptor fd = ParcelFileDescriptor.open(
                    localFile, ParcelFileDescriptor.MODE_READ_ONLY);
            return fd;
        }
        default: {
            return super.openFile(uri, mode);
        }
        }
    }


    /** Write a source stream to a file.  The source is closed. */
    private static void writeToFile(InputStream src, String srcName, File file)
        throws IOException
    {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new BufferedInputStream(src);
            FileOutputStream fos = new FileOutputStream(file);
            os = new BufferedOutputStream(fos);
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
            }
            fos.getFD().sync();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing " + srcName, e);
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing " + file.getAbsolutePath(), e);
                }
            }
        }
    }
}
