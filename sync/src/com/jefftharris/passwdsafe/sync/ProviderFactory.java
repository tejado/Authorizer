/*
 * Copyright (©) 2013-2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import java.util.EnumMap;

import android.content.Context;

import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.sync.box.BoxProvider;
import com.jefftharris.passwdsafe.sync.dropbox.DropboxProvider;
import com.jefftharris.passwdsafe.sync.gdrive.GDriveProvider;
import com.jefftharris.passwdsafe.sync.lib.Provider;

/**
 * Factory for creating Providers
 */
public class ProviderFactory
{
    private static EnumMap<ProviderType, Provider> itsProviders =
            new EnumMap<ProviderType, Provider>(ProviderType.class);

    /** Get the provider implementation for the type */
    public static synchronized Provider getProvider(ProviderType type,
                                                    Context ctx)
    {
        Provider provider = itsProviders.get(type);
        if (provider == null) {
            Context appCtx = ctx.getApplicationContext();
            switch (type) {
            case GDRIVE: {
                provider = new GDriveProvider(appCtx);
                break;
            }
            case DROPBOX: {
                provider = new DropboxProvider(appCtx);
                break;
            }
            case BOX: {
                provider = new BoxProvider(appCtx);
                break;
            }
            }

            itsProviders.put(type, provider);
        }
        return provider;
    }
}
