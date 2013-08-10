/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
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

/**
 *  A Dropbox sync operation to sync a remote file to a local file
 */
public class DropboxRemoteToLocalOper extends DropboxSyncOper
{
    private static final String TAG = "DropboxRemoteToLocalOper";

    private String itsLocalFileName;
    private boolean itsIsDownloaded = false;

    /** Constructor */
    protected DropboxRemoteToLocalOper(SyncDb.DbFile file)
    {
        super(file);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.DropboxSyncOper#doOper(com.dropbox.sync.android.DbxFileSystem, android.content.Context)
     */
    @Override
    public void doOper(DbxFileSystem fs, Context ctx) throws IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "syncRemoteToLocal %s", itsFile);
        itsLocalFileName = ProviderSyncer.getLocalFileName(itsFile.itsId);

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
                        ctx.openFileOutput(itsLocalFileName,
                                           Context.MODE_PRIVATE));
                Utils.copyStream(is, os);
            } finally {
                Utils.closeStreams(is, os);
            }

            java.io.File localFile = ctx.getFileStreamPath(itsLocalFileName);
            DbxFileInfo fileInfo = file.getInfo();
            localFile.setLastModified(fileInfo.modifiedTime.getTime());
            itsIsDownloaded = true;
        } catch (Exception e) {
            ctx.deleteFile(itsLocalFileName);
            itsIsDownloaded = false;
            Log.e(TAG, "Sync failed to download " + path, e);
        } finally {
            if (file != null) {
                file.close();
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
        if (itsIsDownloaded && (itsLocalFileName != null)) {
            try {
                SyncDb.updateLocalFile(itsFile.itsId, itsLocalFileName,
                                       itsFile.itsRemoteTitle,
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
                             itsFile.itsRemoteTitle);
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
