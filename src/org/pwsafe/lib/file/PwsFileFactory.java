/*
 * $Id: PwsFileFactory.java 404 2009-09-21 19:19:25Z roxon $
 *
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import org.pwsafe.lib.I18nHelper;
import org.pwsafe.lib.Log;
import org.pwsafe.lib.Util;
import org.pwsafe.lib.crypto.BlowfishPwsECB;
import org.pwsafe.lib.crypto.SHA1;
import org.pwsafe.lib.datastore.PwsEntryStore;
import org.pwsafe.lib.datastore.PwsEntryStoreImpl;
import org.pwsafe.lib.exception.EndOfFileException;
import org.pwsafe.lib.exception.InvalidPassphraseException;
import org.pwsafe.lib.exception.PasswordSafeException;
import org.pwsafe.lib.exception.UnsupportedFileVersionException;

/**
 * This is a singleton factory class used to load a PasswordSafe file.  It is able to
 * determine which version of the file format the file has and returns the correct
 * subclass of {@link PwsFile}
 *
 * @author Kevin Preece
 */
public class PwsFileFactory {

	private static final Log LOG = Log.getInstance(PwsFileFactory.class.getPackage().getName());

	/**
	 * Private for the singleton pattern.
	 */
	private PwsFileFactory()
	{
	}

	/**
	 * Verifies that <code>passphrase</code> is actually the passphrase for the file.  It returns
	 * normally if everything is OK or {@link InvalidPassphraseException} if the passphrase is
	 * incorrect.
	 *
	 * @param filename   the name of the file to be opened.
	 * @param passphrase the file's passphrase.
	 *
	 * @throws InvalidPassphraseException If the passphrase is not the correct one for the file.
	 * @throws FileNotFoundException      If the given file does not exist.
	 * @throws IOException                If an error occurs whilst reading from the file.
	 * @throws NoSuchAlgorithmException   If no SHA-1 implementation is found.
	 */
	private static final void checkPassword( String filename, String passphrase )
	throws InvalidPassphraseException, FileNotFoundException, IOException, NoSuchAlgorithmException
	{
		LOG.enterMethod( "PwsFileFactory.checkPassword" );

		FileInputStream	fis				= null;
		byte []			stuff;
		byte []			fudged;
		byte []			fhash;
		byte []			phash;
		boolean			handlingError	= false;

		try
		{
			fis		= new FileInputStream( filename );
			stuff	= new byte[ PwsFile.STUFF_LENGTH ];
			fhash	= new byte[ PwsFile.HASH_LENGTH ];

			fis.read( stuff );
			fis.read( fhash );

			fudged	= new byte[ PwsFile.STUFF_LENGTH + 2 ];

			for ( int ii = 0; ii < PwsFile.STUFF_LENGTH; ++ii )
			{
				fudged[ii] = stuff[ii];
			}
			stuff	= null;
			boolean validPassword = false;
	        for (String charset : PwsFile.getPasswordEncodings()) {
	            LOG.debug1("Trying " + charset);
	            try {
	                phash = genRandHash(passphrase, charset, fudged);
	            } catch(UnsupportedEncodingException e) {
	                // Skip this charset
	                continue;
	            }

	            if (Util.bytesAreEqual(fhash, phash)) {
	                validPassword = true;
	                break;
	            }
	        }

			if ( !validPassword )
			{
				LOG.debug1( "Password is incorrect - throwing InvalidPassphraseException" );
				LOG.leaveMethod( "PwsFileFactory.checkPassword" );
				throw new InvalidPassphraseException();
			}
		}
		catch ( IOException e )
		{
			handlingError = true;
			LOG.error( I18nHelper.getInstance().formatMessage("E00007", new Object [] { e.getClass().getName() } ), e );
			LOG.info( "I00001" );
			LOG.leaveMethod( "PwsFileFactory.checkPassword" );
			throw e;
		}
		finally
		{
			if ( fis != null )
			{
				try
				{
					LOG.debug1( "Attempting to close the file" );
					fis.close();

					fis = null;
				}
				catch ( IOException e )
				{
					// log the exception then decide what we're going to do with it
					LOG.error( I18nHelper.getInstance().formatMessage("E00007", new Object [] { e.getClass().getName() } ), e );
					if ( handlingError )
					{
						// ignore the error
						LOG.info( I18nHelper.getInstance().formatMessage( "I00002" ) );
					}
					else
					{
						LOG.info( I18nHelper.getInstance().formatMessage( "I00001" ) );
						throw e;
					}
				}
			}
		}

		LOG.debug1( "Password is OK" );
		LOG.leaveMethod( "PwsFileFactory.checkPassword" );
	}

	static final byte[] genRandHash(String passphrase, byte[] stuff)
	{
	    try {
	        return genRandHash(passphrase, null, stuff);
	    } catch (UnsupportedEncodingException e) {
	        return new byte[0];
	    }
	}

	/**
	 * Generates a checksum from the passphrase and some random bytes.
	 *
	 * @param  passphrase the passphrase.
	 * @param  charEnc    the passphrase charset encoding
	 * @param  stuff      the random bytes.
	 *
	 * @return the generated checksum.
	 */
	static final byte [] genRandHash( String passphrase,
	                                  String charEnc,
	                                  byte [] stuff )
	    throws UnsupportedEncodingException
	{
		LOG.enterMethod( "PwsFileFactory.genRandHash" );

		SHA1			md;
		BlowfishPwsECB	ecb;
		byte []			pw;
		byte []			digest;
		byte []			tmp;

		pw	= (charEnc == null) ?
		    passphrase.getBytes() : passphrase.getBytes(charEnc);
		md	= new SHA1();

		md.update( stuff, 0, stuff.length );
		md.update( pw, 0, pw.length );
		md.finalize();
		digest = md.getDigest();

		try {
			ecb = new BlowfishPwsECB(digest);
			tmp	= Util.cloneByteArray( stuff, 8 );

			Util.bytesToLittleEndian( tmp );

			for ( int ii = 0; ii < 1000; ++ii )
			{
				ecb.encrypt(tmp);
			}

			Util.bytesToLittleEndian( tmp );
			tmp = Util.cloneByteArray( tmp, 10 );

			md.clear();
			md.update( tmp, 0, tmp.length );
			md.finalize();

		} catch (PasswordSafeException e) {
			LOG.error(e.getMessage()); // This should not happen!
		}

		LOG.leaveMethod( "PwsFileFactory.genRandHash" );
		return md.getDigest();
	}

	/**
	 * Loads a Password Safe file.  It returns the appropriate subclass of {@link PwsFile}.
	 *
	 * @deprecated use the StringBuilder version instead
	 * @param filename   the name of the file to open
	 * @param passphrase the passphrase for the file
	 *
	 * @return The correct subclass of {@link PwsFile} for the file.
	 *
	 * @throws EndOfFileException
	 * @throws FileNotFoundException
	 * @throws InvalidPassphraseException
	 * @throws IOException
	 * @throws UnsupportedFileVersionException
	 * @throws NoSuchAlgorithmException        If no SHA-1 implementation is found.
	 */
	@Deprecated
	public static final PwsFile loadFile( String filename, String passphrase )
	throws EndOfFileException, FileNotFoundException, InvalidPassphraseException, IOException, UnsupportedFileVersionException, NoSuchAlgorithmException
	{
		return loadFile(filename, new StringBuilder(passphrase));
	}

	/**
	 * Loads a Password Safe file.  It returns the appropriate subclass of {@link PwsFile}.
	 *
	 * @param filename   the name of the file to open
	 * @param passphrase the passphrase for the file
	 *
	 * @return The correct subclass of {@link PwsFile} for the file.
	 *
	 * @throws EndOfFileException
	 * @throws FileNotFoundException
	 * @throws InvalidPassphraseException
	 * @throws IOException
	 * @throws UnsupportedFileVersionException
	 * @throws NoSuchAlgorithmException        If no SHA-1 implementation is found.
	 */
	public static final PwsFile loadFile( String filename, StringBuilder aPassphrase )
	throws EndOfFileException, FileNotFoundException, InvalidPassphraseException, IOException, UnsupportedFileVersionException, NoSuchAlgorithmException
	{
		LOG.enterMethod( "PwsFileFactory.loadFile" );

		PwsFile		file;

		//TODO change to StringBuilder Constructors
		String passphrase = aPassphrase.toString();

		// First check for a v3 file...
		FileInputStream fis = new FileInputStream(filename);
		byte[] first4Bytes = new byte[4];
		fis.read(first4Bytes);
		fis.close();
		if (Util.bytesAreEqual("PWS3".getBytes(), first4Bytes)) {
			LOG.debug1( "This is a V3 format file." );
			file = new PwsFileV3(new PwsFileStorage(filename), passphrase);
			file.readAll();
			file.close();
			return file;
		}

		PwsRecordV1	rec;

		checkPassword( filename, passphrase );

		file	= new PwsFileV1( new PwsFileStorage(filename), passphrase );
		try {
			rec		= (PwsRecordV1) file.readRecord();
		} catch (PasswordSafeException e) {
			throw new IllegalStateException(e);
		}

		file.close();

		// TODO what can we do about this?
		// it will probably be fooled if someone is daft enough to create a V1 file with the
		// title of the first record set to the value of PwsFileV2.ID_STRING!

		if ( rec.getField(PwsRecordV1.TITLE).equals(PwsFileV2.ID_STRING) ) {
			LOG.debug1( "This is a V2 format file." );
			file = new PwsFileV2( new PwsFileStorage(filename), passphrase );
		} else {
			LOG.debug1( "This is a V1 format file." );
			file = new PwsFileV1( new PwsFileStorage(filename), passphrase );
		}
		file.readAll();
		file.close();

		LOG.debug1( "File contains " + file.getRecordCount() + " records." );
		LOG.leaveMethod( "PwsFileFactory.loadFile" );
		return file;
	}

	/**
	 * Creates a new, empty PasswordSafe database in memory.  The database will always
	 * be the latest version supported by this library which for this release is version 2.
	 *
	 * @return A new empty PasswordSafe database.
	 */
	public static final PwsFile newFile()
	{
		return new PwsFileV3();
	}

	public static PwsEntryStore getStore (PwsFile aFile) {
		return new PwsEntryStoreImpl(aFile);
	}

	public static PwsEntryStore getStore (PwsFile aFile, Set<PwsFieldType> sparseFields) {
		return new PwsEntryStoreImpl(aFile, sparseFields);

	}

}
