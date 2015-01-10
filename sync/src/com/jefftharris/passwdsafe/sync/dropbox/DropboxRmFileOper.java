/*
 * Copyright (©) 2013-2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.dropbox;

import java.io.IOException;

import android.content.Context;

import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.jefftharris.passwdsafe.sync.lib.AbstractRmSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;

/**
 *  A Dropbox sync operation to remove a file
 */
public class DropboxRmFileOper extends AbstractRmSyncOper<DbxFileSystem>
{
    private static final String TAG = "DropboxRmFileOper";

    /** Constructor */
    protected DropboxRmFileOper(DbFile file)
    {
        super(file, TAG);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractRmSyncOper#doRemoteRemove(java.lang.Object, android.content.Context)
     */
    @Override
    protected void doRemoteRemove(DbxFileSystem providerClient, Context ctx)
            throws IOException
    {
        DbxPath path = new DbxPath(itsFile.itsRemoteId);
        if (providerClient.exists(path)) {
            providerClient.delete(path);
        }
    }
}
