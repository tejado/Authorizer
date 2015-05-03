/*
 * Copyright (©) 2013-2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.dropbox;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

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
import com.jefftharris.passwdsafe.sync.lib.AbstractLocalToRemoteSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer;
import com.jefftharris.passwdsafe.sync.lib.AbstractRemoteToLocalSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractRmSyncOper;
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
        syncDisplayName();

        HashMap<String, ProviderRemoteFile> dbxfiles = new HashMap<>();
        getDirFiles(DbxPath.ROOT, dbxfiles);

        updateDbFiles(dbxfiles);
        return resolveSyncOpers();
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer#createLocalToRemoteOper(com.jefftharris.passwdsafe.sync.lib.DbFile)
     */
    @Override
    protected AbstractLocalToRemoteSyncOper<DbxFileSystem>
    createLocalToRemoteOper(DbFile dbfile)
    {
        return null;
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer#createRemoteToLocalOper(com.jefftharris.passwdsafe.sync.lib.DbFile)
     */
    @Override
    protected AbstractRemoteToLocalSyncOper<DbxFileSystem>
    createRemoteToLocalOper(DbFile dbfile)
    {
        return new DropboxRemoteToLocalOper(dbfile);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer#createRmFileOper(com.jefftharris.passwdsafe.sync.lib.DbFile)
     */
    @Override
    protected AbstractRmSyncOper<DbxFileSystem>
    createRmFileOper(DbFile dbfile)
    {
        return new DropboxRmFileOper(dbfile);
    }


    /** Sync the display name of the user */
    private void syncDisplayName()
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
    }


    /** Get all of the files under the path */
    private void getDirFiles(DbxPath path,
                             HashMap<String, ProviderRemoteFile> files)
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
                    DropboxProviderFile provFile =
                            new DropboxProviderFile(info);
                    files.put(provFile.getRemoteId(), provFile);
                }
            }
        }
    }
}
