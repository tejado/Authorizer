/*
 * $Id: PwsFile.java 411 2009-09-25 18:19:34Z roxon $
 *
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import org.bouncycastle.crypto.RuntimeCryptoException;
import org.pwsafe.lib.Log;
import org.pwsafe.lib.Util;
import org.pwsafe.lib.crypto.InMemoryKey;
import org.pwsafe.lib.exception.EndOfFileException;
import org.pwsafe.lib.exception.MemoryKeyException;
import org.pwsafe.lib.exception.UnsupportedFileVersionException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * This is the base class for all variations of the PasswordSafe file format.
 * <p>
 * <tt>
 * <pre>
 * +--------------+-----------+----------------------------------------------------+
 * |       Length | Name      | Description
 * |
 * +--------------+-----------+----------------------------------------------------+
 * |            8 | RandStuff | Random bytes
 * |
 * |           20 | RandHash  | Random hash
 * |
 * |           20 | Salt      | Salt
 * |
 * |            8 | IpThing   | Initial vector for decryption
 * |
 * +--------------+-----------+----------------------------------------------------+</pre>
 * </tt>
 * </p><p>
 * The records follow immediately after the header.  Each record has the same
 * layout:
 * </p><p>
 * <tt>
 * <pre>
 * +--------------+-----------+----------------------------------------------------+
 * |  BLOCK_LENGTH| RecLen    | Actual length of the data that follows
 * (encrypted) |
 * |n*BLOCK_LENGTH| RecData   | The encrypted data.  The length is RecLen
 * rounded  |
 * |              |           | up to be a multiple of BLOCK_LENGTH
 * |
 * +--------------+-----------+----------------------------------------------------+</pre>
 * </tt>
 * </p>
 */
public abstract class PwsFile
{
    private static final Log LOG = Log.getInstance(
            PwsFile.class.getPackage().getName());

    /**
     * Length of RandStuff in bytes.
     */
    public static final int STUFF_LENGTH = 8;

    /**
     * Length of RandHash in bytes.
     */
    public static final int HASH_LENGTH = 20;

    /**
     * Block length - the minimum size of a data block.  All data written
     * to the database is in blocks that are an integer multiple of
     * <code>BLOCK_LENGTH</code> in size. The exception is time field, there
     * the size used is 4.
     */
    private static final int BLOCK_LENGTH = 8;


    /**
     * Default encoding which should work with most Windows files
     */
    public static final String DEFAULT_PASSWORD_CHARSET = "windows-1252";

    /**
     * List of all charset encodings to try when opening files
     */
    public static final Collection<String> ALL_PASSWORD_CHARSETS;

    static {
        ALL_PASSWORD_CHARSETS = new LinkedHashSet<>();
        ALL_PASSWORD_CHARSETS.add(DEFAULT_PASSWORD_CHARSET);
        ALL_PASSWORD_CHARSETS.add(Charset.defaultCharset().name());
        ALL_PASSWORD_CHARSETS.add("US-ASCII");
        ALL_PASSWORD_CHARSETS.add("ISO-8859-1");
        ALL_PASSWORD_CHARSETS.add("UTF-8");
        ALL_PASSWORD_CHARSETS.add("ISO-8859-2");
        ALL_PASSWORD_CHARSETS.add("windows-1250");
        ALL_PASSWORD_CHARSETS.add("UTF-16");

        ALL_PASSWORD_CHARSETS.addAll(Charset.availableCharsets().keySet());
    }

    /**
     * Encoding to use for the file's password
     */
    private static String itsPasswordEncoding = DEFAULT_PASSWORD_CHARSET;

    /**
     * Cipher spec for storing passwords in memory
     */
    private static final String CIPHER_SPEC = "AES/CBC/PKCS5Padding";

    /**
     * Cipher key spec for storing passwords in memory
     */
    private static final String CIPHER_KEY_SPEC = "AES";

    /**
     * Cipher key length for storing passwords in memory
     */
    private static final int CIPHER_KEY_LEN = 16;

    /**
     * The storage implementation associated with this file
     */
    protected PwsStorage storage;

    /**
     * The passphrase for the file.
     */
    private SealedObject passphrase;

    /**
     * The stream used to read data from the storage.  It is non-null only
     * whilst data are being read from the file.
     */
    protected InputStream inStream;

    /**
     * The stream used to write data to the storage.  It is non-null only
     * whilst data are being written to the file.
     */
    protected OutputStream outStream;

    /**
     * The records that are part of the file.
     */
    private final ArrayList<PwsRecord> records = new ArrayList<>();

    /**
     * Flag indicating whether (<code>true</code>) or not (<code>false</code>)
     * the storage has been modified in memory and not yet written back to the
     * filesystem.
     */
    protected boolean modified = false;

    /**
     * Flag indicating whether the storage may be changed or saved.
     */
    private boolean readOnly = false;

    /**
     * Last modification Date and time of the underlying storage.
     */
    protected Date lastStorageChange;

    private InMemoryKey memoryKey;
    private byte[] memoryIv;

    private Cipher itsReadCipher;
    private Cipher itsWriteCipher;

    /**
     * The password encoding which was used to open the file
     */
    private String itsOpenPasswordEncoding;

    /**
     * Constructs and initialises a new, empty PasswordSafe database in memory.
     */
    protected PwsFile()
    {
        storage = null;
    }

    /**
     * Construct the PasswordSafe file by reading it from the file.
     *
     * @param aStorage the storage of the database to open.
     * @param passwd   the passphrase for the database.
     * @param encoding the passphrase encoding (if known)
     * @throws EndOfFileException
     * @throws IOException
     * @throws UnsupportedFileVersionException
     */
    protected PwsFile(PwsStorage aStorage,
                      Owner<PwsPassword>.Param passwd, String encoding)
            throws EndOfFileException, IOException,
                   UnsupportedFileVersionException
    {
        this.storage = aStorage;
        open(passwd, encoding);
    }

    /**
     * Adds a record to the file.
     *
     * @param rec the record to be added.
     */
    public void add(final PwsRecord rec)
    {
        if (isReadOnly()) {
            LOG.error("Illegal add on read only file - " +
                      "saving won't be possible");
        }

        this.doAdd(rec);
        setModified();
    }

    private void doAdd(final PwsRecord rec)
    {
        records.add(rec);
    }

    /**
     * Allocates a byte array at least <code>length</code> bytes in length
     * and which is an integer multiple of <code>BLOCK_LENGTH</code>.
     *
     * @param length the number of bytes the array must hold.
     * @return A byte array of the correct length.
     */
    static byte[] allocateBuffer(int length)
    {
        int bLen;
        bLen = calcBlockLength(length);
        return new byte[bLen];
    }

    /**
     * Calculates the next integer multiple of <code>BLOCK_LENGTH</code> &gt;
     * = <code>length</code>. If <code>length</code> is zero, however,
     * then <code>BLOCK_LENGTH </code> is returned as the calculated block
     * length.
     *
     * @param length the minimum block length
     * @return <code>length</code> rounded up to the next multiple of
     * <code>BLOCK_LENGTH</code>.
     * @throws IllegalArgumentException if length &lt; zero.
     */
    static int calcBlockLength(int length)
    {
        int result;
        if (length < 0) {
            throw new IllegalArgumentException("length");
        }
        result = (length == 0) ? BLOCK_LENGTH :
                 ((length + (BLOCK_LENGTH - 1)) / BLOCK_LENGTH) * BLOCK_LENGTH;
        return result;
    }

    /**
     * Attempts to close the file.
     *
     * @throws IOException If the attempt fails.
     */
    void close() throws IOException
    {
        if (inStream != null) {
            inStream.close();
            inStream = null;
        }
    }

    /**
     * Wipes any sensitive data from memory.
     */
    public void dispose()
    {
        passphrase = null;
        if (memoryKey != null) {
            memoryKey.dispose();
        }
        if (memoryIv != null) {
            Arrays.fill(memoryIv, (byte)0);
            memoryIv = null;
        }
    }

    final Cipher getReadCipher()
    {
        return getCipher(false);
    }

    final Cipher getWriteCipher()
    {
        return getCipher(true);
    }

    private Cipher getCipher(boolean forWriting)
    {
        if (memoryIv == null) {
            memoryIv = new byte[CIPHER_KEY_LEN];
            Util.newRandBytes(memoryIv);
        }

        if (forWriting && (itsWriteCipher != null)) {
            return itsWriteCipher;
        } else if (!forWriting && (itsReadCipher != null)) {
            return itsReadCipher;
        }

        SecretKeySpec key = new SecretKeySpec(getKeyBytes(), CIPHER_KEY_SPEC);
        IvParameterSpec ivSpec = new IvParameterSpec(memoryIv);
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(CIPHER_SPEC);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e1) {
            throw new MemoryKeyException("memory key generation failed", e1);
        }

        try {
            if (forWriting) {
                cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
                itsWriteCipher = cipher;
            } else {
                cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
                itsReadCipher = cipher;
            }
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new MemoryKeyException("memory key generation failed", e);
        }

        return cipher;
    }

    private byte[] getKeyBytes()
    {
        if (memoryKey == null) {
            memoryKey = new InMemoryKey(CIPHER_KEY_LEN);
            memoryKey.init();
        }
        return memoryKey.getKey(CIPHER_KEY_LEN);
    }

    /**
     * Returns the storage implementation for this file
     */
    public PwsStorage getStorage()
    {
        return storage;
    }

    /**
     * Allow the storage implementation associated with this file to be
     * set.
     *
     * @param storage An implementation of the PwsStorage interface.
     */
    public void setStorage(PwsStorage storage)
    {
        this.storage = storage;
    }

    /**
     * Returns the major version number for the file.
     *
     * @return The major version number for the file.
     */
    public abstract int getFileVersionMajor();


    /**
     * Returns the passphrase used to open the file.
     *
     * @return The file's passphrase.
     */
    public Owner<PwsPassword> getPassphrase()
    {
        try {
            return new Owner<>(
                    PwsPassword.unseal(passphrase,
                                       getReadCipher()));
        } catch (IllegalBlockSizeException | BadPaddingException |
                ClassNotFoundException | IOException e) {
            throw new RuntimeCryptoException(e.getMessage());
        }
    }

    /**
     * Returns the number of records in the file.
     *
     * @return The number of records in the file.
     */
    public int getRecordCount()
    {
        return records.size();
    }

    /**
     * Returns an iterator over the records.  Records may be deleted from
     * the file by calling the <code>remove()</code> method on the iterator.
     *
     * @return An <code>Iterator</code> over the records.
     */
    public Iterator<PwsRecord> getRecords()
    {
        return new FileIterator(this, records.iterator());
    }

    /**
     * Returns a record.
     *
     * @return the PwsRecord at that index
     */
    @SuppressWarnings("unused")
    public PwsRecord getRecord(int index)
    {
        return records.get(index);
    }

    /**
     * Returns an flag as to whether this file or any of its records have
     * been modified.
     *
     * @return <code>true</code> if the file has been modified,
     * <code>false</code> if it hasn't.
     */
    @SuppressWarnings("unused")
    public boolean isModified()
    {
        return modified;
    }


    /**
     * Allocates a new, empty record unowned by any file.
     *
     * @return A new empty record
     */
    public abstract PwsRecord newRecord();

    /**
     * Updates a Record.
     * Important to use this method as soon as getRecord will return copies
     * made from encrypted records.
     */
    public void set(int index, PwsRecord aRecord)
    {
        records.set(index, aRecord);
        setModified();
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
    protected abstract void open(Owner<PwsPassword>.Param passwd,
                                 String encoding)
            throws EndOfFileException, IOException,
                   UnsupportedFileVersionException;

    /**
     * Reads all records from the file.
     *
     * @throws IOException                     If an error occurs reading
     * from the file.
     * @throws UnsupportedFileVersionException If the file is an unsupported
     * version
     */
    void readAll() throws IOException, UnsupportedFileVersionException
    {
        try {
            //noinspection InfiniteLoopStatement
            for (; ; ) {
                final PwsRecord rec = PwsRecord.read(this);

                if (rec.isValid()) {
                    this.doAdd(rec);
                }
            }
        } catch (EndOfFileException e) {
            // OK
        }
    }

    /**
     * Allocates a block of <code>BLOCK_LENGTH</code> bytes then reads and
     * decrypts this many bytes from the file.
     *
     * @return A byte array containing the decrypted data.
     * @throws EndOfFileException If end of file occurs whilst reading the data.
     * @throws IOException        If an error occurs whilst reading the
     * file.
     */
    protected byte[] readBlock()
            throws EndOfFileException, IOException
    {
        byte[] block;

        block = new byte[getBlockSize()];
        readDecryptedBytes(block);

        return block;
    }

    /**
     * Reads raw (un-decrypted) bytes from the file.  The method attempts
     * to read <code>bytes.length</code> bytes from the file.
     *
     * @param bytes the array to be filled from the file.
     * @throws EndOfFileException If end of file occurs whilst reading the data.
     * @throws IOException        If an error occurs whilst reading the
     * file.
     */
    public final void readBytes(byte[] bytes)
            throws IOException, EndOfFileException
    {
        int count = inStream.read(bytes);

        if (count == -1) {
            throw new EndOfFileException();
        } else if (count < bytes.length) {
            throw new IOException("short read");
        }
    }

    /**
     * Reads bytes from the file and decrypts them.  <code>buff</code> may be
     * any length provided
     * that is a multiple of <code>getBlockSize()</code> bytes in length.
     *
     * @param buff the buffer to read the bytes into.
     * @throws EndOfFileException       If end of file has been reached.
     * @throws IOException              If a read error occurs.
     * @throws IllegalArgumentException If <code>buff.length</code> is not
     * an integral multiple of <code>BLOCK_LENGTH</code>.
     */
    public abstract void readDecryptedBytes(byte[] buff)
            throws EndOfFileException, IOException;

    /**
     * Reads any additional header from the file.  Subclasses should override
     * this a necessary as the default implementation does nothing.
     *
     * @param file the {@link PwsFile} instance to read the header from.
     *
     * @throws EndOfFileException              If end of file is reached.
     * @throws IOException                     If an error occurs while reading the file.
     * @throws UnsupportedFileVersionException If the file's version is unsupported.
     */
    protected void readExtraHeader( PwsFile file )
            throws EndOfFileException, IOException, UnsupportedFileVersionException
    {
    }

    /**
     * Reads a single record from the file.  The correct subclass of
     * PwsRecord is
     * returned depending on the version of the file.
     *
     * @return The record read from the file.
     * @throws EndOfFileException              When end-of-file is reached.
     * @throws IOException
     * @throws UnsupportedFileVersionException If this version of the file
     * cannot be handled.
     */
    protected PwsRecord readRecord()
            throws EndOfFileException, IOException,
                   UnsupportedFileVersionException
    {
        final PwsRecord rec = PwsRecord.read(this);

        if (rec.isValid()) {
            this.add(rec);
        }

        return rec;
    }

    /**
     * Remove a record
     *
     * @return true if a record was removed
     */
    public boolean removeRecord(int index)
    {
        boolean success = records.remove(index) != null;
        if (success)
            setModified();
        return success;
    }

    /**
     * Writes this file back to the filesystem.  If successful the modified
     * flag is also
     * reset on the file and all records.
     *
     * @throws IOException                     if the attempt fails.
     * @throws ConcurrentModificationException if the underlying store was
     *                                         independently changed
     */
    public abstract void save()
            throws IOException, ConcurrentModificationException;

    /**
     * Set the flag to indicate that the file has been modified.  There
     * should not normally
     * be any reason to call this method as it should be called indirectly
     * when a record is
     * added, changed or removed.
     */
    private void setModified()
    {
        modified = true;
    }

    /**
     * Sets the passphrase that will be used to encrypt the file when it is
     * saved.
     */
    public void setPassphrase(Owner<PwsPassword>.Param passwdParam)
    {
        Owner<PwsPassword> passwd = passwdParam.use();
        try {
            passphrase = passwd.get().seal(getWriteCipher());
        } catch (IllegalBlockSizeException | IOException e) {
            throw new RuntimeCryptoException(e.getMessage());
        } finally {
            passwd.close();
        }
    }

    /**
     * Writes unencrypted bytes to the file.
     *
     * @param buffer the data to be written.
     * @throws IOException
     */
    public void writeBytes(byte[] buffer)
            throws IOException
    {
        outStream.write(buffer);
    }

    /**
     * Encrypts then writes the contents of <code>buff</code> to the file.
     *
     * @param buff the data to be written.
     * @throws IOException
     */
    public abstract void writeEncryptedBytes(byte[] buff)
            throws IOException;

    /**
     * Writes any additional header.  This default implementation does nothing.
     * Subclasses should override this as necessary.
     *
     * @param file the {@link PwsFile} instance to write the header to.
     *
     * @throws IOException
     */
    protected void writeExtraHeader( PwsFile file )
            throws IOException
    {
    }

    /**
     * Returns the size of blocks in this file type.
     *
     * @return the size of blocks in this file type as an int
     */
    abstract int getBlockSize();

    /**
     * May this file be changed?
     *
     * @return the readOnly
     */
    public boolean isReadOnly()
    {
        return readOnly;
    }

    /**
     * Sets whether this file may be changed.
     *
     * @param readOnly the readOnly to set
     */
    public void setReadOnly(boolean readOnly)
    {
        this.readOnly = readOnly;
    }

    protected void setOpenPasswordEncoding(String encoding)
    {
        itsOpenPasswordEncoding = encoding;
    }

    public String getOpenPasswordEncoding()
    {
        return itsOpenPasswordEncoding;
    }


    public static synchronized List<String> getPasswordEncodings()
    {
        return Collections.singletonList(itsPasswordEncoding);
    }

    public static synchronized String getUpdatePasswordEncoding()
    {
        return itsPasswordEncoding;
    }

    public static synchronized void setPasswordEncoding(String encoding)
    {
        itsPasswordEncoding = encoding;
    }

    /**
     * This provides a wrapper around the <code>Iterator</code> that is returned
     * by the <code>iterator()</code> method on the Collections class used to
     * store the PasswordSafe records.  It allows us to mark the file as
     * modified when records are deleted file using the iterator's
     * <code>remove()</code> method.
     */
    private class FileIterator implements Iterator<PwsRecord>
    {
        private final Log LOG = Log
                .getInstance(FileIterator.class.getPackage().getName());

        private final PwsFile file;
        private final Iterator<PwsRecord> recDelegate;

        /**
         * Construct the <code>Iterator</code> linking it to the given
         * PasswordSafe file.
         *
         * @param file the file this iterator is linked to.
         * @param iter the <code>Iterator</code> over the records.
         */
        public FileIterator(PwsFile file, Iterator<PwsRecord> iter)
        {
            this.file = file;
            recDelegate = iter;
        }

        /**
         * Returns <code>true</code> if the iteration has more elements. (In
         * other words, returns <code>true</code> if next would return an
         * element rather than throwing an exception.)
         *
         * @return <code>true</code> if the iterator has more elements.
         * @see java.util.Iterator#hasNext()
         */
        public final boolean hasNext()
        {
            return recDelegate.hasNext();
        }

        /**
         * Returns the next PasswordSafe record in the iteration.  The object
         * returned will be a subclass of {@link PwsRecord}
         *
         * @return the next element in the iteration.
         * @see java.util.Iterator#next()
         */
        public final PwsRecord next()
        {
            return recDelegate.next();
        }

        /**
         * Removes the last record returned by
         * {@link PwsFile.FileIterator#next()} from the PasswordSafe file and
         * marks the file as modified.
         *
         * @see java.util.Iterator#remove()
         */
        public final void remove()
        {
            if (isReadOnly())
                LOG.error("Illegal remove on read only file - saving won't be" +
                          " possible");

            recDelegate.remove();
            file.setModified();
        }
    }

}
