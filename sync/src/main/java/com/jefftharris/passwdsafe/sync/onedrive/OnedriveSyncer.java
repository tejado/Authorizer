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
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;
import com.microsoft.onedriveaccess.IOneDriveService;
import com.microsoft.onedriveaccess.model.Drive;

import java.util.List;

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


    /**
     * Perform a sync of the files
     */
    @Override
    protected List<AbstractSyncOper<IOneDriveService>> performSync()
            throws Exception
    {
        syncDisplayName();
        return resolveSyncOpers();
    }


    /**
     * Create an operation to sync local to remote
     */
    @Override
    protected AbstractLocalToRemoteSyncOper<IOneDriveService>
    createLocalToRemoteOper(DbFile dbfile)
    {
        return null;
    }


    /**
     * Create an operation to sync remote to local
     */
    @Override
    protected AbstractRemoteToLocalSyncOper<IOneDriveService>
    createRemoteToLocalOper(DbFile dbfile)
    {
        return null;
    }


    /**
     * Create an operation to remove a file
     */
    @Override
    protected AbstractRmSyncOper<IOneDriveService>
    createRmFileOper(DbFile dbfile)
    {
        return null;
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
}
