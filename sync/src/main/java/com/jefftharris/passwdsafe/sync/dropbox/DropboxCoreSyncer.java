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
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.lib.AbstractLocalToRemoteSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer;
import com.jefftharris.passwdsafe.sync.lib.AbstractRemoteToLocalSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractRmSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;

import java.util.List;

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

        DropboxAPI.Entry root = itsProviderClient.metadata("/", 0, null,
                                                           true, null);
        PasswdSafeUtil.dbginfo(TAG, "root: %s", entryToString(root));
        for (DropboxAPI.Entry child: root.contents) {
            PasswdSafeUtil.dbginfo(TAG, "child: %s", entryToString(child));
        }

        // TODO: sync files
        // TODO: update sync exception?
        // TODO: lowercase remote id to match dbx path case insensitivity
        return resolveSyncOpers();
    }


    /** Create an operation to sync local to remote */
    @Override
    protected AbstractLocalToRemoteSyncOper<DropboxAPI<AndroidAuthSession>>
    createLocalToRemoteOper(DbFile dbfile)
    {
        // TODO: local to remote
        return null;
    }


    /** Create an operation to sync remote to local */
    @Override
    protected AbstractRemoteToLocalSyncOper<DropboxAPI<AndroidAuthSession>>
    createRemoteToLocalOper(DbFile dbfile)
    {
        // TODO: remote to local
        return null;
    }


    /** Create an operation to remove a file */
    @Override
    protected AbstractRmSyncOper<DropboxAPI<AndroidAuthSession>>
    createRmFileOper(DbFile dbfile)
    {
        // TODO: remove file
        return null;
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


    /** Create a string form of a file entry */
    private String entryToString(DropboxAPI.Entry entry)
    {
        return String.format(
                "{name: %s, hash: %s, rev: %s, dir: %b, modified: %s}",
                entry.path, entry.hash, entry.rev, entry.isDir, entry.modified);
    }
}
