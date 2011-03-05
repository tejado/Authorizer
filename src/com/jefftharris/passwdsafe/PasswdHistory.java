package com.jefftharris.passwdsafe;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.pwsafe.lib.file.PwsStringUnicodeField;

public class PasswdHistory
{
    public static class Entry implements Comparable<Entry>
    {
        private final Date itsDate;
        private final String itsPasswd;

        public Entry(Date date, String passwd)
        {
            itsDate = date;
            itsPasswd = passwd;
        }

        public Date getDate()
        {
            return itsDate;
        }

        public String getPasswd()
        {
            return itsPasswd;
        }

        public int compareTo(Entry arg0)
        {
            // Sort descending
            return -itsDate.compareTo(arg0.itsDate);
        }
    }

    private boolean itsIsEnabled;
    private int itsMaxSize;
    private List<Entry> itsPasswds = new ArrayList<Entry>();

    public PasswdHistory(PwsStringUnicodeField field)
        throws IllegalArgumentException
    {
        byte[] bytes = field.getBytes();
        if (bytes.length < 5) {
            throw new IllegalArgumentException(
                "Field length (" + bytes.length + ") too short: " +
                5);
        }

        itsIsEnabled = bytes[0] != 0;
        itsMaxSize = PasswdFileData.hexBytesToInt(bytes, 1, 2);
        if (itsMaxSize > 255) {
            throw new IllegalArgumentException(
                "Invalid max size: " + itsMaxSize);
        }
        int numEntries = PasswdFileData.hexBytesToInt(bytes, 3, 2);
        if (numEntries > 255) {
            throw new IllegalArgumentException(
                "Invalid numEntries: " + numEntries);
        }

        Charset cs = Charset.forName("UTF-8");
        CharsetDecoder csDecoder = cs.newDecoder();
        ByteBuffer byteBuf = ByteBuffer.wrap(bytes, 0, bytes.length);

        int pos = 5;
        while (pos < bytes.length)
        {
            if (pos + 8 + 4 >= bytes.length) {
                throw new IllegalArgumentException(
                    "Field length (" + bytes.length + ") too short: " +
                    pos + 8 + 4);
            }

            long date = PasswdFileData.hexBytesToInt(bytes, pos, 8);
            int passwdLen = PasswdFileData.hexBytesToInt(bytes, pos + 8, 4);
            pos += 8 + 4;
            if (pos + passwdLen > bytes.length) {
                throw new IllegalArgumentException(
                    "Field length (" + bytes.length + ") too short: " +
                    pos + passwdLen);
            }

            CharBuffer passwdChars = CharBuffer.allocate(passwdLen);
            byteBuf.position(pos);
            csDecoder.reset();
            CoderResult rc = csDecoder.decode(byteBuf, passwdChars, true);
            String passwd = null;
            if ((rc == CoderResult.OVERFLOW) ||
                (passwdChars.position() == passwdLen)) {
                passwdChars.rewind();
                passwd = passwdChars.toString();
            }

            itsPasswds.add(new Entry(new Date(date * 1000L), passwd));
            pos = byteBuf.position();
        }

        Collections.sort(itsPasswds);
    }

    public boolean isEnabled()
    {
        return itsIsEnabled;
    }

    public int getMaxSize()
    {
        return itsMaxSize;
    }

    public List<Entry> getPasswds()
    {
        return itsPasswds;
    }
}
