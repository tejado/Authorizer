/*
 * Copyright (©) 2014-2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.box;

import android.content.Context;

import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.models.BoxSession;
import com.jefftharris.passwdsafe.sync.lib.AbstractRmSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;

/**
 * A Box sync operation to remove a file
 */
public class BoxRmFileOper extends AbstractRmSyncOper<BoxSession>
{
    private static final String TAG = "BoxRmFileOper";

    public BoxRmFileOper(DbFile dbfile)
    {
        super(dbfile, TAG);
    }

    @Override
    protected void doRemoteRemove(BoxSession providerClient, Context ctx)
            throws Exception
    {
        BoxApiFile fileApi = new BoxApiFile(providerClient);
        fileApi.getDeleteRequest(itsFile.itsRemoteId)
                .send();
    }
}
