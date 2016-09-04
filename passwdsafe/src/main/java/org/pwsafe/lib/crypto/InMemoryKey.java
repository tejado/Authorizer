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

import android.annotation.SuppressLint;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

/**
 * Tries to hide create a key which is hidden from an occasional onlooker.
 * Uses direct allocated memory to distribute the key data.
 * <p/>
 * It's unchecked whether this makes any difference for swapped out memory.
 *
 * @author roxon
 */
public class InMemoryKey
{
    private final static int BUFFER_SIZE = 1024;
    private final short[] access;
    private ByteBuffer buffer;

    private InMemoryKey(short aSize)
    {
        access = new short[aSize];
    }

    public InMemoryKey(int aSize)
    {
        this((short)aSize);
    }

    @SuppressLint("Assert")
    public void init()
    {
        final byte[] accessBytes = new byte[access.length * 2];
        org.pwsafe.lib.Util.newRandBytes(accessBytes);
        ShortBuffer accessShorts = ByteBuffer.wrap(accessBytes).asShortBuffer();
        for (int i = 0; i < access.length; i++) {
            access[i] = accessShorts.get(i);
        }

        buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        assert (buffer.isDirect());

        for (int i = 0; i < BUFFER_SIZE; i++) {
            buffer.put(org.pwsafe.lib.Util.newRand());
        }
        buffer.flip();
    }

    public byte[] getKey(int size)
    {
        if (buffer == null) {
            throw new IllegalStateException(
                    "InMemoryKey has not been initialized or been disposed");
        }
        if (size > access.length) {
            throw new IllegalArgumentException("Size " + size + " too small");
        }

        final byte[] content = new byte[size];

        // TODOlib: use higher bits of short value for content rotate
        for (int i = 0; i < content.length; i++) {
            final short pos = access[i];
            content[i] = buffer.get(Math.abs(pos) % BUFFER_SIZE);
        }
        return content;
    }

    public void dispose()
    {
        if (buffer != null) {
            if (buffer.hasArray()) {
                byte[] content = buffer.array();
                Arrays.fill(content, (byte)0);
            }
            buffer = null;
        }
    }

}
