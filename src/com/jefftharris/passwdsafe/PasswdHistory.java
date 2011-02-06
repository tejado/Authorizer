package com.jefftharris.passwdsafe;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.pwsafe.lib.file.PwsStringUnicodeField;

public class PasswdHistory
{
    public static class Entry
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
                "Field too short: " + bytes.length);
        }

        itsIsEnabled = bytes[0] != 0;
        itsMaxSize = Integer.valueOf(new String(bytes, 1, 2), 16);
        if (itsMaxSize > 255) {
            throw new IllegalArgumentException(
                "Invalid max size: " + itsMaxSize);
        }
        int numEntries = Integer.valueOf(new String(bytes, 3, 2), 16);
        if (numEntries > 255) {
            throw new IllegalArgumentException(
                "Invalid numEntries: " + numEntries);
        }

        int pos = 5;
        while (pos < bytes.length)
        {
            if (pos + 8 + 4 >= bytes.length) {
                break;
            }

        }
    }
}
