/*
 * $Id$
 * 
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.crypto;

import org.pwsafe.lib.Util;
import org.pwsafe.lib.exception.PasswordSafeException;

public class BlowfishPwsECB extends BlowfishPws
{
    /**
     * Constructor, sets the initial vector to zero.
     *
     * @param bfkey the encryption/decryption key.
     */
    public BlowfishPwsECB(byte[] bfkey)
    {
        super(bfkey, zeroIV(), false);
    }

    /**
     * Decrypts <code>buffer</code> in place.
     *
     * @param buffer the buffer to be decrypted.
     * @throws PasswordSafeException
     */
    public void decrypt(byte[] buffer) throws PasswordSafeException
    {
        /* The endian conversion is simply to make this compatible with
	 * use in previous versions of PasswordSafe (in ECB mode).  Why
	 * the inversion is necessary for CBC mode and why it has to
	 * "cancelled out" in this (ECB mode), I don't know but it
	 * is the only way to get the correct ordering for the
	 * CBC and ECB contexts within a standard password safe file.
	 */
        Util.bytesToLittleEndian(buffer);
        super.decrypt(buffer);
        Util.bytesToLittleEndian(buffer);
    }

    /**
     * Encrypts <code>buffer</code> in place.
     *
     * @param buffer the buffer to be encrypted.
     * @throws PasswordSafeException
     */
    public void encrypt(byte[] buffer) throws PasswordSafeException
    {
	/* The endian conversion is simply to make this compatible with
	 * use in previous versions of PasswordSafe (in ECB mode).  Why
	 * the inversion is necessary for CBC mode and why it has to
	 * "cancelled out" in this (ECB mode), I don't know but it
	 * is the only way to get the correct ordering for the
	 * CBC and ECB contexts within a standard password safe file.
	 */
        Util.bytesToLittleEndian(buffer);
        super.encrypt(buffer);
        Util.bytesToLittleEndian(buffer);
    }
}
