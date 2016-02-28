/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.onedrive;

import android.net.Uri;

import com.jefftharris.passwdsafe.sync.lib.ProviderRemoteFile;
import com.microsoft.onedriveaccess.model.Item;

/**
 *  Abstraction of an OneDrive remote file
 */
public class OnedriveProviderFile implements ProviderRemoteFile
{
    public static final String DRIVE_ROOT_PATH = "/drive/root:";

    private final Item itsItem;
    private final String itsRemoteId;
    private final String itsPath;

    /**
     * Constructor
     */
    public OnedriveProviderFile(Item item)
    {
        itsItem = item;
        Uri.Builder builder = new Uri.Builder();
        if (itsItem.ParentReference != null) {
            builder.encodedPath(
                    itsItem.ParentReference.Path.substring(
                            DRIVE_ROOT_PATH.length()));
        }
        builder.appendPath(itsItem.Name);
        Uri uri = builder.build();
        itsPath = uri.getPath();
        itsRemoteId = uri.getEncodedPath().toLowerCase();
    }

    /**
     * Get the file's remote identifier
     */
    @Override
    public String getRemoteId()
    {
        return itsRemoteId;
    }

    /**
     * Get the file's path for display
     */
    @Override
    public String getDisplayPath()
    {
        return itsPath;
    }

    /**
     * Get the file's title
     */
    @Override
    public String getTitle()
    {
        return itsItem.Name;
    }

    /**
     * Get the file's folder
     */
    @Override
    public String getFolder()
    {
        int pos = itsPath.lastIndexOf(PATH_SEPARATOR);
        if (pos >= 0) {
            return itsPath.substring(0, pos);
        } else {
            return itsPath;
        }
    }

    /**
     * Get the file's modification time
     */
    @Override
    public long getModTime()
    {
        return itsItem.LastModifiedDateTime.getTime();
    }

    /**
     * Get the file's hash code
     */
    @Override
    public String getHash()
    {
        return (itsItem.File != null) ?
                itsItem.File.Hashes.Sha1Hash : itsItem.ETag;
    }

    /**
     * Is the file a folder
     */
    @Override
    public boolean isFolder()
    {
        return (itsItem.Folder != null);
    }

    /**
     * Get a debugging string for the file
     */
    @Override
    public String toDebugString()
    {
        return String.format(
                "{name: %s, parent: %s, id: %s, folder: %b, remid: %s, mod: %s}",
                itsItem.Name,
                (itsItem.ParentReference != null) ?
                        itsItem.ParentReference.Path : "null",
                itsItem.Id, itsRemoteId,
                (itsItem.Folder != null), itsItem.LastModifiedDateTime);
    }
}
