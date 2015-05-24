/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.dropbox;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxServerException;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The DropboxCoreSyncer class encapsulates a Dropbox sync operation
 */
public class DropboxCoreSyncer
        extends AbstractProviderSyncer<DropboxAPI<AndroidAuthSession>>
{
    private static final String TAG = "DropboxCoreSyncer";

    /** Constructor */
    public DropboxCoreSyncer(DropboxAPI<AndroidAuthSession> api,
                             DbProvider provider, SQLiteDatabase db,
                             SyncLogRecord logrec, Context ctx)
    {
        super(api, provider, db, logrec, ctx, TAG);
    }


    /** Perform a sync of the files */
    @Override
    protected List<AbstractSyncOper<DropboxAPI<AndroidAuthSession>>>
    performSync() throws Exception
    {
        syncDisplayName();
        updateDbFiles(getDropboxFiles());
        // TODO: handle remote file moves/renames?
        // TODO: update sync exception?
        // TODO: lowercase remote id to match dbx path case insensitivity
        // TODO: handle revoked access (from dev best practices)
        return resolveSyncOpers();
    }


    /** Create an operation to sync local to remote */
    @Override
    protected AbstractLocalToRemoteSyncOper<DropboxAPI<AndroidAuthSession>>
    createLocalToRemoteOper(DbFile dbfile)
    {
        return new DropboxCoreLocalToRemoteOper(dbfile);
    }


    /** Create an operation to sync remote to local */
    @Override
    protected AbstractRemoteToLocalSyncOper<DropboxAPI<AndroidAuthSession>>
    createRemoteToLocalOper(DbFile dbfile)
    {
        return new DropboxCoreRemoteToLocalOper(dbfile);
    }


    /** Create an operation to remove a file */
    @Override
    protected AbstractRmSyncOper<DropboxAPI<AndroidAuthSession>>
    createRmFileOper(DbFile dbfile)
    {
        return new DropboxCoreRmFileOper(dbfile);
    }


    /** Sync account display name */
    private void syncDisplayName() throws DropboxException
    {
        DropboxAPI.Account acct = itsProviderClient.accountInfo();
        if (acct != null) {
            if (!TextUtils.equals(itsProvider.itsDisplayName,
                                  acct.displayName)) {
                SyncDb.updateProviderDisplayName(itsProvider.itsId,
                                                 acct.displayName, itsDb);
            }
        } else {
            SyncDb.updateProviderDisplayName(itsProvider.itsId, null, itsDb);
        }
    }


    /** Get the remote Dropbox files to sync */
    private Map<String, ProviderRemoteFile> getDropboxFiles()
            throws DropboxException
    {
        Map<String, ProviderRemoteFile> files = new HashMap<>();

        for (DbFile dbfile: SyncDb.getFiles(itsProvider.itsId, itsDb)) {
            if (dbfile.itsRemoteId == null) {
                continue;
            }

            switch (dbfile.itsRemoteChange) {
            case NO_CHANGE:
            case ADDED:
            case MODIFIED: {
                try {
                    DropboxAPI.Entry entry = itsProviderClient.metadata(
                            dbfile.itsRemoteId, 1, null, false, null);
                    PasswdSafeUtil.dbginfo(
                            TAG, "dbx file: %s",
                            DropboxCoreProviderFile.entryToString(entry));

                    if (!entry.isDeleted) {
                        DropboxCoreProviderFile remfile =
                                new DropboxCoreProviderFile(entry);
                        files.put(remfile.getRemoteId(), remfile);
                    }
                } catch (DropboxServerException e) {
                    if (e.error != DropboxServerException._404_NOT_FOUND) {
                        throw e;
                    }
                }
                break;
            }
            case REMOVED: {
                break;
            }
            }
        }

        return files;
    }
}
