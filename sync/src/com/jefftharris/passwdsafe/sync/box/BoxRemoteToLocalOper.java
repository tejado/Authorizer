/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.box;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.box.boxjavalibv2.BoxClient;
import com.box.boxjavalibv2.requests.requestobjects.BoxDefaultRequestObject;
import com.box.boxjavalibv2.resourcemanagers.BoxFilesManager;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.Utils;
import com.jefftharris.passwdsafe.sync.R;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncHelper;

/**
 * A Box sync operation to sync a remote file to a local one
 */
public class BoxRemoteToLocalOper extends AbstractSyncOper<BoxClient>
{
    private static final String TAG = "BoxRemoteToLocalOper";

    private String itsLocalFileName;
    private boolean itsIsDownloaded = false;

    /** Constructor */
    public BoxRemoteToLocalOper(DbFile file)
    {
        super(file);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper#doOper(java.lang.Object, android.content.Context)
     */
    @Override
    public void doOper(BoxClient providerClient, Context ctx)
            throws IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "syncRemoteToLocal %s", itsFile);
        itsLocalFileName = SyncHelper.getLocalFileName(itsFile.itsId);

        try {
            InputStream is = null;
            OutputStream os = null;
            try {
                BoxDefaultRequestObject req = new BoxDefaultRequestObject();
                BoxFilesManager fileMgr = providerClient.getFilesManager();
                is = fileMgr.downloadFile(itsFile.itsRemoteId, req);
                os = new BufferedOutputStream(
                        ctx.openFileOutput(itsLocalFileName,
                                           Context.MODE_PRIVATE));
                Utils.copyStream(is, os);
            } finally {
                Utils.closeStreams(is, os);
            }

            java.io.File localFile = ctx.getFileStreamPath(itsLocalFileName);
            localFile.setLastModified(itsFile.itsRemoteModDate);
            itsIsDownloaded = true;
        } catch (Exception e) {
            ctx.deleteFile(itsLocalFileName);
            itsIsDownloaded = false;
            Log.e(TAG, "Sync failed to download: " + itsFile, e);
        }
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.SyncOper#doPostOperUpdate(android.database.sqlite.SQLiteDatabase, android.content.Context)
     */
    @Override
    public void doPostOperUpdate(SQLiteDatabase db, Context ctx)
            throws IOException, SQLException
    {
        if (itsIsDownloaded && (itsLocalFileName != null)) {
            try {
                SyncDb.updateLocalFile(itsFile.itsId, itsLocalFileName,
                                       itsFile.itsRemoteTitle,
                                       itsFile.itsRemoteFolder,
                                       itsFile.itsRemoteModDate, db);
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
    public String getDescription(Context ctx)
    {
        return ctx.getString(R.string.sync_oper_remote_to_local,
                             itsFile.itsRemoteTitle +
                             " [" + itsFile.itsRemoteFolder + "]");
    }
}
