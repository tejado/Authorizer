/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.dropbox;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.GetMetadataError;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.lib.AbstractLocalToRemoteSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer;
import com.jefftharris.passwdsafe.sync.lib.AbstractRemoteToLocalSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractRmSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.ProviderRemoteFile;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;
import com.jefftharris.passwdsafe.sync.lib.SyncRemoteFiles;

import java.util.List;

/**
 * The DropboxCoreSyncer class encapsulates a Dropbox sync operation
 */
public class DropboxCoreSyncer extends AbstractProviderSyncer<DbxClientV2>
{
    private static final String TAG = "DropboxCoreSyncer";

    /** Constructor */
    public DropboxCoreSyncer(DbxClientV2 client,
                             DbProvider provider, SQLiteDatabase db,
                             SyncLogRecord logrec, Context ctx)
    {
        super(client, provider, db, logrec, ctx, TAG);
    }

    /** Create a remote identifier from the local name of a file */
    public static String createRemoteIdFromLocal(DbFile dbfile)
    {
        return ProviderRemoteFile.PATH_SEPARATOR +
               dbfile.itsLocalTitle.toLowerCase();
    }

    /**
     * Get the account display name
     */
    public static String getDisplayName(DbxClientV2 client) throws DbxException
    {
        FullAccount acct = client.users().getCurrentAccount();
        return acct.getName().getDisplayName();
    }

    /** Perform a sync of the files */
    @Override
    protected List<AbstractSyncOper<DbxClientV2>> performSync() throws Exception
    {
        syncDisplayName();
        updateDbFiles(getDropboxFiles());
        return resolveSyncOpers();
    }


    /** Create an operation to sync local to remote */
    @Override
    protected AbstractLocalToRemoteSyncOper<DbxClientV2>
    createLocalToRemoteOper(DbFile dbfile)
    {
        return new DropboxCoreLocalToRemoteOper(dbfile);
    }


    /** Create an operation to sync remote to local */
    @Override
    protected AbstractRemoteToLocalSyncOper<DbxClientV2>
    createRemoteToLocalOper(DbFile dbfile)
    {
        return new DropboxCoreRemoteToLocalOper(dbfile);
    }


    /** Create an operation to remove a file */
    @Override
    protected AbstractRmSyncOper<DbxClientV2>
    createRmFileOper(DbFile dbfile)
    {
        return new DropboxCoreRmFileOper(dbfile);
    }


    /** Sync account display name */
    private void syncDisplayName() throws DbxException
    {
        String name = getDisplayName(itsProviderClient);
        if (!TextUtils.equals(itsProvider.itsDisplayName, name)) {
            SyncDb.updateProviderDisplayName(itsProvider.itsId, name, itsDb);
        }
    }


    /** Get the remote Dropbox files to sync */
    private SyncRemoteFiles getDropboxFiles()
            throws DbxException
    {
        SyncRemoteFiles files = new SyncRemoteFiles();
        for (DbFile dbfile: SyncDb.getFiles(itsProvider.itsId, itsDb)) {
            if (dbfile.itsRemoteId == null) {
                Metadata entry = getRemoteFile(createRemoteIdFromLocal(dbfile));
                if (entry instanceof FileMetadata) {
                    PasswdSafeUtil.dbginfo(
                            TAG, "dbx file for local: %s",
                            DropboxCoreProviderFile.entryToString(entry));
                    files.addRemoteFileForNew(
                            dbfile.itsId, new DropboxCoreProviderFile(entry));
                }
            } else {
                switch (dbfile.itsRemoteChange) {
                case NO_CHANGE:
                case ADDED:
                case MODIFIED: {
                    Metadata entry = getRemoteFile(dbfile.itsRemoteId);
                    if (entry instanceof FileMetadata) {
                        PasswdSafeUtil.dbginfo(
                                TAG, "dbx file: %s",
                                DropboxCoreProviderFile.entryToString(entry));
                        files.addRemoteFile(new DropboxCoreProviderFile(entry));
                    }
                    break;
                }
                case REMOVED: {
                    break;
                }
                }
            }
        }
        return files;
    }

    /**
     * Get a remote file's metadata from Dropbox
     * @return The file's FileMetadata if found; null or DeletedMetadata if
     * not found
     */
    private Metadata getRemoteFile(String remoteId)
            throws DbxException
    {
        try {
            return itsProviderClient.files()
                                    .getMetadataBuilder(remoteId)
                                    .withIncludeDeleted(true)
                                    .start();
        } catch (GetMetadataErrorException e) {
            GetMetadataError error = e.errorValue;
            if (error.getPathValue().isNotFound()) {
                return null;
            }
            throw e;
        }
    }
}
