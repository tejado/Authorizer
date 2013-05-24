/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import java.io.IOException;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 *  A Google Drive sync operation
 */
public abstract class GDriveSyncOper
{
    protected final SyncDb.DbFile itsFile;

    /** Constructor */
    protected GDriveSyncOper(SyncDb.DbFile file)
    {
        itsFile = file;
    }

    /** Get the sync database file */
    public SyncDb.DbFile getFile()
    {
        return itsFile;
    }

    /** Perform the sync operation */
    public abstract void doOper(Drive drive, Context ctx)
            throws IOException;

    /** Perform the database update after the sync operation */
    public abstract void doPostOperUpdate(SQLiteDatabase db, Context ctx)
            throws IOException, SQLException;

    /** Get a file's metadata */
    protected static File getFile(String id, Drive drive)
            throws IOException
    {
        return drive.files().get(id)
                .setFields(GDriveSyncer.FILE_FIELDS).execute();
    }
}
