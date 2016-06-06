/*
 * Copyright (©) 2013-2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.gdrive;

import java.io.IOException;

import android.content.Context;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.jefftharris.passwdsafe.sync.lib.AbstractRmSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;

/**
 * A Google Drive sync operation to remove a file
 */
public class GDriveRmFileOper extends AbstractRmSyncOper<Drive>
{
    private static final String TAG = "GDriveRmFileOper";

    /** Constructor */
    public GDriveRmFileOper(DbFile file)
    {
        super(file, TAG);
    }

    @Override
    protected void doRemoteRemove(Drive providerClient, Context ctx)
            throws IOException
    {
        File updatedFile = new File();
        updatedFile.setTrashed(true);
        providerClient.files()
                      .update(itsFile.itsRemoteId, updatedFile)
                      .execute();
    }
}
