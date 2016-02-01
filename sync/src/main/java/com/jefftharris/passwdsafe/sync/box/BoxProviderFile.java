/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.box;

import com.box.androidsdk.content.models.BoxFile;
import com.jefftharris.passwdsafe.sync.lib.ProviderRemoteFile;

/**
 * Abstraction of a Box remote file
 */
class BoxProviderFile implements ProviderRemoteFile
{
    private final BoxFile itsFile;

    /** Constructor */
    public BoxProviderFile(BoxFile file)
    {
        itsFile = file;
    }


    @Override
    public String getRemoteId()
    {
        return itsFile.getId();
    }

    @Override
    public String getPath()
    {
        return getFolder() + PATH_SEPARATOR + getTitle();
    }

    @Override
    public String getTitle()
    {
        return itsFile.getName();
    }

    @Override
    public String getFolder()
    {
        return BoxSyncer.getFileFolder(itsFile);
    }

    @Override
    public long getModTime()
    {
        return itsFile.getModifiedAt().getTime();
    }

    @Override
    public String getHash()
    {
        return itsFile.getSha1();
    }

    /**
     * Is the file a folder
     */
    @Override
    public boolean isFolder()
    {
        return false;
    }

    /**
     * Get a debugging string for the file
     */
    @Override
    public String toDebugString()
    {
        return null;
    }
}
