/*
 * Copyright (©) 2013-2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.gdrive;

import java.io.IOException;

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

    private final FileFolders itsFileFolders;
    private File itsUpdatedFile;
    private String itsUpdateFolders;

    /** Constructor */
    public GDriveLocalToRemoteOper(DbFile file, FileFolders fileFolders)
    {
        super(file);
        itsFileFolders = fileFolders;
    }

    @Override
    public void doOper(Drive drive, Context ctx) throws IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "syncLocalToRemote %s", itsFile);

        File fileUpdates = new File();
        fileUpdates.setName(itsFile.itsLocalTitle);
        fileUpdates.setMimeType("application/psafe3");
        if (isInsert()) {
            fileUpdates.setDescription("Password Safe file");
        }

        Drive.Files files = drive.files();
        if (itsFile.itsLocalFile != null) {
            setLocalFile(ctx.getFileStreamPath(itsFile.itsLocalFile));
            FileContent fileMedia = new FileContent(fileUpdates.getMimeType(),
                                                    getLocalFile());
            if (isInsert()) {
                itsUpdatedFile = files.create(fileUpdates, fileMedia)
                                      .setFields(GDriveProvider.FILE_FIELDS)
                                      .execute();
            } else {
                itsUpdatedFile = files.update(itsFile.itsRemoteId, fileUpdates,
                                              fileMedia)
                                      .setFields(GDriveProvider.FILE_FIELDS)
                                      .execute();
            }
        } else {
            itsUpdatedFile = files.create(fileUpdates)
                                  .setFields(GDriveProvider.FILE_FIELDS)
                                  .execute();
        }

        itsUpdateFolders = itsFileFolders.computeFileFolders(itsUpdatedFile);
        if (itsUpdateFolders == null) {
            itsUpdateFolders = itsFile.itsLocalFolder;
        }
    }

    @Override
    public void doPostOperUpdate(SQLiteDatabase db, Context ctx)
            throws IOException, SQLException
    {
        doPostOperFileUpdates(itsUpdatedFile.getId(),
                              itsUpdatedFile.getName(),
                              itsUpdateFolders,
                              itsUpdatedFile.getModifiedTime().getValue(),
                              itsUpdatedFile.getMd5Checksum(),
                              db);
    }
}
