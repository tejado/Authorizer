/*
 * Copyright (©) 2017 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.test.util;

import org.junit.Test;
import org.pwsafe.lib.crypto.HmacPws;

import java.security.InvalidKeyException;

import static org.junit.Assert.assertArrayEquals;

/**
 * Tests for HMAC 256.  Test vectors from https://tools.ietf.org/html/rfc4231
 */

public final class HmacTest
{
    @Test
    public void testHmac() throws InvalidKeyException
    {
        hmacTest("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b",
                 "4869205468657265",
                 "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7");
        hmacTest("4a656665",
                 "7768617420646f2079612077616e7420666f72206e6f7468696e673f",
                 "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843");
        hmacTest("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                 "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd",
                 "773ea91e36800e46854db8ebd09181a72959098b3ef8c122d9635514ced565fe");
        hmacTest("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                 "5468697320697320612074657374207573696e672061206c6172676572207468616e20626c6f636b2d73697a65206b657920616e642061206c6172676572207468616e20626c6f636b2d73697a6520646174612e20546865206b6579206e6565647320746f20626520686173686564206265666f7265206265696e6720757365642062792074686520484d414320616c676f726974686d2e",
                 "9b09ffa71b942fcb27635fbcd5b0e944bfdc63644f0713938a7f51535c3a35e2");
    }

    private static void hmacTest(String keystr,
                                 String msgstr,
                                 String msghmacstr) throws InvalidKeyException
    {
        byte[] key = TestUtils.hexToBytes(keystr);
        byte[] msg = TestUtils.hexToBytes(msgstr);
        byte[] msghmac = TestUtils.hexToBytes(msghmacstr);

        HmacPws hmac = new HmacPws(key);
        hmac.digest(msg);

        byte[] rc = hmac.doFinal();
        assertArrayEquals(msghmac, rc);
    }
}
