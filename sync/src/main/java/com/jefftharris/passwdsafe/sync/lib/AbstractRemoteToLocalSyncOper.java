/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import java.io.IOException;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.jefftharris.passwdsafe.sync.R;

/**
 * Abstract sync operation to sync a remote file to a local file
 */
public abstract class AbstractRemoteToLocalSyncOper<ProviderClientT> extends
        AbstractSyncOper<ProviderClientT>
{
    private String itsLocalFileName;
    private boolean itsIsDownloaded = false;

    /** Constructor */
    protected AbstractRemoteToLocalSyncOper(DbFile dbfile)
    {
        super(dbfile);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.SyncOper#doPostOperUpdate(android.database.sqlite.SQLiteDatabase, android.content.Context)
     */
    @Override
    public final void doPostOperUpdate(SQLiteDatabase db, Context ctx)
            throws IOException, SQLException
    {
        if (itsIsDownloaded && (itsLocalFileName != null)) {
            try {
                SyncDb.updateLocalFile(itsFile.itsId, itsLocalFileName,
                                       itsFile.itsRemoteTitle,
                                       itsFile.itsRemoteFolder,
                                       itsFile.itsRemoteModDate, db);
                clearFileChanges(db);
            } catch (SQLException e) {
                ctx.deleteFile(itsLocalFileName);
                throw e;
            }
        }
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.SyncOper#getDescription(android.content.Context)
     */
    @Override
    public final String getDescription(Context ctx)
    {
        return ctx.getString(R.string.sync_oper_remote_to_local,
                             itsFile.getRemoteTitleAndFolder());
    }

    /** Get the local file name */
    protected String getLocalFileName()
    {
        return itsLocalFileName;
    }

    /** Set the local file name */
    protected void setLocalFileName(String fileName)
    {
        itsLocalFileName = fileName;
    }

    /** Set whether the file was downloaded */
    protected void setDownloaded(boolean downloaded)
    {
        itsIsDownloaded = downloaded;
    }
}
