/*
 * Copyright (©) 2013-2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import java.io.IOException;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 *  A generic sync operation
 */
public abstract class SyncOper
{
    protected final DbFile itsFile;

    /** Constructor */
    public SyncOper(DbFile file)
    {
        itsFile = file;
    }

    /** Get the sync database file */
    public DbFile getFile()
    {
        return itsFile;
    }

    /** Perform the database update after the sync operation */
    @SuppressWarnings("RedundantThrows")
    public abstract void doPostOperUpdate(SQLiteDatabase db, Context ctx)
            throws IOException, SQLException;

    /** Get a description of the operation */
    public abstract String getDescription(Context ctx);

    /** Clear the file change indications */
    protected void clearFileChanges(SQLiteDatabase db)
            throws SQLException
    {
        SyncDb.updateRemoteFileChange(itsFile.itsId,
                                      DbFile.FileChange.NO_CHANGE, db);
        SyncDb.updateLocalFileChange(itsFile.itsId,
                                     DbFile.FileChange.NO_CHANGE, db);
    }
}
