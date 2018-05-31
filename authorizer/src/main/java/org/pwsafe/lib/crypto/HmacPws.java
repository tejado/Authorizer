/*
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.crypto;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * HMAC implementation. Currently uses BouncyCastle provider underneath.
 *
 * @author Glen Smith
 */
public class HmacPws
{

    private final HMac mac;

    public HmacPws(byte[] key)
    {
        mac = new HMac(new SHA256Digest());
        KeyParameter kp = new KeyParameter(key);
        mac.init(kp);
    }

    public final void digest(byte[] incoming)
    {
        mac.update(incoming, 0, incoming.length);
    }

    public final byte[] doFinal()
    {
        byte[] output = new byte[mac.getUnderlyingDigest().getDigestSize()];
        mac.doFinal(output, 0);
        return output;
    }
}
