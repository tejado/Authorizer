/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import java.io.IOException;

import android.content.Context;

import com.dropbox.sync.android.DbxFileSystem;

/**
 *  A Dropbox sync operation
 */
public abstract class DropboxSyncOper extends SyncOper
{
    /** Constructor */
    protected DropboxSyncOper(SyncDb.DbFile file)
    {
        super(file);
    }

    /** Perform the sync operation */
    public abstract void doOper(DbxFileSystem fs, Context ctx)
            throws IOException;
}
