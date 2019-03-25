/*
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.crypto;

import android.os.Build;
import androidx.annotation.NonNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * SHA256 implementation. Currently uses default digester provider or native
 * code underneath.
 *
 * @author Glen Smith
 * @author Jeff Harris
 */
public class SHA256Pws {

    private static final boolean IS_CHROME;
    static {
        String brand = Build.BRAND.toLowerCase(Locale.getDefault());
        IS_CHROME = (brand.contains("chromium"));
    }

    /**
     * Hash the incoming bytes iter+1 times
     */
    public static byte[] digestN(byte[] p, int iter)
    {
        if (IS_CHROME) {
            return digestNJava(p, iter);
        } else {
            return digestNNative(p, iter);
        }
    }

    /**
     * Hash the incoming bytes
     */
    public static byte[] digest(byte[] incoming) {

        return getSha().digest(incoming);
    }

    /**
     * Hash the incoming bytes iter+1 times using the Java provider
     */
    public static byte[] digestNJava(byte[] p, int iter)
    {
        MessageDigest digest = getSha();
        byte[] output = digest.digest(p);

        for (int i = 0; i < iter; ++i) {
            output = digest.digest(output);
        }

        return output;
    }

    /**
     * Hash the incoming bytes iter+1 times using native code
     */
    public static native byte[] digestNNative(byte[] p, int iter);

    /**
     * Get the default provider's SHA-256 digester
     */
    @NonNull
    private static MessageDigest getSha()
    {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            //noinspection ConstantConditions
            return null;
        }
    }
}
