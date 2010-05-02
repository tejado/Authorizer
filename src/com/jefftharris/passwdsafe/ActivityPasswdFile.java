/*
 * Copyright (©) 2009-2010 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */

package com.jefftharris.passwdsafe;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ConcurrentModificationException;

/**
 * The ActivityPasswdFile interface provides access to the password file data
 * for an application.
 *
 * @author Jeff Harris
 */
public interface ActivityPasswdFile
{
    /**
     * @return the fileData
     */
    public PasswdFileData getFileData();

    public boolean isOpen();

    public void setFileData(PasswdFileData fileData);

    /**
     * Save the file.  Will likely be called in a background thread.
     * @throws IOException
     * @throws ConcurrentModificationException
     * @throws NoSuchAlgorithmException
     */
    public void save()
        throws NoSuchAlgorithmException, ConcurrentModificationException,
               IOException;

    public void touch();

    public void close();

    public void pauseFileTimer();
    public void resumeFileTimer();
}