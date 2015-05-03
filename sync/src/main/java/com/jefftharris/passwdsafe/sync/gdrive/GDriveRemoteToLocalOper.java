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
import java.util.HashMap;

import android.content.Context;
import android.util.Log;

import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
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

    private File itsDriveFile;

    /** Constructor */
    public GDriveRemoteToLocalOper(DbFile file,
                                   HashMap<String, File> cache)
    {
        super(file);
        itsDriveFile = cache.get(itsFile.itsRemoteId);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper#doOper(java.lang.Object, android.content.Context)
     */
    @Override
    public void doOper(Drive drive, Context ctx) throws IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "syncRemoteToLocal %s", itsFile);
        if (itsDriveFile == null) {
            itsDriveFile = GDriveSyncer.getFile(itsFile.itsRemoteId, drive);
        }
        setLocalFileName(SyncHelper.getLocalFileName(itsFile.itsId));
        setDownloaded(downloadFile(drive, ctx));
    }


    /** Download a file */
    private boolean downloadFile(Drive drive, Context ctx)
    {
        String url = itsDriveFile.getDownloadUrl();
        if ((url == null) || (url.length() <= 0)) {
            return false;
        }

        PasswdSafeUtil.dbginfo(TAG, "downloadFile %s from %s",
                               itsDriveFile.getId(), url);
        try {
            GenericUrl downloadUrl = new GenericUrl(url);
            OutputStream os = null;
            try {
                os = new BufferedOutputStream(
                        ctx.openFileOutput(getLocalFileName(),
                                           Context.MODE_PRIVATE));
                HttpRequestFactory reqFactory = drive.getRequestFactory();
                MediaHttpDownloader dl = new MediaHttpDownloader(
                    reqFactory.getTransport(), reqFactory.getInitializer());
                dl.setDirectDownloadEnabled(true);
                dl.download(downloadUrl, os);
            } finally {
                if (os != null) {
                    os.close();
                }
            }

            java.io.File localFile = ctx.getFileStreamPath(getLocalFileName());
            if (!localFile.setLastModified(
                    itsDriveFile.getModifiedDate().getValue())) {
                Log.e(TAG, "Can't set mod time on " + itsFile);
            }
        } catch (IOException e) {
            ctx.deleteFile(getLocalFileName());
            Log.e(TAG, "Sync failed to download " + itsDriveFile.getTitle(), e);
            return false;
        }
        return true;
    }
}
