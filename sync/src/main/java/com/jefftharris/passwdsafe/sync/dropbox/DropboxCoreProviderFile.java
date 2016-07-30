/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.dropbox;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.Metadata;
import com.jefftharris.passwdsafe.sync.lib.ProviderRemoteFile;

/**
 *  Abstraction of a Dropbox remote file
 */
public class DropboxCoreProviderFile implements ProviderRemoteFile
{
    private final Metadata itsFile;
    private final String itsRemoteId;
    private final String itsFolder;

    /** Constructor */
    public DropboxCoreProviderFile(Metadata file)
    {
        itsFile = file;
        itsRemoteId = itsFile.getPathLower();
        int lastSlash = itsRemoteId.lastIndexOf('/');
        itsFolder = (lastSlash >= 0) ? itsRemoteId.substring(0, lastSlash) : "";
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
        return itsFile.getPathDisplay();
    }

    /**
     * Get the file's title
     */
    @Override
    public String getTitle()
    {
        return itsFile.getName();
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
        return isFolder() ?
               0 : ((FileMetadata)itsFile).getServerModified().getTime();
    }

    /**
     * Get the file's hash code
     */
    @Override
    public String getHash()
    {
        return isFolder() ? "0" : ((FileMetadata)itsFile).getRev();
    }

    /**
     * Is the file a folder
     */
    @Override
    public boolean isFolder()
    {
        return itsFile instanceof FolderMetadata;
    }

    /**
     * Get a debugging string for the file
     */
    @Override
    public String toDebugString()
    {
        return entryToString(itsFile);
    }

    /** Create a string form of a file entry */
    public static String entryToString(Metadata entry)
    {
        if (entry == null) {
            return "{null}";
        }
        boolean isDir;
        boolean deleted;
        String rev;
        String modified;
        String clientModified;
        if (entry instanceof FileMetadata) {
            FileMetadata file = (FileMetadata)entry;
            isDir = false;
            deleted = false;
            rev = file.getRev();
            modified = file.getServerModified().toString();
            clientModified = file.getClientModified().toString();
        } else if (entry instanceof FolderMetadata) {
            isDir = true;
            deleted = false;
            rev = null;
            modified = null;
            clientModified = null;
        } else {
            isDir = false;
            deleted = true;
            rev = null;
            modified = null;
            clientModified = null;
        }
        return String.format(
                "{path: %s, rev: %s, dir: %b, modified: %s, client mod: %s, " +
                "deleted: %b}",
                entry.getPathLower(), rev, isDir, modified, clientModified,
                deleted);
    }
}
