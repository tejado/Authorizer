/*
 * $Id: BlowfishPws.java 317 2009-01-26 20:20:54Z ronys $
 * 
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.crypto;

import java.nio.ByteBuffer;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.engines.BlowfishEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.pwsafe.lib.Util;
import org.pwsafe.lib.exception.PasswordSafeException;

/**
 * A reimplementation of the BlowfishPws class to use the Bouncy Castle
 * implementation of Blowfish.
 * 
 * @author Michael Tiller
 */
public class BlowfishPws
{ 
	private boolean cbc;
	private BlockCipher decipher;
	private BlockCipher encipher;
	private ParametersWithIV div;
	private KeyParameter dkp;
	private ParametersWithIV eiv;
	private KeyParameter ekp;
	
	/**
	 * Constructor, sets the initial vector to zero.
	 * 
	 * @param bfkey the encryption/decryption key.
	 * @param cbc Use CBC mode (otherwise ECB is used).  Normally this should be true.
	 * @throws PasswordSafeException 
	 */
	public BlowfishPws( byte[] bfkey ) throws PasswordSafeException
	{
		this(bfkey, zeroIV(), true);
	}

	/**
	 * Constructor, sets the initial vector to the value given.
	 * 
	 * @param bfkey      the encryption/decryption key.
	 * @param lInitCBCIV the initial vector.
	 * @param cbc Use CBC mode (otherwise ECB is used).  Normally this should be true.
	 * @throws PasswordSafeException 
	 */
	public BlowfishPws( byte[] bfkey, long lInitCBCIV ) throws PasswordSafeException
	{
		this(bfkey, makeByteKey(lInitCBCIV), true);
	}

	/**
	 * Constructor, sets the initial vector to the value given.
	 * 
	 * @param bfkey      the encryption/decryption key.
	 * @param ivBytes the initial vector.
	 * @param cbc Use CBC mode (otherwise ECB is used).  Normally this should be true.
	 * @throws PasswordSafeException 
	 */
	public BlowfishPws( byte[] bfkey, byte[] ivBytes ) {
		this(bfkey, ivBytes, true);
	}

	/**
	 * Constructor, sets the initial vector to the value given.
	 * 
	 * @param bfkey      the encryption/decryption key.
	 * @param ivBytes the initial vector.
	 * @param cbc Use CBC mode (otherwise ECB is used).  Normally this should be true.
	 * @throws PasswordSafeException 
	 */
	BlowfishPws( byte[] bfkey, byte[] ivBytes, boolean cbc )
	{
		this.cbc = cbc;
		byte[] riv = Util.cloneByteArray(ivBytes);
		Util.bytesToLittleEndian(riv);
		if (cbc) {
			decipher = new CBCBlockCipher(new BlowfishEngine());
			encipher = new CBCBlockCipher(new BlowfishEngine());
	    	dkp = new KeyParameter(bfkey);
	    	div = new ParametersWithIV(dkp, riv);
	    	ekp = new KeyParameter(bfkey);
	    	eiv = new ParametersWithIV(ekp, riv);
			decipher.init(false, div);
			encipher.init(true, eiv);
		} else {
			/* ECB mode */
			decipher = new BlowfishEngine();
			encipher = new BlowfishEngine();
	    	dkp = new KeyParameter(bfkey);
	    	ekp = new KeyParameter(bfkey);
	    	div = null;
	    	eiv = null;
			decipher.init(false, dkp);
			encipher.init(true, ekp);
		}
	}

	/**
	 * Decrypts <code>buffer</code> in place.
	 * 
	 * @param buffer the buffer to be decrypted.
	 * @throws PasswordSafeException 
	 */
	public void decrypt( byte[] buffer ) throws PasswordSafeException
	{
		int bs = decipher.getBlockSize();
		byte[] temp = new byte[buffer.length];
		Util.bytesToLittleEndian( buffer );

		if ((buffer.length % bs)!=0) {
			throw new PasswordSafeException("Block size must be a multiple of cipher block size ("+bs+")");
		}
		for(int i=0;i<buffer.length;i+=bs) {
			decipher.processBlock(buffer, i, temp, i);
		}
		
		Util.copyBytes(temp, buffer);
		Util.bytesToLittleEndian( buffer );
	}

	/**
	 * Encrypts <code>buffer</code> in place.
	 * 
	 * @param buffer the buffer to be encrypted.
	 * @throws PasswordSafeException 
	 */
	public void encrypt( byte[] buffer ) throws PasswordSafeException
	{
		int bs = encipher.getBlockSize();
		byte[] temp = new byte[buffer.length];
		Util.bytesToLittleEndian( buffer );

		if ((buffer.length % bs)!=0) {
			throw new PasswordSafeException("Block size must be a multiple of cipher block size ("+bs+")");
		}
		for(int i=0;i<buffer.length;i+=bs) {
			encipher.processBlock(buffer, i, temp, i);
		}

		Util.bytesToLittleEndian( temp );
		Util.copyBytes(temp, buffer);
	}

	/**
	 * Sets the initial vector.
	 * 
	 * @param newCBCIV the new value for the initial vector.
	 */
	public void setCBCIV( byte[] ivBytes )
	{
		byte[] riv = Util.cloneByteArray(ivBytes);
		Util.bytesToLittleEndian(riv);
		// Set the initial vector
		div = new ParametersWithIV(dkp, riv);
		eiv = new ParametersWithIV(ekp, riv);
		decipher.init(false, div);
		encipher.init(true, eiv);
	}

	protected static byte[] zeroIV() {
		byte[] ret = new byte[8];
		for(int i=0;i<8;i++) { ret[i] = 0; }
		return ret;
	}
	
	public static byte[] makeByteKey(long key) {
		byte ivBytes[] = new byte[8];
		ByteBuffer buf = ByteBuffer.wrap(ivBytes);  
		buf.putLong(key);
		return ivBytes;
	}
}
