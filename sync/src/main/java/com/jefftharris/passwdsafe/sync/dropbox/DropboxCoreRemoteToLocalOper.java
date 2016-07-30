/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.dropbox;

import android.content.Context;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.lib.AbstractRemoteToLocalSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.SyncHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A Dropbox sync operation to sync a remote file to a local one
 */
public class DropboxCoreRemoteToLocalOper
        extends AbstractRemoteToLocalSyncOper<DbxClientV2>
{
    private static final String TAG = "DropboxCoreRemoteToLoca";

    /** Constructor */
    public DropboxCoreRemoteToLocalOper(DbFile dbfile)
    {
        super(dbfile);
    }

    /** Perform the sync operation */
    @Override
    public void doOper(DbxClientV2 providerClient,
                       Context ctx) throws DbxException, IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "syncRemoteToLocal %s", itsFile);
        setLocalFileName(SyncHelper.getLocalFileName(itsFile.itsId));

        FileOutputStream fos = null;
        try {
            fos = ctx.openFileOutput(getLocalFileName(), Context.MODE_PRIVATE);
            providerClient.files().download(itsFile.itsRemoteId).download(fos);

            File localFile = ctx.getFileStreamPath(getLocalFileName());
            if (!localFile.setLastModified(itsFile.itsRemoteModDate)) {
                Log.e(TAG, "Can't set mod time on " + itsFile);
            }
            setDownloaded(true);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing", e);
                }
            }
        }
    }
}
