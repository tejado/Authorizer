/*
 * Copyright (©) 2013-2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.gdrive;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.content.Context;
import android.util.Log;

import com.google.api.services.drive.Drive;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.Utils;
import com.jefftharris.passwdsafe.sync.lib.AbstractRemoteToLocalSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.SyncHelper;

/**
 * A Google Drive sync operation to sync a remote file to a local file
 */
public class GDriveRemoteToLocalOper
        extends AbstractRemoteToLocalSyncOper<Drive>
{
    private static final String TAG = "GDriveRemoteToLocalOper";

    /** Constructor */
    public GDriveRemoteToLocalOper(DbFile file)
    {
        super(file);
    }

    @Override
    public void doOper(Drive drive, Context ctx) throws IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "syncRemoteToLocal %s", itsFile);
        setLocalFileName(SyncHelper.getLocalFileName(itsFile.itsId));

        try {
            OutputStream os = null;
            try {
                os = new BufferedOutputStream(
                        ctx.openFileOutput(getLocalFileName(),
                                           Context.MODE_PRIVATE));

                drive.files().get(itsFile.itsRemoteId)
                     .executeMediaAndDownloadTo(os);
            } finally {
                Utils.closeStreams(null, os);
            }

            java.io.File localFile = ctx.getFileStreamPath(getLocalFileName());
            if (!localFile.setLastModified(itsFile.itsRemoteModDate)) {
                Log.e(TAG, "Can't set mod time on " + itsFile);
            }
            setDownloaded(true);
        } catch (IOException e) {
            ctx.deleteFile(getLocalFileName());
            setDownloaded(false);
            Log.e(TAG, "Sync failed to download " + itsFile, e);
        }
    }
}
