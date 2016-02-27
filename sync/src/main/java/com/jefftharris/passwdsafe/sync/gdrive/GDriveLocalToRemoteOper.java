/*
 * Copyright (©) 2013-2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.gdrive;

import java.io.IOException;
import java.util.HashMap;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.lib.AbstractLocalToRemoteSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;

/**
 * A Google Drive sync operation to sync a local file to a remote file
 */
public class GDriveLocalToRemoteOper
        extends AbstractLocalToRemoteSyncOper<Drive>
{
    private static final String TAG = "GDriveLocalToRemoteOper";

    private File itsDriveFile;
    private String itsUpdateFolders;

    /** Constructor */
    public GDriveLocalToRemoteOper(DbFile file,
                                   HashMap<String, File> cache,
                                   boolean forceInsert)
    {
        super(file, forceInsert);
        if (isInsert()) {
            itsDriveFile = new File();
            itsDriveFile.setDescription("Password Safe file");
        } else {
            itsDriveFile = cache.get(itsFile.itsRemoteId);
        }
    }

    @Override
    public void doOper(Drive drive, Context ctx) throws IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "syncLocalToRemote %s", itsFile);

        if (!isInsert() && (itsDriveFile == null)) {
            itsDriveFile = GDriveSyncer.getFile(itsFile.itsRemoteId, drive);
        }

        File updatedFile = new File();
        updatedFile.setName(itsFile.itsLocalTitle);
        updatedFile.setMimeType("application/psafe3");
        if (isInsert()) {
            updatedFile.setDescription("Password Safe file");
        }

        Drive.Files files = drive.files();
        if (itsFile.itsLocalFile != null) {
            setLocalFile(ctx.getFileStreamPath(itsFile.itsLocalFile));
            FileContent fileMedia = new FileContent(itsDriveFile.getMimeType(),
                                                    getLocalFile());
            if (isInsert()) {
                itsDriveFile = files.create(updatedFile, fileMedia)
                                    .setFields(GDriveProvider.FILE_FIELDS)
                                    .execute();
            } else {
                itsDriveFile = files.update(itsFile.itsRemoteId, updatedFile,
                                            fileMedia)
                                    .setFields(GDriveProvider.FILE_FIELDS)
                                    .execute();
            }
        } else {
            itsDriveFile = files.create(updatedFile)
                                .setFields(GDriveProvider.FILE_FIELDS)
                                .execute();
        }

        FileFolders fileFolders = new FileFolders(drive);
        itsUpdateFolders = fileFolders.computeFileFolders(itsDriveFile);
    }

    @Override
    public void doPostOperUpdate(SQLiteDatabase db, Context ctx)
            throws IOException, SQLException
    {
        doPostOperFileUpdates(itsDriveFile.getId(),
                              itsDriveFile.getName(),
                              (itsUpdateFolders != null) ?
                                      itsUpdateFolders : itsFile.itsLocalFolder,
                              itsDriveFile.getModifiedTime().getValue(),
                              itsDriveFile.getMd5Checksum(),
                              db);
    }
}
