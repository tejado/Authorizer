/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.box;

import com.box.boxjavalibv2.dao.BoxFile;
import com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer.ProviderRemoteFile;

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


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer.ProviderRemoteFile#getRemoteId()
     */
    @Override
    public String getRemoteId()
    {
        return itsFile.getId();
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer.RemoteProviderFile#getTitle()
     */
    @Override
    public String getTitle()
    {
        return itsFile.getName();
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer.RemoteProviderFile#getFolder()
     */
    @Override
    public String getFolder()
    {
        return BoxSyncer.getFileFolder(itsFile);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer.RemoteProviderFile#getModTime()
     */
    @Override
    public long getModTime()
    {
        return itsFile.dateModifiedAt().getTime();
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer.RemoteProviderFile#getHash()
     */
    @Override
    public String getHash()
    {
        return itsFile.getSha1();
    }
}