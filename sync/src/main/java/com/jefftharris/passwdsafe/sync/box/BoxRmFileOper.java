/*
 * Copyright (©) 2014-2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.box;

import android.content.Context;

import com.box.boxjavalibv2.BoxClient;
import com.box.boxjavalibv2.requests.requestobjects.BoxFileRequestObject;
import com.box.boxjavalibv2.resourcemanagers.BoxFilesManager;
import com.jefftharris.passwdsafe.sync.lib.AbstractRmSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;

/**
 * A Box sync operation to remove a file
 */
public class BoxRmFileOper extends AbstractRmSyncOper<BoxClient>
{
    private static final String TAG = "BoxRmFileOper";

    public BoxRmFileOper(DbFile dbfile)
    {
        super(dbfile, TAG);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractRmSyncOper#doRemoteRemove(java.lang.Object, android.content.Context)
     */
    @Override
    protected void doRemoteRemove(BoxClient providerClient, Context ctx)
            throws Exception
    {
        BoxFileRequestObject req =
                BoxFileRequestObject.deleteFileRequestObject();
        BoxFilesManager fileMgr = providerClient.getFilesManager();
        fileMgr.deleteFile(itsFile.itsRemoteId, req);
    }
}
