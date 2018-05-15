/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.crypto;

import org.pwsafe.lib.Util;

/**
 * SHA-1 message digest implementation, translated from C source code (the
 * origin is unknown).
 */
public class SHA1
{
    /**
     * size of a SHA-1 digest in octets
     */
    private final static int DIGEST_SIZE = 20;

    ///////////////////////////////////////////////////////////////////////////

    private final int[] m_state;
    private long m_lCount;
    private final byte[] m_digestBits;
    private final int[] m_block;
    private int m_nBlockIndex;

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     */
    public SHA1()
    {
        m_state = new int[5];
        m_block = new int[16];
        m_digestBits = new byte[DIGEST_SIZE];
        reset();
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Clears all data, use reset() to start again.
     */
    public void clear()
    {
        int nI;

        for (nI = 0; nI < m_state.length; nI++) {
            m_state[nI] = 0;
        }
        for (nI = 0; nI < m_digestBits.length; nI++) {
            m_digestBits[nI] = 0;
        }
        for (nI = 0; nI < m_block.length; nI++) {
            m_block[nI] = 0;
        }
        m_lCount = 0;
        m_nBlockIndex = 0;
    }

    ///////////////////////////////////////////////////////////////////////////

    private static int rol(int nValue, int nBits)
    {
        return ((nValue << nBits) | (nValue >>> (32 - nBits)));
    }

    ///////////////////////////////////////////////////////////////////////////

    private int blk0(int nI)
    {
        return m_block[nI] =
                ((rol(m_block[nI], 24) & 0xff00ff00) |
                 (rol(m_block[nI], 8) & 0x00ff00ff));
    }

    ///////////////////////////////////////////////////////////////////////////

    private int blk(int nI)
    {
        return (m_block[nI & 15] =
                        rol(m_block[(nI + 13) & 15] ^
                            m_block[(nI + 8) & 15] ^
                            m_block[(nI + 2) & 15] ^
                            m_block[nI & 15],
                            1));
    }

    ///////////////////////////////////////////////////////////////////////////

    private void r0(int data[], int nV, int nW, int nX, int nY, int nZ, int nI)
    {
        data[nZ] += ((data[nW] & (data[nX] ^ data[nY])) ^ data[nY])
                    + blk0(nI)
                    + 0x5a827999
                    + rol(data[nV], 5);
        data[nW] = rol(data[nW], 30);
    }

    private void r1(int data[], int nV, int nW, int nX, int nY, int nZ, int nI)
    {
        data[nZ] += ((data[nW] & (data[nX] ^ data[nY])) ^ data[nY])
                    + blk(nI)
                    + 0x5a827999
                    + rol(data[nV], 5);
        data[nW] = rol(data[nW], 30);
    }

    private void r2(int data[], int nV, int nW, int nX, int nY, int nZ, int nI)
    {
        data[nZ] += (data[nW] ^ data[nX] ^ data[nY])
                    + blk(nI)
                    + 0x6eD9eba1
                    + rol(data[nV], 5);
        data[nW] = rol(data[nW], 30);
    }

    private void r3(int data[], int nV, int nW, int nX, int nY, int nZ, int nI)
    {
        data[nZ]
                += (((data[nW] | data[nX]) & data[nY]) | (data[nW] & data[nX]))
                   + blk(nI)
                   + 0x8f1bbcdc
                   + rol(data[nV], 5);
        data[nW] = rol(data[nW], 30);
    }

    private void r4(int data[], int nV, int nW, int nX, int nY, int nZ, int nI)
    {
        data[nZ] += (data[nW] ^ data[nX] ^ data[nY])
                    + blk(nI)
                    + 0xca62c1d6
                    + rol(data[nV], 5);
        data[nW] = rol(data[nW], 30);
    }

    ///////////////////////////////////////////////////////////////////////////

    private void transform()
    {

        int[] dd = new int[5];
        dd[0] = m_state[0];
        dd[1] = m_state[1];
        dd[2] = m_state[2];
        dd[3] = m_state[3];
        dd[4] = m_state[4];
        r0(dd, 0, 1, 2, 3, 4, 0);
        r0(dd, 4, 0, 1, 2, 3, 1);
        r0(dd, 3, 4, 0, 1, 2, 2);
        r0(dd, 2, 3, 4, 0, 1, 3);
        r0(dd, 1, 2, 3, 4, 0, 4);
        r0(dd, 0, 1, 2, 3, 4, 5);
        r0(dd, 4, 0, 1, 2, 3, 6);
        r0(dd, 3, 4, 0, 1, 2, 7);
        r0(dd, 2, 3, 4, 0, 1, 8);
        r0(dd, 1, 2, 3, 4, 0, 9);
        r0(dd, 0, 1, 2, 3, 4, 10);
        r0(dd, 4, 0, 1, 2, 3, 11);
        r0(dd, 3, 4, 0, 1, 2, 12);
        r0(dd, 2, 3, 4, 0, 1, 13);
        r0(dd, 1, 2, 3, 4, 0, 14);
        r0(dd, 0, 1, 2, 3, 4, 15);
        r1(dd, 4, 0, 1, 2, 3, 16);
        r1(dd, 3, 4, 0, 1, 2, 17);
        r1(dd, 2, 3, 4, 0, 1, 18);
        r1(dd, 1, 2, 3, 4, 0, 19);
        r2(dd, 0, 1, 2, 3, 4, 20);
        r2(dd, 4, 0, 1, 2, 3, 21);
        r2(dd, 3, 4, 0, 1, 2, 22);
        r2(dd, 2, 3, 4, 0, 1, 23);
        r2(dd, 1, 2, 3, 4, 0, 24);
        r2(dd, 0, 1, 2, 3, 4, 25);
        r2(dd, 4, 0, 1, 2, 3, 26);
        r2(dd, 3, 4, 0, 1, 2, 27);
        r2(dd, 2, 3, 4, 0, 1, 28);
        r2(dd, 1, 2, 3, 4, 0, 29);
        r2(dd, 0, 1, 2, 3, 4, 30);
        r2(dd, 4, 0, 1, 2, 3, 31);
        r2(dd, 3, 4, 0, 1, 2, 32);
        r2(dd, 2, 3, 4, 0, 1, 33);
        r2(dd, 1, 2, 3, 4, 0, 34);
        r2(dd, 0, 1, 2, 3, 4, 35);
        r2(dd, 4, 0, 1, 2, 3, 36);
        r2(dd, 3, 4, 0, 1, 2, 37);
        r2(dd, 2, 3, 4, 0, 1, 38);
        r2(dd, 1, 2, 3, 4, 0, 39);
        r3(dd, 0, 1, 2, 3, 4, 40);
        r3(dd, 4, 0, 1, 2, 3, 41);
        r3(dd, 3, 4, 0, 1, 2, 42);
        r3(dd, 2, 3, 4, 0, 1, 43);
        r3(dd, 1, 2, 3, 4, 0, 44);
        r3(dd, 0, 1, 2, 3, 4, 45);
        r3(dd, 4, 0, 1, 2, 3, 46);
        r3(dd, 3, 4, 0, 1, 2, 47);
        r3(dd, 2, 3, 4, 0, 1, 48);
        r3(dd, 1, 2, 3, 4, 0, 49);
        r3(dd, 0, 1, 2, 3, 4, 50);
        r3(dd, 4, 0, 1, 2, 3, 51);
        r3(dd, 3, 4, 0, 1, 2, 52);
        r3(dd, 2, 3, 4, 0, 1, 53);
        r3(dd, 1, 2, 3, 4, 0, 54);
        r3(dd, 0, 1, 2, 3, 4, 55);
        r3(dd, 4, 0, 1, 2, 3, 56);
        r3(dd, 3, 4, 0, 1, 2, 57);
        r3(dd, 2, 3, 4, 0, 1, 58);
        r3(dd, 1, 2, 3, 4, 0, 59);
        r4(dd, 0, 1, 2, 3, 4, 60);
        r4(dd, 4, 0, 1, 2, 3, 61);
        r4(dd, 3, 4, 0, 1, 2, 62);
        r4(dd, 2, 3, 4, 0, 1, 63);
        r4(dd, 1, 2, 3, 4, 0, 64);
        r4(dd, 0, 1, 2, 3, 4, 65);
        r4(dd, 4, 0, 1, 2, 3, 66);
        r4(dd, 3, 4, 0, 1, 2, 67);
        r4(dd, 2, 3, 4, 0, 1, 68);
        r4(dd, 1, 2, 3, 4, 0, 69);
        r4(dd, 0, 1, 2, 3, 4, 70);
        r4(dd, 4, 0, 1, 2, 3, 71);
        r4(dd, 3, 4, 0, 1, 2, 72);
        r4(dd, 2, 3, 4, 0, 1, 73);
        r4(dd, 1, 2, 3, 4, 0, 74);
        r4(dd, 0, 1, 2, 3, 4, 75);
        r4(dd, 4, 0, 1, 2, 3, 76);
        r4(dd, 3, 4, 0, 1, 2, 77);
        r4(dd, 2, 3, 4, 0, 1, 78);
        r4(dd, 1, 2, 3, 4, 0, 79);
        m_state[0] += dd[0];
        m_state[1] += dd[1];
        m_state[2] += dd[2];
        m_state[3] += dd[3];
        m_state[4] += dd[4];
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Initializes (or resets) the hasher for a new session.
     */
    private void reset()
    {
        m_state[0] = 0x67452301;
        m_state[1] = 0xefcdab89;
        m_state[2] = 0x98badcfe;
        m_state[3] = 0x10325476;
        m_state[4] = 0xc3d2e1f0;
        m_lCount = 0;
        m_nBlockIndex = 0;
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Adds a single byte to the digest.
     *
     * @param bB the byte to add
     */
    private void update(byte bB)
    {
        int nMask = (m_nBlockIndex & 3) << 3;

        m_lCount += 8;
        m_block[m_nBlockIndex >> 2] &= ~(0xff << nMask);
        m_block[m_nBlockIndex >> 2] |= (bB & 0xff) << nMask;
        m_nBlockIndex++;
        if (m_nBlockIndex == 64) {
            transform();
            m_nBlockIndex = 0;
        }
    }

    ///////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////

    /**
     * Adds a portion of a byte array to the digest.
     *
     * @param data the data to add
     */
    public void update(byte[] data,
                       @SuppressWarnings("SameParameterValue") int nOfs,
                       int nLen)
    {
        for (int nEnd = nOfs + nLen; nOfs < nEnd; nOfs++) {
            update(data[nOfs]);
        }
    }

    ///////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////

    /**
     * Finishes the digest.
     */
    public void finish()
    {
        int nI;
        byte bits[] = new byte[8];


        for (nI = 0; nI < 8; nI++) {
            bits[nI] = (byte)((m_lCount >>> (((7 - nI) << 3))) & 0xff);
        }

        update((byte)128);
        while (m_nBlockIndex != 56) {
            update((byte)0);
        }

        for (nI = 0; nI < bits.length; nI++) {
            update(bits[nI]);
        }

        for (nI = 0; nI < 20; nI++) {
            m_digestBits[nI] =
                    (byte)((m_state[nI >> 2] >> ((3 - (nI & 3)) << 3)) & 0xff);
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Retrieves the digest.
     *
     * @return the digst bytes as an array if DIGEST_SIZE bytes
     */
    public byte[] getDigest()
    {
        byte[] result = new byte[DIGEST_SIZE];
        System.arraycopy(m_digestBits, 0, result, 0, DIGEST_SIZE);
        return result;
    }

    ///////////////////////////////////////////////////////////////////////////


    /**
     * makes a binhex string representation of the current digest
     *
     * @return the string representation
     */
    @Override
    public String toString()
    {
        return Util.bytesToHex(m_digestBits);
    }
}
