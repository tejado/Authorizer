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
public final class ApiCompatKitkat
{
    private static Method itsTakePersistableUriPermissionMeth;

    static {
        try {
            itsTakePersistableUriPermissionMeth =
                    ContentResolver.class.getMethod(
                            "takePersistableUriPermission",
                            Uri.class,
                            int.class);
        } catch (NoSuchMethodException e) {
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
}
