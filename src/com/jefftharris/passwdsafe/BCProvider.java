/*
 * Copyright (©) 2009-2011 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.security.Provider;

public class BCProvider extends Provider
{
    private static final long serialVersionUID = 1L;

    public BCProvider() {
        super("BC", 1.00, "Replacement BC provider with Blowfish");

        put("Cipher.BLOWFISH",
            "org.bouncycastle.jce.provider.JCEBlockCipher$Blowfish");
        put("Cipher.1.3.6.1.4.1.3029.1.2",
            "org.bouncycastle.jce.provider.JCEBlockCipher$BlowfishCBC");
        put("KeyGenerator.BLOWFISH",
            "org.bouncycastle.jce.provider.JCEKeyGenerator$Blowfish");
        put("Alg.Alias.KeyGenerator.1.3.6.1.4.1.3029.1.2", "BLOWFISH");
        put("AlgorithmParameters.BLOWFISH",
            "org.bouncycastle.jce.provider.JDKAlgorithmParameters$IVAlgorithmParameters");
        put("Alg.Alias.AlgorithmParameters.1.3.6.1.4.1.3029.1.2",
            "BLOWFISH");

    }
}
