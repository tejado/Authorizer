/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import java.io.IOException;

import android.content.Context;

/**
 * Abstract sync operation using a provider client
 */
public abstract class AbstractSyncOper<ProviderClientT>
        extends SyncOper
{
    /** Constructor */
    protected AbstractSyncOper(DbFile file)
    {
        super(file);
    }

    /** Perform the sync operation */
    public abstract void doOper(ProviderClientT providerClient, Context ctx)
            throws IOException;
}
