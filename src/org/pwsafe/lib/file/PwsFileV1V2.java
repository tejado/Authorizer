/*
 * $Id$
 * 
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

import org.pwsafe.lib.I18nHelper;
import org.pwsafe.lib.Log;
import org.pwsafe.lib.Util;
import org.pwsafe.lib.crypto.BlowfishPws;
import org.pwsafe.lib.crypto.SHA1;
import org.pwsafe.lib.exception.EndOfFileException;
import org.pwsafe.lib.exception.PasswordSafeException;
import org.pwsafe.lib.exception.UnsupportedFileVersionException;

/**
 * Superclass for common functionality for V1 and V2 Files.
 * 
 * @author mueller
 *
 */
public abstract class PwsFileV1V2 extends PwsFile {

	private static final Log LOG = Log.getInstance(PwsFileV1V2.class.getPackage().getName());


	/**
	 * The file's standard header.
	 */
	private PwsFileHeader		header;

	/**
	 * The Blowfish object being used to encrypt or decrypt data as it is written to or
	 * read from the file. 
	 */
	private BlowfishPws			algorithm;


	/**
	 * 
	 */
	public PwsFileV1V2() {
		super ();
		header	= new PwsFileHeader();
	}

	/**
	 * @param storage
	 * @param passphrase
	 * @throws EndOfFileException
	 * @throws IOException
	 * @throws UnsupportedFileVersionException
	 * @throws NoSuchAlgorithmException
	 */
	public PwsFileV1V2(PwsStorage storage, String passphrase)
			throws EndOfFileException, IOException,
			UnsupportedFileVersionException, NoSuchAlgorithmException {
		super(storage, passphrase);
	}

	
	/* (non-Javadoc)
	 * @see org.pwsafe.lib.file.PwsFile#close()
	 */
	@Override
	void close() throws IOException {
		super.close();
		algorithm	= null;
	}

	/* (non-Javadoc)
	 * @see org.pwsafe.lib.file.PwsFile#getBlockSize()
	 */
	@Override
	int getBlockSize() {
		return 8;
	}

	/**
	 * Returns the file header.
	 * 
	 * @return The file header.
	 */
	PwsFileHeader getHeader()
	{
		return header;
	}

	/**
	 * Constructs and initialises the blowfish encryption routines ready to decrypt or
	 * encrypt data.
	 * 
	 * @param aPassphrase
	 * 
	 * @return A properly initialised {@link BlowfishPws} object.
	 */
	private BlowfishPws makeBlowfish( byte [] aPassphrase )
	{
		SHA1	sha1;
		byte	salt[];
		
		sha1 = new SHA1();
		salt = header.getSalt();

		sha1.update( aPassphrase, 0, aPassphrase.length );
		sha1.update( salt, 0, salt.length );
		sha1.finalize();

		return new BlowfishPws( sha1.getDigest(), header.getIpThing() );
	}

	/**
	 * Opens the database.
	 * 
	 * @param aPassphrase the passphrase for the file.
	 * 
	 * @throws EndOfFileException
	 * @throws IOException
	 * @throws UnsupportedFileVersionException
	 * @throws NoSuchAlgorithmException if no SHA-1 implementation is found.
	 */
	@Override
	protected void open( String aPassphrase )
	throws EndOfFileException, IOException, UnsupportedFileVersionException, NoSuchAlgorithmException
	{
		LOG.enterMethod( "PwsFile.init" );

		setPassphrase(new StringBuilder(aPassphrase));
		
		if (storage!=null) {
			inStream		= new ByteArrayInputStream(storage.load());
			lastStorageChange = storage.getModifiedDate();
		}
		header			= new PwsFileHeader( this );
		algorithm		= makeBlowfish( aPassphrase.getBytes() );

		readExtraHeader( this );

		LOG.leaveMethod( "PwsFile.init" );
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
	@Override
	public void readDecryptedBytes( byte [] buff )
	throws EndOfFileException, IOException
	{
		if ( (buff.length == 0) || ((buff.length % getBlockSize()) != 0) )
		{
			throw new IllegalArgumentException( I18nHelper.getInstance().formatMessage("E00001") );
		}
		readBytes( buff );
		try {
			algorithm.decrypt( buff );
		} catch (PasswordSafeException e) {
			LOG.error(e.getMessage());
		}
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
	@Override
	public void save()
	throws IOException, NoSuchAlgorithmException, ConcurrentModificationException
	{
		if (isReadOnly())
			throw new IOException("File is read only");

		if (lastStorageChange != null && // check for concurrent change
			storage.getModifiedDate().after(lastStorageChange)) {
			throw new ConcurrentModificationException("Password store was changed independently - no save possible!");
		}

		// For safety we'll write to a temporary file which will be renamed to the
		// real name if we manage to write it successfully.

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		outStream	= baos;

		try
		{
			header.save( this );

			// Can only be created once the V1 header's been written.
			//TODO: check whether this can be performed without toString 
			algorithm	= makeBlowfish(getPassphrase().toString().getBytes());

			writeExtraHeader( this );

			PwsRecord	rec;
			for ( Iterator<? extends PwsRecord> iter = getRecords(); iter.hasNext(); )
			{
				rec = iter.next();
	
				rec.saveRecord( this );
			}
	
			outStream.close();
	
			if (storage.save(baos.toByteArray())) {
				modified = false;
				lastStorageChange = storage.getModifiedDate();
			}
			else
			{
				// FIXME: I'm not sure what this message should be, but it should
				// reflect the fact that storage failed, not anything about a temp file.
				LOG.error( I18nHelper.getInstance().formatMessage("E00010", new Object [] { "Storage file" } ) );
				// TODO Throw an exception here?
				return;
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
	 * 
	 * @throws IOException
	 */
	@Override
	public void writeEncryptedBytes( byte [] buff )
	throws IOException
	{
		if ( (buff.length == 0) || ((buff.length % getBlockSize()) != 0) )
		{
			throw new IllegalArgumentException( I18nHelper.getInstance().formatMessage("E00001") );
		}
		
		byte [] temp = Util.cloneByteArray( buff );
		try {
			algorithm.encrypt( temp );
		} catch (PasswordSafeException e) {
			LOG.error(e.getMessage());
		}
		writeBytes( temp );
	}

}
