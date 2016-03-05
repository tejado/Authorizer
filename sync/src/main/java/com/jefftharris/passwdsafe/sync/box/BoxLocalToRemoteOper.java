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

import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.BoxConstants;
import com.box.androidsdk.content.listeners.ProgressListener;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxSession;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.lib.AbstractLocalToRemoteSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;

/**
 * A Box sync operation to sync a local file to a remote one
 */
public class BoxLocalToRemoteOper
        extends AbstractLocalToRemoteSyncOper<BoxSession>
{
    private static final String TAG = "BoxLocalToRemoteOper";

    private BoxFile itsUpdatedFile;

    /** Constructor */
    public BoxLocalToRemoteOper(DbFile file)
    {
        super(file);
    }

    @Override
    public void doOper(BoxSession providerClient, Context ctx) throws Exception
    {
        PasswdSafeUtil.dbginfo(TAG, "syncLocalToRemote %s", itsFile);

        BoxApiFile fileApi = new BoxApiFile(providerClient);
        ProgressListener uploadProgress = new ProgressListener()
        {
            @Override
            public void onProgressChanged(long numBytes, long totalBytes)
            {
                PasswdSafeUtil.dbginfo(TAG, "progress %d/%d",
                                       numBytes, totalBytes);
            }
        };

        if (itsFile.itsLocalFile != null) {
            setLocalFile(ctx.getFileStreamPath(itsFile.itsLocalFile));
            if (isInsert()) {
                itsUpdatedFile = fileApi
                        .getUploadRequest(getLocalFile(),
                                          BoxConstants.ROOT_FOLDER_ID)
                        .setFileName(itsFile.itsLocalTitle)
                        .setProgressListener(uploadProgress)
                        .send();
            } else {
                itsUpdatedFile = fileApi
                        .getUploadNewVersionRequest(getLocalFile(),
                                                    itsFile.itsRemoteId)
                        .setProgressListener(uploadProgress)
                        .send();
            }
        } else {
            ByteArrayInputStream is = new ByteArrayInputStream(new byte[0]);
            //noinspection TryFinallyCanBeTryWithResources
            try {
                itsUpdatedFile = fileApi
                        .getUploadRequest(is, itsFile.itsLocalTitle,
                                          BoxConstants.ROOT_FOLDER_ID)
                        .setProgressListener(uploadProgress)
                        .send();
            } finally {
                is.close();
            }
        }

        PasswdSafeUtil.dbginfo(TAG, "syncLocalToRemote updated %s",
                               BoxProvider.boxToString(itsUpdatedFile));
    }

    @Override
    public void doPostOperUpdate(SQLiteDatabase db, Context ctx)
            throws IOException, SQLException
    {
        doPostOperFileUpdates(itsUpdatedFile.getId(),
                              itsUpdatedFile.getName(),
                              BoxSyncer.getFileFolder(itsUpdatedFile),
                              itsUpdatedFile.getModifiedAt().getTime(),
                              itsUpdatedFile.getSha1(),
                              db);
    }
}
