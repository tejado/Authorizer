/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.dropbox;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.RESTUtility;
import com.jefftharris.passwdsafe.sync.lib.ProviderRemoteFile;

/**
 *  Abstraction of a Dropbox remote file
 */
public class DropboxCoreProviderFile implements ProviderRemoteFile
{
    private final DropboxAPI.Entry itsFile;

    /** Constructor */
    public DropboxCoreProviderFile(DropboxAPI.Entry file)
    {
        itsFile = file;
    }

    /**
     * Get the file's remote identifier
     */
    @Override
    public String getRemoteId()
    {
        return itsFile.path;
    }

    /**
     * Get the file's title
     */
    @Override
    public String getTitle()
    {
        return itsFile.fileName();
    }

    /**
     * Get the file's folder
     */
    @Override
    public String getFolder()
    {
        return itsFile.parentPath();
    }

    /**
     * Get the file's modification time
     */
    @Override
    public long getModTime()
    {
        return RESTUtility.parseDate(itsFile.modified).getTime();
    }

    /**
     * Get the file's hash code
     */
    @Override
    public String getHash()
    {
        return isFolder() ? itsFile.hash : itsFile.rev;
    }

    /**
     * Is the file a folder
     */
    @Override
    public boolean isFolder()
    {
        return itsFile.isDir;
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
    public static String entryToString(DropboxAPI.Entry entry)
    {
        if (entry == null) {
            return "{null}";
        }
        return String.format(
                "{path: %s, hash: %s, rev: %s, dir: %b, modified: %s, " +
                "mime: %s, deleted: %b}",
                entry.path, entry.hash, entry.rev, entry.isDir, entry.modified,
                entry.mimeType, entry.isDeleted);
    }
}
