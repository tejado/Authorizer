/*
 * Copyright (©) 2009-2010 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SealedObject;

import org.bouncycastle.crypto.RuntimeCryptoException;


public abstract class AbstractPwsPasswdField extends PwsField
{
    private static final long serialVersionUID = -5633832199601878672L;

    // TODO: store just encrypted bytes instead of SealedObject?
    private final Cipher itsReadCipher;
    private final String itsStrEncoding;

    public AbstractPwsPasswdField(int type, byte[] value, PwsFile file,
                                  String encoding)
    {
        super(type, sealValue(value, encoding, file.getWriteCipher()));
        Arrays.fill(value, (byte)0);
        itsReadCipher = file.getReadCipher();
        itsStrEncoding = encoding;
    }


    public AbstractPwsPasswdField(int type, String value, PwsFile file,
                                  String encoding)
    {
        super(type, sealValue(value, file.getWriteCipher()));
        itsReadCipher = file.getReadCipher();
        itsStrEncoding = encoding;
    }


    public AbstractPwsPasswdField(PwsFieldType type, String encoding)
    {
        super(type, null);
        itsReadCipher = null;
        itsStrEncoding = encoding;
    }


    // TODO: equals methods??

    public int compareTo(Object arg0)
    {
        return toString().compareTo(((AbstractPwsPasswdField)arg0).toString());
    }


    @Override
    public String toString()
    {
        SealedObject sealValue = (SealedObject)getValue();
        try {
            if (sealValue == null) {
                return "";
            } else {
                return (String) sealValue.getObject(itsReadCipher);
            }
        }
        catch (IllegalBlockSizeException e) {
            throw new RuntimeCryptoException(e.getMessage());
        }
        catch (BadPaddingException e) {
            throw new RuntimeCryptoException(e.getMessage());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeCryptoException(e.getMessage());
        }
    }


    @Override
    public byte[] getBytes()
    {
        try {
            return toString().getBytes(itsStrEncoding);
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }


    private static SealedObject sealValue(byte[] value, String encoding,
                                          Cipher cipher)
    {
        try {
            return sealValue(new String(value, encoding), cipher);
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }


    private static SealedObject sealValue(String value, Cipher cipher)
    {
        try {
            return new SealedObject(value, cipher);
        }
        catch (IllegalBlockSizeException e) {
            throw new RuntimeCryptoException(e.getMessage());
        }
        catch (IOException e) {
            throw new RuntimeCryptoException(e.getMessage());
        }
    }
}
