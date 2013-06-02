/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import java.io.IOException;

import com.google.api.services.drive.Drive;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * A Google Drive sync operation to remove a file
 */
public class GDriveRmFileOper extends GDriveSyncOper
{
    private static final String TAG = "GDriveRmFileOper";

    /** Constructor */
    public GDriveRmFileOper(SyncDb.DbFile file)
    {
        super(file);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.GDriveSyncOper#doOper()
     */
    @Override
    public void doOper(Drive drive, Context ctx) throws IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "removeFile %s", itsFile);
        if (itsFile.itsLocalFile != null) {
            ctx.deleteFile(itsFile.itsLocalFile);
        }

        if (!itsFile.itsIsRemoteDeleted &&
                (itsFile.itsRemoteId != null)) {
            drive.files().trash(itsFile.itsRemoteId).execute();
        }
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.GDriveSyncOper#doPostOperUpdate(android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void doPostOperUpdate(SQLiteDatabase db, Context ctx)
            throws IOException, SQLException
    {
        SyncDb.removeFile(itsFile.itsId, db);
    }
}
