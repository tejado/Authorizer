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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

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

	private static final int MAX_HEADER_LEN = PwsFile.STUFF_LENGTH +
	                                          PwsFile.HASH_LENGTH;

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
	 * @param header the file header bytes
	 * @param passphrase the file's passphrase.
	 *
	 * @return the password encoding
	 *
	 * @throws InvalidPassphraseException If the passphrase is not the correct one for the file.
	 * @throws NoSuchAlgorithmException   If no SHA-1 implementation is found.
	 */
	private static final String checkPassword( byte[] header,
	                                           String passphrase )
	    throws InvalidPassphraseException, NoSuchAlgorithmException
	{
	    LOG.enterMethod( "PwsFileFactory.checkPassword" );

	    byte []			phash;
	    String encoding = null;

	    byte[] stuff = Util.getBytes(header, 0, PwsFile.STUFF_LENGTH);
	    byte[] fhash = Util.getBytes(header, PwsFile.STUFF_LENGTH,
	                                 PwsFile.HASH_LENGTH);
	    byte[] fudged = new byte[ PwsFile.STUFF_LENGTH + 2 ];
	    System.arraycopy(stuff, 0, fudged, 0, PwsFile.STUFF_LENGTH);
	    stuff = null;

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
	            encoding = charset;
	            break;
	        }
	    }

	    if ( !validPassword )
	    {
	        LOG.debug1( "Password is incorrect - throwing InvalidPassphraseException" );
	        LOG.leaveMethod( "PwsFileFactory.checkPassword" );
	        throw new InvalidPassphraseException();
	    }

	    LOG.debug1( "Password is OK" );
	    LOG.leaveMethod( "PwsFileFactory.checkPassword" );
	    return encoding;
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

	    PwsFile file;

	    //TODOlib change to StringBuilder Constructors
	    String passphrase = aPassphrase.toString();

	    PwsStorage storage = new PwsFileStorage(filename);

	    try
	    {
	        byte[] header = storage.openForLoad(MAX_HEADER_LEN);

	        // First check for a v3 file...
	        byte[] first4Bytes = Util.getBytes(header, 0, 4);
	        if (Util.bytesAreEqual("PWS3".getBytes(), first4Bytes)) {
	            LOG.debug1( "This is a V3 format file." );
	            file = new PwsFileV3(storage, passphrase);
	            file.readAll();
	            file.close();
	            return file;
	        }

	        PwsRecordV1	rec;
	        String encoding = checkPassword( header, passphrase );
	        file = new PwsFileV1( storage, passphrase, encoding );
	        try {
	            rec = (PwsRecordV1) file.readRecord();
	        } catch (PasswordSafeException e) {
	            throw new IllegalStateException(e);
	        }
	        file.close();

	        // TODOlib what can we do about this?
	        // it will probably be fooled if someone is daft enough to create a V1 file with the
	        // title of the first record set to the value of PwsFileV2.ID_STRING!

	        if ( rec.getField(PwsRecordV1.TITLE).equals(PwsFileV2.ID_STRING) ) {
	            LOG.debug1( "This is a V2 format file." );
	            file = new PwsFileV2( storage, passphrase, encoding );
	        } else {
	            LOG.debug1( "This is a V1 format file." );
	            file = new PwsFileV1( storage, passphrase, encoding );
	        }
	        file.readAll();
	        file.close();

	        LOG.debug1( "File contains " + file.getRecordCount() + " records." );
	        LOG.leaveMethod( "PwsFileFactory.loadFile" );
	        return file;
	    } finally {
	        try {
	            storage.closeAfterLoad();
	        } catch (IOException ioe) {
	            LOG.error("Error closing file " + filename, ioe);
	        }
	    }
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
