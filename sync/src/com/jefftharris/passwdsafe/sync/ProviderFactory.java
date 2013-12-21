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
import java.lang.reflect.Constructor;

import android.content.Context;

import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.lib.Utils;
//import com.jefftharris.passwdsafe.sync.dropbox.DropboxProvider;
import com.jefftharris.passwdsafe.sync.lib.Provider;

import dalvik.system.DexClassLoader;

/**
 * Factory for creating Providers
 */
public class ProviderFactory
{
    /** Get the provider implementation for the type */
    public static Provider getProvider(ProviderType type, Context ctx)
    {
        switch (type) {
        case GDRIVE: {
            return new GDriveProvider(ctx);
        }
        case DROPBOX: {
            try {
                return createDropboxPlugin(ctx);
            } catch (Exception e) {
                return null;
            }
            //return new DropboxProvider(ctx);
        }
        }
        return null;
    }

    private static ClassLoader itsDbxClassLoader;

    private static synchronized Provider createDropboxPlugin(Context ctx)
            throws Exception
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
