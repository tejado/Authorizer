/*
 * Copyright (©) 2014-2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.box;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.util.Log;

import com.box.boxjavalibv2.BoxClient;
import com.box.boxjavalibv2.requests.requestobjects.BoxDefaultRequestObject;
import com.box.boxjavalibv2.resourcemanagers.BoxFilesManager;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.Utils;
import com.jefftharris.passwdsafe.sync.lib.AbstractRemoteToLocalSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.SyncHelper;

/**
 * A Box sync operation to sync a remote file to a local one
 */
public class BoxRemoteToLocalOper
        extends AbstractRemoteToLocalSyncOper<BoxClient>
{
    private static final String TAG = "BoxRemoteToLocalOper";

    /** Constructor */
    public BoxRemoteToLocalOper(DbFile file)
    {
        super(file);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper#doOper(java.lang.Object, android.content.Context)
     */
    @Override
    public void doOper(BoxClient providerClient, Context ctx)
            throws IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "syncRemoteToLocal %s", itsFile);
        setLocalFileName(SyncHelper.getLocalFileName(itsFile.itsId));

        try {
            InputStream is = null;
            OutputStream os = null;
            try {
                BoxDefaultRequestObject req = new BoxDefaultRequestObject();
                BoxFilesManager fileMgr = providerClient.getFilesManager();
                is = fileMgr.downloadFile(itsFile.itsRemoteId, req);
                os = new BufferedOutputStream(
                        ctx.openFileOutput(getLocalFileName(),
                                           Context.MODE_PRIVATE));
                Utils.copyStream(is, os);
            } finally {
                Utils.closeStreams(is, os);
            }

            java.io.File localFile = ctx.getFileStreamPath(getLocalFileName());
            localFile.setLastModified(itsFile.itsRemoteModDate);
            setDownloaded(true);
        } catch (Exception e) {
            ctx.deleteFile(getLocalFileName());
            setDownloaded(false);
            Log.e(TAG, "Sync failed to download: " + itsFile, e);
        }
    }
}
