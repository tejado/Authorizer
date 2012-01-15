/*
 * $Id:$
 *
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
 *
 * In many ways, this interface is a simplified combination of InputStream
 * and OutputStream.
 *
 * Note that all bytes handled by IO functions in this interface are
 * <b>already encrypted</b>.  This interface does not handle any unencrypted
 * data.
 *
 * @author mtiller
 *
 */
public interface PwsStorage {

        public interface SaveHelper
        {
            public String getSaveFileName(File file, boolean isV3);

            public void createBackupFile(File fromFile, File toFile)
                throws IOException;
        }

        /**
         * Open the file for loading
         * @return The header bytes
         * @throws IOException
         */
        public byte[] openForLoad(int headerLen) throws IOException;

        /**
         * Grab all the bytes in the file
         * @return The bytes in the file
         * @throws IOException
         */
	public byte[] load() throws IOException;

	/**
	 * Close the file after being loaded
	 * @throws IOException
	 */
	public void closeAfterLoad() throws IOException;

	/**
	 * This method takes a series of bytes as input and then attempts
	 * to save them to the underlying storage provider.  It returns
	 * true if the save was successful and false otherwise.
	 *
	 * Note that this interface does not care what version or format the
	 * file is.  That is handled at the PwSFile layer.
	 *
	 * TODOlib Should this throw an exception instead?
	 *
	 * @param data The bytes making up the PasswordSafe file
	 * @param isV3 Is the file version 3
	 * @return true if save was successful
	 * @throws IOException
	 */
	public boolean save(byte[] data, boolean isV3);

	/**
	 * Returns a human readable identifier of this storage that might be presented
	 * to the user.
	 */
	public String getIdentifier();

	/**
	 *
	 * @return the modification date
	 * @return null if the method is not supported
	 */
	public Date getModifiedDate ();

	public void setSaveHelper(SaveHelper helper);
}
