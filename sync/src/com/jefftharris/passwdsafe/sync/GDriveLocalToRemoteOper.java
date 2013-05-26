/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

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

/**
 * A Google Drive sync operation to sync a local file to a remote file
 */
public class GDriveLocalToRemoteOper extends GDriveSyncOper
{
    private static final String TAG = "GDriveLocalToRemoteOper";

    private final boolean itsIsInsert;
    private File itsDriveFile;
    private java.io.File itsLocalFile;

    /** Constructor */
    public GDriveLocalToRemoteOper(SyncDb.DbFile file,
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
        itsDriveFile.setMimeType("application/x-psafe3");
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
        itsLocalFile = ctx.getFileStreamPath(itsFile.itsLocalFile);
        FileContent fileMedia = new FileContent(itsDriveFile.getMimeType(),
                                                itsLocalFile);
        if (itsIsInsert) {
            itsDriveFile =
                    drive.files().insert(itsDriveFile, fileMedia).execute();
        } else {
            itsDriveFile =
                    drive.files().update(itsFile.itsRemoteId, itsDriveFile,
                                         fileMedia).execute();
        }
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
        SyncDb.updateRemoteFile(itsFile.itsId, itsDriveFile.getId(),
                                   title, modDate, db);
        SyncDb.updateLocalFile(itsFile.itsId, itsFile.itsLocalFile,
                                  title, modDate, db);
        itsLocalFile.setLastModified(modDate);
    }
}
