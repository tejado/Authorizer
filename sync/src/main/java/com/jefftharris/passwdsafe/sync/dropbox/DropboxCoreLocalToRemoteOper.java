/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.dropbox;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.lib.AbstractLocalToRemoteSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.ProviderRemoteFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * A Dropbox sync operation to sync a local file to a remote one
 */
public class DropboxCoreLocalToRemoteOper
        extends AbstractLocalToRemoteSyncOper<DropboxAPI<AndroidAuthSession>>
{
    private static final String TAG = "DropboxCoreLocalToRemot";

    private ProviderRemoteFile itsUpdatedFile = null;

    /** Constructor */
    public DropboxCoreLocalToRemoteOper(DbFile dbfile)
    {
        super(dbfile);
    }

    /** Perform the sync operation */
    @Override
    public void doOper(DropboxAPI<AndroidAuthSession> providerClient,
                       Context ctx) throws DropboxException, IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "syncLocalToRemote %s", itsFile);

        File tmpFile = null;
        FileInputStream fis = null;
        try {
            File uploadFile;
            String remotePath;
            String parentRev = null;
            if (itsFile.itsLocalFile != null) {
                uploadFile = ctx.getFileStreamPath(itsFile.itsLocalFile);
                setLocalFile(uploadFile);
                if (isInsert()) {
                    remotePath = ProviderRemoteFile.PATH_SEPARATOR +
                                 itsFile.itsLocalTitle;
                } else {
                    remotePath = itsFile.itsRemoteId;
                    parentRev = itsFile.itsRemoteHash;
                }
            } else {
                tmpFile = File.createTempFile("passwd", ".psafe3");
                tmpFile.deleteOnExit();
                uploadFile = tmpFile;
                remotePath = ProviderRemoteFile.PATH_SEPARATOR +
                             itsFile.itsLocalTitle;
            }

            fis = new FileInputStream(uploadFile);
            ProgressListener listener = new ProgressListener()
            {
                @Override
                public void onProgress(long bytes, long total)
                {
                    PasswdSafeUtil.dbginfo(TAG, "syncLocalToRemote %d/%d",
                                           bytes, total);
                }
            };
            DropboxAPI.Entry updatedEntry = providerClient.putFile(
                    remotePath, fis, uploadFile.length(), parentRev,
                    true, listener);

            PasswdSafeUtil.dbginfo(
                    TAG, "updated file: %s",
                    DropboxCoreProviderFile.entryToString(updatedEntry));
            itsUpdatedFile = new DropboxCoreProviderFile(updatedEntry);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    Log.e(TAG, "Can't close file", e);
                }
            }
            if ((tmpFile != null) && !tmpFile.delete()) {
                Log.e(TAG, "Can't delete temp file " + tmpFile);
            }
        }
    }

    /** Perform the database update after the sync operation */
    @Override
    public void doPostOperUpdate(SQLiteDatabase db,
                                 Context ctx) throws IOException, SQLException
    {
        doPostOperFileUpdates(itsUpdatedFile.getRemoteId(),
                              itsUpdatedFile.getTitle(),
                              itsUpdatedFile.getFolder(),
                              itsUpdatedFile.getModTime(),
                              itsUpdatedFile.getHash(), db);
    }
}
