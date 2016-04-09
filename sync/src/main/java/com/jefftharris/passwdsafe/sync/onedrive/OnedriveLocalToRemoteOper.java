/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.onedrive;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.lib.AbstractLocalToRemoteSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.ProviderRemoteFile;
import com.microsoft.onedriveaccess.IOneDriveService;
import com.microsoft.onedriveaccess.model.Item;

import java.io.File;
import java.io.IOException;

import retrofit.mime.TypedFile;

/**
 * An OneDrive sync operation to sync a local file to a remote one
 */
public class OnedriveLocalToRemoteOper
        extends AbstractLocalToRemoteSyncOper<IOneDriveService>
{
    private static final String TAG = "OnedriveLocalToRemoteOp";

    private ProviderRemoteFile itsUpdatedFile = null;

    /** Constructor */
    public OnedriveLocalToRemoteOper(DbFile dbfile)
    {
        super(dbfile);
    }

    /** Perform the sync operation */
    @Override
    public void doOper(IOneDriveService providerClient,
                       Context ctx) throws Exception
    {
        PasswdSafeUtil.dbginfo(TAG, "syncLocalToRemote %s", itsFile);

        File tmpFile = null;
        try {
            File uploadFile;
            String remotePath;
            if (itsFile.itsLocalFile != null) {
                uploadFile = ctx.getFileStreamPath(itsFile.itsLocalFile);
                setLocalFile(uploadFile);
                if (isInsert()) {
                    remotePath = ProviderRemoteFile.PATH_SEPARATOR +
                                 Uri.encode(itsFile.itsLocalTitle);
                } else {
                    remotePath = itsFile.itsRemoteId;
                }
            } else {
                tmpFile = File.createTempFile("passwd", ".psafe3");
                tmpFile.deleteOnExit();
                uploadFile = tmpFile;
                remotePath = ProviderRemoteFile.PATH_SEPARATOR +
                             Uri.encode(itsFile.itsLocalTitle);
            }

            Item updatedItem = providerClient.uploadItemByPath(
                    remotePath,
                    new TypedFile("application/psafe3", uploadFile));
            itsUpdatedFile = new OnedriveProviderFile(updatedItem);
            PasswdSafeUtil.dbginfo(TAG, "updated file: %s", itsUpdatedFile);

        } finally {
            if ((tmpFile != null) && !tmpFile.delete()) {
                Log.e(TAG, "Can't delete temp file " + tmpFile);
            }

        }
    }

    /** Perform the database update after the sync operation */
    @Override
    public void doPostOperUpdate(SQLiteDatabase db,
                                 Context ctx) throws IOException, SQLException
    {
        doPostOperFileUpdates(itsUpdatedFile.getRemoteId(),
                              itsUpdatedFile.getTitle(),
                              itsUpdatedFile.getFolder(),
                              itsUpdatedFile.getModTime(),
                              itsUpdatedFile.getHash(), db);
    }
}
