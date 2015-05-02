/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.lib;

import java.lang.reflect.Method;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.net.Uri;

/**
 *  The ApiCompatKitkat class contains helper compatibility methods for Kitkat
 *  and higher
 */
@TargetApi(19)
@SuppressWarnings("unchecked")
public final class ApiCompatKitkat
{
    private static Method itsTakePersistableUriPermissionMeth;
    private static Method itsDeleteDocumentMeth;

    static {
        try {
            itsTakePersistableUriPermissionMeth =
                    ContentResolver.class.getMethod(
                            "takePersistableUriPermission",
                            Uri.class,
                            int.class);

            ClassLoader loader = ApiCompatKitkat.class.getClassLoader();
            Class docContractClass =
                    loader.loadClass("android.provider.DocumentsContract");

            itsDeleteDocumentMeth = docContractClass.getMethod(
                    "deleteDocument", ContentResolver.class, Uri.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    /** API compatible call for ContentResolver.takePersistableUriPermission */
    public static void takePersistableUriPermission(ContentResolver cr,
                                                    Uri uri,
                                                    int flags)
    {
        try {
            itsTakePersistableUriPermissionMeth.invoke(cr, uri, flags);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /** API compatible call for DocumentsContract.deleteDocument */
    public static boolean documentsContractDeleteDocument(ContentResolver cr,
                                                          Uri uri)
    {
        try {
            Object rc = itsDeleteDocumentMeth.invoke(null, cr, uri);
            return (Boolean)rc;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
