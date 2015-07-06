/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.onedrive;

import com.jefftharris.passwdsafe.sync.lib.ProviderRemoteFile;
import com.microsoft.onedriveaccess.model.Item;

/**
 *  Abstraction of an OneDrive remote file
 */
public class OnedriveProviderFile implements ProviderRemoteFile
{
    public static final String DRIVE_ROOT_PATH = "/drive/root:";

    private final Item itsItem;
    private final String itsFolder;

    /**
     * Constructor
     */
    public OnedriveProviderFile(Item item)
    {
        itsItem = item;

        if (itsItem.ParentReference == null) {
            itsFolder = PATH_SEPARATOR;
        } else {
            itsFolder = itsItem.ParentReference.Path.substring(
                    DRIVE_ROOT_PATH.length());
        }
    }

    /**
     * Get the file's remote identifier
     */
    @Override
    public String getRemoteId()
    {
        return getPath().toLowerCase();
    }

    /**
     * Get the file's path for display
     */
    @Override
    public String getPath()
    {
        if (itsItem.ParentReference == null) {
            return PATH_SEPARATOR;
        } else {
            return itsFolder + "/" + itsItem.Name;
        }
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
        return itsFolder;
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
                "{name: %s, parent: %s, id: %s, folder: %b, mod: %s}",
                itsItem.Name,
                (itsItem.ParentReference != null) ?
                        itsItem.ParentReference.Path : "null",
                itsItem.Id,
                (itsItem.Folder != null), itsItem.LastModifiedDateTime);
    }
}
