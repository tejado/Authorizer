/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import org.pwsafe.lib.Util;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SealedObject;

/**
 * Password wrapper which will clear its contents when closed
 */
public class PwsPassword implements Closeable
{
    private final char[] itsPasswd;
    private final List<byte[]> itsEncBytes = new ArrayList<>(1);

    /**
     * Constructor from a CharSequence
     */
    public PwsPassword(CharSequence chars)
    {
        itsPasswd = new char[chars.length()];
        int len = chars.length();
        for (int i = 0; i < len; ++i) {
            itsPasswd[i] = chars.charAt(i);
        }
    }

    /**
     * Internal constructor from char array
     */
    private PwsPassword(char[] chars)
    {
        itsPasswd = chars;
    }

    /**
     * Get the password bytes encoded with the passed charset name
     */
    public byte[] getBytes(String charsetStr)
            throws UnsupportedEncodingException
    {
        Charset charset;
        try {
            charset = (charsetStr != null) ? Charset.forName(charsetStr) :
                      Charset.defaultCharset();
        } catch (Exception e) {
            throw new UnsupportedEncodingException(charsetStr);
        }
        ByteBuffer buf = charset.encode(CharBuffer.wrap(itsPasswd));
        byte[] bytes = new byte[buf.limit()];
        buf.get(bytes);
        Util.clearArray(buf.array());
        itsEncBytes.add(bytes);
        return bytes;
    }

    /**
     * Seal the password
     */
    public SealedObject seal(Cipher cipher)
            throws IOException, IllegalBlockSizeException
    {
        return new SealedObject(itsPasswd, cipher);
    }

    /**
     * Unseal a password
     */
    public static PwsPassword unseal(SealedObject obj, Cipher cipher)
            throws ClassNotFoundException, BadPaddingException,
                   IllegalBlockSizeException, IOException
    {
        char[] chars = (char[])obj.getObject(cipher);
        return new PwsPassword(chars);
    }

    /**
     * Close the password and clear any stored values
     */
    @Override
    public void close()
    {
        Arrays.fill(itsPasswd, '\uA5A5');
        Arrays.fill(itsPasswd, '\u5A5A');
        Arrays.fill(itsPasswd, '\0');
        for (byte[] bytes: itsEncBytes) {
            Util.clearArray(bytes);
        }
    }

    /**
     * Finalize the object
     */
    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        close();
    }
}
