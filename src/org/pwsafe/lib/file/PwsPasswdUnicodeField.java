package org.pwsafe.lib.file;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SealedObject;

import org.bouncycastle.crypto.RuntimeCryptoException;

public class PwsPasswdUnicodeField extends PwsField
{
    private static final long serialVersionUID = 1969424649189400429L;
    // TODO: store just encrypted bytes instead of SealedObject?
    private final Cipher itsReadCipher;


    public PwsPasswdUnicodeField(int type, byte[] value, PwsFile file)
    {
        super(type, sealValue(value, file.getWriteCipher()));
        Arrays.fill(value, (byte)0);
        itsReadCipher = file.getReadCipher();
    }


    public PwsPasswdUnicodeField(int type, String value, PwsFile file)
    {
        super(type, sealValue(value, file.getWriteCipher()));
        itsReadCipher = file.getReadCipher();
    }


    public PwsPasswdUnicodeField(PwsFieldType type)
    {
        super(type, null);
        itsReadCipher = null;
    }


    public int compareTo(Object arg0)
    {
        return toString().compareTo(((PwsPasswdUnicodeField)arg0).toString());
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
            return toString().getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }


    private static SealedObject sealValue(byte[] value, Cipher cipher)
    {
        try {
            return sealValue(new String(value, "UTF-8"), cipher);
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
