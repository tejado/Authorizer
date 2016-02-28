/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.gdrive;

import com.google.api.services.drive.model.File;
import com.jefftharris.passwdsafe.sync.lib.ProviderRemoteFile;

/**
 * Abstraction of a Google Drive remote file
 */
public class GDriveProviderFile implements ProviderRemoteFile
{
    private final File itsFile;
    private final String itsFolder;

    /**
     * Constructor
     */
    public GDriveProviderFile(File file, String folder)
    {
        itsFile = file;
        itsFolder = folder;
    }

    @Override
    public String getRemoteId()
    {
        return itsFile.getId();
    }

    @Override
    public String getDisplayPath()
    {
        return itsFolder + "/" + getTitle();
    }

    @Override
    public String getTitle()
    {
        return itsFile.getName();
    }

    @Override
    public String getFolder()
    {
        return itsFolder;
    }

    @Override
    public long getModTime()
    {
        return itsFile.getModifiedTime().getValue();
    }

    @Override
    public String getHash()
    {
        return itsFile.getMd5Checksum();
    }

    @Override
    public boolean isFolder()
    {
        return GDriveSyncer.isFolderFile(itsFile);
    }

    @Override
    public String toDebugString()
    {
        return GDriveSyncer.fileToString(itsFile);
    }
}
