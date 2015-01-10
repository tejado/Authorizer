/*
 * Copyright (©) 2014-2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.box;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.box.boxjavalibv2.BoxClient;
import com.box.boxjavalibv2.dao.BoxFile;
import com.box.boxjavalibv2.requests.requestobjects.BoxFileUploadRequestObject;
import com.box.boxjavalibv2.resourcemanagers.BoxFilesManager;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.lib.AbstractLocalToRemoteSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;

/**
 * A Box sync operation to sync a local file to a remote one
 */
public class BoxLocalToRemoteOper
        extends AbstractLocalToRemoteSyncOper<BoxClient>
{
    private static final String TAG = "BoxLocalToRemoteOper";

    private BoxFile itsUpdatedFile;

    /** Constructor */
    public BoxLocalToRemoteOper(DbFile file)
    {
        super(file, false);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper#doOper(java.lang.Object, android.content.Context)
     */
    @Override
    public void doOper(BoxClient providerClient, Context ctx) throws Exception
    {
        PasswdSafeUtil.dbginfo(TAG, "syncLocalToRemote %s", itsFile);

        BoxFilesManager fileMgr = providerClient.getFilesManager();
        if (itsFile.itsLocalFile != null) {
            setLocalFile(ctx.getFileStreamPath(itsFile.itsLocalFile));
            if (isInsert()) {
                BoxFileUploadRequestObject req =
                    BoxFileUploadRequestObject.uploadFileRequestObject(
                        BoxSyncer.ROOT_FOLDER, itsFile.itsLocalTitle,
                        getLocalFile(), providerClient.getJSONParser());
                itsUpdatedFile = fileMgr.uploadFile(req);
            } else {
                BoxFileUploadRequestObject req =
                    BoxFileUploadRequestObject.uploadNewVersionRequestObject(
                        itsFile.itsLocalTitle, getLocalFile());
                itsUpdatedFile =
                        fileMgr.uploadNewVersion(itsFile.itsRemoteId, req);
            }
        } else {
            ByteArrayInputStream is = new ByteArrayInputStream(new byte[0]);
            BoxFileUploadRequestObject req =
                    BoxFileUploadRequestObject.uploadFileRequestObject(
                        BoxSyncer.ROOT_FOLDER, itsFile.itsLocalTitle, is);
            try {
                itsUpdatedFile = fileMgr.uploadFile(req);
            } finally {
                is.close();
            }
        }

        PasswdSafeUtil.dbginfo(TAG, "syncLocalToRemote updated %s",
                               BoxProvider.boxToString(itsUpdatedFile,
                                                       providerClient));
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.SyncOper#doPostOperUpdate(android.database.sqlite.SQLiteDatabase, android.content.Context)
     */
    @Override
    public void doPostOperUpdate(SQLiteDatabase db, Context ctx)
            throws IOException, SQLException
    {
        doPostOperFileUpdates(itsUpdatedFile.getId(),
                              itsUpdatedFile.getName(),
                              BoxSyncer.getFileFolder(itsUpdatedFile),
                              itsUpdatedFile.dateModifiedAt().getTime(),
                              itsUpdatedFile.getSha1(),
                              db);
    }
}
