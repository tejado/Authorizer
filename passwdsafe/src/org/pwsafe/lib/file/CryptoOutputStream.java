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

import java.io.IOException;
import java.io.OutputStream;

import org.pwsafe.lib.Log;
import org.pwsafe.lib.Util;
import org.pwsafe.lib.crypto.BlowfishPws;
import org.pwsafe.lib.crypto.SHA1;
import org.pwsafe.lib.exception.PasswordSafeException;

/**
 * This class is used to encrypt an existing OutputStream
 * while providing an OutputStream interface itself.
 * 
 * Note that because of the block nature of encryption there
 * will normally be extra bytes at the end of such a stream to
 * represented the encrypted "zero padding" added at the end of
 * the original stream.
 * 
 * Also because of the block nature of this stream, there is
 * implicit (and unflushable) buffering involved so closing
 * the stream is extremely important.
 * 
 * @author mtiller
 *
 */
public class CryptoOutputStream extends OutputStream {
	private static final Log LOG = Log.getInstance(CryptoOutputStream.class.getPackage().getName());
	
	private final byte [] block = new byte[16];
	private int index = 0;
	/* Header info */
	private byte [] 	randStuff = null;
	private byte []	randHash = null;
	private byte [] 	salt = null;
	private byte [] 	ipThing = null;
	
	private final String passphrase;
	private final OutputStream rawStream;
	private BlowfishPws engine;
	/**
	 * The constructor for the encrytped output stream class.
	 * @param passphrase A passphrase used for encryption
	 * @param stream The stream to be encrypted.
	 */
	public CryptoOutputStream(String passphrase, OutputStream stream) {
		rawStream = stream;
		this.passphrase = passphrase;
	}
	/**
	 * Constructs and initialises the blowfish encryption routines ready to decrypt or
	 * encrypt data.
	 * 
	 * @param passphrase
	 * 
	 * @return A properly initialised {@link BlowfishPws} object.
	 */
	private BlowfishPws makeBlowfish( byte [] passphrase )
	{
		SHA1	sha1;
		
		sha1 = new SHA1();

		sha1.update( passphrase, 0, passphrase.length );
		sha1.update( salt, 0, salt.length );
		sha1.finalize();

		return new BlowfishPws( sha1.getDigest(), ipThing );
	}
	/**
	 * a routine to initialize the encryption data structures
	 * and generate some random header data.
	 * @throws IOException
	 */
	private void initialize() throws IOException {
		randStuff = new byte[8];
		randHash = new byte[20];
		salt = new byte[20];
		ipThing = new byte[8];
		Util.newRandBytes(randStuff);
		byte [] temp = Util.cloneByteArray( randStuff, 10 );
		randHash = PwsFileFactory.genRandHash( passphrase, temp );
		Util.newRandBytes(salt);
		Util.newRandBytes(ipThing);
		engine = makeBlowfish(passphrase.getBytes());
		rawStream.write(randStuff);
		rawStream.write(randHash);
		rawStream.write(salt);
		rawStream.write(ipThing);
	}
	/**
	 * Closes the stream and writes out the final block (zero padded if
	 * necessary.
	 */
	@Override
	public void close() throws IOException {
		if (salt==null) initialize();
		for(;index<16;index++) { block[index] = 0; }
		index = 0;
		try {
			engine.encrypt(block);
		} catch (PasswordSafeException e) {
			LOG.error(e.getMessage());
		}
		rawStream.write(block);
		rawStream.close();
		super.close();
	}
	/**
	 * Writes an individual byte.
	 */
	@Override
	public void write(int b) throws IOException {
		/** first time through, parse header and set up engine */
		if (salt==null) initialize();
		if (index==16) {
			try {
				engine.encrypt(block);
			} catch (PasswordSafeException e) {
				LOG.error(e.getMessage());
			}
			rawStream.write(block);
			index = 0;
		}
		block[index] = (byte)b;
		index++;
	}
}
