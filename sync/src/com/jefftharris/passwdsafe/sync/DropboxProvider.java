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
import java.util.Map;
import java.util.TreeMap;

import android.accounts.Account;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.SyncDb.DbProvider;

/**
 *  The DropboxProvider class encapsulates Dropbox
 */
public class DropboxProvider implements Provider
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

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.Provider#cleanupOnDelete(java.lang.String)
     */
    @Override
    public void cleanupOnDelete(String acctName)
    {
        // TODO: only one Dropbox provider allowed
        getSyncApp().unlinkDropbox();
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
        DbxFileSystem fs = getSyncApp().getDropboxFs();
        if (fs == null) {
            PasswdSafeUtil.dbginfo(TAG, "sync: no fs");
            return;
        }

        if (manual) {
            fs.syncNowAndWait();
        }

        new Syncer(fs, provider, db, logrec, itsContext).sync();
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
        private final SyncDb.DbProvider itsProvider;
        private final SQLiteDatabase itsDb;
        private final SyncLogRecord itsLogrec;
        private final Context itsContext;

        /** Constructor */
        public Syncer(DbxFileSystem fs,
                      SyncDb.DbProvider provider,
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
            TreeMap<DbxPath, DbxFileInfo> dbxfiles =
                    new TreeMap<DbxPath, DbxFileInfo>();
            getDirFiles(DbxPath.ROOT, dbxfiles);

            List<SyncDb.DbFile> dbfiles = SyncDb.getFiles(itsProvider.itsId,
                                                          itsDb);
            for (SyncDb.DbFile dbfile: dbfiles) {
                DbxPath dbpath = new DbxPath(dbfile.itsRemoteId);
                DbxFileInfo dbpathinfo = dbxfiles.get(dbpath);
                if (dbpathinfo != null) {
                    PasswdSafeUtil.dbginfo(TAG,
                                           "performSync update remote %s",
                                           dbfile.itsRemoteId);
                    SyncDb.updateRemoteFile(
                            dbfile.itsId, dbfile.itsRemoteId,
                            dbpath.getName(),
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
                String fileId = entry.getKey().toString();
                PasswdSafeUtil.dbginfo(TAG, "performSync add remote %s",
                                       fileId);
                SyncDb.addRemoteFile(itsProvider.itsId, fileId,
                                     entry.getKey().getName(),
                                     entry.getValue().modifiedTime.getTime(),
                                     itsDb);
            }

            List<DropboxSyncOper> opers = new ArrayList<DropboxSyncOper>();
            dbfiles = SyncDb.getFiles(itsProvider.itsId, itsDb);
            for (SyncDb.DbFile dbfile: dbfiles) {
                if (dbfile.itsIsRemoteDeleted || dbfile.itsIsLocalDeleted) {
                    opers.add(new DropboxRmFileOper(dbfile));
                } else {
                    opers.add(new DropboxRemoteToLocalOper(dbfile));
                }
            }
            return opers;
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
