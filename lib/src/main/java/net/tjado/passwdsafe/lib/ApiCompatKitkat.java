/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.IBinder;
import android.view.inputmethod.InputMethodManager;

/**
 *  The ApiCompatKitkat class contains helper compatibility methods for Kitkat
 *  and higher
 */
@TargetApi(19)
@SuppressWarnings({"unchecked", "CanBeFinal"})
public final class ApiCompatKitkat
{
    private static Method itsTakePersistableUriPermissionMeth;
    private static Method itsReleasePersistableUriPermissionMeth;
    private static Method itsGetPersistedUriPermissionsMeth;
    private static Method itsDeleteDocumentMeth;
    private static Method itsUriPermissionsGetUriMeth;

    static {
        try {
            itsTakePersistableUriPermissionMeth =
                    ContentResolver.class.getMethod(
                            "takePersistableUriPermission",
                            Uri.class,
                            int.class);


            itsReleasePersistableUriPermissionMeth =
                    ContentResolver.class.getMethod(
                            "releasePersistableUriPermission",
                            Uri.class,
                            int.class);

            itsGetPersistedUriPermissionsMeth =
                    ContentResolver.class.getMethod(
                            "getPersistedUriPermissions");

            ClassLoader loader = ApiCompatKitkat.class.getClassLoader();
            Class docContractClass =
                    loader.loadClass("android.provider.DocumentsContract");

            itsDeleteDocumentMeth = docContractClass.getMethod(
                    "deleteDocument", ContentResolver.class, Uri.class);

            Class uriPermissionsClass =
                    loader.loadClass("android.content.UriPermission");
            itsUriPermissionsGetUriMeth =
                    uriPermissionsClass.getMethod("getUri");
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


    /** API compatible call for
     * ContentResolver.releasePersistableUriPermission */
    public static void releasePersistableUriPermission(ContentResolver cr,
                                                       Uri uri,
                                                       int flags)
    {
        try {
            itsReleasePersistableUriPermissionMeth.invoke(cr, uri, flags);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /** API compatible call for ContentResolver.getPersistedUriPermissions */
    public static List<Uri> getPersistedUriPermissions(ContentResolver cr)
    {
        try {
            List<Object> perms =
                    (List<Object>) itsGetPersistedUriPermissionsMeth.invoke(cr);

            List<Uri> uris = new ArrayList<>(perms.size());
            for (Object perm: perms) {
                uris.add((Uri)itsUriPermissionsGetUriMeth.invoke(perm));
            }
            return uris;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }


    /**
     * API compatible call for
     * InputMethodManager.shouldOfferSwitchingToNextInputMethod
     */
    public static boolean shouldOfferSwitchingToNextInputMethod(
            InputMethodManager imm,
            IBinder imeToken)
    {
        return imm.shouldOfferSwitchingToNextInputMethod(imeToken);
    }

    /**
     * API compatible call for
     * InputMethodManager.switchToNextInputMethod
     */
    public static boolean switchToNextInputMethod(InputMethodManager imm,
                                                  IBinder imeToken,
                                                  boolean onlyCurrentIme)
    {
        return imm.switchToNextInputMethod(imeToken, onlyCurrentIme);
    }
}
