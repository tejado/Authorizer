/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.box;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.box.boxjavalibv2.BoxClient;
import com.box.boxjavalibv2.dao.BoxFile;
import com.box.boxjavalibv2.requests.requestobjects.BoxFileUploadRequestObject;
import com.box.boxjavalibv2.resourcemanagers.BoxFilesManager;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.R;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;

/**
 * A Box sync operation to sync a local file to a remote one
 */
public class BoxLocalToRemoteOper extends AbstractSyncOper<BoxClient>
{
    private static final String TAG = "BoxLocalToRemoteOper";

    private final boolean itsIsInsert;
    private File itsLocalFile;
    private BoxFile itsUpdatedFile;
    private String itsFolder;

    /** Constructor */
    public BoxLocalToRemoteOper(DbFile file)
    {
        super(file);
        itsFolder = file.itsLocalFolder;
        if (TextUtils.isEmpty(itsFile.itsRemoteId)) {
            itsIsInsert = true;
        } else {
            itsIsInsert = false;
        }
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
            itsLocalFile = ctx.getFileStreamPath(itsFile.itsLocalFile);
            if (itsIsInsert) {
                BoxFileUploadRequestObject req =
                    BoxFileUploadRequestObject.uploadFileRequestObject(
                        BoxSyncer.ROOT_FOLDER, itsFile.itsLocalTitle,
                        itsLocalFile, providerClient.getJSONParser());
                itsUpdatedFile = fileMgr.uploadFile(req);
            } else {
                BoxFileUploadRequestObject req =
                    BoxFileUploadRequestObject.uploadNewVersionRequestObject(
                        itsFile.itsLocalTitle, itsLocalFile);
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
        String title = itsUpdatedFile.getName();
        long modDate = itsUpdatedFile.dateModifiedAt().getTime();
        itsFolder = BoxSyncer.getFileFolder(itsUpdatedFile);
        // Box seems to add a second to the time at the next sync, so increment
        // here to avoid the extra copy from remote.
        modDate += 1000;
        SyncDb.updateRemoteFile(itsFile.itsId, itsUpdatedFile.getId(),
                                title, itsFolder, modDate, db);
        SyncDb.updateLocalFile(itsFile.itsId, itsFile.itsLocalFile,
                               title, itsFolder, modDate, db);
        clearFileChanges(db);
        if (itsLocalFile != null) {
            itsLocalFile.setLastModified(modDate);
        }
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.SyncOper#getDescription(android.content.Context)
     */
    @Override
    public String getDescription(Context ctx)
    {
        return ctx.getString(R.string.sync_oper_local_to_remote,
                             itsFile.itsLocalTitle + " [" + itsFolder + "]");
    }
}
