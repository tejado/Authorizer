/*
 * Copyright (©) 2022 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The ApiCompatQ class contains helper methods that are usable on
 * Q and higher
 */
@TargetApi(Build.VERSION_CODES.Q)
public final class ApiCompatQ
{
    /**
     * API compatible call to get the root URI for the primary storage volume
     */
    public static @Nullable
    Uri getPrimaryStorageRootUri(@NonNull Context ctx)
    {
        StorageManager smgr = (StorageManager)
                ctx.getSystemService(Context.STORAGE_SERVICE);
        if (smgr == null) {
            return null;
        }
        StorageVolume vol = smgr.getPrimaryStorageVolume();
        Intent primVolIntent = vol.createOpenDocumentTreeIntent();
        return primVolIntent.getParcelableExtra(
                DocumentsContract.EXTRA_INITIAL_URI);
    }
}
