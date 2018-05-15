/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import org.pwsafe.lib.Log;
import org.pwsafe.lib.Util;
import org.pwsafe.lib.crypto.BlowfishPws;
import org.pwsafe.lib.crypto.SHA1;
import org.pwsafe.lib.exception.EndOfFileException;
import org.pwsafe.lib.exception.PasswordSafeException;
import org.pwsafe.lib.exception.UnsupportedFileVersionException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

/**
 * Superclass for common functionality for V1 and V2 Files.
 *
 * @author mueller
 */
public abstract class PwsFileV1V2 extends PwsFile
{
    private static final Log LOG = Log.getInstance(
            PwsFileV1V2.class.getPackage().getName());


    /**
     * The file's standard header.
     */
    private PwsFileHeader header;

    /**
     * The Blowfish object being used to encrypt or decrypt data as it is
     * written to or read from the file.
     */
    private BlowfishPws algorithm;


    /**
     * Create a v1 or v2 file from storage
     *
     * @throws EndOfFileException
     * @throws IOException
     * @throws UnsupportedFileVersionException
     */
    protected PwsFileV1V2(PwsStorage storage,
                          Owner<PwsPassword>.Param passwd, String encoding)
            throws EndOfFileException, IOException,
                   UnsupportedFileVersionException
    {
        super(storage, passwd, encoding);
    }


    /* (non-Javadoc)
     * @see org.pwsafe.lib.file.PwsFile#close()
     */
    @Override
    void close() throws IOException
    {
        super.close();
        algorithm = null;
    }

    /* (non-Javadoc)
     * @see org.pwsafe.lib.file.PwsFile#getBlockSize()
     */
    @Override
    int getBlockSize()
    {
        return 8;
    }

    /**
     * Constructs and initialises the blowfish encryption routines ready
     * to decrypt or encrypt data.
     *
     * @param passwdParam the passphrase
     * @param encoding    the passphrase encoding (if known)
     * @return A properly initialised {@link BlowfishPws} object.
     * @throws UnsupportedEncodingException
     */
    private BlowfishPws makeBlowfish(Owner<PwsPassword>.Param passwdParam,
                                     String encoding)
            throws UnsupportedEncodingException
    {
        SHA1 sha1;
        byte salt[];

        sha1 = new SHA1();
        salt = header.getSalt();

        Owner<PwsPassword> passwd = passwdParam.use();
        try {
            byte[] passwordBytes = passwd.get().getBytes(encoding);
            sha1.update(passwordBytes, 0, passwordBytes.length);
            sha1.update(salt, 0, salt.length);
        } finally {
            passwd.close();
        }

        return new BlowfishPws(sha1.getDigest(), header.getIpThing());
    }

    /**
     * Opens the database.
     *
     * @param passwd   the passphrase for the file.
     * @param encoding the passphrase encoding (if known)
     * @throws EndOfFileException
     * @throws IOException
     * @throws UnsupportedFileVersionException
     */
    @Override
    protected void open(Owner<PwsPassword>.Param passwd, String encoding)
            throws EndOfFileException, IOException,
                   UnsupportedFileVersionException
    {
        setPassphrase(passwd);

        if (storage != null) {
            inStream = new ByteArrayInputStream(storage.load());
            lastStorageChange = storage.getModifiedDate();
        }
        header = new PwsFileHeader(this);
        algorithm = makeBlowfish(passwd, encoding);

        readExtraHeader(this);

        setOpenPasswordEncoding(
                (encoding == null) ? Charset.defaultCharset().name() :
                encoding);
    }

    /**
     * Reads bytes from the file and decrypts them.  <code>buff</code> may be
     * any length provided that is a multiple of <code>getBlockSize()</code>
     * bytes in length.
     *
     * @param buff the buffer to read the bytes into.
     * @throws EndOfFileException       If end of file has been reached.
     * @throws IOException              If a read error occurs.
     * @throws IllegalArgumentException If <code>buff.length</code> is not
     * an integral multiple of <code>BLOCK_LENGTH</code>.
     */
    @Override
    public void readDecryptedBytes(byte[] buff)
            throws EndOfFileException, IOException
    {
        if ((buff.length == 0) || ((buff.length % getBlockSize()) != 0)) {
            throw new IllegalArgumentException("buff length");
        }
        readBytes(buff);
        try {
            algorithm.decrypt(buff);
        } catch (PasswordSafeException e) {
            LOG.error(e.getMessage());
        }
    }

    /**
     * Writes this file back to the filesystem.  If successful the modified
     * flag is also reset on the file and all records.
     *
     * @throws IOException                     if the attempt fails.
     * @throws ConcurrentModificationException if the underlying store was
     *                                         independently changed
     */
    @Override
    public void save()
            throws IOException, ConcurrentModificationException
    {
        if (isReadOnly())
            throw new IOException("File is read only");

        if (lastStorageChange != null && // check for concurrent change
            storage.getModifiedDate().after(lastStorageChange)) {
            throw new ConcurrentModificationException(
                    "Password store was changed independently - no save " +
                    "possible!");
        }

        // For safety we'll write to a temporary file which will be renamed
	    // to the
        // real name if we manage to write it successfully.

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        outStream = baos;

        try {
            header.save(this);

            // Can only be created once the V1 header's been written.
            Owner<PwsPassword> passwd = getPassphrase();
            try {
                algorithm = makeBlowfish(
                        passwd.pass(),
                        PwsFile.getUpdatePasswordEncoding());
            } finally {
                passwd.close();
            }

            writeExtraHeader(this);

            PwsRecord rec;
            for (Iterator<? extends PwsRecord> iter = getRecords(); iter
                    .hasNext(); ) {
                rec = iter.next();

                rec.saveRecord(this);
            }

            outStream.close();

            if (storage.save(baos.toByteArray(), false)) {
                modified = false;
                lastStorageChange = storage.getModifiedDate();
            } else {
                throw new IOException("Unable to save file");
            }
        } catch (IOException e) {
            try {
                outStream.close();
            } catch (Exception e2) {
                // do nothing we're going to throw the original exception
            }
            throw e;
        } finally {
            outStream = null;
            algorithm = null;
        }
    }

    /**
     * Encrypts then writes the contents of <code>buff</code> to the file.
     *
     * @param buff the data to be written.
     * @throws IOException
     */
    @Override
    public void writeEncryptedBytes(byte[] buff)
            throws IOException
    {
        if ((buff.length == 0) || ((buff.length % getBlockSize()) != 0)) {
            throw new IllegalArgumentException("buff length");
        }

        byte[] temp = Util.cloneByteArray(buff);
        try {
            algorithm.encrypt(temp);
        } catch (PasswordSafeException e) {
            LOG.error(e.getMessage());
        }
        writeBytes(temp);
    }
}
