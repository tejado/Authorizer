/*
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * This interface is an abstraction of the storage mechanism.  The idea is that
 * each potential medium that the information could be stored (e.g. a file)
 * will have an associated provider implementation (e.g. PwsFileStorage).
 * <p/>
 * In many ways, this interface is a simplified combination of InputStream
 * and OutputStream.
 * <p/>
 * Note that all bytes handled by IO functions in this interface are
 * <b>already encrypted</b>.  This interface does not handle any unencrypted
 * data.
 *
 * @author mtiller
 */
public interface PwsStorage
{
    interface SaveHelper
    {
        String getSaveFileName(File file, boolean isV3);

        void createBackupFile(File fromFile, File toFile)
                throws IOException;
    }

    /**
     * Open the file for loading
     *
     * @return The header bytes
     * @throws IOException
     */
    byte[] openForLoad(int headerLen) throws IOException;

    /**
     * Grab all the bytes in the file
     *
     * @return The bytes in the file
     * @throws IOException
     */
    byte[] load() throws IOException;

    /**
     * Close the file after being loaded
     *
     * @throws IOException
     */
    void closeAfterLoad() throws IOException;

    /**
     * This method takes a series of bytes as input and then attempts
     * to save them to the underlying storage provider.  It returns
     * true if the save was successful and false otherwise.
     * <p/>
     * Note that this interface does not care what version or format the
     * file is.  That is handled at the PwSFile layer.
     * <p/>
     *
     * @param data The bytes making up the PasswordSafe file
     * @param isV3 Is the file version 3
     * @return true if save was successful
     */
    boolean save(byte[] data, boolean isV3);

    /**
     * Returns a human readable identifier of this storage that might be
     * presented to the user.
     */
    String getIdentifier();

    /**
     * @return null if the method is not supported
     */
    Date getModifiedDate();

    /**
     * Set the save helper
     */
    void setSaveHelper(SaveHelper helper);
}
