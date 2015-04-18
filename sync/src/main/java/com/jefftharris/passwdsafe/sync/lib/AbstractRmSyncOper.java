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

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.R;

/**
 * Abstract sync operation to remove a file
 */
public abstract class AbstractRmSyncOper<ProviderClientT> extends
        AbstractSyncOper<ProviderClientT>
{
    protected final String itsTag;
    private final boolean itsIsRmLocal;
    private final boolean itsIsRmRemote;

    /** Constructor */
    public AbstractRmSyncOper(DbFile dbfile, String tag)
    {
        super(dbfile);
        itsTag = tag;
        itsIsRmLocal = (itsFile.itsLocalFile != null);
        itsIsRmRemote = (!itsFile.itsIsRemoteDeleted &&
                (itsFile.itsRemoteId != null));
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper#doOper(java.lang.Object, android.content.Context)
     */
    @Override
    public final void doOper(ProviderClientT providerClient, Context ctx)
            throws Exception
    {
        PasswdSafeUtil.dbginfo(itsTag, "removeFile %s", itsFile);
        if (itsIsRmLocal) {
            ctx.deleteFile(itsFile.itsLocalFile);
        }

        if (itsIsRmRemote) {
            doRemoteRemove(providerClient, ctx);
        }
    }
    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.SyncOper#doPostOperUpdate(android.database.sqlite.SQLiteDatabase, android.content.Context)
     */
    @Override
    public void doPostOperUpdate(SQLiteDatabase db, Context ctx)
            throws IOException, SQLException
    {
        SyncDb.removeFile(itsFile.itsId, db);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.SyncOper#getDescription(android.content.Context)
     */
    @Override
    public String getDescription(Context ctx)
    {
        String name = (itsFile.itsLocalTitle != null) ?
                itsFile.getLocalTitleAndFolder() :
                itsFile.getRemoteTitleAndFolder();

        if (itsIsRmLocal && !itsIsRmRemote) {
            return ctx.getString(R.string.sync_oper_rmfile_local, name);
        } else if (!itsIsRmLocal && itsIsRmRemote) {
            return ctx.getString(R.string.sync_oper_rmfile_remote, name);
        } else {
            return ctx.getString(R.string.sync_oper_rmfile, name);
        }
    }

    /** Remove the remote file */
    protected abstract void doRemoteRemove(ProviderClientT providerClient,
                                           Context ctx)
            throws Exception;
}
