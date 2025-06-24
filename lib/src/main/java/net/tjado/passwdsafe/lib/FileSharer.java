/*
 * Copyright (©) 2017 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * The FileSharer class provides a method for sharing files with other apps
 */
public final class FileSharer
{
    private final String itsPkgName;
    private final File itsFile;

    /**
     * Constructor
     */
    public FileSharer(String fileName, Context ctx, String pkgName)
            throws IOException
    {
        itsPkgName = pkgName;

        File shareDir = new File(ctx.getCacheDir(), "shared-tmpfiles");
        if (!shareDir.isDirectory() && !shareDir.mkdirs()) {
            throw new IOException("Error creating: " + shareDir);
        }

        itsFile = new File(shareDir, fileName);
        if (itsFile.exists() && !itsFile.delete()) {
            throw new IOException("Error deleting: " + itsFile);
        }
    }

    /**
     * Get the file being shared
     */
    public File getFile()
    {
        return itsFile;
    }

    /**
     * Share the file
     */
    @SuppressLint("ObsoleteSdkInt")
    @SuppressWarnings("RedundantThrows")
    public void share(String chooserMsg,
                      String contentType,
                      String[] emailAddrs,
                      String subject,
                      Activity act)
            throws Exception
    {
        Uri fileUri = FileProvider.getUriForFile(act,
                                                 itsPkgName + ".fileprovider",
                                                 itsFile);
        Intent sendIntent = ShareCompat.IntentBuilder
                .from(act)
                .setStream(fileUri)
                .setType(contentType)
                .setEmailTo(emailAddrs)
                .setSubject(subject)
                .getIntent()
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Workaround for Android bug.
        // grantUriPermission also needed for KITKAT,
        // see https://code.google.com/p/android/issues/detail?id=76683
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            List<ResolveInfo> resInfoList =
                    act.getPackageManager().queryIntentActivities(
                            sendIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                act.grantUriPermission(
                        resolveInfo.activityInfo.packageName, fileUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        }

        act.startActivity(Intent.createChooser(sendIntent, chooserMsg));
    }
}
