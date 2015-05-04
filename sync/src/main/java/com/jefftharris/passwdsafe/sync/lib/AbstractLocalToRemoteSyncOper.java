/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import java.io.File;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.jefftharris.passwdsafe.sync.R;

/**
 * Abstrct sync operation to sync a local file to a remote file
 */
public abstract class AbstractLocalToRemoteSyncOper<ProviderClientT> extends
        AbstractSyncOper<ProviderClientT>
{
    private final boolean itsIsInsert;
    private File itsLocalFile;

    /** Constructor */
    protected AbstractLocalToRemoteSyncOper(DbFile file, boolean forceInsert)
    {
        super(file);
        itsIsInsert = forceInsert || TextUtils.isEmpty(itsFile.itsRemoteId);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.GDriveSyncOper#getDescription(android.content.Context)
     */
    @Override
    public final String getDescription(Context ctx)
    {
        return ctx.getString(R.string.sync_oper_local_to_remote,
                             itsFile.getLocalTitleAndFolder());
    }


    /** Update local items after the operation is complete */
    protected final void doPostOperFileUpdates(String remId,
                                               String title,
                                               String folders,
                                               long modDate,
                                               String remHash,
                                               SQLiteDatabase db)
            throws SQLException
    {
        SyncDb.updateRemoteFile(itsFile.itsId, remId, title, folders, modDate,
                                remHash, db);
        SyncDb.updateLocalFile(itsFile.itsId, itsFile.itsLocalFile,
                               title, folders, modDate, db);
        clearFileChanges(db);
        if (itsLocalFile != null) {
            //noinspection ResultOfMethodCallIgnored
            itsLocalFile.setLastModified(modDate);
        }
    }


    /** Get whether an insert is performed instead of an update */
    protected final boolean isInsert()
    {
        return itsIsInsert;
    }


    /** Get the local file which was updated */
    protected final File getLocalFile()
    {
        return itsLocalFile;
    }


    /** Set the local file which was updated */
    protected final void setLocalFile(File file)
    {
        itsLocalFile = file;
    }
}
