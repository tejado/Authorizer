/*
 * Copyright (©) 2014-2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.box;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.content.Context;
import android.util.Log;

import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.listeners.ProgressListener;
import com.box.androidsdk.content.models.BoxSession;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.Utils;
import com.jefftharris.passwdsafe.sync.lib.AbstractRemoteToLocalSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.SyncHelper;

/**
 * A Box sync operation to sync a remote file to a local one
 */
public class BoxRemoteToLocalOper
        extends AbstractRemoteToLocalSyncOper<BoxSession>
{
    private static final String TAG = "BoxRemoteToLocalOper";

    /** Constructor */
    public BoxRemoteToLocalOper(DbFile file)
    {
        super(file);
    }

    @Override
    public void doOper(BoxSession providerClient, Context ctx)
            throws IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "syncRemoteToLocal %s", itsFile);
        setLocalFileName(SyncHelper.getLocalFileName(itsFile.itsId));

        try {
            OutputStream os = null;
            try {
                BoxApiFile fileApi = new BoxApiFile(providerClient);
                os = new BufferedOutputStream(
                        ctx.openFileOutput(getLocalFileName(),
                                           Context.MODE_PRIVATE));
                fileApi.getDownloadRequest(os, itsFile.itsRemoteId)
                        .setProgressListener(new ProgressListener()
                        {
                            @Override
                            public void onProgressChanged(long numBytes,
                                                          long totalBytes)
                            {
                                PasswdSafeUtil.dbginfo(TAG, "progress %d/%d",
                                                       numBytes, totalBytes);
                            }
                        })
                        .send();
            } finally {
                Utils.closeStreams(null, os);
            }

            java.io.File localFile = ctx.getFileStreamPath(getLocalFileName());
            if (!localFile.setLastModified(itsFile.itsRemoteModDate)) {
                Log.e(TAG, "Can't set mod time on " + itsFile);
            }
            setDownloaded(true);
        } catch (Exception e) {
            ctx.deleteFile(getLocalFileName());
            setDownloaded(false);
            Log.e(TAG, "Sync failed to download: " + itsFile, e);
        }
    }
}
