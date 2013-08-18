/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import java.io.IOException;

import android.content.Context;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;


/**
 *  A Google Drive sync operation
 */
public abstract class GDriveSyncOper extends SyncOper
{
    /** Constructor */
    protected GDriveSyncOper(DbFile file)
    {
        super(file);
    }

    /** Perform the sync operation */
    public abstract void doOper(Drive drive, Context ctx) throws IOException;

    /** Get a file's metadata */
    protected static File getFile(String id, Drive drive)
            throws IOException
    {
        return drive.files().get(id)
                .setFields(GDriveProvider.FILE_FIELDS).execute();
    }
}
