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

import org.bouncycastle.crypto.RuntimeCryptoException;
import org.pwsafe.lib.I18nHelper;
import org.pwsafe.lib.Log;
import org.pwsafe.lib.Util;
import org.pwsafe.lib.crypto.InMemoryKey;
import org.pwsafe.lib.exception.EndOfFileException;
import org.pwsafe.lib.exception.MemoryKeyException;
import org.pwsafe.lib.exception.PasswordSafeException;
import org.pwsafe.lib.exception.UnsupportedFileVersionException;

/**
 * This is the base class for all variations of the PasswordSafe file format.
 * <p>
 * <tt>
 * <pre> +--------------+-----------+----------------------------------------------------+
 * |       Length | Name      | Description                                        |
 * +--------------+-----------+----------------------------------------------------+
 * |            8 | RandStuff | Random bytes                                       |
 * |           20 | RandHash  | Random hash                                        |
 * |           20 | Salt      | Salt                                               |
 * |            8 | IpThing   | Initial vector for decryption                      |
 * +--------------+-----------+----------------------------------------------------+</pre>
 * </tt>
 * </p><p>
 * The records follow immediately after the header.  Each record has the same
 * layout:
 * </p><p>
 * <tt>
 * <pre> +--------------+-----------+----------------------------------------------------+
 * |  BLOCK_LENGTH| RecLen    | Actual length of the data that follows (encrypted) |
 * |n*BLOCK_LENGTH| RecData   | The encrypted data.  The length is RecLen rounded  |
 * |              |           | up to be a multiple of BLOCK_LENGTH                |
 * +--------------+-----------+----------------------------------------------------+</pre>
 * </tt>
 * </p>
 */
public abstract class PwsFile
{
	private static final Log LOG = Log.getInstance(PwsFile.class.getPackage().getName());

	/**
	 * Length of RandStuff in bytes.
	 */
	public static final int	STUFF_LENGTH	= 8;

	/**
	 * Length of RandHash in bytes.
	 */
	public static final int	HASH_LENGTH		= 20;

	/**
	 * Block length - the minimum size of a data block.  All data written to the database is
	 * in blocks that are an integer multiple of <code>BLOCK_LENGTH</code> in size.
	 * The exception is time field, there the size used is 4.
	 */
	public static final int BLOCK_LENGTH	= 8;


	/** Default encoding which should work with most Windows files */
	public static final String DEFAULT_PASSWORD_CHARSET = "windows-1252";

	/** List of all charset encodings to try when opening files */
	public static final Collection<String> ALL_PASSWORD_CHARSETS;
	static {
	    ALL_PASSWORD_CHARSETS = new LinkedHashSet<String>();
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

	/** Encoding to use for the file's password */
	private static String itsPasswordEncoding = DEFAULT_PASSWORD_CHARSET;


	/** The storage implementation associated with this file */
	protected PwsStorage		storage;

	/**
	 * The passphrase for the file.
	 */
	protected SealedObject passphrase;

	/**
	 * The stream used to read data from the storage.  It is non-null only whilst data
	 * are being read from the file.
	 */
	protected InputStream		inStream;

	/**
	 * The stream used to write data to the storage.  It is non-null only whilst data are
	 * being written to the file.
	 */
	protected OutputStream		outStream;

	/**
	 * The records that are part of the file.
	 */
	protected ArrayList<PwsRecord> records = new ArrayList<PwsRecord>();

	/**
	 * Flag indicating whether (<code>true</code>) or not (<code>false</code>) the storage
	 * has been modified in memory and not yet written back to the filesystem.
	 */
	protected boolean			modified		= false;


	/**
	 * Flag indicating whether the storage may be changed or saved.
	 *
	 */
	protected boolean 			readOnly 		= false;

	/**
	 * Last modification Date and time of the underlying storage.
	 */
	protected Date 				lastStorageChange;

	private InMemoryKey			memoryKey;
	private byte[] 				memoryIv;

	/** The password encoding which was used to open the file */
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
	 * @param aStorage  the storage of the database to open.
	 * @param aPassphrase the passphrase for the database.
	 * @param encoding the passphrase encoding (if known)
	 *
	 * @throws EndOfFileException
	 * @throws IOException
	 * @throws UnsupportedFileVersionException
	 * @throws NoSuchAlgorithmException if no SHA-1 implementation is found.
	 */
	protected PwsFile( PwsStorage aStorage, String aPassphrase, String encoding )
	throws EndOfFileException, IOException, UnsupportedFileVersionException, NoSuchAlgorithmException
	{
		LOG.enterMethod( "PwsFile.PwsFile( String )" );
		this.storage = aStorage;
		open( aPassphrase, encoding );

		LOG.leaveMethod( "PwsFile.PwsFile( String )" );
	}

	/**
	 * Adds a record to the file.
	 *
	 * @param rec the record to be added.
	 *
	 * @throws PasswordSafeException if the record has already been added to another file.
	 */
	public void add(final PwsRecord rec ) throws PasswordSafeException {
		LOG.enterMethod( "PwsFile.add" );

		if (isReadOnly())
			LOG.error("Illegal add on read only file - saving won't be possible");

		this.add(rec, getCipher(true));
		//recordSet.add( rec );
		setModified();

		LOG.leaveMethod( "PwsFile.add" );
	}

	protected void add ( final PwsRecord rec, final Cipher aCipher ) {

		// TODOlib validate the record before adding it
	    // TODO: remove method
	    records.add(rec);
	}

	/**
	 * Allocates a byte array at least <code>length</code> bytes in length and which is an integer multiple
	 * of <code>BLOCK_LENGTH</code>.
	 *
	 * @param length the number of bytes the array must hold.
	 *
	 * @return A byte array of the correct length.
	 */
	static final byte [] allocateBuffer( int length )
	{
		LOG.enterMethod( "PwsFile.allocateBuffer" );

		int	bLen;

		bLen	= calcBlockLength( length );

		LOG.leaveMethod( "PwsFile.allocateBuffer" );

		return new byte[ bLen ];
	}

	/**
	 * Calculates the next integer multiple of <code>BLOCK_LENGTH</code> &gt;= <code>length</code>.
	 * If <code>length</code> is zero, however, then <code>BLOCK_LENGTH</code> is returned as the
	 * calculated block length.
	 *
	 * @param length the minimum block length
	 *
	 * @return <code>length</code> rounded up to the next multiple of <code>BLOCK_LENGTH</code>.
	 *
	 * @throws IllegalArgumentException if length &lt; zero.
	 */
	static final int calcBlockLength( int length )
	{
		LOG.enterMethod( "PwsFile.calcBlockLength" );

		int result;

		if ( length < 0 )
		{
			LOG.error( I18nHelper.getInstance().formatMessage("E00004") );
			LOG.leaveMethod( "PwsFile.calcBlockLength" );
			throw new IllegalArgumentException( I18nHelper.getInstance().formatMessage("E00004") );
		}
		result = ( length == 0 ) ? BLOCK_LENGTH : ( (length + (BLOCK_LENGTH - 1)) / BLOCK_LENGTH ) * BLOCK_LENGTH;

		LOG.debug1( "Length = " + length + ", BlockLength = " + result );

		LOG.leaveMethod( "PwsFile.calcBlockLength" );

		return result;
	}

	/**
	 * Attempts to close the file.
	 *
	 * @throws IOException If the attempt fails.
	 */
	void close()
	throws IOException
	{
		LOG.enterMethod( "PwsFile.close" );

		if ( inStream != null )
		{
			inStream.close();

			inStream	= null;
		}

		LOG.leaveMethod( "PwsFile.close" );
	}

    /**
     * Wipes any sensitive data from memory.
     */
    public void dispose () {
        passphrase = null;
        if (memoryKey != null) {
        	memoryKey.dispose();
        }
        if (memoryIv != null) {
        	Arrays.fill(memoryIv, (byte) 0);
        	memoryIv = null;
        }
    }

    private Cipher itsReadCipher;
    private Cipher itsWriteCipher;

    protected Cipher getCipher (boolean forWriting) {
    	if (memoryIv == null) {
    		memoryIv = new byte[8];
    		Util.newRandBytes(memoryIv);
    	}

    	if (forWriting && (itsWriteCipher != null)) {
    	    return itsWriteCipher;
    	} else if (!forWriting && (itsReadCipher != null)) {
    	    return itsReadCipher;
    	}

    	//TODOlib: use BouncyCastle Provider!
        SecretKeySpec   key = new SecretKeySpec(getKeyBytes(), "Blowfish");
        IvParameterSpec ivSpec = new IvParameterSpec(memoryIv);
        Cipher cipher = null;
		try {
			cipher = Cipher.getInstance("Blowfish/CBC/PKCS5Padding");
		} catch (NoSuchAlgorithmException e1) {
			throw new MemoryKeyException("memory key generation failed",e1);
//		} catch (NoSuchProviderException e1) {
//			throw new MemoryKeyException("memory Key generation failed", e1);
		} catch (NoSuchPaddingException e1) {
			throw new MemoryKeyException("memory key generation failed",e1);
		}

        try {
        	if (forWriting) {
        		cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        		//itsWriteCipher = cipher;
        	} else {
        		cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        		//itsReadCipher = cipher;
        	}
		} catch (InvalidKeyException e) {
			throw new MemoryKeyException("memory key generation failed",e);
		} catch (InvalidAlgorithmParameterException e) {
			throw new MemoryKeyException("memory key generation failed",e);
		}

    	return cipher;
    }

    private byte[] getKeyBytes () {
    	if (memoryKey == null) {
    		memoryKey = new InMemoryKey(16);
    		memoryKey.init();
	    }
    	return memoryKey.getKey();
    }

	/**
	 * Returns the storage implementation for this file
	 */
	public PwsStorage getStorage() {
		return storage;
	}

	/**
	 * Allow the storage implementation associated with this file to be set.
	 * @param storage An implementation of the PwsStorage interface.
	 */
	public void setStorage(PwsStorage storage) {
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
	public String getPassphrase()
	{
		try {
			return passphrase == null ? null : passphrase.getObject(getCipher(false)).toString();
		} catch (IllegalBlockSizeException e) {
			throw new RuntimeCryptoException(e.getMessage());
		} catch (BadPaddingException e) {
			throw new RuntimeCryptoException(e.getMessage());
		} catch (IOException e) {
			throw new RuntimeCryptoException(e.getMessage());
		} catch (ClassNotFoundException e) {
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
		LOG.enterMethod( "PwsFile.getRecordCount" );

		int size = records.size();

		LOG.leaveMethod( "PwsFile.getRecordCount" );

		return size;
	}

	/**
	 * Returns an iterator over the records.  Records may be deleted from the file by
	 * calling the <code>remove()</code> method on the iterator.
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
    public PwsRecord getRecord(int index)
    {
        // TODO: need getCipher here?
    	getCipher(true);
        return records.get(index);
    }

	/**
	 * Returns an flag as to whether this file or any of its records have been modified.
	 *
	 * @return <code>true</code> if the file has been modified, <code>false</code> if it hasn't.
	 */
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
     * Important to use this method as soon as getRecord
     * will return copies made from encrypted records.
     *
     * @param index
     * @param aRecord
     */
    public void set(int index, PwsRecord aRecord)
    {
        records.set(index, aRecord);
        setModified();
    }

	/**
	 * Opens the database.
	 *
	 * @param aPassphrase the passphrase for the file.
	 * @param encoding the passphrase encoding (if known)
	 *
	 * @throws EndOfFileException
	 * @throws IOException
	 * @throws UnsupportedFileVersionException
	 * @throws NoSuchAlgorithmException if no SHA-1 implementation is found.
	 */
	protected abstract void open( String aPassphrase, String encoding )
	throws EndOfFileException, IOException, UnsupportedFileVersionException, NoSuchAlgorithmException;

	/**
	 * Reads all records from the file.
	 *
	 * @throws IOException  If an error occurs reading from the file.
	 * @throws UnsupportedFileVersionException  If the file is an unsupported version
	 */
	void readAll() throws IOException, UnsupportedFileVersionException {
		try {
			final Cipher c = getCipher(true);
			for ( ;; ) {
				final PwsRecord	rec = PwsRecord.read( this );

				if ( rec.isValid() ){
					this.add( rec, c );
				}
			}
		} catch ( EndOfFileException e ) {
			// OK
		}
	}

	/**
	 * Allocates a block of <code>BLOCK_LENGTH</code> bytes then reads and decrypts this many
	 * bytes from the file.
	 *
	 * @return A byte array containing the decrypted data.
	 *
	 * @throws EndOfFileException If end of file occurs whilst reading the data.
	 * @throws IOException        If an error occurs whilst reading the file.
	 */
	protected byte [] readBlock()
	throws EndOfFileException, IOException
	{
		byte []	block;

		block = new byte[ getBlockSize() ];
		readDecryptedBytes( block );

		return block;
	}

	/**
	 * Reads raw (undecrypted) bytes from the file.  The method attepts to read
	 * <code>bytes.length</code> bytes from the file.
	 *
	 * @param bytes the array to be filled from the file.
	 *
	 * @throws EndOfFileException If end of file occurs whilst reading the data.
	 * @throws IOException        If an error occurs whilst reading the file.
	 */
	public void readBytes( byte [] bytes )
	throws IOException, EndOfFileException
	{
		int count;

		count = inStream.read( bytes );

		if ( count == -1 )
		{
			LOG.debug1( "END OF FILE" );
			throw new EndOfFileException();
		}
		else if ( count < bytes.length )
		{
			LOG.info( I18nHelper.getInstance().formatMessage("I00003", new Object [] { new Integer(bytes.length), new Integer(count) } ) );
			throw new IOException( I18nHelper.getInstance().formatMessage("E00006") );
		}
		LOG.debug1( "Read " + count + " bytes" );
	}

	/**
	 * Reads bytes from the file and decryps them.  <code>buff</code> may be any length provided
	 * that is a multiple of <code>getBlockSize()</code> bytes in length.
	 *
	 * @param buff the buffer to read the bytes into.
	 *
	 * @throws EndOfFileException If end of file has been reached.
	 * @throws IOException If a read error occurs.
	 * @throws IllegalArgumentException If <code>buff.length</code> is not an integral multiple of <code>BLOCK_LENGTH</code>.
	 */
	public abstract void readDecryptedBytes( byte [] buff )
	throws EndOfFileException, IOException;

	/**
	 * Reads any additional header from the file.  Subclasses should override this a necessary
	 * as the default implementation does nothing.
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
	 * Reads a single record from the file.  The correct subclass of PwsRecord is
	 * returned depending on the version of the file.
	 *
	 * @return The record read from the file.
	 *
	 * @throws EndOfFileException When end-of-file is reached.
	 * @throws IOException
	 * @throws UnsupportedFileVersionException If this version of the file cannot be handled.
	 */
	protected PwsRecord readRecord()
	throws EndOfFileException, IOException, UnsupportedFileVersionException, PasswordSafeException
	{
		final PwsRecord	rec = PwsRecord.read( this );

		if ( rec.isValid() ){
			this.add( rec );
		}

		return rec;
	}

    /**
     *
     * @param index
     * @return true if a record was removed
     */
    public boolean removeRecord (int index) {
        boolean success = records.remove(index) != null;
        if (success)
            setModified();
        return success;
    }

	/**
	 * Writes this file back to the filesystem.  If successful the modified flag is also
	 * reset on the file and all records.
	 *
	 * @throws IOException if the attempt fails.
	 * @throws NoSuchAlgorithmException if no SHA-1 implementation is found.
	 * @throws ConcurrentModificationException if the underlying store was
	 * independently changed
	 */
	public abstract void save()
	throws IOException, NoSuchAlgorithmException, ConcurrentModificationException;

	/**
	 * Set the flag to indicate that the file has been modified.  There should not normally
	 * be any reason to call this method as it should be called indirectly when a record is
	 * added, changed or removed.
	 */
	protected void setModified()
	{
		modified = true;
	}

	/**
	 * Sets the passphrase that will be used to encrypt the file when it is saved.
	 * @deprecated
	 * @param pass
	 */
	@Deprecated
	public void setPassphrase( String pass )
	{
        this.setPassphrase(new StringBuilder(pass));

	}

	/**
	 * Sets the passphrase that will be used to encrypt the file when it is saved.
	 *
	 * @param pass
	 */
	public void setPassphrase( StringBuilder pass ) {
	    // TODOlib: convert to byte[] first
		try {
			passphrase	= new SealedObject(pass, getCipher(true));
			// now overwrite given StringBuider
			Util.clear(pass);
		} catch (IllegalBlockSizeException e) {
			throw new RuntimeCryptoException(e.getMessage());
		} catch (IOException e) {
			throw new RuntimeCryptoException(e.getMessage());
		}
	}

	/**
	 * Writes unencrypted bytes to the file.
	 *
	 * @param buffer the data to be written.
	 *
	 * @throws IOException
	 */
	public void writeBytes( byte [] buffer )
	throws IOException
	{
		outStream.write( buffer );
		LOG.debug1( "Wrote " + buffer.length + " bytes" );
	}

	/**
	 * Encrypts then writes the contents of <code>buff</code> to the file.
	 *
	 * @param buff the data to be written.
	 *
	 * @throws IOException
	 */
	public abstract void writeEncryptedBytes( byte [] buff )
	throws IOException;

	/**
	 * Writes any additional header.  This default implementation does nothing.  Subclasses
	 * should override this as necessary.
	 *
	 * @param file
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
	 * @return the readOnly
	 */
	public boolean isReadOnly() {
		return readOnly;
	}

	/**
	 * Sets whether this file may be changed.
	 * @param readOnly the readOnly to set
	 */
	public void setReadOnly(boolean readOnly) {
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
     * This provides a wrapper around the <code>Iterator</code> that is returned by the
     * <code>iterator()</code> method on the Collections class used to store the PasswordSafe
     * records.  It allows us to mark the file as modified when records are deleted file
     * using the iterator's <code>remove()</code> method.
     */
    private class FileIterator implements Iterator<PwsRecord>
    {
        private final Log LOG = Log.getInstance(FileIterator.class.getPackage().getName());

        private final PwsFile file;
        private final Iterator<PwsRecord> recDelegate;

        /**
         * Construct the <code>Iterator</code> linking it to the given PasswordSafe
         * file.
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
         * Returns <code>true</code> if the iteration has more elements. (In other words, returns
         * <code>true</code> if next would return an element rather than throwing an exception.)
         *
         * @return <code>true</code> if the iterator has more elements.
         *
         * @see java.util.Iterator#hasNext()
         */
        public final boolean hasNext()
        {
            return recDelegate.hasNext();
        }

        /**
         * Returns the next PasswordSafe record in the iteration.  The object returned will
         * be a subclass of {@link PwsRecord}
         *
         * @return the next element in the iteration.
         *
         * @see java.util.Iterator#next()
         */
        public final PwsRecord next() {
            return recDelegate.next();
        }

        /**
         * Removes the last record returned by {@link PwsFile.FileIterator#next()} from the PasswordSafe
         * file and marks the file as modified.
         *
         * @see java.util.Iterator#remove()
         */
        public final void remove()
        {
            LOG.enterMethod( "PwsFile$FileIterator.remove" );

            if (isReadOnly())
                LOG.error("Illegal remove on read only file - saving won't be possible");

            recDelegate.remove();
            file.setModified();

            LOG.leaveMethod( "PwsFile$FileIterator.remove" );
        }
    }

}
