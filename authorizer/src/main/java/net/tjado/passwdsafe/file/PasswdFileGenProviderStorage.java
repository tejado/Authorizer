/*
 * Copyright (©) 2017 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.file;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.Utils;

import org.pwsafe.lib.file.PwsStreamStorage;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** A PwsStreamStorage implementation for a generic provider */
public class PasswdFileGenProviderStorage extends PwsStreamStorage
{
    private final Uri itsUri;

    private static final String TAG = "PasswdFileGenProviderSt";

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
                PasswdFileSaveHelper helper =
                        (PasswdFileSaveHelper)getSaveHelper();
                Context ctx = helper.getContext();

                helper.createBackup(itsUri, getIdentifier());

                var cr = ctx.getContentResolver();
                try {
                    String mode = "wt";
                    String host = itsUri.getHost();
                    if ((host != null) &&
                        host.startsWith("com.box.android.documents")) {
                        mode = "w";
                    }

                    pfd = cr.openFileDescriptor(itsUri, mode);
                } catch (Exception e) {
                    Log.w(TAG, "Error opening for truncate", e);
                    pfd = cr.openFileDescriptor(itsUri, "w");
                }

                if (pfd == null) {
                    throw new IOException(itsUri.toString());
                }
                fos = new FileOutputStream(pfd.getFileDescriptor());
                try {
                    fos.getChannel().truncate(0);
                } catch (Exception e) {
                    Log.w(TAG, "Error truncating file", e);
                }

                fos.write(data);

                PasswdSafeUtil.dbginfo(TAG, "GenProviderStorage update %s",
                                       itsUri);
                return true;
            } finally {
                Utils.closeStreams(fos, pfd);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving " + itsUri, e);
            return false;
        }
    }
}
