/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.file;

import java.io.FileOutputStream;
import java.io.InputStream;

import org.pwsafe.lib.file.PwsStreamStorage;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

/** A PwsStreamStorage implementation for a generic provider */
public class PasswdFileGenProviderStorage extends PwsStreamStorage
{
    private static final String TAG = "PasswdFileGenProviderStorage";
    private final Uri itsUri;

    /** Constructor */
    public PasswdFileGenProviderStorage(Uri uri, String id, InputStream stream)
    {
        super(id, stream);
        itsUri = uri;
    }

    /** Save the file contents */
    @Override
    public boolean save(byte[] data, boolean isV3)
    {
        ParcelFileDescriptor pfd = null;
        FileOutputStream fos = null;
        try {
            try {
                PasswdFileUri.SaveHelper helper =
                        (PasswdFileUri.SaveHelper)getSaveHelper();
                Context ctx = helper.getContext();

                pfd = ctx.getContentResolver().openFileDescriptor(itsUri, "w");
                fos = new FileOutputStream(pfd.getFileDescriptor());
                fos.write(data);

                PasswdSafeUtil.dbginfo(TAG, "GenProviderStorage update %s",
                                       itsUri);
                return true;
            } finally {
                if (fos != null) {
                    fos.close();
                }
                if (pfd != null) {
                    pfd.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving " + itsUri, e);
            return false;
        }
    }
}