/*
 * Copyright (©) 2017 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.test.util;

import org.junit.Test;
import org.pwsafe.lib.crypto.SHA1;
import org.pwsafe.lib.crypto.SHA256Pws;

import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertArrayEquals;

/**
 * Tests for SHA hashing.  Test values from
 * http://csrc.nist.gov/groups/STM/cavp/secure-hashing.html#sha-1
 */

public final class SHATest
{
    @Test
    public void testSha1() throws NoSuchAlgorithmException
    {
        sha1Test("", "da39a3ee5e6b4b0d3255bfef95601890afd80709");
        sha1Test("36", "c1dfd96eea8cc2b62785275bca38ac261256e278");
        sha1Test("549e959e", "b78bae6d14338ffccfd5d5b5674a275f6ef9c717");
        sha1Test("63a3cc83fd1ec1b6680e9974a0514e1a9ecebb6a",
                 "8bb8c0d815a9c68a1d2910f39d942603d807fbcc");
        sha1Test("45927e32ddf801caf35e18e7b5078b7f5435278212ec6bb99df884f49b327c6486feae46ba187dc1cc9145121e1492e6b06e9007394dc33b7748f86ac3207cfe",
                 "a70cfbfe7563dd0e665c7c6715a96a8d756950c0");
    }

    @Test
    public void testSha256() throws NoSuchAlgorithmException
    {
        sha256Test("",
                   "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                   "0dc9b0e0900f0ce71f36c359cbcf968d6366f2762f5699a2f5ea5fdccb70f0c8");
        sha256Test("d3",
                   "28969cdfa74a12c82f3bad960b0b000aca2ac329deea5c2328ebc6f2ba9802c1",
                   "7476e1d6a2b34c25f7d81f27c90c7dae56dd55ad99aa4237681df0f2e746c370");
        sha256Test("74ba2521",
                   "b16aa56be3880d18cd41e68384cf1ec8c17680c45a02b1575dc1518923ae8b0e",
                   "f36e570f0aa117bc0f141a1d0561ece96b20715ab5dbd55720effd4af19831a9");
        sha256Test("09fc1accc230a205e4a208e64a8f204291f581a12756392da4b8c0cf5ef02b95",
                   "4f44c1c7fbebb6f9601829f3897bfd650c56fa07844be76489076356ac1886a4",
                   "06c2b00af2857cac1e8004ce46ecd055ccba64817192e9b480e7279a9d67549b");
        sha256Test("5a86b737eaea8ee976a0a24da63e7ed7eefad18a101c1211e2b3650c5187c2a8a650547208251f6d4237e661c7bf4c77f335390394c37fa1a9f9be836ac28509",
                   "42e61e174fbb3897d6dd6cef3dd2802fe67b331953b06114a65c772859dfc1aa",
                   "6ec162a3d8b2070178bbbc296de29b4e2bf3263c5729b9cb11a4ef7cd36f7915");
    }

    private static void sha1Test(String msgstr, String mdstr)
    {
        byte[] msg = TestUtils.hexToBytes(msgstr);
        byte[] md = TestUtils.hexToBytes(mdstr);
        byte[] msgmd;

        SHA1 sha = new SHA1();
        sha.update(msg, 0, msg.length);
        sha.finish();
        msgmd = sha.getDigest();
        assertArrayEquals(md, msgmd);
    }

    private static void sha256Test(String msgstr, String mdstr,
                                   String md1000str)
    {
        byte[] msg = TestUtils.hexToBytes(msgstr);
        byte[] md = TestUtils.hexToBytes(mdstr);
        byte[] md1000 = TestUtils.hexToBytes(md1000str);

        byte[] msgmd = SHA256Pws.digest(msg);
        assertArrayEquals(md, msgmd);
        msgmd = SHA256Pws.digestN(msg, 0);
        assertArrayEquals(md, msgmd);

        msgmd = SHA256Pws.digestN(msg, 1000);
        assertArrayEquals(md1000, msgmd);
        msgmd = SHA256Pws.digestNJava(msg, 1000);
        assertArrayEquals(md1000, msgmd);
        msgmd = SHA256Pws.digestNNative(msg, 1000);
        assertArrayEquals(md1000, msgmd);
    }
}
