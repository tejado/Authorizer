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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.pwsafe.lib.Log;

/**
 * An implementation of the PwsStorage class that reads and writes to files.
 * @author mtiller
 *
 */
public class PwsFileStorage implements PwsStorage {

	/**
	 * Default file extension of the password safe file.
	 */
	public static final String FILE_EXTENSION = ".psafe3";

	/**
	 * An object for logging activity in this class.
	 */
	private static final Log LOG = Log.getInstance(PwsFileStorage.class.getPackage().getName());

	/** The filename used for storage */
	private final String filename;

	private SaveHelper itsSaveHelper;

	/*
	 * Build an implementation given the filename for the underlying storage.
	 */
	public PwsFileStorage(String filename) throws IOException {
		this.filename = filename;
	}

	/** Grab all the bytes in the file */
	public byte[] load() throws IOException {
		File file = new File(filename);
        InputStream is = new BufferedInputStream(new FileInputStream(file));

        // Get the size of the file
        long length = file.length();

        if (length > Integer.MAX_VALUE) {
            // File is too large
        }

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int)length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
               && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file "+file.getName());
        }

        // Close the input stream and return bytes
        is.close();
        return bytes;
	}

	/**
	 * Takes the (encrypted) bytes and writes them out to the file.
	 *
	 * This particular method takes steps to make sure that the
	 * original file is not overwritten or deleted until the
	 * new file has been successfully saved.
	 */
	public boolean save(byte[] data) {
		try {
			LOG.debug1("Number of bytes to save = "+data.length);
			LOG.debug1("Original file: "+filename);

			File file = new File( filename );
			if (!file.exists()) {
				/* Original file doesn't exist, just go ahead and write it
				 * (no backup, temp files needed).
				 */
			        writeFile(file, data);
				return true;
			}
			LOG.debug1("Original file path: "+file.getAbsolutePath());
			File dir = file.getCanonicalFile().getParentFile();
			if (dir==null) {
				LOG.error("Couldn't find the parent directory for: "+file.getAbsolutePath());
				return false;
			}
			File FilePath = dir.getAbsoluteFile();
			String FileName	= file.getName();

			File oldFile = new File( FilePath, FileName );

			File tempFile = null;
			try {
			    tempFile = File.createTempFile("pwsafe", null,
			                                   FilePath);
			    writeFile(tempFile, data);

                            createBackupFile(oldFile);

			    if (tempFile.renameTo(oldFile)) {
			        LOG.debug1("Temp file renamed to " + oldFile);
			        tempFile = null;
			    } else {
			        throw new IOException("Error renaming " +
			                              tempFile + " to " +
			                              oldFile);
			    }
			} finally {
			    if (tempFile != null) {
			        tempFile.delete();
			    }
			}

			return true;
		} catch (Exception e) {
			LOG.error(e.getMessage());
			return false;
		}
	}

	/**
	 * This method is *not* part of the storage interface but specific to
	 * this particular implementation.
	 *
	 * @return Name of the file used for storage.
	 */
	public String getFilename() { return filename; }

	public void setPassphrase(String passphrase) {
		/* Do nothing since there is no additional encrypted information associated
		 * with this storage mechanism
		 */
	}

	public String getIdentifier() {
		return this.filename;
	}


	public Date getModifiedDate() {
		File file = new File(filename);
		Date modified = null;
		if (file.exists())
			modified = new Date(file.lastModified());
		return modified;
	}

	public void setSaveHelper(SaveHelper helper)
	{
	    itsSaveHelper = helper;
	}

	private void createBackupFile(File file) throws IOException
	{
	    if (itsSaveHelper != null) {
	        itsSaveHelper.createBackupFile(file);
	    } else {
	        File bakFile = new File(file.getParentFile(),
	                                file.getName() + "~");

	        if (bakFile.exists()) {
	            if (!bakFile.delete()) {
	                throw new IOException("Can not delete backup file: " +
	                                      bakFile);
	            }
	        }

	        if (file.exists()) {
	            if (!file.renameTo(bakFile)) {
	                throw new IOException("Can not create backup file: " +
	                                      bakFile);
	            }
	        }
	    }
	}

	private void writeFile(File file, byte[] data) throws IOException
	{
            FileOutputStream outStream = new FileOutputStream(file);
            try {
                outStream.write(data);
                outStream.getFD().sync();
            } catch (IOException e) {
                try {
                    outStream.close();
                } catch (IOException e2) {
                    LOG.error(e2.getMessage());
                }
                outStream = null;
                throw e;
            } finally {
                if (outStream != null) {
                    outStream.close();
                }
            }
	}
}
