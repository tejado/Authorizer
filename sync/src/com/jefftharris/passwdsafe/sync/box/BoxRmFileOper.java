/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.box;

import java.io.IOException;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.box.boxjavalibv2.BoxClient;
import com.box.boxjavalibv2.requests.requestobjects.BoxFileRequestObject;
import com.box.boxjavalibv2.resourcemanagers.BoxFilesManager;
import com.box.restclientv2.exceptions.BoxSDKException;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.R;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;

/**
 * A Box sync operation to remove a file
 */
public class BoxRmFileOper extends AbstractSyncOper<BoxClient>
{
    private static final String TAG = "BoxRmFileOper";

    private final boolean itsIsRmLocal;
    private final boolean itsIsRmRemote;

    public BoxRmFileOper(DbFile dbfile)
    {
        super(dbfile);
        itsIsRmLocal = (itsFile.itsLocalFile != null);
        itsIsRmRemote = (!itsFile.itsIsRemoteDeleted &&
                (itsFile.itsRemoteId != null));
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper#doOper(java.lang.Object, android.content.Context)
     */
    @Override
    public void doOper(BoxClient providerClient, Context ctx)
            throws BoxSDKException
    {
        PasswdSafeUtil.dbginfo(TAG, "removeFile %s", itsFile);
        if (itsIsRmLocal) {
            ctx.deleteFile(itsFile.itsLocalFile);
        }

        if (itsIsRmRemote) {
            BoxFileRequestObject req =
                    BoxFileRequestObject.deleteFileRequestObject();
            BoxFilesManager fileMgr = providerClient.getFilesManager();
            fileMgr.deleteFile(itsFile.itsRemoteId, req);
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