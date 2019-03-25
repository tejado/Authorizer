/*
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import androidx.annotation.NonNull;

import org.pwsafe.lib.exception.EndOfFileException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Encapsulates a version 1 record.
 *
 * @author Kevin Preece
 */
public class PwsRecordV1 extends PwsRecord implements Comparable<Object>
{
    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_TYPE = 0;

    /**
     * The string that, if present in the title, indicates that the
     * default username is to be used for this record.
     */
    private static final String DefUserString;

    /**
     * The string that separates the title from the username when both are
     * present in the title.
     */
    private static final String SplitChar;

    /**
     * The string that separates the title and username when it is a
     * composite field.
     */
    private static final String SplitString;

    /**
     * Constant for the title field.
     */
    public static final int TITLE = 3;

    /**
     * Constant for the username field.
     */
    public static final int USERNAME = 4;

    /**
     * Constant for the notes field.
     */
    public static final int NOTES = 5;

    /**
     * Constant for the passphrase field.
     */
    public static final int PASSWORD = 6;

    /**
     * Constant for the phantom UUID field comprised of the TITLE and
     * USERNAME
     */
    public static final int UUID = 7;

    /**
     * All the valid type codes.
     */
    private static final Object[] VALID_TYPES = new Object[]
            {
                    new Object[]{Integer.valueOf(TITLE), "TITLE",
                                 PwsStringField.class},
                    new Object[]{Integer.valueOf(USERNAME), "USERNAME",
                                 PwsStringField.class},
                    new Object[]{Integer.valueOf(NOTES), "NOTES",
                                 PwsStringField.class},
                    new Object[]{Integer.valueOf(PASSWORD), "PASSWORD",
                                 PwsPasswdField.class},
                    new Object[]{Integer.valueOf(UUID), "UUID",
                                 PwsStringField.class}
            };

    static {
        // Must be done here as they could theoretically throw an
        // exception, though in practice they won't unless the JVM
        // implementation is wrong.
        DefUserString = getDefaultString(new byte[]{-96});
        SplitChar = getDefaultString(new byte[]{-83});
        SplitString = getDefaultString(new byte[]{32, 32, -83, 32, 32});
    }

    /**
     * Create a new record with all mandatory fields given their default
     * value.
     */
    PwsRecordV1()
    {
        super(VALID_TYPES);

        // Set default values
        setField(new PwsStringField(PwsFieldTypeV1.TITLE, ""));
        setField(new PwsStringField(PwsFieldTypeV1.USERNAME, ""));
        setField(new PwsPasswdField(PwsFieldTypeV1.PASSWORD));
        setField(new PwsStringField(PwsFieldTypeV1.NOTES, ""));
        setField(new PwsStringField(PwsFieldTypeV1.UUID, ""));
    }

    /**
     * Create a new record by reading it from <code>file</code>.
     *
     * @param file the file to read data from.
     * @throws EndOfFileException If end of file is reached
     * @throws IOException        If a read error occurs.
     */
    PwsRecordV1(PwsFile file)
            throws EndOfFileException, IOException
    {
        super(file, VALID_TYPES);
    }

    /**
     * Compares this record to another returning a value that is less than
     * zero if
     * this record is "less than" <code>other</code>, zero if they are
     * "equal", or
     * greater than zero if this record is "greater than"
     * <code>other</code>.
     *
     * @param other the record to compare this record to.
     * @return A value &lt; zero if this record is "less than"
     * <code>other</code>,
     * zero if they're equal and &gt; zero if this record is "greater than"
     * <code>other</code>.
     * @throws ClassCastException If <code>other</code> is not a
     * <code>PwsRecordV1</code>.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(@NonNull Object other)
    {
        return compareTo((PwsRecordV1)other);
    }

    /**
     * Compares this record to another returning a value that is less than
     * zero if
     * this record is "less than" <code>other</code>, zero if they are
     * "equal", or
     * greater than zero if this record is "greater than"
     * <code>other</code>.
     *
     * @param other the record to compare this record to.
     * @return A value &lt; zero if this record is "less than"
     * <code>other</code>,
     * zero if they're equal and &gt; zero if this record is "greater than"
     * <code>other</code>.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    private int compareTo(PwsRecordV1 other)
    {
        int retCode;

        if ((retCode = getField(TITLE).compareTo(other.getField(TITLE))) == 0) {
            if ((retCode = getField(USERNAME)
                    .compareTo(other.getField(USERNAME))) == 0) {
                return getField(NOTES).compareTo(other.getField(NOTES));
            }
        }
        return retCode;
    }

    /**
     * Compares this record to another returning <code>true</code> if they're
     * equal
     * and <code>false</code> if they're unequal.
     *
     * @param rec the record this one is compared to.
     * @return <code>true</code> if the records are equal,
     * <code>false</code> if
     * they're unequal.
     * @throws ClassCastException if <code>rec</code> is not a
     * <code>PwsRecordV1</code>.
     */
    @Override
    public boolean equals(Object rec)
    {
        return (rec instanceof PwsRecordV1) && equals((PwsRecordV1)rec);

    }

    /**
     * Compares this record to another returning <code>true</code> if they're
     * equal
     * and <code>false</code> if they're unequal.
     *
     * @param other the record this one is compared to.
     * @return <code>true</code> if the records are equal,
     * <code>false</code> if
     * they're unequal.
     */
    private boolean equals(PwsRecordV1 other)
    {
        return (getField(NOTES).equals(other.getField(NOTES))
                && getField(PASSWORD).equals(other.getField(PASSWORD))
                && getField(TITLE).equals(other.getField(TITLE))
                && getField(USERNAME).equals(other.getField(USERNAME)));
    }

    /**
     * Validates the record, returning <code>true</code> if it's valid or
     * <code>false</code>
     * if unequal.
     *
     * @return <code>true</code> if it's valid or <code>false</code> if
     * unequal.
     */
    @Override
    protected boolean isValid()
    {
        // All records should be added to the file.
        return true;
    }

    /**
     * Initialises this record by reading its data from <code>file</code>.
     *
     * @param file the file to read the data from.
     * @throws EndOfFileException
     * @throws IOException
     */
    @Override
    protected void loadRecord(PwsFile file)
            throws EndOfFileException, IOException
    {
        String str;
        int pos;

        str = new Item(file).getData();

        String title;
        String username;
        pos = str.indexOf(SplitChar);
        if (pos == -1) {
            // This is not a composite of title and username

            pos = str.indexOf(DefUserString);
            if (pos == -1) {
                title = str;
            } else {
                title = str.substring(0, pos);
            }
            username = "";
        } else {
            title = str.substring(0, pos).trim();
            username = str.substring(pos + 1).trim();
        }
        setField(new PwsStringField(TITLE, title));
        setField(new PwsStringField(USERNAME, username));
        Item item = new Item(file);
        setField(new PwsPasswdField(PASSWORD, item.getData(), file));
        item.clear();
        setField(new PwsStringField(NOTES, new Item(file).getData()));

        String uuid;
        if (username.trim().length() == 0) {
            uuid = title;
        } else {
            uuid = title + SplitString + username;
        }
        setField(new PwsStringField(UUID, uuid));
    }

    /**
     * Saves this record to <code>file</code>.
     *
     * @param file the file that the record will be written to.
     * @throws IOException if a write error occurs.
     * @see org.pwsafe.lib.file.PwsRecord#saveRecord(org.pwsafe.lib.file.PwsFile)
     */
    @Override
    protected void saveRecord(PwsFile file) throws IOException
    {
        PwsField title;

        if (getField(USERNAME).toString().trim().length() == 0) {
            title = getField(TITLE);
        } else {
            title = new PwsStringField(TITLE, getField(TITLE).toString() +
                                              SplitString +
                                              getField(USERNAME).toString());
        }

        writeField(file, title, DEFAULT_TYPE);
        writeField(file, getField(PASSWORD), DEFAULT_TYPE);
        writeField(file, getField(NOTES), DEFAULT_TYPE);
    }

    /**
     * Returns a string representation of this record.
     *
     * @return A string representation of this object.
     */
    @Override
    public String toString()
    {
        return "{ \"" + getField(TITLE) + "\", \"" +
               getField(USERNAME) + "\" }";
    }

    /**
     * Get a w
     */
    private static String getDefaultString(byte[] bytes)
    {
        try {
            return new String(bytes, PwsRecord.DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException e) {
            // Should never get here - DEFAULT_CHARSET must be supported by all
            // Java implementations.
            return "";
        }
    }
}
