/*
 * Copyright (©) 2014-2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.R;

/**
 * Common implementation of the sync operation for a provider
 */
public abstract class AbstractProviderSyncer<ProviderClientT>
        extends ProviderSyncer<ProviderClientT>
{
    /** Abstraction of a remote file for operations */
    public interface ProviderRemoteFile
    {
        /** Get the file's remote identifier */
        String getRemoteId();

        /** Get the file's title */
        String getTitle();

        /** Get the file's folder */
        String getFolder();

        /** Get the file's modification time */
        long getModTime();

        /** Get the file's hash code */
        String getHash();
    }


    /** Constructor */
    public AbstractProviderSyncer(ProviderClientT providerClient,
                                  DbProvider provider, SQLiteDatabase db,
                                  SyncLogRecord logrec, Context ctx,
                                  String tag)
    {
        super(providerClient, provider, db, logrec, ctx, tag);
    }


    /** Sync the provider */
    public final void sync()
            throws Exception
    {
        itsLogrec.setFullSync(true);
        List<AbstractSyncOper<ProviderClientT>> opers = null;

        try {
            try {
                itsDb.beginTransaction();
                opers = performSync();
                itsDb.setTransactionSuccessful();
            } catch (Exception e) {
                throw updateSyncException(e);
            } finally {
                itsDb.endTransaction();
            }

            if (opers != null) {
                for (AbstractSyncOper<ProviderClientT> oper: opers) {
                    if (oper == null) {
                        continue;
                    }
                    try {
                        itsLogrec.addEntry(oper.getDescription(itsContext));
                        oper.doOper(itsProviderClient, itsContext);
                        try {
                            itsDb.beginTransaction();
                            oper.doPostOperUpdate(itsDb, itsContext);
                            itsDb.setTransactionSuccessful();
                        } finally {
                            itsDb.endTransaction();
                        }
                    } catch (Exception e) {
                        e = updateSyncException(e);
                        Log.e(itsTag, "Sync error for file " + oper.getFile(),
                              e);
                        itsLogrec.addFailure(e);
                    }
                }
            }
        } finally {
            itsContext.getContentResolver().notifyChange(
                    PasswdSafeContract.CONTENT_URI, null, false);
        }
    }


    /** Perform a sync of the files */
    protected abstract List<AbstractSyncOper<ProviderClientT>> performSync()
            throws Exception;


    /** Create an operation to sync local to remote */
    protected abstract AbstractLocalToRemoteSyncOper<ProviderClientT>
    createLocalToRemoteOper(DbFile dbfile);


    /** Create an operation to sync remote to local */
    protected abstract AbstractRemoteToLocalSyncOper<ProviderClientT>
    createRemoteToLocalOper(DbFile dbfile);


    /** Create an operation to remove a file */
    protected abstract AbstractRmSyncOper<ProviderClientT>
    createRmFileOper(DbFile dbfile);


    /** Update database files from the remote files */
    protected void updateDbFiles(Map<String, ProviderRemoteFile> remfiles)
    {
        List<DbFile> dbfiles = SyncDb.getFiles(itsProvider.itsId, itsDb);
        for (DbFile dbfile: dbfiles) {
            if ((dbfile.itsRemoteId == null) ||
                    (dbfile.itsLocalChange == DbFile.FileChange.ADDED)) {
                continue;
            }
            ProviderRemoteFile remfile = remfiles.get(dbfile.itsRemoteId);
            if (remfile != null) {
                checkRemoteFileChange(dbfile, remfile);
                remfiles.remove(dbfile.itsRemoteId);
            } else {
                PasswdSafeUtil.dbginfo(itsTag, "updateDbFiles remove remote %s",
                                       dbfile.itsRemoteId);
                SyncDb.updateRemoteFileDeleted(dbfile.itsId, itsDb);
            }
        }

        for (Map.Entry<String, ProviderRemoteFile> entry: remfiles.entrySet()) {
            String fileId = entry.getKey();
            ProviderRemoteFile remfile = entry.getValue();
            PasswdSafeUtil.dbginfo(itsTag, "updateDbFiles add remote %s",
                                   fileId);
            SyncDb.addRemoteFile(itsProvider.itsId, fileId,
                                 remfile.getTitle(), remfile.getFolder(),
                                 remfile.getModTime(), remfile.getHash(),
                                 itsDb);
        }
    }


    /** Resolve the sync operations after the database files are updated */
    protected List<AbstractSyncOper<ProviderClientT>> resolveSyncOpers()
    {
        List<AbstractSyncOper<ProviderClientT>> opers = new ArrayList<>();
        List<DbFile> dbfiles = SyncDb.getFiles(itsProvider.itsId, itsDb);
        for (DbFile dbfile: dbfiles) {
            resolveSyncOper(dbfile, opers);
        }
        return opers;
    }


    /** Update an exception thrown during syncing */
    protected Exception updateSyncException(Exception e)
    {
        return e;
    }


    /** Check for a remote file change and update */
    private void checkRemoteFileChange(DbFile dbfile, ProviderRemoteFile remfile)
    {
        String remTitle = remfile.getTitle();
        String remFolder = remfile.getFolder();
        long remModDate = remfile.getModTime();
        String remHash = remfile.getHash();
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

        PasswdSafeUtil.dbginfo(itsTag, "updateDbFiles update remote %s",
                               dbfile);
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
    private void resolveSyncOper(DbFile dbfile,
                                 List<AbstractSyncOper<ProviderClientT>> opers)
            throws SQLException
    {
        if ((dbfile.itsLocalChange != DbFile.FileChange.NO_CHANGE) ||
                (dbfile.itsRemoteChange != DbFile.FileChange.NO_CHANGE)) {
            PasswdSafeUtil.dbginfo(itsTag, "resolveSyncOper %s", dbfile);
        }

        switch (dbfile.itsLocalChange) {
        case ADDED: {
            switch (dbfile.itsRemoteChange) {
            case ADDED:
            case MODIFIED: {
                logConflictFile(dbfile, true);
                splitConflictedFile(dbfile, opers);
                break;
            }
            case NO_CHANGE: {
                opers.add(createLocalToRemoteOper(dbfile));
                break;
            }
            case REMOVED: {
                logConflictFile(dbfile, true);
                recreateRemoteRemovedFile(dbfile, opers);
                break;
            }
            }
            break;
        }
        case MODIFIED: {
            switch (dbfile.itsRemoteChange) {
            case ADDED:
            case MODIFIED: {
                logConflictFile(dbfile, true);
                splitConflictedFile(dbfile, opers);
                break;
            }
            case NO_CHANGE: {
                opers.add(createLocalToRemoteOper(dbfile));
                break;
            }
            case REMOVED: {
                logConflictFile(dbfile, true);
                recreateRemoteRemovedFile(dbfile, opers);
                break;
            }
            }
            break;
        }
        case NO_CHANGE: {
            switch (dbfile.itsRemoteChange) {
            case ADDED:
            case MODIFIED: {
                opers.add(createRemoteToLocalOper(dbfile));
                break;
            }
            case NO_CHANGE: {
                // Nothing
                break;
            }
            case REMOVED: {
                opers.add(createRmFileOper(dbfile));
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

                opers.add(createRemoteToLocalOper(newRemfile));
                opers.add(createRmFileOper(updatedLocalFile));
                break;
            }
            case NO_CHANGE:
            case REMOVED: {
                opers.add(createRmFileOper(dbfile));
                break;
            }
            }
            break;
        }
        }
    }


    /** Split the file.  A new added remote file is created with the remote id,
     * and the file is updated to resemble a new local file with the same id but
     * a different name indicating a conflict
     */
    private void splitConflictedFile
    (
            DbFile dbfile,
            List<AbstractSyncOper<ProviderClientT>> opers
    )
            throws SQLException
    {
        DbFile newRemfile = splitRemoteToNewFile(dbfile);
        DbFile updatedLocalFile = updateFileAsLocallyAdded(
                dbfile, itsContext.getString(R.string.conflicted_local_copy));

        opers.add(createRemoteToLocalOper(newRemfile));
        opers.add(createLocalToRemoteOper(updatedLocalFile));
    }


    /** Recreate a remotely deleted file from local updates */
    private void recreateRemoteRemovedFile
    (
            DbFile dbfile,
            List<AbstractSyncOper<ProviderClientT>> opers
    )
            throws SQLException
    {
        resetRemoteFields(dbfile);
        DbFile updatedLocalFile = updateFileAsLocallyAdded(
                dbfile, itsContext.getString(R.string.recreated_local_copy));
        opers.add(createLocalToRemoteOper(updatedLocalFile));
    }
}
