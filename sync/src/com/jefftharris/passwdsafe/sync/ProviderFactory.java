/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;

import android.content.Context;

import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.lib.Utils;
import com.jefftharris.passwdsafe.sync.lib.Provider;

import dalvik.system.DexClassLoader;

/**
 * Factory for creating Providers
 */
public class ProviderFactory
{
    private static EnumMap<ProviderType, Provider> itsProviders =
            new EnumMap<ProviderType, Provider>(ProviderType.class);
    private static ClassLoader itsDbxClassLoader;

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
                try {
                    provider = createDropboxPlugin(appCtx);
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Failed to create Dropbox plugin", e);
                }
                break;
            }
            }

            itsProviders.put(type, provider);
        }
        return provider;
    }

    private static Provider createDropboxPlugin(Context ctx)
            throws IOException, ClassNotFoundException, NoSuchMethodException,
            IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException
    {
        // TODO: optimize copy and dex optimization
        if (itsDbxClassLoader == null) {
            String dbxClassesDex = "dropbox-classes.dex";
            File dexInternal = new File(ctx.getDir("dex", Context.MODE_PRIVATE),
                                        dbxClassesDex);
            BufferedInputStream bis = null;
            BufferedOutputStream bos = null;
            try {
                bis = new BufferedInputStream(ctx.getAssets().open(dbxClassesDex));
                bos = new BufferedOutputStream(new FileOutputStream(dexInternal));
                Utils.copyStream(bis, bos);
            } finally {
                Utils.closeStreams(bis, bos);
            }

            File optimizedDir = ctx.getDir("outdex", Context.MODE_PRIVATE);
            itsDbxClassLoader =
                    new DexClassLoader(dexInternal.getAbsolutePath(),
                                       optimizedDir.getAbsolutePath(),
                                       null, ctx.getClassLoader());
        }
        Class<?> dbxClass = itsDbxClassLoader.loadClass(
                "com.jefftharris.passwdsafe.sync.dropbox.DropboxProvider");
        @SuppressWarnings("unchecked")
        Constructor<Provider> ctor = (Constructor<Provider>)
                dbxClass.getDeclaredConstructor(Context.class);
        return ctor.newInstance(ctx);
    }
}
