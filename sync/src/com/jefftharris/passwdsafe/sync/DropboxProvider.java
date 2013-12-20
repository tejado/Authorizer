/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import android.accounts.Account;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountInfo;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.lib.Utils;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.Provider;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;

/**
 *  The DropboxProvider class encapsulates Dropbox
 */
public class DropboxProvider extends Provider
{
    private final Context itsContext;

    private static final String TAG = "DropboxProvider";

    /** Constructor */
    public DropboxProvider(Context ctx)
    {
        itsContext = ctx;
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.Provider#getAccount(java.lang.String)
     */
    @Override
    public Account getAccount(String acctName)
    {
        return new Account(acctName, SyncDb.DROPBOX_ACCOUNT_TYPE);
    }


    /** Check whether a provider can be added */
    @Override
    public void checkProviderAdd(SQLiteDatabase db)
            throws Exception
    {
        List<DbProvider> providers = SyncDb.getProviders(db);
        for (DbProvider provider: providers) {
            if (provider.itsType == ProviderType.DROPBOX) {
                throw new Exception("Only one Dropbox account allowed");
            }
        }
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.Provider#cleanupOnDelete(java.lang.String)
     */
    @Override
    public void cleanupOnDelete(String acctName)
    {
        SyncApp.get(itsContext).unlinkDropbox();
    }

    /** Update a provider's sync frequency */
    @Override
    public void updateSyncFreq(Account acct, int freq)
    {
        SyncApp.get(itsContext).updateDropboxSyncFreq(freq);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.Provider#sync(android.accounts.Account, com.jefftharris.passwdsafe.sync.SyncDb.DbProvider, android.database.sqlite.SQLiteDatabase, com.jefftharris.passwdsafe.sync.SyncLogRecord)
     */
    @Override
    public void sync(Account acct,
                     DbProvider provider,
                     SQLiteDatabase db,
                     boolean manual,
                     SyncLogRecord logrec) throws Exception
    {
        DbxFileSystem fs = SyncApp.get(itsContext).getDropboxFs();
        if (fs == null) {
            PasswdSafeUtil.dbginfo(TAG, "sync: no fs");
            return;
        }
        new Syncer(fs, provider, db, logrec, itsContext).sync();
    }


    /** Insert a local file */
    @Override
    public long insertLocalFile(long providerId, String title,
                                SQLiteDatabase db)
            throws Exception
    {
        long fileId = SyncDb.addLocalFile(providerId, title,
                                          System.currentTimeMillis(), db);

        DbxPath path = new DbxPath(DbxPath.ROOT, title);
        SyncDb.updateRemoteFile(fileId, path.toString(), path.getName(), null,
                                -1, db);
        return fileId;
    }


    /** Update a local file */
    @Override
    public synchronized void updateLocalFile(DbFile file,
                                             String localFileName,
                                             java.io.File localFile,
                                             SQLiteDatabase db)
            throws Exception
    {
        SyncDb.updateLocalFile(file.itsId, localFileName,
                               file.itsLocalTitle, file.itsLocalFolder,
                               localFile.lastModified(), db);

        DbxPath path = new DbxPath(file.itsRemoteId);
        DbxFile dbxfile = null;
        try {
            DbxFileSystem fs = getSyncApp().getDropboxFs();
            if (fs.exists(path)) {
                dbxfile = fs.open(path);
            } else {
                dbxfile = fs.create(path);
            }

            InputStream is = null;
            OutputStream os = null;
            try {
                is = new BufferedInputStream(new FileInputStream(localFile));
                os = new BufferedOutputStream(dbxfile.getWriteStream());
                Utils.copyStream(is, os);
            } finally {
                Utils.closeStreams(is, os);
            }
        } finally {
            if (dbxfile != null) {
                dbxfile.close();
            }
        }
    }


    /** Delete a local file */
    @Override
    public void deleteLocalFile(DbFile file, SQLiteDatabase db)
            throws Exception
    {
        SyncDb.updateLocalFileDeleted(file.itsId, db);

        DbxFileSystem fs = getSyncApp().getDropboxFs();
        if (fs == null) {
            PasswdSafeUtil.dbginfo(TAG, "deleteLocalFile: no fs");
            return;
        }

        if (file.itsIsRemoteDeleted || (file.itsRemoteId == null)) {
            return;
        }

        DbxPath path = new DbxPath(file.itsRemoteId);
        if (fs.exists(path)) {
            fs.delete(path);
        }
    }


    /** Get the SyncApp */
    private final SyncApp getSyncApp()
    {
        return (SyncApp)itsContext.getApplicationContext();
    }


    /** The Syncer class encapsulates a sync operation */
    private static class Syncer
    {
        private final DbxFileSystem itsFs;
        private final DbProvider itsProvider;
        private final SQLiteDatabase itsDb;
        private final SyncLogRecord itsLogrec;
        private final Context itsContext;

        /** Constructor */
        public Syncer(DbxFileSystem fs,
                      DbProvider provider,
                      SQLiteDatabase db, SyncLogRecord logrec, Context ctx)
        {
            itsFs = fs;
            itsProvider = provider;
            itsDb = db;
            itsLogrec = logrec;
            itsContext = ctx;
        }


        /** Sync the provider */
        public final void sync()
                throws DbxException, SQLException
        {
            itsLogrec.setFullSync(true);
            List<DropboxSyncOper> opers = null;

            try {
                itsDb.beginTransaction();
                opers = performSync();
                itsDb.setTransactionSuccessful();
            } finally {
                itsDb.endTransaction();
            }

            if (opers != null) {
                for (DropboxSyncOper oper: opers) {
                    try {
                        itsLogrec.addEntry(oper.getDescription(itsContext));
                        oper.doOper(itsFs, itsContext);
                        try {
                            itsDb.beginTransaction();
                            oper.doPostOperUpdate(itsDb, itsContext);
                            itsDb.setTransactionSuccessful();
                        } finally {
                            itsDb.endTransaction();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Sync error for file " + oper.getFile(), e);
                        itsLogrec.addFailure(e);
                    }
                }
            }

            itsContext.getContentResolver().notifyChange(
                    PasswdSafeContract.CONTENT_URI, null, false);
        }


        /** Perform a sync of the files */
        private final List<DropboxSyncOper> performSync()
                throws DbxException, SQLException
        {
            DbxAccount acct = itsFs.getAccount();
            if (acct != null) {
                DbxAccountInfo info = acct.getAccountInfo();
                if (info != null) {
                    if (!TextUtils.equals(itsProvider.itsDisplayName,
                                          info.displayName)) {
                        SyncDb.updateProviderDisplayName(itsProvider.itsId,
                                                         info.displayName,
                                                         itsDb);
                    }
                } else {
                    SyncDb.updateProviderDisplayName(itsProvider.itsId,
                                                     null, itsDb);
                }
            }

            TreeMap<DbxPath, DbxFileInfo> dbxfiles =
                    new TreeMap<DbxPath, DbxFileInfo>();
            getDirFiles(DbxPath.ROOT, dbxfiles);
            TreeMap<DbxPath, DbxFileInfo> allDbxfiles =
                    new TreeMap<DbxPath, DbxFileInfo>(dbxfiles);

            List<DbFile> dbfiles = SyncDb.getFiles(itsProvider.itsId,
                                                          itsDb);
            for (DbFile dbfile: dbfiles) {
                DbxPath dbpath = new DbxPath(dbfile.itsRemoteId);
                DbxFileInfo dbpathinfo = dbxfiles.get(dbpath);
                if (dbpathinfo != null) {
                    PasswdSafeUtil.dbginfo(TAG,
                                           "performSync update remote %s",
                                           dbfile.itsRemoteId);
                    SyncDb.updateRemoteFile(
                            dbfile.itsId, dbfile.itsRemoteId,
                            dbpath.getName(), dbpath.getParent().toString(),
                            dbpathinfo.modifiedTime.getTime(), itsDb);

                    dbxfiles.remove(dbpath);
                } else {
                    PasswdSafeUtil.dbginfo(TAG,
                                           "performSync remove remote %s",
                                           dbfile.itsRemoteId);
                    SyncDb.updateRemoteFileDeleted(dbfile.itsId, itsDb);
                }
            }

            for (Map.Entry<DbxPath, DbxFileInfo> entry: dbxfiles.entrySet()) {
                DbxPath dbpath = entry.getKey();
                String fileId = dbpath.toString();
                PasswdSafeUtil.dbginfo(TAG, "performSync add remote %s",
                                       fileId);
                SyncDb.addRemoteFile(itsProvider.itsId, fileId,
                                     dbpath.getName(),
                                     dbpath.getParent().toString(),
                                     entry.getValue().modifiedTime.getTime(),
                                     itsDb);
            }

            List<DropboxSyncOper> opers = new ArrayList<DropboxSyncOper>();
            dbfiles = SyncDb.getFiles(itsProvider.itsId, itsDb);
            for (DbFile dbfile: dbfiles) {
                if (dbfile.itsIsRemoteDeleted || dbfile.itsIsLocalDeleted) {
                    opers.add(new DropboxRmFileOper(dbfile));
                } else if (isRemoteNewer(dbfile, allDbxfiles)) {
                    opers.add(new DropboxRemoteToLocalOper(dbfile));
                }
            }
            return opers;
        }


        /** Is the remote file newer than the local */
        private final boolean isRemoteNewer(DbFile dbfile,
                                            Map<DbxPath, DbxFileInfo> dbxfiles)
        {
            if (dbfile.itsRemoteId == null) {
                return false;
            }
            if (dbfile.itsRemoteModDate != dbfile.itsLocalModDate) {
                return true;
            }
            if (!TextUtils.equals(dbfile.itsLocalFolder,
                                  dbfile.itsRemoteFolder)) {
                return true;
            }

            if (TextUtils.isEmpty(dbfile.itsLocalFile)) {
                return true;
            }
            java.io.File localFile =
                    itsContext.getFileStreamPath(dbfile.itsLocalFile);
            if (!localFile.exists()) {
                return true;
            }

            DbxPath path = new DbxPath(dbfile.itsRemoteId);
            DbxFileInfo pathinfo = dbxfiles.get(path);
            if (pathinfo == null) {
                return true;
            }

            if (pathinfo.size != localFile.length()) {
                return true;
            }

            // TODO: checksum files for changes?

            return false;
        }


        /** Get all of the files under the path */
        private final void getDirFiles(DbxPath path,
                                       Map<DbxPath, DbxFileInfo> files)
                throws DbxException
        {
            List<DbxFileInfo> children = itsFs.listFolder(path);
            for (DbxFileInfo info: children) {
                if (info.isFolder) {
                    getDirFiles(info.path, files);
                } else {
                    String filename =
                        info.path.getName().toLowerCase(Locale.getDefault());
                    if (filename.endsWith(".psafe3")) {
                        files.put(info.path, info);
                    }
                }
            }
        }
    }
}
