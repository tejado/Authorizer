/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.CheckResult;

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
     * Create from a character sequence
     */
    public static @CheckResult Owner<PwsPassword> create(CharSequence chars)
    {
        int len = chars.length();
        char[] passwd = new char[len];
        for (int i = 0; i < len; ++i) {
            passwd[i] = chars.charAt(i);
        }
        return createOwner(passwd);
    }

    /**
     * Create from an EditText
     */
    public static @CheckResult Owner<PwsPassword> create(EditText tv)
    {
        return create(tv.getText());
    }

    /**
     * Create from a char array.  The passed characters are cleared.
     */
    public static @CheckResult Owner<PwsPassword> create(char[] chars)
    {
        try {
            return createOwner(Arrays.copyOf(chars, chars.length));
        } finally {
            Util.clearArray(chars);
            Runtime.getRuntime().gc();
        }
    }

    /**
     * Create from a byte array.  The passwd bytes are cleared.
     */
    public static @CheckResult Owner<PwsPassword> create(byte[] bytes,
                                                         String charsetStr)
            throws UnsupportedEncodingException
    {
        try {
            Charset charset = getCharset(charsetStr);
            CharBuffer buf = charset.decode(ByteBuffer.wrap(bytes));
            try {
                char[] chars = new char[buf.limit()];
                buf.get(chars);
                return createOwner(chars);
            } finally {
                Util.clearArray(buf.array());
            }
        } finally {
            Util.clearArray(bytes);
            Runtime.getRuntime().gc();
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
     * Get the length of the password
     */
    public int length()
    {
        return itsPasswd.length;
    }

    /**
     * Get a character of the password
     */
    public char charAt(int i)
    {
        return itsPasswd[i];
    }

    /**
     * Get the password bytes encoded with the passed charset name
     */
    public byte[] getBytes(String charsetStr)
            throws UnsupportedEncodingException
    {
        Charset charset = getCharset(charsetStr);
        ByteBuffer buf = charset.encode(CharBuffer.wrap(itsPasswd));
        byte[] bytes = new byte[buf.limit()];
        buf.get(bytes);
        Util.clearArray(buf.array());
        itsEncBytes.add(bytes);
        return bytes;
    }

    /**
     * Set the password into a text view
     */
    public void setInto(TextView tv)
    {
        tv.setText(itsPasswd, 0, itsPasswd.length);
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
     * Does the password equals the passed password
     */
    public boolean equals(String password)
    {
        if ((password == null) || (itsPasswd.length != password.length())) {
            return false;
        }

        for (int i = 0; i < itsPasswd.length; ++i) {
            if (itsPasswd[i] != password.charAt(i)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Close the password and clear any stored values
     */
    @Override
    public void close()
    {
        Util.clearArray(itsPasswd);
        for (byte[] bytes: itsEncBytes) {
            Util.clearArray(bytes);
        }
        itsEncBytes.clear();
        Runtime.getRuntime().gc();
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

    /**
     * Create an owned PwsPassword
     */
    private static @CheckResult Owner<PwsPassword> createOwner(char[] chars)
    {
        return new Owner<>(new PwsPassword(chars));
    }

    /**
     * Get a Charset from its name
     */
    private static Charset getCharset(String charsetStr)
            throws UnsupportedEncodingException
    {
        try {
            return (charsetStr != null) ? Charset.forName(charsetStr) :
                    Charset.defaultCharset();
        } catch (Exception e) {
            throw new UnsupportedEncodingException(charsetStr);
        }
    }
}
