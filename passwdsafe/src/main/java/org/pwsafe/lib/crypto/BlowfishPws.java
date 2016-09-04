/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.crypto;

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
    private final BlockCipher decipher;
    private final BlockCipher encipher;

    /**
     * Constructor, sets the initial vector to the value given.
     *
     * @param bfkey   the encryption/decryption key.
     * @param ivBytes the initial vector.
     */
    public BlowfishPws(byte[] bfkey, byte[] ivBytes)
    {
        this(bfkey, ivBytes, true);
    }

    /**
     * Constructor, sets the initial vector to the value given.
     *
     * @param bfkey   the encryption/decryption key.
     * @param ivBytes the initial vector.
     * @param cbc     Use CBC mode (otherwise ECB is used).  Normally this
     *                       should be true.
     */
    BlowfishPws(byte[] bfkey, byte[] ivBytes, boolean cbc)
    {
        byte[] riv = Util.cloneByteArray(ivBytes);
        Util.bytesToLittleEndian(riv);
        KeyParameter dkp;
        KeyParameter ekp;
        if (cbc) {
            decipher = new CBCBlockCipher(new BlowfishEngine());
            encipher = new CBCBlockCipher(new BlowfishEngine());
            dkp = new KeyParameter(bfkey);
            ParametersWithIV div = new ParametersWithIV(dkp, riv);
            ekp = new KeyParameter(bfkey);
            ParametersWithIV eiv = new ParametersWithIV(ekp, riv);
            decipher.init(false, div);
            encipher.init(true, eiv);
        } else {
                        /* ECB mode */
            decipher = new BlowfishEngine();
            encipher = new BlowfishEngine();
            dkp = new KeyParameter(bfkey);
            ekp = new KeyParameter(bfkey);
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
    public void decrypt(byte[] buffer) throws PasswordSafeException
    {
        int bs = decipher.getBlockSize();
        byte[] temp = new byte[buffer.length];
        Util.bytesToLittleEndian(buffer);

        if ((buffer.length % bs) != 0) {
            throw new PasswordSafeException(
                    "Block size must be a multiple of cipher block size (" +
                    bs + ")");
        }
        for (int i = 0; i < buffer.length; i += bs) {
            decipher.processBlock(buffer, i, temp, i);
        }

        Util.copyBytes(temp, buffer);
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
        int bs = encipher.getBlockSize();
        byte[] temp = new byte[buffer.length];
        Util.bytesToLittleEndian(buffer);

        if ((buffer.length % bs) != 0) {
            throw new PasswordSafeException(
                    "Block size must be a multiple of cipher block size (" +
                    bs + ")");
        }
        for (int i = 0; i < buffer.length; i += bs) {
            encipher.processBlock(buffer, i, temp, i);
        }

        Util.bytesToLittleEndian(temp);
        Util.copyBytes(temp, buffer);
    }

    protected static byte[] zeroIV()
    {
        byte[] ret = new byte[8];
        for (int i = 0; i < 8; i++) {
            ret[i] = 0;
        }
        return ret;
    }
}
