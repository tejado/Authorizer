/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.dropbox;

import java.io.IOException;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncLibR;

/**
 *  A Dropbox sync operation to remove a file
 */
public class DropboxRmFileOper extends DropboxSyncOper
{
    private static final String TAG = "DropboxRmFileOper";

    private final boolean itsIsRmLocal;
    private final boolean itsIsRmRemote;

    /** Constructor */
    protected DropboxRmFileOper(DbFile file)
    {
        super(file);

        itsIsRmLocal = (itsFile.itsLocalFile != null);
        itsIsRmRemote = (!itsFile.itsIsRemoteDeleted &&
                (itsFile.itsRemoteId != null));
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.DropboxSyncOper#doOper(com.dropbox.sync.android.DbxFileSystem, android.content.Context)
     */
    @Override
    public void doOper(DbxFileSystem fs, Context ctx) throws IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "removeFile %s", itsFile);
        if (itsIsRmLocal) {
            ctx.deleteFile(itsFile.itsLocalFile);
        }

        if (itsIsRmRemote) {
            DbxPath path = new DbxPath(itsFile.itsRemoteId);
            if (fs.exists(path)) {
                fs.delete(path);
            }
        }
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.SyncOper#doPostOperUpdate(android.database.sqlite.SQLiteDatabase, android.content.Context)
     */
    @Override
    public void doPostOperUpdate(SQLiteDatabase db, Context ctx)
            throws IOException, SQLException
    {
        SyncDb.removeFile(itsFile.itsId, db);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.SyncOper#getDescription(android.content.Context)
     */
    @Override
    public String getDescription(Context ctx)
    {
        String name = itsFile.itsLocalTitle;
        if (name == null) {
            name = itsFile.itsRemoteTitle;
        }

        if (itsIsRmLocal && !itsIsRmRemote) {
            return ctx.getString(SyncLibR.string.sync_oper_rmfile_local, name);
        } else if (!itsIsRmLocal && itsIsRmRemote) {
            return ctx.getString(SyncLibR.string.sync_oper_rmfile_remote, name);
        } else {
            return ctx.getString(SyncLibR.string.sync_oper_rmfile, name);
        }
    }
}
