/*
 * Copyright (©) 2013-2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.dropbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountInfo;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;

/** The Syncer class encapsulates a sync operation */
public class DropboxSyncer extends AbstractProviderSyncer<DbxFileSystem>
{
    private static final String TAG = "DropboxSyncer";

    /** Constructor */
    public DropboxSyncer(DbxFileSystem fs, DbProvider provider,
                         SQLiteDatabase db, SyncLogRecord logrec, Context ctx)
    {
        super(fs, provider, db, logrec, ctx, TAG);
    }



    /** Perform a sync of the files */
    @Override
    protected final List<AbstractSyncOper<DbxFileSystem>> performSync()
            throws DbxException, SQLException
    {
        DbxAccount acct = itsProviderClient.getAccount();
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

        List<DbFile> dbfiles = SyncDb.getFiles(itsProvider.itsId, itsDb);
        for (DbFile dbfile: dbfiles) {
            if ((dbfile.itsRemoteId == null) ||
                    (dbfile.itsLocalChange == DbFile.FileChange.ADDED)) {
                continue;
            }
            DbxPath dbpath = new DbxPath(dbfile.itsRemoteId);
            DbxFileInfo dbpathinfo = dbxfiles.get(dbpath);
            if (dbpathinfo != null) {
                checkRemoteFileChange(dbfile, dbpath, dbpathinfo);
                dbxfiles.remove(dbpath);
            } else {
                PasswdSafeUtil.dbginfo(TAG, "performSync remove remote %s",
                                       dbfile.itsRemoteId);
                SyncDb.updateRemoteFileDeleted(dbfile.itsId, itsDb);
            }
        }

        for (Map.Entry<DbxPath, DbxFileInfo> entry: dbxfiles.entrySet()) {
            DbxPath dbpath = entry.getKey();
            String fileId = dbpath.toString();
            PasswdSafeUtil.dbginfo(TAG, "performSync add remote %s", fileId);
            SyncDb.addRemoteFile(itsProvider.itsId, fileId,
                                 dbpath.getName(),
                                 dbpath.getParent().toString(),
                                 entry.getValue().modifiedTime.getTime(),
                                 null, itsDb);
        }

        List<AbstractSyncOper<DbxFileSystem>> opers =
                new ArrayList<AbstractSyncOper<DbxFileSystem>>();
        dbfiles = SyncDb.getFiles(itsProvider.itsId, itsDb);
        for (DbFile dbfile: dbfiles) {
            resolveSyncOper(dbfile, opers);
        }
        return opers;
    }


    /** Check for a remote file change and update */
    private final void checkRemoteFileChange(DbFile dbfile,
                                             DbxPath dbpath,
                                             DbxFileInfo dbpathinfo)
    {
        String remTitle = dbpath.getName();
        String remFolder = dbpath.getParent().toString();
        long remModDate = dbpathinfo.modifiedTime.getTime();
        String remHash = null;
        boolean changed = true;
        do {
            if (!TextUtils.equals(dbfile.itsRemoteTitle, remTitle) ||
                    !TextUtils.equals(dbfile.itsRemoteFolder, remFolder) ||
                    (dbfile.itsRemoteModDate != remModDate) ||
                    !TextUtils.equals(dbfile.itsRemoteHash, remHash) ||
                    TextUtils.isEmpty(dbfile.itsLocalFile)) {
                break;
            }

            java.io.File localFile =
                    itsContext.getFileStreamPath(dbfile.itsLocalFile);
            if (!localFile.exists()) {
                break;
            }

            changed = false;
        } while(false);

        if (!changed) {
            return;
        }

        PasswdSafeUtil.dbginfo(TAG, "performSync update remote %s", dbfile);
        SyncDb.updateRemoteFile(dbfile.itsId, dbfile.itsRemoteId,
                                remTitle, remFolder, remModDate, remHash,
                                itsDb);
        switch (dbfile.itsRemoteChange) {
        case NO_CHANGE:
        case REMOVED: {
            SyncDb.updateRemoteFileChange(dbfile.itsId,
                                          DbFile.FileChange.MODIFIED, itsDb);
            break;
        }
        case ADDED:
        case MODIFIED: {
            break;
        }
        }
    }


    /** Resolve the sync operations for a file */
    private final void resolveSyncOper(
            DbFile dbfile,
            List<AbstractSyncOper<DbxFileSystem>> opers)
            throws SQLException
    {
        if ((dbfile.itsLocalChange != DbFile.FileChange.NO_CHANGE) ||
                (dbfile.itsRemoteChange != DbFile.FileChange.NO_CHANGE)) {
            PasswdSafeUtil.dbginfo(TAG, "resolveSyncOper %s", dbfile);
        }

        switch (dbfile.itsLocalChange) {
        case ADDED:
        case MODIFIED: {
            // Adds and modifications handled immediately in the provider
            break;
        }
        case NO_CHANGE: {
            switch (dbfile.itsRemoteChange) {
            case ADDED:
            case MODIFIED: {
                opers.add(new DropboxRemoteToLocalOper(dbfile));
                break;
            }
            case NO_CHANGE: {
                // Nothing
                break;
            }
            case REMOVED: {
                opers.add(new DropboxRmFileOper(dbfile));
                break;
            }
            }
            break;
        }
        case REMOVED: {
            switch (dbfile.itsRemoteChange) {
            case ADDED:
            case MODIFIED: {
                logConflictFile(dbfile, false);
                DbFile newRemfile = splitRemoteToNewFile(dbfile);
                DbFile updatedLocalFile = SyncDb.getFile(dbfile.itsId, itsDb);

                opers.add(new DropboxRemoteToLocalOper(newRemfile));
                opers.add(new DropboxRmFileOper(updatedLocalFile));
                break;
            }
            case NO_CHANGE:
            case REMOVED: {
                opers.add(new DropboxRmFileOper(dbfile));
                break;
            }
            }
            break;
        }
        }
    }


    /** Get all of the files under the path */
    private final void getDirFiles(DbxPath path,
                                   Map<DbxPath, DbxFileInfo> files)
            throws DbxException
    {
        List<DbxFileInfo> children = itsProviderClient.listFolder(path);
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