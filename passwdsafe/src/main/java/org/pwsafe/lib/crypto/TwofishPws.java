/*
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.crypto;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.engines.TwofishEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * Twofish implementation wrapper. Current implementation uses
 * BouncyCastle provider.
 *
 * @author Glen Smith
 */
public class TwofishPws
{
    private final CBCBlockCipher cipher;

    public TwofishPws(byte[] key, boolean forEncryption, byte[] IV)
    {
        TwofishEngine tfe = new TwofishEngine();
        cipher = new CBCBlockCipher(tfe);
        KeyParameter kp = new KeyParameter(key);
        ParametersWithIV piv = new ParametersWithIV(kp, IV);
        cipher.init(forEncryption, piv);
    }

    public final byte[] processCBC(byte[] input)
    {
        byte[] out = new byte[input.length];
        cipher.processBlock(input, 0, out, 0);
        return out;
    }

    public static byte[] processECB(byte[] key, boolean forEncryption,
                                    byte[] input)
    {
        BufferedBlockCipher cipher = new BufferedBlockCipher(
                new TwofishEngine());
        KeyParameter kp = new KeyParameter(key);
        cipher.init(forEncryption, kp);
        byte[] out = new byte[input.length];

        int len1 = cipher.processBytes(input, 0, input.length, out, 0);

        try {
            cipher.doFinal(out, len1);
        } catch (CryptoException e) {
            throw new RuntimeException(e);
        }
        return out;
    }
}
