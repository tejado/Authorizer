/*
 * $Id: CryptoInputStream.java 401 2009-09-07 21:41:10Z roxon $
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

import org.pwsafe.lib.Log;
import org.pwsafe.lib.crypto.BlowfishPws;
import org.pwsafe.lib.crypto.SHA1;
import org.pwsafe.lib.exception.PasswordSafeException;

/**
 * This class is used to decrypt an existing InputStream
 * while providing an InputStream interface itself.
 * 
 * Note that because of the block nature of encryption there
 * will normally be extra bytes at the end of such a stream.
 * 
 * @author mtiller
 *
 */
@SuppressWarnings("ALL")
public class CryptoInputStream extends InputStream {
	private static final Log LOG = Log.getInstance(CryptoInputStream.class.getPackage().getName());
	
	private byte [] block = new byte[16];
	private int index = 0;
	private int curBlockSize = 0;
	/* Header info */
	private byte []	randStuff = null;
	private byte []	randHash = null;
	private byte [] salt = null;
	private byte [] ipThing = null;
	
	private String passphrase;
	private InputStream rawStream;
	private BlowfishPws engine;
	/**
	 * Constructor
	 * @param passphrase The passphrase used to decrypt the stream
	 * @param stream The stream to be decrypted
	 */
	public CryptoInputStream(String passphrase, InputStream stream) {
		rawStream = stream;
		this.passphrase = passphrase;
	}

	/**
	 * Read a single byte.  Behind the scenes, a complete block
	 * must be read in and decrypted.
	 */
	public int read() throws IOException {
		/** first time through, parse header and set up engine */
		if (salt==null) {
			randStuff = new byte[8];
			randHash = new byte[20];
			salt = new byte[20];
			ipThing = new byte[8];
			rawStream.read(randStuff);
			rawStream.read(randHash);
			rawStream.read(salt);
			rawStream.read(ipThing);
			engine = makeBlowfish(passphrase.getBytes());
			curBlockSize = rawStream.read(block);
			if (curBlockSize==-1) { return -1; } 
			try {
				engine.decrypt(block);
			} catch (PasswordSafeException e) {
				LOG.error(e.getMessage());
			}
		}
		if (index<curBlockSize) {
			/** Get next byte in existing buffer */
			index++;
			return (int) block[index-1] & 0xff;
		} else {
			/** Read a new block */
			curBlockSize = rawStream.read(block);
			if (curBlockSize==-1) { return -1; }
			try {
				engine.decrypt(block);
			} catch (PasswordSafeException e) {
				LOG.error(e.getMessage());
			}
			index = 1;
			return (int)block[0] & 0xff;
		}
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
	 * Closes the stream (and the stream it is reading from)
	 */
	public void close() throws IOException {
		rawStream.close();
		super.close();
	}
}
