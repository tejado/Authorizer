/*
 * $Id: PwsFileHeader.java 307 2004-02-27 23:11:59 +0000 (Fri, 27 Feb 2004) preecek $
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
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import org.pwsafe.lib.Log;
import org.pwsafe.lib.Util;
import org.pwsafe.lib.crypto.HmacPws;
import org.pwsafe.lib.crypto.SHA256Pws;
import org.pwsafe.lib.crypto.TwofishPws;
import org.pwsafe.lib.exception.EndOfFileException;

/**
 * This class encapsulates the header fields of a PasswordSafe database.  The header comprises:
 * </p><p>
 * <tt>
 * <pre> +--------+-----------+-----------------------------------------------+
 * | Length | Name      | Description                                   |
 * +--------+-----------+-----------------------------------------------+
 * |      8 | tag       | PWS3 tag                                      |
 * |     32 | salt      | Salt                                          |
 * |      4 | iter      | number of iterations                          |
 * |     32 | password  | Sha256 of stretched password                  |
 * |     16 | b1        | key material                                  |
 * |     16 | b2        | key material                                  |
 * |     16 | b3        | key material                                  |
 * |     16 | b6        | key material                                  |
 * |     16 | IV        | twofish IV                                    |
 * +--------+-----------+-----------------------------------------------+</pre>
 * </tt>
 * </p>
 *
 * @author Glen Smith (based on the work of Kevin Preece)
 */
public class PwsFileHeaderV3 implements Serializable
{
	/**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Log LOG = Log.getInstance(PwsFileHeaderV3.class.getPackage().getName());

	private byte [] tag 			= new byte[4];
	private final byte [] salt		= new byte[32];
	private final int iter;
	private byte [] password		= new byte[32];
	private byte [] b1 				= new byte[16];
	private byte [] b2 				= new byte[16];
	private byte [] b3 				= new byte[16];
	private byte [] b4 				= new byte[16];
	private final byte [] IV 		= new byte[16];


	/**
	 * Creates an empty file header.
	 */
	PwsFileHeaderV3()
	{
		tag = PwsFileV3.ID_STRING;
		iter = 2048;
		Util.newRandBytes(salt);
		Util.newRandBytes(IV);
	}

	/**
	 * Constructs the PasswordSafe file header by reading the header data from <code>file</code>.
	 *
	 * @param file the file to read the header from.
	 *
	 * @throws IOException        If an error occurs whilst reading from the file.
	 * @throws EndOfFileException If end of file is reached before reading all the data.
	 */
	public PwsFileHeaderV3( PwsFile file )
	throws IOException, EndOfFileException
	{
		file.readBytes( tag );
		file.readBytes( salt );
		byte[] iterBytes = new byte[4];
		file.readBytes( iterBytes );
		iter = Util.getIntFromByteArray(iterBytes, 0);
		file.readBytes( password );
		file.readBytes( b1 );
		file.readBytes( b2 );
		file.readBytes( b3 );
		file.readBytes( b4 );
		file.readBytes( IV );
	}

	/**
	 * Constructs the PasswordSafe file header by reading the
	 * header data from an <code>InputStream</code>.
	 *
	 * @param stream the file to read the header from.
	 *
	 * @throws IOException        If an error occurs whilst reading from the file.
	 * @throws EndOfFileException If end of file is reached before reading all the data.
	 */
	public PwsFileHeaderV3( InputStream stream )
	throws IOException, EndOfFileException
	{
		stream.read( tag );
		stream.read( salt );
		byte[] iterBytes = new byte[4];
		stream.read( iterBytes );
		iter = Util.getIntFromByteArray(iterBytes, 0);
		stream.read( password );
		stream.read( b1 );
		stream.read( b2 );
		stream.read( b3 );
		stream.read( b4 );
		stream.read( IV );
	}

	/**
	 * Gets a copy of Tag.
	 *
	 * @return A copy of Tag
	 */
	public byte [] getTag()
	{
		return Util.cloneByteArray( tag );
	}

	/**
	 * Gets a copy of Salt.
	 *
	 * @return A copy of Salt
	 */
	public byte [] getSalt()
	{
		return Util.cloneByteArray( salt );
	}

	/**
	 * Gets the number of Iterations.
	 *
	 * @return number of iterations
	 */
	public int getIter()
	{
		return iter;
	}

	/**
	 * Gets a copy of the stretched password.
	 *
	 * @return A copy of the stretched password
	 */
	public byte [] getPassword() {
		return Util.cloneByteArray(password);
	}

	/**
	 * Gets a copy of Salt.
	 *
	 * @return a copy of Salt.
	 */
	public byte [] getB1()
	{
		return Util.cloneByteArray( b1 );
	}

	/**
	 * Gets a copy of Salt.
	 *
	 * @return a copy of Salt.
	 */
	public byte [] getB2()
	{
		return Util.cloneByteArray( b2 );
	}

	/**
	 * Gets a copy of Salt.
	 *
	 * @return a copy of Salt.
	 */
	public byte [] getB3()
	{
		return Util.cloneByteArray( b3 );
	}

	/**
	 * Gets a copy of Salt.
	 *
	 * @return a copy of Salt.
	 */
	public byte [] getB4()
	{
		return Util.cloneByteArray( b4 );
	}

	/**
	 * Gets a copy of IV.
	 *
	 * @return a copy of IV.
	 */
	public byte [] getIV()
	{
		return Util.cloneByteArray( IV );
	}

	/**
	 * Write the header to the file.
	 *
	 * @param file the file to write the header to.
	 *
	 * @throws IOException
	 */
	public void save( PwsFile file )
	throws IOException
	{
		LOG.enterMethod( "PwsFileHeaderV3.save" );

		update( file.getPassphrase(), (PwsFileV3) file );

		file.writeBytes( tag );
		file.writeBytes( salt );
		final byte[] iterBytes = new byte[4];
		Util.putIntToByteArray(iterBytes, iter, 0);
		file.writeBytes( iterBytes );
		file.writeBytes( password );
		file.writeBytes( b1 );
		file.writeBytes( b2 );
		file.writeBytes( b3 );
		file.writeBytes( b4 );
		file.writeBytes( IV );

		LOG.leaveMethod( "PwsFileHeaderV3.save" );
	}

	/**
	 * Updates the header ready for saving.
	 *
	 * @param aPassphrase the passphrase to be used to encrypt the database.
	 * @throws UnsupportedEncodingException
	 */
	private void update( String aPassphrase, PwsFileV3 file )
	    throws UnsupportedEncodingException
	{
		LOG.enterMethod( "PwsFileHeaderV3.update" );

		final byte[] stretchedPassword =
		    Util.stretchPassphrase(
		        aPassphrase.getBytes(PwsFile.getUpdatePasswordEncoding()),
		        salt, iter);

		password = SHA256Pws.digest(stretchedPassword);

		final byte[] b1pt = new byte[16];
		Util.newRandBytes(b1pt);

		final byte[] b2pt = new byte[16];
		Util.newRandBytes(b2pt);

		b1 = TwofishPws.processECB(stretchedPassword, true, b1pt);
		b2 = TwofishPws.processECB(stretchedPassword, true, b2pt);

		file.decryptedRecordKey = Util.mergeBytes(b1pt, b2pt);

		final byte[] b3pt = new byte[16];
		Util.newRandBytes(b3pt);

		final byte[] b4pt = new byte[16];
		Util.newRandBytes(b4pt);

		b3 = TwofishPws.processECB(stretchedPassword, true, b3pt);
		b4 = TwofishPws.processECB(stretchedPassword, true, b4pt);

		file.decryptedHmacKey = Util.mergeBytes(b3pt, b4pt);
		file.hasher = new HmacPws(file.decryptedHmacKey);

		LOG.leaveMethod( "PwsFileHeaderV3.update" );
	}
}
