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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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

	private BufferedInputStream itsLoadStream;

	private byte[] itsLoadBytes;

	/*
	 * Build an implementation given the filename for the underlying storage.
	 */
	public PwsFileStorage(String filename) throws IOException {
		this.filename = filename;
	}

	public byte[] openForLoad(int headerLen) throws IOException
	{
	    itsLoadStream =
	        new BufferedInputStream(new FileInputStream(filename));

	    itsLoadStream.mark(headerLen);
	    byte[] bytes = new byte[headerLen];
	    int offset = 0;
	    int numread = 0;
	    while (offset < bytes.length) {
	        numread = itsLoadStream.read(bytes, offset,
	                                     bytes.length - offset);
	        if (numread < 0) {
	            throw new IOException("Error reading header from " +
	                                  filename);
	        }
	        offset += numread;
	    }
	    itsLoadStream.reset();
	    return bytes;
	}

	public byte[] load() throws IOException
	{
	    if (itsLoadBytes == null) {
	        ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        byte[] buf = new byte[1024];
	        int numread = 0;

	        for (;;) {
	            numread = itsLoadStream.read(buf);
	            if (numread > 0) {
	                bos.write(buf, 0, numread);
	            } else if (numread < 0) {
	                break;
	            }
	        }
	        closeAfterLoad();
	        itsLoadBytes = bos.toByteArray();
	    }
	    return itsLoadBytes;
	}

	public void closeAfterLoad() throws IOException
	{
	    itsLoadBytes = null;
	    if (itsLoadStream != null) {
	        try {
	            itsLoadStream.close();
	        } finally {
	            itsLoadStream = null;
	        }
	    }
	}

	/**
	 * Takes the (encrypted) bytes and writes them out to the file.
	 *
	 * This particular method takes steps to make sure that the
	 * original file is not overwritten or deleted until the
	 * new file has been successfully saved.
	 */
	public boolean save(byte[] data, boolean isV3) {
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
			File fromFile = new File(FilePath, file.getName());
			File toFile = new File(FilePath,
			                       getSaveFileName(file, isV3));

			File tempFile = null;
			try {
			    tempFile = File.createTempFile("pwsafe", null,
			                                   FilePath);
			    writeFile(tempFile, data);

                            createBackupFile(fromFile, toFile);

			    if (tempFile.renameTo(toFile)) {
			        LOG.debug1("Temp file renamed to " + toFile);
			        tempFile = null;
			    } else {
			        throw new IOException("Error renaming " +
			                              tempFile + " to " +
			                              toFile);
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

	private String getSaveFileName(File file, boolean isV3)
	{
	    if (itsSaveHelper != null) {
	        return itsSaveHelper.getSaveFileName(file, isV3);
	    }
	    return file.getName();
	}

	private void createBackupFile(File fromFile, File toFile)
	    throws IOException
	{
	    if (itsSaveHelper != null) {
	        itsSaveHelper.createBackupFile(fromFile, toFile);
	    } else {
	        File bakFile = new File(toFile.getParentFile(),
	                                toFile.getName() + "~");

	        if (bakFile.exists()) {
	            if (!bakFile.delete()) {
	                throw new IOException("Can not delete backup file: " +
	                                      bakFile);
	            }
	        }

	        if (toFile.exists()) {
	            if (!toFile.renameTo(bakFile)) {
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
