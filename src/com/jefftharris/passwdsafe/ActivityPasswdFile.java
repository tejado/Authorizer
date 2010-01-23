/*
 * Copyright (©) 2009-2010 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */

package com.jefftharris.passwdsafe;

import android.app.Activity;

/**
 * The ActivityPasswdFile interface provides access to the password file data
 * for an application.
 *
 * @author Jeff Harris
 */
public abstract class ActivityPasswdFile
{
    /// The file data
    PasswdFileData itsFileData;

    /// The activity
    Activity itsActivity;

    /// Constructor
    public ActivityPasswdFile(PasswdFileData fileData,
                              Activity activity)
    {
        itsFileData = fileData;
        itsActivity = activity;

        touch();
    }

    /**
     * @return the fileData
     */
    public final PasswdFileData getFileData()
    {
        touch();
        return itsFileData;
    }

    public final boolean isOpen()
    {
        return (itsFileData != null);
    }

    public final void setFileData(PasswdFileData fileData)
    {
        doSetFileData(fileData);
        itsFileData = fileData;
    }

    public abstract void touch();

    public final void close()
    {
        doClose();
        itsFileData = null;
    }

    protected abstract void doSetFileData(PasswdFileData fileData);
    protected abstract void doClose();
}