/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.util.Log;

import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.lib.Utils;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.Provider;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncHelper;

/**
 *  The PasswdSafeProvider class is a content provider for synced
 *  password files
 */
public class PasswdSafeProvider extends ContentProvider
{
    private static final String TAG = "PasswdSafeProvider";

    private static final HashMap<String, String> PROVIDERS_MAP;
    private static final HashMap<String, String> FILES_MAP;
    private static final HashMap<String, String> REMOTE_FILES_MAP;
    private static final HashMap<String, String> SYNC_LOGS_MAP;

    @SuppressWarnings("FieldCanBeLocal")
    private OnAccountsUpdateListener itsListener;

    static {
        PROVIDERS_MAP = new HashMap<>();
        PROVIDERS_MAP.put(PasswdSafeContract.Providers._ID,
                          SyncDb.DB_COL_PROVIDERS_ID);
        PROVIDERS_MAP.put(PasswdSafeContract.Providers.COL_TYPE,
                          SyncDb.DB_COL_PROVIDERS_TYPE);
        PROVIDERS_MAP.put(PasswdSafeContract.Providers.COL_ACCT,
                          SyncDb.DB_COL_PROVIDERS_ACCT);
        PROVIDERS_MAP.put(PasswdSafeContract.Providers.COL_SYNC_FREQ,
                          SyncDb.DB_COL_PROVIDERS_SYNC_FREQ);
        PROVIDERS_MAP.put(PasswdSafeContract.Providers.COL_DISPLAY_NAME,
                          SyncDb.DB_COL_PROVIDERS_DISPLAY_NAME);

        FILES_MAP = new HashMap<>();
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
        FILES_MAP.put(PasswdSafeContract.Files.COL_FOLDER,
                      SyncDb.DB_COL_FILES_LOCAL_FOLDER + " AS " +
                              PasswdSafeContract.Files.COL_FOLDER);

        REMOTE_FILES_MAP = new HashMap<>();
        REMOTE_FILES_MAP.put(PasswdSafeContract.RemoteFiles._ID,
                             SyncDb.DB_COL_FILES_ID);
        REMOTE_FILES_MAP.put(PasswdSafeContract.RemoteFiles.COL_REMOTE_ID,
                             SyncDb.DB_COL_FILES_REMOTE_ID + " AS " +
                                 PasswdSafeContract.RemoteFiles.COL_REMOTE_ID);

        SYNC_LOGS_MAP = new HashMap<>();
        SYNC_LOGS_MAP.put(PasswdSafeContract.SyncLogs._ID,
                          SyncDb.DB_COL_SYNC_LOGS_ID);
        SYNC_LOGS_MAP.put(PasswdSafeContract.SyncLogs.COL_ACCT,
                          SyncDb.DB_COL_SYNC_LOGS_ACCT);
        SYNC_LOGS_MAP.put(PasswdSafeContract.SyncLogs.COL_START,
                          SyncDb.DB_COL_SYNC_LOGS_START);
        SYNC_LOGS_MAP.put(PasswdSafeContract.SyncLogs.COL_END,
                          SyncDb.DB_COL_SYNC_LOGS_END);
        SYNC_LOGS_MAP.put(PasswdSafeContract.SyncLogs.COL_FLAGS,
                          SyncDb.DB_COL_SYNC_LOGS_FLAGS);
        SYNC_LOGS_MAP.put(PasswdSafeContract.SyncLogs.COL_LOG,
                          SyncDb.DB_COL_SYNC_LOGS_LOG);
        SYNC_LOGS_MAP.put(PasswdSafeContract.SyncLogs.COL_STACK,
                          SyncDb.DB_COL_SYNC_LOGS_STACK);
    }


    /* (non-Javadoc)
     * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
     */
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs)
    {
        if (selection != null) {
            throw new IllegalArgumentException("selection not supported");
        }
        if (selectionArgs != null) {
            throw new IllegalArgumentException("selectionArgs not supported");
        }

        switch (PasswdSafeContract.MATCHER.match(uri)) {
        case PasswdSafeContract.MATCH_PROVIDER: {
            PasswdSafeUtil.dbginfo(TAG, "Delete provider: %s", uri);
            long id = PasswdSafeContract.Providers.getId(uri);
            SyncDb syncDb = SyncDb.acquire();
            try {
                SQLiteDatabase db = syncDb.beginTransaction();
                DbProvider provider = SyncDb.getProvider(id, db);
                if (provider == null) {
                    return 0;
                }

                ProviderSyncer.deleteProvider(provider, db, getContext());
                db.setTransactionSuccessful();
                return 1;
            } catch (Exception e) {
                String msg = "Error deleting provier: " + uri;
                Log.e(TAG, msg, e);
                throw new RuntimeException(msg, e);
            } finally {
                syncDb.endTransactionAndRelease();
            }
        }
        case PasswdSafeContract.MATCH_PROVIDER_FILE: {
            PasswdSafeUtil.dbginfo(TAG, "Delete file: %s", uri);
            SyncDb syncDb = SyncDb.acquire();
            try {
                SQLiteDatabase db = syncDb.beginTransaction();
                long providerId = PasswdSafeContract.Providers.getId(uri);
                long id = PasswdSafeContract.Files.getId(uri);
                DbFile file = SyncDb.getFile(id, db);
                if (file == null) {
                    return 0;
                }

                DbProvider dbProvider = SyncDb.getProvider(providerId,
                                                                  db);
                Provider provider =
                        ProviderFactory.getProvider(dbProvider.itsType,
                                                    getContext());
                provider.deleteLocalFile(file, db);

                db.setTransactionSuccessful();

                notifyFileChanges(providerId, id);
                return 1;
            } catch (Exception e) {
                String msg = "Error deleting file: " + uri;
                Log.e(TAG, msg, e);
                throw new RuntimeException(msg, e);
            } finally {
                syncDb.endTransactionAndRelease();
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
    public String getType(@NonNull Uri uri)
    {
        switch (PasswdSafeContract.MATCHER.match(uri)) {
        case PasswdSafeContract.MATCH_PROVIDERS: {
            return PasswdSafeContract.Providers.CONTENT_TYPE;
        }
        case PasswdSafeContract.MATCH_PROVIDER: {
            return PasswdSafeContract.Providers.CONTENT_ITEM_TYPE;
        }
        case PasswdSafeContract.MATCH_PROVIDER_FILES: {
            return PasswdSafeContract.Files.CONTENT_TYPE;
        }
        case PasswdSafeContract.MATCH_PROVIDER_FILE: {
            return PasswdSafeContract.Files.CONTENT_ITEM_TYPE;
        }
        case PasswdSafeContract.MATCH_SYNC_LOGS: {
            return PasswdSafeContract.SyncLogs.CONTENT_TYPE;
        }
        case PasswdSafeContract.MATCH_METHODS: {
            return PasswdSafeContract.Methods.CONTENT_TYPE;
        }
        case PasswdSafeContract.MATCH_PROVIDER_REMOTE_FILES: {
            return PasswdSafeContract.RemoteFiles.CONTENT_TYPE;
        }
        case PasswdSafeContract.MATCH_PROVIDER_REMOTE_FILE: {
            return PasswdSafeContract.RemoteFiles.CONTENT_ITEM_TYPE;
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
    public Uri insert(@NonNull Uri uri, ContentValues values)
    {
        switch (PasswdSafeContract.MATCHER.match(uri)) {
        case PasswdSafeContract.MATCH_PROVIDERS: {
            String acct = values.getAsString(
                    PasswdSafeContract.Providers.COL_ACCT);
            if (acct == null) {
                throw new IllegalArgumentException("No acct for provider");
            }
            ProviderType type = ProviderType.fromString(
                    values.getAsString(PasswdSafeContract.Providers.COL_TYPE));
            if (type == null) {
                throw new IllegalArgumentException("Invalid type for provider");
            }
            PasswdSafeUtil.dbginfo(TAG, "Insert provider: %s", acct);
            SyncDb syncDb = SyncDb.acquire();
            try {
                SQLiteDatabase db = syncDb.beginTransaction();
                long id = ProviderSyncer.addProvider(acct, type, db,
                                                     getContext());
                db.setTransactionSuccessful();

                return ContentUris.withAppendedId(
                        PasswdSafeContract.Providers.CONTENT_URI, id);
            } catch (Exception e) {
                String msg = "Error adding provider: " + acct;
                Log.e(TAG, msg, e);
                throw new RuntimeException(msg, e);
            } finally {
                syncDb.endTransactionAndRelease();
            }
        }
        case PasswdSafeContract.MATCH_PROVIDER_FILES: {
            String title = values.getAsString(
                    PasswdSafeContract.Files.COL_TITLE);
            if (title == null) {
                throw new IllegalArgumentException("No title for file");
            }
            PasswdSafeUtil.dbginfo(TAG, "Insert file \"%s\" for %s",
                                   title, uri);
            SyncDb syncDb = SyncDb.acquire();
            try {
                SQLiteDatabase db = syncDb.beginTransaction();
                long providerId = PasswdSafeContract.Providers.getId(uri);
                DbProvider dbProvider = SyncDb.getProvider(providerId,
                                                                  db);
                if (dbProvider == null) {
                    throw new Exception("No provider for " + providerId);
                }

                Provider provider =
                        ProviderFactory.getProvider(dbProvider.itsType,
                                                    getContext());
                long id = provider.insertLocalFile(providerId, title, db);
                db.setTransactionSuccessful();

                notifyFileChanges(providerId, -1);
                return ContentUris.withAppendedId(uri, id);
            } catch (Exception e) {
                String msg = "Error adding file: " + title;
                Log.e(TAG, msg, e);
                throw new RuntimeException(msg, e);
            } finally {
                syncDb.endTransactionAndRelease();
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
        //noinspection ConstantConditions
        SyncDb.initializeDb(getContext().getApplicationContext());
        itsListener = new OnAccountsUpdateListener()
        {
            @Override
            public void onAccountsUpdated(Account[] accounts)
            {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params)
                    {
                        SyncDb syncDb = SyncDb.acquire();
                        try {
                            SQLiteDatabase db = syncDb.beginTransaction();
                            ProviderSyncer.validateAccounts(db, getContext());
                            db.setTransactionSuccessful();
                        } catch (Exception e) {
                            Log.e(TAG, "Error validating accounts", e);
                        } finally {
                            syncDb.endTransactionAndRelease();
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
    public Cursor query(@NonNull Uri uri,
                        String[] projection,
                        String selection,
                        String[] selectionArgs,
                        String sortOrder)
    {
        PasswdSafeUtil.dbginfo(TAG, "query uri: %s", uri);

        boolean selectionValid = (selection == null);
        boolean selectionArgsValid = (selectionArgs == null);
        boolean sortOrderValid = (sortOrder == null);

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (PasswdSafeContract.MATCHER.match(uri)) {
        case PasswdSafeContract.MATCH_PROVIDERS: {
            qb.setTables(SyncDb.DB_TABLE_PROVIDERS);
            qb.setProjectionMap(PROVIDERS_MAP);

            if (PasswdSafeContract.Providers.PROVIDER_SORT_ORDER.equals(
                    sortOrder)) {
                sortOrderValid = true;
            }
            break;
        }
        case PasswdSafeContract.MATCH_PROVIDER: {
            qb.setTables(SyncDb.DB_TABLE_PROVIDERS);
            qb.setProjectionMap(PROVIDERS_MAP);
            selection = SyncDb.DB_MATCH_PROVIDERS_ID;
            selectionArgs =
                    new String[] { PasswdSafeContract.Providers.getIdStr(uri) };
            break;
        }
        case PasswdSafeContract.MATCH_PROVIDER_FILES: {
            qb.setTables(SyncDb.DB_TABLE_FILES);
            qb.setProjectionMap(FILES_MAP);

            StringBuilder fullSelection =
                    new StringBuilder(SyncDb.DB_MATCH_FILES_PROVIDER_ID);
            if (PasswdSafeContract.Files.NOT_DELETED_SELECTION.equals(
                        selection)) {
                selectionValid = true;
                fullSelection.append(" and ");
                fullSelection.append(selection);
            }
            selection = fullSelection.toString();

            selectionArgs =
                    new String[] { PasswdSafeContract.Providers.getIdStr(uri) };
            if (PasswdSafeContract.Files.TITLE_SORT_ORDER.equals(sortOrder)) {
                sortOrderValid = true;
            }
            break;
        }
        case PasswdSafeContract.MATCH_PROVIDER_FILE: {
            qb.setTables(SyncDb.DB_TABLE_FILES);
            qb.setProjectionMap(FILES_MAP);
            selection = SyncDb.DB_MATCH_FILES_ID;
            selectionArgs =
                    new String[] { PasswdSafeContract.Files.getIdStr(uri) };
            break;
        }
        case PasswdSafeContract.MATCH_SYNC_LOGS: {
            qb.setTables(SyncDb.DB_TABLE_SYNC_LOGS);
            qb.setProjectionMap(SYNC_LOGS_MAP);
            if (PasswdSafeContract.SyncLogs.START_SORT_ORDER.equals(sortOrder)) {
                sortOrderValid = true;
            }
            if (PasswdSafeContract.SyncLogs.DEFAULT_SELECTION.equals(
                        selection)) {
                selectionValid = true;
            }
            break;
        }
        case PasswdSafeContract.MATCH_METHODS: {
            try {
                doMethod(selectionArgs);
                return null;
            } catch (Exception e) {
                String msg = "Error executing method";
                Log.e(TAG, msg, e);
                throw new RuntimeException(msg, e);
            }
        }
        case PasswdSafeContract.MATCH_PROVIDER_REMOTE_FILES: {
            qb.setTables(SyncDb.DB_TABLE_FILES);
            qb.setProjectionMap(REMOTE_FILES_MAP);

            StringBuilder fullSelection =
                    new StringBuilder(SyncDb.DB_MATCH_FILES_PROVIDER_ID);
            if (PasswdSafeContract.RemoteFiles.NOT_DELETED_SELECTION.equals(
                        selection)) {
                selectionValid = true;
                fullSelection.append(" and ");
                fullSelection.append(selection);
            }
            selection = fullSelection.toString();

            selectionArgs =
                    new String[] { PasswdSafeContract.Providers.getIdStr(uri) };
            break;
        }
        case PasswdSafeContract.MATCH_PROVIDER_REMOTE_FILE: {
            qb.setTables(SyncDb.DB_TABLE_FILES);
            qb.setProjectionMap(REMOTE_FILES_MAP);
            selection = SyncDb.DB_MATCH_FILES_ID;
            selectionArgs =
                new String[] { PasswdSafeContract.RemoteFiles.getIdStr(uri) };
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
        if (!selectionArgsValid) {
            throw new IllegalArgumentException("selectionArgs not supported");
        }
        if (!sortOrderValid) {
            throw new IllegalArgumentException("sortOrder not supported");
        }

        SyncDb syncDb = SyncDb.acquire();
        try {
            SQLiteDatabase db = syncDb.getDb();
            Cursor c = qb.query(db, projection, selection, selectionArgs,
                                null, null, sortOrder);
            Context ctx = getContext();
            if ((c != null) && (ctx != null)) {
                c.setNotificationUri(ctx.getContentResolver(),
                                     PasswdSafeContract.CONTENT_URI);
            }
            return c;
        } finally {
            syncDb.release();
        }
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
        switch (PasswdSafeContract.MATCHER.match(uri)) {
        case PasswdSafeContract.MATCH_PROVIDER: {
            PasswdSafeUtil.dbginfo(TAG, "Update provider: %s", uri);
            long id = PasswdSafeContract.Providers.getId(uri);
            SyncDb syncDb = SyncDb.acquire();
            try {
                SQLiteDatabase db = syncDb.beginTransaction();
                DbProvider provider = SyncDb.getProvider(id, db);
                if (provider == null) {
                    return 0;
                }

                Integer syncFreq = values.getAsInteger(
                        PasswdSafeContract.Providers.COL_SYNC_FREQ);
                if ((syncFreq != null) && (provider.itsSyncFreq != syncFreq)) {
                    PasswdSafeUtil.dbginfo(TAG, "Update sync freq %d",
                                           syncFreq);
                    ProviderSyncer.updateSyncFreq(provider, syncFreq,
                                                db, getContext());
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                String msg = "Error deleting provier: " + uri;
                Log.e(TAG, msg, e);
                throw new RuntimeException(msg, e);
            } finally {
                syncDb.endTransactionAndRelease();
            }
            return 1;
        }
        case PasswdSafeContract.MATCH_PROVIDER_FILE: {
            long providerId = PasswdSafeContract.Providers.getId(uri);
            long id = PasswdSafeContract.Files.getId(uri);
            String updateUri =
                    values.getAsString(PasswdSafeContract.Files.COL_FILE);
            if (updateUri == null) {
                throw new IllegalArgumentException("File missing");
            }

            File tmpFile = null;
            SyncDb syncDb = SyncDb.acquire();
            try {
                SQLiteDatabase db = syncDb.beginTransaction();
                Context ctx = getContext();
                if (ctx == null) {
                    throw new NullPointerException("ctx");
                }

                DbFile file = SyncDb.getFile(id, db);
                if (file == null) {
                    throw new IllegalArgumentException(
                            "File not found: " + uri);
                }

                String localFileName = (file.itsLocalFile != null) ?
                    file.itsLocalFile : SyncHelper.getLocalFileName(id);
                tmpFile = File.createTempFile("passwd", ".tmp",
                                              ctx.getFilesDir());

                ContentResolver cr = ctx.getContentResolver();
                writeToFile(cr.openInputStream(Uri.parse(updateUri)),
                            tmpFile);

                File localFile = ctx.getFileStreamPath(localFileName);
                if (!tmpFile.renameTo(localFile)) {
                    throw new IOException(
                             "Error renaming " + tmpFile.getAbsolutePath() +
                             " to " + localFile.getAbsolutePath());
                }
                tmpFile = null;

                DbProvider dbProvider = SyncDb.getProvider(providerId,
                                                                  db);
                Provider provider =
                        ProviderFactory.getProvider(dbProvider.itsType,
                                                    getContext());
                provider.updateLocalFile(file, localFileName, localFile, db);
                db.setTransactionSuccessful();

                notifyFileChanges(providerId, id);
            } catch (Exception e) {
                Log.e(TAG, "Error updating " + uri, e);
                return 0;
            } finally {
                syncDb.endTransactionAndRelease();
                if ((tmpFile != null) && !tmpFile.delete()) {
                    Log.e(TAG, "Error deleting tmp file " +
                            tmpFile.getAbsolutePath());
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
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode)
            throws FileNotFoundException
    {
        if (!mode.equals("r")) {
            throw new IllegalArgumentException("Invalid mode: " + mode);
        }

        switch (PasswdSafeContract.MATCHER.match(uri)) {
        case PasswdSafeContract.MATCH_PROVIDER_FILE: {
            long id = PasswdSafeContract.Files.getId(uri);
            DbFile file;
            SyncDb syncDb = SyncDb.acquire();
            try {
                SQLiteDatabase db = syncDb.beginTransaction();
                file = SyncDb.getFile(id, db);
                db.setTransactionSuccessful();
            } finally {
                syncDb.endTransactionAndRelease();
            }
            Context ctx = getContext();
            if ((file == null) || (file.itsLocalFile == null) ||
                (ctx == null)) {
                throw new FileNotFoundException(uri.toString());
            }
            File localFile = ctx.getFileStreamPath(file.itsLocalFile);
            PasswdSafeUtil.dbginfo(TAG, "openFile uri %s, file %s",
                                   uri, localFile);
            return ParcelFileDescriptor.open(
                    localFile, ParcelFileDescriptor.MODE_READ_ONLY);
        }
        default: {
            return super.openFile(uri, mode);
        }
        }
    }


    /** Execute a method */
    private void doMethod(String[] args)
    {
        if (args.length < 1) {
            throw new IllegalArgumentException("No method args");
        }

        String name = args[0];
        if (name.equals(PasswdSafeContract.Methods.METHOD_SYNC)) {
            if (args.length > 2) {
                throw new IllegalArgumentException("Invalid number of args");
            }

            Long id = null;
            if (args.length > 1) {
                Uri providerUri = Uri.parse(args[1]);
                int match = PasswdSafeContract.MATCHER.match(providerUri);
                if (match != PasswdSafeContract.MATCH_PROVIDER) {
                    throw new IllegalArgumentException(
                            "Invalid provider URI: " + providerUri);
                }

                id = PasswdSafeContract.Providers.getId(providerUri);
            }

            SyncDb syncDb = SyncDb.acquire();
            List<DbProvider> providers;
            try {
                SQLiteDatabase db = syncDb.beginTransaction();
                if (id == null) {
                    providers = SyncDb.getProviders(db);
                } else {
                    DbProvider provider = SyncDb.getProvider(id, db);
                    if (provider == null) {
                        return;
                    }
                    providers = Collections.singletonList(provider);
                }
                db.setTransactionSuccessful();
            } finally {
                syncDb.endTransactionAndRelease();
            }

            for (DbProvider provider: providers) {
                Provider providerImpl = ProviderFactory.getProvider(
                        provider.itsType, getContext());
                providerImpl.requestSync(true);
            }
        } else {
            throw new IllegalArgumentException("Unknown method: " + name);
        }
    }


    /** Notify both files and remote files listeners for changes */
    private void notifyFileChanges(long providerId, long fileId)
    {
        Context ctx = getContext();
        if (ctx == null) {
            return;
        }
        ContentResolver cr = ctx.getContentResolver();

        Uri providerUri = ContentUris.withAppendedId(
                PasswdSafeContract.Providers.CONTENT_URI, providerId);

        Uri.Builder builder = providerUri.buildUpon();
        builder.appendPath(PasswdSafeContract.Files.TABLE);
        if (fileId >= 0) {
            ContentUris.appendId(builder, fileId);
        }
        cr.notifyChange(builder.build(), null);

        builder = providerUri.buildUpon();
        builder.appendPath(PasswdSafeContract.RemoteFiles.TABLE);
        if (fileId >= 0) {
            ContentUris.appendId(builder, fileId);
        }
        cr.notifyChange(builder.build(), null);
    }


    /** Write a source stream to a file.  The source is closed. */
    private static void writeToFile(InputStream src, File file)
        throws IOException
    {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new BufferedInputStream(src);
            FileOutputStream fos = new FileOutputStream(file);
            os = new BufferedOutputStream(fos);
            Utils.copyStream(is, os);
            fos.getFD().sync();
        } finally {
            Utils.closeStreams(is, os);
        }
    }
}
