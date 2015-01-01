/*
 * Copyright (©) 2013-2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
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
import android.text.TextUtils;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.R;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;

/**
 * A Google Drive sync operation to sync a local file to a remote file
 */
public class GDriveLocalToRemoteOper extends GDriveSyncOper
{
    private static final String TAG = "GDriveLocalToRemoteOper";

    private final boolean itsIsInsert;
    private File itsDriveFile;
    private String itsUpdateFolders;
    private java.io.File itsLocalFile;

    /** Constructor */
    public GDriveLocalToRemoteOper(DbFile file,
                                   HashMap<String, File> cache,
                                   boolean forceInsert)
    {
        super(file);
        if (forceInsert || TextUtils.isEmpty(itsFile.itsRemoteId)) {
            itsDriveFile = new File();
            itsDriveFile.setDescription("Password Safe file");
            itsIsInsert = true;
        } else {
            itsDriveFile = cache.get(itsFile.itsRemoteId);
            itsIsInsert = false;
        }
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.GDriveSyncOper#doOper(com.google.api.services.drive.Drive, android.content.Context)
     */
    @Override
    public void doOper(Drive drive, Context ctx) throws IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "syncLocalToRemote %s", itsFile);

        if (!itsIsInsert && (itsDriveFile == null)) {
            itsDriveFile = getFile(itsFile.itsRemoteId, drive);
        }

        itsDriveFile.setTitle(itsFile.itsLocalTitle);
        itsDriveFile.setMimeType("application/psafe3");

        Drive.Files files = drive.files();
        if (itsFile.itsLocalFile != null) {
            itsLocalFile = ctx.getFileStreamPath(itsFile.itsLocalFile);
            FileContent fileMedia = new FileContent(itsDriveFile.getMimeType(),
                                                    itsLocalFile);
            if (itsIsInsert) {
                itsDriveFile =
                        files.insert(itsDriveFile, fileMedia).execute();
            } else {
                itsDriveFile =
                        files.update(itsFile.itsRemoteId, itsDriveFile,
                                     fileMedia).execute();
            }
        } else {
            itsDriveFile = files.insert(itsDriveFile).execute();
        }

        FileFolders fileFolders = new FileFolders(drive);
        itsUpdateFolders = fileFolders.computeFileFolders(itsDriveFile);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.GDriveSyncOper#doPostOperUpdate(android.database.sqlite.SQLiteDatabase, android.content.Context)
     */
    @Override
    public void doPostOperUpdate(SQLiteDatabase db, Context ctx)
            throws IOException, SQLException
    {
        String title = itsDriveFile.getTitle();
        long modDate = itsDriveFile.getModifiedDate().getValue();
        String folders = (itsUpdateFolders != null) ?
                itsUpdateFolders : itsFile.itsLocalFolder;
        SyncDb.updateRemoteFile(itsFile.itsId, itsDriveFile.getId(),
                                   title, folders, modDate, db);
        SyncDb.updateLocalFile(itsFile.itsId, itsFile.itsLocalFile,
                                  title, folders, modDate, db);
        clearFileChanges(db);
        if (itsLocalFile != null) {
            itsLocalFile.setLastModified(modDate);
        }
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.GDriveSyncOper#getDescription(android.content.Context)
     */
    @Override
    public String getDescription(Context ctx)
    {
        return ctx.getString(R.string.sync_oper_local_to_remote,
                             itsFile.getLocalTitleAndFolder());
    }
}
