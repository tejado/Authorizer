/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.owncloud;

import android.content.Context;

import com.jefftharris.passwdsafe.sync.lib.AbstractRmSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.RemoveRemoteFileOperation;

/**
 * An ownCloud sync operation to remove a file
 */
public class OwncloudRmFileOper extends AbstractRmSyncOper<OwnCloudClient>
{
    private static final String TAG = "OwncloudRmFileOper";

    public OwncloudRmFileOper(DbFile dbfile)
    {
        super(dbfile, TAG);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractRmSyncOper#doRemoteRemove(java.lang.Object, android.content.Context)
     */
    @Override
    protected void doRemoteRemove(OwnCloudClient providerClient, Context ctx)
            throws Exception
    {
        RemoveRemoteFileOperation oper =
                new RemoveRemoteFileOperation(itsFile.itsRemoteId);
        RemoteOperationResult res = oper.execute(providerClient);
        OwncloudSyncer.checkOperationResult(res);
    }
}
