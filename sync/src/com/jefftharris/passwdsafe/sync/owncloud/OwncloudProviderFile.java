/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.owncloud;

import java.io.File;
import java.util.Locale;

import android.text.TextUtils;

import com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer.ProviderRemoteFile;
import com.owncloud.android.lib.resources.files.RemoteFile;

/**
 * Abstraction of an ownCloud remote file
 */
class OwncloudProviderFile implements ProviderRemoteFile
{
    private final RemoteFile itsFile;
    private final String itsTitle;
    private final String itsFolder;

    /** Constructor */
    public OwncloudProviderFile(RemoteFile file)
    {
        itsFile = file;
        File f = new File(file.getRemotePath());
        itsTitle = f.getName();
        itsFolder = f.getParent();
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer.ProviderRemoteFile#getRemoteId()
     */
    @Override
    public String getRemoteId()
    {
        return itsFile.getRemotePath();
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer.RemoteProviderFile#getTitle()
     */
    @Override
    public String getTitle()
    {
        return itsTitle;
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer.RemoteProviderFile#getFolder()
     */
    @Override
    public String getFolder()
    {
        return itsFolder;
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer.RemoteProviderFile#getModTime()
     */
    @Override
    public long getModTime()
    {
        return itsFile.getModifiedTimestamp();
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer.RemoteProviderFile#getHash()
     */
    @Override
    public String getHash()
    {
        return itsFile.getEtag();
    }

    /** Get the remote file */
    public final RemoteFile getRemoteFile()
    {
        return itsFile;
    }

    /** Get a string form for a remote file */
    public static String fileToString(RemoteFile file)
    {
        if (file == null) {
            return "{null}";
        }
        return String.format(Locale.US,
                             "{id: %s, path:%s, mime:%s, hash:%s}",
                             file.getRemoteId(), file.getRemotePath(),
                             file.getMimeType(), file.getEtag());
    }

    /** Is a file a folder */
    public static boolean isFolder(RemoteFile file)
    {
        return TextUtils.equals(file.getMimeType(), "DIR");
    }

    /** Is a file a password file */
    public static boolean isPasswordFile(RemoteFile file)
    {
        return !isFolder(file) && file.getRemotePath().endsWith(".psafe3");
    }
}
