/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.onedrive;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

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
import com.microsoft.onedriveaccess.IOneDriveService;
import com.microsoft.onedriveaccess.model.Drive;
import com.microsoft.onedriveaccess.model.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit.RetrofitError;

/**
 * The OnedriveSyncer class encapsulates an OneDrive sync operation
 */
public class OnedriveSyncer
    extends AbstractProviderSyncer<IOneDriveService>
{
    private static final String TAG = "OnedriveSyncer";

    /**
     * Constructor
     */
    public OnedriveSyncer(IOneDriveService service,
                          DbProvider provider, SQLiteDatabase db,
                          SyncLogRecord logrec, Context ctx)
    {
        super(service, provider, db, logrec, ctx, TAG);
    }


    /** Is the error a 404 not-found error */
    public static boolean isNot404Error(RetrofitError e)
    {
        return (e.isNetworkError() ||
                (e.getResponse() == null) ||
                (e.getResponse().getStatus() != 404));
    }


    /**
     * Perform a sync of the files
     */
    @Override
    protected List<AbstractSyncOper<IOneDriveService>> performSync()
            throws Exception
    {
        syncDisplayName();
        updateDbFiles(getOnedriveFiles());
        return resolveSyncOpers();
    }


    /**
     * Create an operation to sync local to remote
     */
    @Override
    protected AbstractLocalToRemoteSyncOper<IOneDriveService>
    createLocalToRemoteOper(DbFile dbfile)
    {
        return new OnedriveLocalToRemoteOper(dbfile);
    }


    /**
     * Create an operation to sync remote to local
     */
    @Override
    protected AbstractRemoteToLocalSyncOper<IOneDriveService>
    createRemoteToLocalOper(DbFile dbfile)
    {
        return new OnedriveRemoteToLocalOper(dbfile);
    }


    /**
     * Create an operation to remove a file
     */
    @Override
    protected AbstractRmSyncOper<IOneDriveService>
    createRmFileOper(DbFile dbfile)
    {
        return new OnedriveRmFileOper(dbfile);
    }


    /**
     * Sync account display name
     */
    private void syncDisplayName() throws RetrofitError
    {
        Drive drive = itsProviderClient.getDrive();
        if ((drive != null) && (drive.Owner != null) &&
            (drive.Owner.User != null)) {
            String displayName = drive.Owner.User.DisplayName;
            if (!TextUtils.equals(itsProvider.itsDisplayName, displayName)) {
                SyncDb.updateProviderDisplayName(itsProvider.itsId, displayName,
                                                 itsDb);
            }
        } else {
            SyncDb.updateProviderDisplayName(itsProvider.itsId, null, itsDb);
        }
    }


    /**
     * Get the remote OneDrive files to sync
     */
    private Map<String, ProviderRemoteFile> getOnedriveFiles()
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
                    Item item = itsProviderClient.getItemByPath(
                            dbfile.itsRemoteId, null);
                    if (item.Deleted == null) {
                        OnedriveProviderFile remfile =
                                new OnedriveProviderFile(item);
                        files.put(remfile.getRemoteId(), remfile);
                    }
                } catch (RetrofitError e) {
                    if (isNot404Error(e)) {
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
