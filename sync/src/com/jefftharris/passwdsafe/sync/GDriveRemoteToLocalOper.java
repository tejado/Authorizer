/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

/**
 * A Google Drive sync operation to sync a remote file to a local file
 */
public class GDriveRemoteToLocalOper extends GDriveSyncOper
{
    private static final String TAG = "GDriveRemoteToLocalOper";

    private File itsDriveFile;
    private String itsLocalFileName;
    private boolean itsIsDownloaded = false;

    /** Constructor */
    public GDriveRemoteToLocalOper(DbFile file,
                                   HashMap<String, File> cache)
    {
        super(file);
        itsDriveFile = cache.get(itsFile.itsRemoteId);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.GDriveSyncOper#doOper(com.google.api.services.drive.Drive, android.content.Context)
     */
    @Override
    public void doOper(Drive drive, Context ctx) throws IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "syncRemoteToLocal %s", itsFile);
        if (itsDriveFile == null) {
            itsDriveFile = getFile(itsFile.itsRemoteId, drive);
        }
        itsLocalFileName = ProviderSyncer.getLocalFileName(itsFile.itsId);
        itsIsDownloaded = downloadFile(drive, ctx);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.GDriveSyncOper#doPostOperUpdate(android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void doPostOperUpdate(SQLiteDatabase db, Context ctx)
            throws IOException, SQLException
    {
        if (itsIsDownloaded && (itsLocalFileName != null)) {
            try {
                SyncDb.updateLocalFile(itsFile.itsId, itsLocalFileName,
                                       itsFile.itsRemoteTitle,
                                       itsFile.itsRemoteModDate, db);
            } catch (SQLException e) {
                ctx.deleteFile(itsLocalFileName);
                throw e;
            }
        }
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.GDriveSyncOper#getDescription(android.content.Context)
     */
    @Override
    public String getDescription(Context ctx)
    {
        return ctx.getString(R.string.sync_oper_remote_to_local,
                             itsFile.itsRemoteTitle);
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
                    ctx.openFileOutput(itsLocalFileName, Context.MODE_PRIVATE));
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

            java.io.File localFile = ctx.getFileStreamPath(itsLocalFileName);
            localFile.setLastModified(
                    itsDriveFile.getModifiedDate().getValue());
        } catch (IOException e) {
            ctx.deleteFile(itsLocalFileName);
            Log.e(TAG, "Sync failed to download " + itsDriveFile.getTitle(), e);
            return false;
        }
        return true;
    }
}
