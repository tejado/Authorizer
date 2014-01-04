/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.gdrive;

import java.io.IOException;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.google.api.services.drive.Drive;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.R;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;

/**
 * A Google Drive sync operation to remove a file
 */
public class GDriveRmFileOper extends GDriveSyncOper
{
    private static final String TAG = "GDriveRmFileOper";

    private final boolean itsIsRmLocal;
    private final boolean itsIsRmRemote;

    /** Constructor */
    public GDriveRmFileOper(DbFile file)
    {
        super(file);

        itsIsRmLocal = (itsFile.itsLocalFile != null);
        itsIsRmRemote = (!itsFile.itsIsRemoteDeleted &&
                (itsFile.itsRemoteId != null));
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.GDriveSyncOper#doOper()
     */
    @Override
    public void doOper(Drive drive, Context ctx) throws IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "removeFile %s", itsFile);
        if (itsIsRmLocal) {
            ctx.deleteFile(itsFile.itsLocalFile);
        }

        if (itsIsRmRemote) {
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

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.GDriveSyncOper#getDescription()
     */
    @Override
    public String getDescription(Context ctx)
    {
        String name = itsFile.itsLocalTitle;
        if (name == null) {
            name = itsFile.itsRemoteTitle;
        }

        if (itsIsRmLocal && !itsIsRmRemote) {
            return ctx.getString(R.string.sync_oper_rmfile_local, name);
        } else if (!itsIsRmLocal && itsIsRmRemote) {
            return ctx.getString(R.string.sync_oper_rmfile_remote, name);
        } else {
            return ctx.getString(R.string.sync_oper_rmfile, name);
        }
    }
}
