/*
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import androidx.annotation.NonNull;

import org.pwsafe.lib.UUID;
import org.pwsafe.lib.exception.EndOfFileException;
import org.pwsafe.lib.exception.UnimplementedConversionException;

import java.io.IOException;
import java.util.Iterator;

/**
 * @author Kevin
 */
@SuppressWarnings("WeakerAccess")
public class PwsRecordV2 extends PwsRecord
{
    private static final long serialVersionUID = 1L;

    /**
     * Constant for the version 2 ID string field.
     */
    public static final int V2_ID_STRING = 0;

    /**
     * Constant for the Universally Unique ID (UUID) field.
     */
    public static final int UUID = 1;

    /**
     * Constant for the group field.
     */
    public static final int GROUP = 2;

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
     * Constant for the creation date field.
     */
    public static final int CREATION_TIME = 7;

    /**
     * Constant for the passphrase modification time field.
     */
    public static final int PASSWORD_MOD_TIME = 8;

    /**
     * Constant for the last access time field.
     */
    public static final int LAST_ACCESS_TIME = 9;

    /**
     * Constant for the passphrase lifetime field.
     */
    public static final int PASSWORD_LIFETIME = 10;

    /**
     * Constant for the passphrase policy field.
     */
    public static final int PASSWORD_POLICY = 11;

    /**
     * Constant for the last time any field in the record was changed.
     */
    public static final int LAST_MOD_TIME = 12;

    /**
     * Constant for the url field
     */
    public static final int URL = 13;

    /**
     * Constant for the end of record marker field.
     */
    public static final int END_OF_RECORD = 255;

    /**
     * All the valid type codes.
     */
    private static final Object[] VALID_TYPES = new Object[] {
            new Object[]{Integer.valueOf(V2_ID_STRING), "V2_ID_STRING",
                         PwsStringField.class},
            new Object[]{Integer.valueOf(UUID), "UUID",
                         PwsUUIDField.class},
            new Object[]{Integer.valueOf(GROUP), "GROUP",
                         PwsStringField.class},
            new Object[]{Integer.valueOf(TITLE), "TITLE",
                         PwsStringField.class},
            new Object[]{Integer.valueOf(USERNAME), "USERNAME",
                         PwsStringField.class},
            new Object[]{Integer.valueOf(NOTES), "NOTES",
                         PwsStringField.class},
            new Object[]{Integer.valueOf(PASSWORD), "PASSWORD",
                         PwsPasswdField.class},
            new Object[]{Integer.valueOf(CREATION_TIME),
                         "CREATION_TIME", PwsTimeField.class},
            new Object[]{Integer.valueOf(PASSWORD_MOD_TIME),
                         "PASSWORD_MOD_TIME", PwsTimeField.class},
            new Object[]{Integer.valueOf(LAST_ACCESS_TIME),
                         "LAST_ACCESS_TIME", PwsTimeField.class},
            new Object[]{Integer.valueOf(PASSWORD_LIFETIME),
                         "PASSWORD_LIFETIME", PwsIntegerField.class},
            new Object[]{Integer.valueOf(PASSWORD_POLICY),
                         "PASSWORD_POLICY", PwsStringField.class},
            new Object[]{Integer.valueOf(LAST_MOD_TIME),
                         "LAST_MOD_TIME", PwsTimeField.class},
            new Object[]{Integer.valueOf(URL), "URL",
                         PwsStringField.class},
            };

    /**
     * Create a new record with all mandatory fields given their default value.
     */
    PwsRecordV2()
    {
        super(VALID_TYPES);

        setField(new PwsUUIDField(PwsFieldTypeV2.UUID, new UUID()));
        setField(new PwsStringField(PwsFieldTypeV2.TITLE, ""));
        setField(new PwsPasswdField(PwsFieldTypeV2.PASSWORD));
    }

    /**
     * Create a new record by reading it from <code>file</code>.
     *
     * @param file the file to read data from.
     * @throws EndOfFileException If end of file is reached
     * @throws IOException        If a read error occurs.
     */
    PwsRecordV2(PwsFile file) throws EndOfFileException, IOException
    {
        super(file, VALID_TYPES);
    }

    /**
     * Compares this record to another returning a value that is less than
     * zero if this record is "less than" <code>other</code>, zero if they are
     * "equal", or greater than zero if this record is "greater than"
     * <code>other</code>.
     *
     * @param other the record to compare this record to.
     * @return A value &lt; zero if this record is "less than"
     * <code>other</code>, zero if they're equal and &gt; zero if this record
     * is "greater than" <code>other</code>.
     * @throws ClassCastException If <code>other</code> is not a
     * <code>PwsRecordV1</code>.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(@NonNull Object other)
    {
        return 0;
    }

    /**
     * Compares this record to another returning <code>true</code> if they're
     * equal and <code>false</code> if they're unequal.
     *
     * @param that the record this one is compared to.
     * @return <code>true</code> if the records are equal, <code>false</code> if
     * they're unequal.
     */
    @Override
    public boolean equals(Object that)
    {
        if (that instanceof PwsRecordV2) {
            UUID thisUUID = (UUID)getField(UUID).getValue();
            UUID thatUUID = (UUID)((PwsRecord)that).getField(UUID).getValue();
            return thisUUID.equals(thatUUID);
        } else {
            return false;
        }
    }

    /**
     * Validates the record, returning <code>true</code> if it's valid or
     * <code>false</code> if unequal.
     *
     * @return <code>true</code> if it's valid or <code>false</code> if unequal.
     */
    @Override
    protected boolean isValid()
    {
        return !((PwsStringField)getField(TITLE)).equals(PwsFileV2.ID_STRING);
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
        Item item;
        PwsField itemVal = null;

        for (; ; ) {
            item = new Item(file);

            if (item.getType() == END_OF_RECORD) {
                break; // out of the for loop
            }
            switch (item.getType()) {
            case UUID:
                itemVal = new PwsUUIDField(item.getType(), item.getByteData());
                break;

            case V2_ID_STRING:
            case GROUP:
            case TITLE:
            case USERNAME:
            case NOTES:
            case URL:
                itemVal = new PwsStringField(item.getType(), item.getData());
                break;

            case PASSWORD:
                itemVal = new PwsPasswdField(item.getType(), item.getData(),
                                             file);
                item.clear();
                break;

            case CREATION_TIME:
            case PASSWORD_MOD_TIME:
            case LAST_ACCESS_TIME:
            case LAST_MOD_TIME:
                itemVal = new PwsTimeField(item.getType(), item.getByteData());
                break;

            case PASSWORD_LIFETIME:
                itemVal = new PwsIntegerField(item.getType(),
                                              item.getByteData());
                break;

            case PASSWORD_POLICY:
                break;

            default:
                throw new UnimplementedConversionException();
            }
            setField(itemVal);
        }
    }

    /**
     * Saves this record to <code>file</code>.
     *
     * @param file the file that the record will be written to.
     * @throws IOException if a write error occurs.
     * @see org.pwsafe.lib.file.PwsRecord#saveRecord(org.pwsafe.lib.file.PwsFile)
     */
    @Override
    protected void saveRecord(PwsFile file)
            throws IOException
    {
        for (Iterator<?> iter = getFields(); iter.hasNext(); ) {
            int type;
            PwsField value;

            type = (Integer)iter.next();
            value = getField(type);

            writeField(file, value);
        }
        writeField(file, new PwsStringField(END_OF_RECORD, ""));
    }

    /**
     * Returns a string representation of this record.
     *
     * @return A string representation of this object.
     */
    @Override
    public String toString()
    {
        boolean first = true;
        final StringBuilder sb = new StringBuilder();

        sb.append("{ ");

        for (Iterator<?> iter = getFields(); iter.hasNext(); ) {
            Integer key;
            String value;

            key = (Integer)iter.next();
            value = getField(key).toString();

            if (!first) {
                sb.append(", ");
            }
            first = false;

            Object[] type = (Object[])VALID_TYPES[key];
            sb.append(type[1]);
            sb.append("=");
            if ((Integer)type[0] != PASSWORD) {
                sb.append(value);
            }
        }
        sb.append(" }");

        return sb.toString();
    }
}
