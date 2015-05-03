/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.owncloud;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.lib.AbstractLocalToRemoteSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.resources.files.UploadRemoteFileOperation;

/**
 * An ownCloud sync operation to sync a local file to a remote one
 */
public class OwncloudLocalToRemoteOper extends
        AbstractLocalToRemoteSyncOper<OwnCloudClient>
{
    private static final String TAG = "OwncloudLocalToRemoteOper";

    private OwncloudProviderFile itsUpdatedFile = null;

    /** Constructor */
    public OwncloudLocalToRemoteOper(DbFile file)
    {
        super(file, false);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper#doOper(java.lang.Object, android.content.Context)
     */
    @Override
    public void doOper(OwnCloudClient providerClient, Context ctx)
            throws Exception
    {
        PasswdSafeUtil.dbginfo(TAG, "syncLocalToRemote %s", itsFile);

        File tmpFile = null;
        File uploadFile = null;
        String remotePath = null;
        try {
            if (itsFile.itsLocalFile != null) {
                uploadFile = ctx.getFileStreamPath(itsFile.itsLocalFile);
                setLocalFile(uploadFile);
                if (isInsert()) {
                    remotePath =
                            FileUtils.PATH_SEPARATOR + itsFile.itsLocalTitle;
                } else {
                    remotePath = itsFile.itsRemoteId;
                }
            } else {
                tmpFile = File.createTempFile("passwd", ".psafe3");
                // TODO: Check whether this create is needed?
                tmpFile.createNewFile();
                tmpFile.deleteOnExit();
                uploadFile = tmpFile;
                remotePath = FileUtils.PATH_SEPARATOR + itsFile.itsLocalTitle;
            }

            UploadRemoteFileOperation oper = new UploadRemoteFileOperation(
                    uploadFile.getAbsolutePath(),
                    remotePath, "application/psafe3");
            RemoteOperationResult res = oper.execute(providerClient);
            OwncloudSyncer.checkOperationResult(res, ctx);

            ReadRemoteFileOperation fileOper =
                    new ReadRemoteFileOperation(remotePath);
            res = fileOper.execute(providerClient);
            OwncloudSyncer.checkOperationResult(res, ctx);
            itsUpdatedFile =
                    new OwncloudProviderFile((RemoteFile)res.getData().get(0));
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.SyncOper#doPostOperUpdate(android.database.sqlite.SQLiteDatabase, android.content.Context)
     */
    @Override
    public void doPostOperUpdate(SQLiteDatabase db, Context ctx)
            throws IOException, SQLException
    {
        doPostOperFileUpdates(itsUpdatedFile.getRemoteId(),
                              itsUpdatedFile.getTitle(),
                              itsUpdatedFile.getFolder(),
                              itsUpdatedFile.getModTime(),
                              itsUpdatedFile.getHash(), db);
    }
}
