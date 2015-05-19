/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.dropbox;

import com.dropbox.sync.android.DbxFileInfo;
import com.jefftharris.passwdsafe.sync.lib.ProviderRemoteFile;

/**
 * Abstraction of a Dropbox remote file
 */
class DropboxProviderFile implements ProviderRemoteFile
{
    private final DbxFileInfo itsFile;

    /** Constructor */
    public DropboxProviderFile(DbxFileInfo file)
    {
        itsFile = file;
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer.ProviderRemoteFile#getRemoteId()
     */
    @Override
    public String getRemoteId()
    {
        return itsFile.path.toString();
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer.RemoteProviderFile#getTitle()
     */
    @Override
    public String getTitle()
    {
        return itsFile.path.getName();
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer.RemoteProviderFile#getFolder()
     */
    @Override
    public String getFolder()
    {
        return itsFile.path.getParent().toString();
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer.RemoteProviderFile#getModTime()
     */
    @Override
    public long getModTime()
    {
        return itsFile.modifiedTime.getTime();
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer.RemoteProviderFile#getHash()
     */
    @Override
    public String getHash()
    {
        return null;
    }
}
