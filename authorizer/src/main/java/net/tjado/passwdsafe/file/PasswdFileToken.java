/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.file;

import androidx.annotation.Nullable;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Token for users of a password file to enforce synchronous access
 */
public class PasswdFileToken
{
    private static final ReentrantLock itsLock = new ReentrantLock();
    
    private final PasswdFileData itsFileData;

    /**
     * Constructor. The token is acquired.
     */
    public PasswdFileToken(@Nullable PasswdFileData fileData)
    {
        // Don't allow reentrant behavior
        if (itsLock.isHeldByCurrentThread()) {
            throw new AssertionError("PasswdFileToken lock held");
        }
        itsLock.lock();
        itsFileData = fileData;
    }

    /**
     * Get the password file data
     */
    public @Nullable PasswdFileData getFileData()
    {
        return itsFileData;
    }

    /**
     * Release the token
     */
    public void release()
    {
        itsLock.unlock();
    }
}
