/*
 * Copyright (©) 2013-2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.dropbox;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.util.Log;

import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFile.Listener;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileStatus;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.Utils;
import com.jefftharris.passwdsafe.sync.lib.AbstractRemoteToLocalSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.SyncHelper;

/**
 *  A Dropbox sync operation to sync a remote file to a local file
 */
public class DropboxRemoteToLocalOper
        extends AbstractRemoteToLocalSyncOper<DbxFileSystem>
{
    private static final String TAG = "DropboxRemoteToLocalOper";

    /** Constructor */
    protected DropboxRemoteToLocalOper(DbFile file)
    {
        super(file);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper#doOper(java.lang.Object, android.content.Context)
     */
    @Override
    public void doOper(DbxFileSystem fs, Context ctx) throws IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "syncRemoteToLocal %s", itsFile);
        setLocalFileName(SyncHelper.getLocalFileName(itsFile.itsId));

        DbxPath path = new DbxPath(itsFile.itsRemoteId);
        DbxFile file = null;
        try {
            file = fs.open(path);
            waitForLatest(file);

            OutputStream os = null;
            InputStream is = null;
            try {
                is = new BufferedInputStream(file.getReadStream());
                os = new BufferedOutputStream(
                        ctx.openFileOutput(getLocalFileName(),
                                           Context.MODE_PRIVATE));
                Utils.copyStream(is, os);
            } finally {
                Utils.closeStreams(is, os);
            }

            java.io.File localFile = ctx.getFileStreamPath(getLocalFileName());
            DbxFileInfo fileInfo = file.getInfo();
            localFile.setLastModified(fileInfo.modifiedTime.getTime());
            setDownloaded(true);
        } catch (Exception e) {
            ctx.deleteFile(getLocalFileName());
            setDownloaded(false);
            Log.e(TAG, "Sync failed to download " + path, e);
        } finally {
            if (file != null) {
                file.close();
            }
        }
    }

    /** Wait until the latest version of a file is downloaded */
    private void waitForLatest(DbxFile file)
            throws DbxException, InterruptedException
    {
        final Object monitor = new Object();
        final DbxPath path = file.getPath();
        Listener l = new Listener() {
            @Override
            public void onFileChange(DbxFile file)
            {
                synchronized(monitor) {
                    PasswdSafeUtil.dbginfo(TAG, "file changed: %s", path);
                    monitor.notifyAll();
                }
            }
        };
        file.addListener(l);
        synchronized(monitor) {
            while (true) {
                DbxFileStatus stat = file.getNewerStatus();
                if (stat == null) {
                    PasswdSafeUtil.dbginfo(TAG, "no newer: %s", path);
                    break;
                }
                if (stat.isCached) {
                    PasswdSafeUtil.dbginfo(TAG, "is cached: %s", path);
                    break;
                }
                PasswdSafeUtil.dbginfo(TAG, "waiting file change: %s", path);
                monitor.wait();
            }
        }
        file.update();
        file.removeListener(l);
    }
}
