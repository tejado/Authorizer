/*
 * $Id: PwsFileV2.java 944 2006-09-08 03:25:19 +0000 (Fri, 08 Sep 2006) glen_a_smith $
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
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SealedObject;

import org.pwsafe.lib.I18nHelper;
import org.pwsafe.lib.Log;
import org.pwsafe.lib.Util;
import org.pwsafe.lib.crypto.HmacPws;
import org.pwsafe.lib.crypto.SHA256Pws;
import org.pwsafe.lib.crypto.TwofishPws;
import org.pwsafe.lib.exception.EndOfFileException;
import org.pwsafe.lib.exception.MemoryKeyException;
import org.pwsafe.lib.exception.UnsupportedFileVersionException;

/**
 * Encapsulates version 3 PasswordSafe files.
 * 
 * @author Glen Smith (based on Kevin Preece's v2 implementation).
 */
public final class PwsFileV3 extends PwsFile {
	
	/**
	 * File extension of the V3 password safe files.
	 */
	public static final String FILE_EXTENSION = ".psafe3";

	private static final Log LOG = Log.getInstance(PwsFileV3.class.getPackage().getName());

	/**
	 * The PasswordSafe database version number that this class supports.
	 */
	public static final int		VERSION		= 3;

	/**
	 * The string that identifies a database as V3 rather than V2 or V1
	 */
	public static final byte[]	ID_STRING	= "PWS3".getBytes();

	/**
	 * The file's standard header.
	 */
	protected PwsFileHeaderV3	headerV3;
	
	private SealedObject sealedHeaderV3;

	/**
	 * End of File marker. HMAC follows this tag.
	 */
	static byte[] EOF_BYTES_RAW = "PWS3-EOFPWS3-EOF".getBytes();
	
	protected byte[] stretchedPassword;
	protected byte[] decryptedRecordKey;
	protected byte[] decryptedHmacKey;
	
	TwofishPws twofishCbc;
	HmacPws hasher;
	PwsRecordV3 headerRecord;

	/**
	 * Constructs and initialises a new, empty version 3 PasswordSafe database in memory.
	 */
	public PwsFileV3()
	{
		super();
		setHeaderV3(new PwsFileHeaderV3());
		headerRecord = new PwsRecordV3();
		headerRecord.setField(new PwsVersionField(0, new byte[] { 1, 3 }));
	}

	/**
	 * Use of this constructor to load a PasswordSafe database is STRONGLY discouraged
	 * since it's use ties the caller to a particular file version.  Use {@link
	 * PwsFileFactory#loadFile(String, String)} instead.
	 * </p><p>
	 * <b>N.B. </b>this constructor's visibility may be reduced in future releases.
	 * </p>
	 * @param storage   the underlying storage to use to open the database.
	 * @param aPassphrase the passphrase for the database.
	 * 
	 * @throws EndOfFileException
	 * @throws IOException
	 * @throws UnsupportedFileVersionException
	 * @throws NoSuchAlgorithmException 
	 */
	public PwsFileV3( PwsStorage storage, String aPassphrase ) 
	throws EndOfFileException, IOException, UnsupportedFileVersionException, NoSuchAlgorithmException
	{
		super( storage, aPassphrase );
	}

	
	/* (non-Javadoc)
	 * @see org.pwsafe.lib.file.PwsFile#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		if (stretchedPassword != null)
			Arrays.fill(stretchedPassword,(byte)0);
		if (decryptedHmacKey != null)
			Arrays.fill(decryptedHmacKey,(byte)0);
		if (decryptedRecordKey != null)
			Arrays.fill(decryptedRecordKey,(byte)0);
	}

	@Override
	protected void open( String aPassphrase )
	throws EndOfFileException, IOException, UnsupportedFileVersionException
	{
		LOG.enterMethod( "PwsFileV3.init" );

		setPassphrase(new StringBuilder(aPassphrase));
		
		if (storage!=null) {
			inStream		= new ByteArrayInputStream(storage.load());
			lastStorageChange = storage.getModifiedDate();
		}
		PwsFileHeaderV3 theHeaderV3		= new PwsFileHeaderV3( this );
		
		setHeaderV3(theHeaderV3);
		
		int iter = theHeaderV3.getIter();
		LOG.debug1("Using iterations: [" + iter + "]");
		stretchedPassword = Util.stretchPassphrase(aPassphrase.getBytes(), theHeaderV3.getSalt(), iter);
		
		if (!Util.bytesAreEqual(theHeaderV3.getPassword(), SHA256Pws.digest(stretchedPassword))) {
			//try another method to avoid asymmetric encoding bug in V0.8 Beta1
	        CharBuffer buf = CharBuffer.wrap(aPassphrase);
			stretchedPassword = Util.stretchPassphrase(Charset.defaultCharset().encode(buf).array(), theHeaderV3.getSalt(), iter);
			if (Util.bytesAreEqual(theHeaderV3.getPassword(), SHA256Pws.digest(stretchedPassword))) {
				LOG.warn("Succeeded workaround for asymmetric password encoding bug");
			} else {
				throw new IOException("Invalid password");
			}
		}
		
		try {
			
			byte[] rka = TwofishPws.processECB(stretchedPassword, false, theHeaderV3.getB1());
			byte[] rkb = TwofishPws.processECB(stretchedPassword, false, theHeaderV3.getB2());
			decryptedRecordKey = Util.mergeBytes(rka, rkb);
			
			byte[] hka = TwofishPws.processECB(stretchedPassword, false, theHeaderV3.getB3());
			byte[] hkb = TwofishPws.processECB(stretchedPassword, false, theHeaderV3.getB4());
			decryptedHmacKey = Util.mergeBytes(hka, hkb);
			hasher = new HmacPws(decryptedHmacKey);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException("Error reading encrypted fields");
		}
		twofishCbc = new TwofishPws(decryptedRecordKey, false, theHeaderV3.getIV());
		
		readExtraHeader( this );

		LOG.leaveMethod( "PwsFileV3.init" );
	}
	
	
	/**
	 * Writes this file back to the filesystem.  If successful the modified flag is also 
	 * reset on the file and all records.
	 * 
	 * @throws IOException if the attempt fails.
	 */
	@Override
	public void save() throws IOException {
		if (isReadOnly())
			throw new IOException("File is read only");

		if (lastStorageChange != null && // check for concurrent change
				storage.getModifiedDate().after(lastStorageChange)) {
				throw new ConcurrentModificationException("Password store was changed independently - no save possible!");
			}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		outStream	= baos;

		try	{
			PwsFileHeaderV3 theHeaderV3 = getHeaderV3();
			theHeaderV3.save( this );

			// Can only be created once the V3 header resets key info

			twofishCbc = new TwofishPws(decryptedRecordKey, true, theHeaderV3.getIV());

			writeExtraHeader( this );
			
			PwsRecordV3	rec;
			for (Iterator<? extends PwsRecord> iter = getRecords(); iter.hasNext();) {
				rec = (PwsRecordV3) iter.next();
				if (!rec.isHeaderRecord())
					rec.saveRecord(this);
			}
			
			outStream.write(PwsRecordV3.EOF_BYTES_RAW);
			outStream.write(hasher.doFinal());
	
			outStream.close();
	
			if (storage.save(baos.toByteArray())) {
				modified = false;
				lastStorageChange = storage.getModifiedDate();
			}
			else
			{
				// FIXME: What is the proper error code (see PwsFile::save).
				LOG.error( I18nHelper.getInstance().formatMessage("E00010", new Object [] { "Storage" } ) );
				// TODO Throw an exception here?
				return;
			}
		}
		catch ( IOException e ) {
			try {
				if (outStream != null) { 
					outStream.close();
				}
			} catch ( Exception e2 ) {
				// do nothing we're going to throw the original exception
			}
			throw e;
		} finally {
			outStream	= null;
		}
	}
	

	/**
	 * Returns the major version number for the file.
	 * 
	 * @return The major version number for the file.
	 */
	@Override
	public int getFileVersionMajor()
	{
		return VERSION;
	}

	/**
	 * Allocates a new, empty record unowned by any file.  The record type is
	 * {@link PwsRecordV2}.
	 * 
	 * @return A new empty record
	 * 
	 * @see org.pwsafe.lib.file.PwsFile#newRecord()
	 */
	@Override
	public PwsRecord newRecord()
	{
		return new PwsRecordV3();
	}
	
	/**
	 * Reads the extra header present in version 2 files.
	 * 
	 * @param file the file to read the header from.
	 * 
	 * @throws EndOfFileException If end of file is reached.
	 * @throws IOException If an error occurs whilst reading. 
	 * @throws UnsupportedFileVersionException If the header is not a valid V2 header.
	 */
	@Override
	protected void readExtraHeader( PwsFile file )
	throws EndOfFileException, IOException, UnsupportedFileVersionException
	{
		//headerRecord = (PwsRecordV3) readRecord();
		headerRecord = new PwsRecordV3(this, true);
	}

	/**
	 * Writes the extra version 2 header.
	 * 
	 * @param file the file to write the header to.
	 * 
	 * @throws IOException if an error occurs whilst writing the header. 
	 */
	@Override
	protected void writeExtraHeader( PwsFile file )
	throws IOException
	{
		headerRecord.saveRecord(this);
	}
	
	/**
	 * Reads bytes from the file and decrypts them.  <code>buff</code> may be any length provided
	 * that is a multiple of <code>BLOCK_LENGTH</code> bytes in length.
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
		if (Util.bytesAreEqual(buff,  EOF_BYTES_RAW)) {
			throw new EndOfFileException();
		}
		
		byte[] decrypted;
		try {
			decrypted = twofishCbc.processCBC(buff);
		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException("Error decrypting field");
		}
		Util.copyBytes(decrypted, buff);
		//Algorithm.decrypt( buff );
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
		
		byte [] temp; // = Util.cloneByteArray( buff );
		try {
			temp = twofishCbc.processCBC(buff);
		} catch(Exception e) {
			throw new IOException("Error writing encrypted field");
		}
		writeBytes( temp );
	}

	/**
	 * @see org.pwsafe.lib.file.PwsFile#getBlockSize()
	 */
	@Override
	protected int getBlockSize() {
		return 16;
	}
	
	/**
	 * @return the headerV3
	 */
	private PwsFileHeaderV3 getHeaderV3() {
		
		try {
			return (PwsFileHeaderV3) sealedHeaderV3.getObject(getCipher(false));
		} catch (IllegalBlockSizeException e) {
			throw new MemoryKeyException(e);
		} catch (IOException e) {
			throw new MemoryKeyException(e);
		} catch (BadPaddingException e) {
			throw new MemoryKeyException(e);
		} catch (ClassNotFoundException e) {
			throw new MemoryKeyException(e);
		}
	}

	/**
	 * @param headerV3 the headerV3 to set
	 */
	private void setHeaderV3(PwsFileHeaderV3 headerV3) {
		try {
			sealedHeaderV3 = new SealedObject(headerV3, getCipher(true));
		} catch (IllegalBlockSizeException e) {
			throw new MemoryKeyException(e);
		} catch (IOException e) {
			throw new MemoryKeyException(e);
		}
		
	}

}
