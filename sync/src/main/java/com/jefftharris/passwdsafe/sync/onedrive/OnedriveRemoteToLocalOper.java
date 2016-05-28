/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.onedrive;

import android.content.Context;
import android.util.Log;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.Utils;
import com.jefftharris.passwdsafe.sync.lib.AbstractRemoteToLocalSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.SyncHelper;
import com.microsoft.onedriveaccess.IOneDriveService;
import com.microsoft.onedriveaccess.model.Item;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import retrofit.RetrofitError;

/**
 * An OneDrive sync operation to sync a remote file to a local one
 */
public class OnedriveRemoteToLocalOper
        extends AbstractRemoteToLocalSyncOper<IOneDriveService>
{
    private static final String TAG = "OnedriveRemoteToLocalOp";

    /** Constructor */
    public OnedriveRemoteToLocalOper(DbFile dbfile)
    {
        super(dbfile);
    }

    /** Perform the sync operation */
    @Override
    public void doOper(IOneDriveService providerClient,
                       Context ctx) throws RetrofitError, IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "syncRemoteToLocal %s", itsFile);
        setLocalFileName(SyncHelper.getLocalFileName(itsFile.itsId));

        OutputStream os = null;
        InputStream is = null;
        HttpURLConnection urlConn = null;
        try {
            Item item = providerClient.getItemByPath(itsFile.itsRemoteId, null);
            URL url = new URL(item.Content_downloadUrl);
            urlConn = (HttpURLConnection)url.openConnection();
            urlConn.setInstanceFollowRedirects(true);
            is = urlConn.getInputStream();

            os = new BufferedOutputStream(
                    ctx.openFileOutput(getLocalFileName(),
                                       Context.MODE_PRIVATE));

            Utils.copyStream(is, os);

            File localFile = ctx.getFileStreamPath(getLocalFileName());
            if (!localFile.setLastModified(itsFile.itsRemoteModDate)) {
                Log.e(TAG, "Can't set mod time on " + itsFile);
            }
            setDownloaded(true);
        } finally {
            try {
                Utils.closeStreams(is, os);
            } catch (IOException e) {
                Log.e(TAG, "Error closing", e);
            }

            if (urlConn != null) {
                urlConn.disconnect();
            }
        }
    }
}
