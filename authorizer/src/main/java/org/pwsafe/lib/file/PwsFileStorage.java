/*
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import org.pwsafe.lib.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

/**
 * An implementation of the PwsStorage class that reads and writes to files.
 *
 * @author mtiller
 */
public class PwsFileStorage extends PwsStreamStorage
{

    /**
     * An object for logging activity in this class.
     */
    private static final Log LOG = Log
            .getInstance(PwsFileStorage.class.getPackage().getName());

    /*
     * Build an implementation given the filename for the underlying storage.
     */
    public PwsFileStorage(String identifier, String fileToOpen)
            throws IOException
    {
        super(identifier,
              (fileToOpen == null) ? null : new FileInputStream(fileToOpen));
    }

    /**
     * Takes the (encrypted) bytes and writes them out to the file.
     * <p/>
     * This particular method takes steps to make sure that the
     * original file is not overwritten or deleted until the
     * new file has been successfully saved.
     */
    @Override
    public boolean save(byte[] data, boolean isV3)
    {
        try {
            File file = new File(getIdentifier());
            if (!file.exists()) {
                /* Original file doesn't exist, just go ahead and write it
                 * (no backup, temp files needed).
                 */
                writeFile(file, data);
                return true;
            }
            File dir = file.getCanonicalFile().getParentFile();
            if (dir == null) {
                LOG.error("Couldn't find the parent directory for: " +
                          file.getAbsolutePath());
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
                    tempFile = null;
                } else {
                    throw new IOException("Error renaming " + tempFile +
                                          " to " + toFile);
                }
            } finally {
                if ((tempFile != null) && !tempFile.delete()) {
                    LOG.error("Error deleting temp file");
                }
            }

            return true;
        } catch (Exception e) {
            LOG.error(e.getMessage());
            return false;
        }
    }

    @Override
    public Date getModifiedDate()
    {
        File file = new File(getIdentifier());
        Date modified = null;
        if (file.exists())
            modified = new Date(file.lastModified());
        return modified;
    }

    private String getSaveFileName(File file, boolean isV3)
    {
        if (getSaveHelper() != null) {
            return getSaveHelper().getSaveFileName(file, isV3);
        }
        return file.getName();
    }

    private void createBackupFile(File fromFile, File toFile)
            throws IOException
    {
        if (getSaveHelper() != null) {
            getSaveHelper().createBackupFile(fromFile, toFile);
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

    public static void writeFile(File file, byte[] data) throws IOException
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
