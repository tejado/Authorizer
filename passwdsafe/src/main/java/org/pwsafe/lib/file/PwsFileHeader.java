/*
 * $Id: PwsFileHeader.java 366 2009-03-26 22:59:08Z roxon $
 * 
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.pwsafe.lib.Log;
import org.pwsafe.lib.Util;
import org.pwsafe.lib.exception.EndOfFileException;

/**
 * This class encapsulates the header fields of a PasswordSafe database.  The header comprises:
 * </p><p>
 * <tt>
 * <pre> +--------+-----------+-----------------------------------------------+
 * | Length | Name      | Description                                   |
 * +--------+-----------+-----------------------------------------------+
 * |      8 | RandStuff | Random bytes                                  |
 * |     20 | RandHash  | Random hash                                   |
 * |     20 | Salt      | Salt                                          |
 * |      8 | IpThing   | Initial vector for decryption                 |
 * +--------+-----------+-----------------------------------------------+</pre>
 * </tt>
 * </p>
 * 
 * @author Kevin Preece
 */
public class PwsFileHeader
{
	private static final Log LOG = Log.getInstance(PwsFileHeader.class.getPackage().getName());

	private final byte []	RandStuff	= new byte[8];
	private byte []	RandHash	= new byte[20];
	private final byte [] Salt		= new byte[20];
	private final byte [] IpThing		= new byte[8];

	/**
	 * Creates an empty file header.
	 */
	PwsFileHeader()
	{
	}

	/**
	 * Constructs the PasswordSafe file header by reading the header data from <code>file</code>.
	 * 
	 * @param file the file to read the header from.
	 * 
	 * @throws IOException        If an error occurs whilst reading from the file.
	 * @throws EndOfFileException If end of file is reached before reading all the data.
	 */
	public PwsFileHeader( PwsFile file )
	throws IOException, EndOfFileException
	{
		file.readBytes( RandStuff );
		file.readBytes( RandHash );
		file.readBytes( Salt );
		file.readBytes( IpThing );
	}

	/**
	 * Gets a copy of IpThing - the initial vector for encryption/decryption.
	 * 
	 * @return A copy of IpThing
	 */
	public byte [] getIpThing()
	{
		return Util.cloneByteArray( IpThing );
	}

	/**
	 * Gets a copy of RandHash.
	 * 
	 * @return A copy of RandHash
	 */
	public byte [] getRandHash()
	{
		return Util.cloneByteArray( RandHash );
	}

	/**
	 * Gets a copy of RandStuff.
	 * 
	 * @return A copy of RandStuff
	 */
	public byte [] getRandStuff()
	{
		return Util.cloneByteArray( RandStuff );
	}

	/**
	 * Gets a copy of Salt.
	 * 
	 * @return a copy of Salt.
	 */
	public byte [] getSalt()
	{
		return Util.cloneByteArray( Salt );
	}

	/**
	 * Write the header to the file.
	 * 
	 * @param file the file to write the header to.
	 * 
	 * @throws IOException
	 * @throws NoSuchAlgorithmException If no SHA-1 implementation is found.
	 */
	public void save( PwsFile file )
	throws IOException, NoSuchAlgorithmException
	{
		LOG.enterMethod( "PwsFileHeader.save" );

		update( file.getPassphrase() );

		file.writeBytes( RandStuff );
		file.writeBytes( RandHash );
		file.writeBytes( Salt );
		file.writeBytes( IpThing );

		LOG.leaveMethod( "PwsFileHeader.save" );
	}

	/**
	 * Updates the header ready for saving.
	 * 
	 * @param passphrase the passphrase to be used to encrypt the database.
	 */
	private void update( String passphrase )
	{
		LOG.enterMethod( "PwsFileHeader.update" );

		byte	temp[];

//		for ( int ii = 0; ii < RandStuff.length; ++ii )
//		{
//			RandStuff[ii] = Util.newRand();
//		}
		Util.newRandBytes(RandStuff);
		temp		= Util.cloneByteArray( RandStuff, 10 );
		RandHash	= PwsFileFactory.genRandHash( passphrase, temp );
		
//		for ( int ii = 0; ii < Salt.length; ++ii )
//		{
//			Salt[ii] = Util.newRand();
//		}
		Util.newRandBytes(Salt);
		
//		for ( int ii = 0; ii < IpThing.length; ++ii )
//		{
//			IpThing[ii] = Util.newRand();
//		}
		Util.newRandBytes(IpThing);

		LOG.leaveMethod( "PwsFileHeader.update" );
	}
}
