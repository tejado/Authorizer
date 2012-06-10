/*
 * $Id: PwsRecordV2.java 561 2005-07-26 10:00:19 +0000 (Tue, 26 Jul 2005)
 * glen_a_smith $ Copyright (c) 2008-2009 David Muller
 * <roxon@users.sourceforge.net>. All rights reserved. Use of the code is
 * allowed under the Artistic License 2.0 terms, as specified in the LICENSE
 * file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import org.pwsafe.lib.Log;
import org.pwsafe.lib.UUID;
import org.pwsafe.lib.Util;
import org.pwsafe.lib.exception.EndOfFileException;

/**
 * Support for new v3 Record type.
 *
 * @author Glen Smith (based on Kevin's code for V2 records)
 */
public class PwsRecordV3 extends PwsRecord
{
    private static final long serialVersionUID = -3160317668375599155L;

    private static final Log LOG =
        Log.getInstance(PwsRecordV3.class.getPackage().getName());

    /**
     * Constant for the version 3 ID string field.
     */
    public static final int V3_ID_STRING = 0;

    /** Minor version for PasswordSafe 3.25 with protected entry support */
    public static final byte DB_FMT_MINOR_3_25 = 8;

    /** Minor version for PasswordSafe 3.28 with password policy support */
    public static final byte DB_FMT_MINOR_3_28 = 10;

    /** Minor version of the max supported database format */
    public static final byte DB_FMT_MINOR_VERSION = DB_FMT_MINOR_3_28;

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
    public static final int PASSWORD_POLICY_DEPRECATED = 11;

    /**
     * Constant for the last modification time field.
     */
    public static final int LAST_MOD_TIME = 12;

    /**
     * Constant for URL related to this entry.
     */
    public static final int URL = 13;

    /**
     * Constant for Autotype information related to this entry.
     */
    public static final int AUTOTYPE = 14;

    /**
     * History of recently used passwords.
     */
    public static final int PASSWORD_HISTORY = 15;

    /**
     * Constant for the password policy field.
     */
    public static final int PASSWORD_POLICY = 16;

    /**
     * History of recently used passwords.
     */
    public static final int PASSWORD_EXPIRY_INTERVAL = 17;

    /**
     * Run Command
     */
    public static final int RUN_COMMAND = 18;

    /**
     * Double-Click action
     */
    public static final int DOUBLE_CLICK_ACTION = 19;

    /**
     * Email
     */
    public static final int EMAIL = 20;

    /**
     * Protected entry
     */
    public static final int PROTECTED_ENTRY = 21;

    /** Own symbols for password */
    public static final int OWN_PASSWORD_SYMBOLS = 22;

    /** Password policy name */
    public static final int PASSWORD_POLICY_NAME = 24;

    /**
     * Header database version
     */
    public static final int HEADER_VERSION = 0;

    /**
     * Header UUID
     */
    public static final int HEADER_UUID = 1;

    /**
     * Header last save timestamp
     */
    public static final int HEADER_LAST_SAVE_TIME = 4;

    /**
     * Header last saved by who (deprecated in db)
     */
    public static final int HEADER_LAST_SAVE_WHO = 5;

    /**
     * Header last saved app
     */
    public static final int HEADER_LAST_SAVE_WHAT = 6;

    /**
     * Header last saved by user
     */
    public static final int HEADER_LAST_SAVE_USER = 7;

    /**
     * Header last saved on host
     */
    public static final int HEADER_LAST_SAVE_HOST = 8;

    /** Header named password policies */
    public static final int HEADER_NAMED_PASSWORD_POLICIES = 16;

    /**
     * Constant for the end of record marker field.
     */
    public static final int END_OF_RECORD = 255;

    /**
     * All the valid type codes.
     */
    private static final Object[] VALID_TYPES =
        new Object[] {
            new Object[] { Integer.valueOf(V3_ID_STRING),
                            "V3_ID_STRING", PwsVersionField.class },
            new Object[] { Integer.valueOf(UUID),
                            "UUID", PwsUUIDField.class },
            new Object[] { Integer.valueOf(GROUP),
                            "GROUP", PwsStringUnicodeField.class },
            new Object[] { Integer.valueOf(TITLE),
                            "TITLE", PwsStringUnicodeField.class },
            new Object[] { Integer.valueOf(USERNAME),
                            "USERNAME", PwsStringUnicodeField.class },
            new Object[] { Integer.valueOf(NOTES),
                            "NOTES", PwsStringUnicodeField.class },
            new Object[] { Integer.valueOf(PASSWORD),
                            "PASSWORD", PwsPasswdUnicodeField.class },
            new Object[] { Integer.valueOf(CREATION_TIME),
                            "CREATION_TIME", PwsTimeField.class },
            new Object[] { Integer.valueOf(PASSWORD_MOD_TIME),
                            "PASSWORD_MOD_TIME", PwsTimeField.class },
            new Object[] { Integer.valueOf(LAST_ACCESS_TIME),
                            "LAST_ACCESS_TIME", PwsTimeField.class },
            new Object[] { Integer.valueOf(PASSWORD_LIFETIME),
                            "PASSWORD_LIFETIME", PwsTimeField.class },
            new Object[] { Integer.valueOf(PASSWORD_POLICY_DEPRECATED),
                            "PASSWORD_POLICY_OLD", PwsStringUnicodeField.class },
            new Object[] { Integer.valueOf(LAST_MOD_TIME),
                            "LAST_MOD_TIME", PwsTimeField.class },
            new Object[] { Integer.valueOf(URL),
                            "URL", PwsStringUnicodeField.class },
            new Object[] { Integer.valueOf(AUTOTYPE),
                            "AUTOTYPE", PwsStringUnicodeField.class },
            new Object[] { Integer.valueOf(PASSWORD_HISTORY),
                            "PASSWORD_HISTORY", PwsStringUnicodeField.class },
            new Object[] { Integer.valueOf(PASSWORD_POLICY),
                            "PASSWORD_POLICY", PwsStringUnicodeField.class },
            new Object[] { Integer.valueOf(PASSWORD_EXPIRY_INTERVAL),
                            "PASSWORD_EXPIRY_INTERVAL", PwsIntegerField.class },
            new Object[] { Integer.valueOf(RUN_COMMAND),
                            "RUN_COMMAND", PwsStringUnicodeField.class },
            new Object[] { Integer.valueOf(DOUBLE_CLICK_ACTION),
                            "DOUBLE_CLICK_ACTION", PwsShortField.class },
            new Object[] { Integer.valueOf(EMAIL),
                            "EMAIL", PwsStringUnicodeField.class },
            new Object[] { Integer.valueOf(PROTECTED_ENTRY),
                            "PROTECTED_ENTRY", PwsByteField.class},
            new Object[] { Integer.valueOf(OWN_PASSWORD_SYMBOLS),
                            "OWN_PASSWORD_SYMBOLS", PwsStringUnicodeField.class },
            new Object[] { Integer.valueOf(PASSWORD_POLICY_NAME),
                            "PASSWORD_POLICY_NAME", PwsStringUnicodeField.class },
    };

    /**
     * Create a new record with all mandatory fields given their default value.
     */
    PwsRecordV3()
    {
        super(VALID_TYPES);

        setField(new PwsUUIDField(PwsFieldTypeV3.UUID, new UUID()));
        setField(new PwsStringUnicodeField(PwsFieldTypeV3.TITLE, ""));
        setField(new PwsPasswdUnicodeField(PwsFieldTypeV3.PASSWORD));
        setField(new PwsTimeField(PwsFieldTypeV3.CREATION_TIME, new Date()));

    }

    /**
     * A special version for header records
     *
     * @param isHeader Marker for header record
     */
    PwsRecordV3(boolean isHeader)
    {
        super(VALID_TYPES, true);
        setField(new PwsVersionField(HEADER_VERSION,
                                     new byte[] { DB_FMT_MINOR_VERSION, 3 }));
        setField(new PwsUUIDField(HEADER_UUID, new UUID()));
    }

    /**
     * Create a new record by reading it from <code>file</code>.
     *
     * @param file the file to read data from.
     * @throws EndOfFileException If end of file is reached
     * @throws IOException If a read error occurs.
     */
    PwsRecordV3(PwsFile file) throws EndOfFileException, IOException
    {
        super(file, VALID_TYPES);
    }

    /**
     * A special version which reads and ignores all headers since they have
     * different ids to standard types.
     *
     * @param file the file to read data from.
     * @param validTypes the types allowable in the incoming data
     * @throws EndOfFileException If end of file is reached
     * @throws IOException If a read error occurs.
     */
    PwsRecordV3(PwsFile file, boolean ignoreFieldTypes)
        throws EndOfFileException, IOException
    {
        super(file, VALID_TYPES, ignoreFieldTypes);
    }

    /**
     * Creates a new record that is a copy <code>base</code>.
     *
     * @param base the record to copy.
     */
    PwsRecordV3(PwsRecord base)
    {
        super(base);
    }

    /**
     * The V3 format allows and requires the ability to add formerly unknown
     * fields.
     *
     * @return true
     */
    @Override
    protected boolean allowUnknownFieldTypes()
    {
        return true;
    }

    /**
     * Creates a deep clone of this record.
     *
     * @return the new record.
     */
    @Override
    public Object clone()
    {
        return new PwsRecordV3(this);
    }

    /**
     * Compares this record to another returning a value that is less than zero
     * if this record is "less than" <code>other</code>, zero if they are
     * "equal", or greater than zero if this record is "greater than"
     * <code>other</code>.
     *
     * @param other the record to compare this record to.
     * @return A value &lt; zero if this record is "less than" <code>other</code>
     *         , zero if they're equal and &gt; zero if this record is
     *         "greater than" <code>other</code>.
     * @throws ClassCastException If <code>other</code> is not a
     *             <code>PwsRecordV1</code>.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Object other)
    {
        // TODOlib Implement me
        return 0;
    }

    /**
     * Compares this record to another returning <code>true</code> if they're
     * equal and <code>false</code> if they're unequal.
     *
     * @param that the record this one is compared to.
     * @return <code>true</code> if the records are equal, <code>false</code> if
     *         they're unequal.
     * @throws ClassCastException if <code>that</code> is not a
     *             <code>PwsRecordV1</code>.
     */
    @Override
    public boolean equals(Object that)
    {
        UUID thisUUID;
        UUID thatUUID;

        if (that instanceof PwsRecordV3) {
            thisUUID = (UUID) ((PwsUUIDField) getField(UUID)).getValue();
            thatUUID =
                (UUID) ((PwsUUIDField) ((PwsRecord) that).getField(UUID)).getValue();

            return thisUUID.equals(thatUUID);
        }
        else {
            return false;
        }
    }

    /**
     * Checks to see whether this record is one that we should display to the
     * user or not. The header record is the only one we suppress, and we
     * determine the header record by checking for the presence of the type 0
     * field which represents the file format version.
     *
     * @return <code>true</code> if it's valid or <code>false</code> if unequal.
     */
    @Override
    protected boolean isValid()
    {
        // TODOlib Ignore those records we read from the header....
        PwsField idField = getField(V3_ID_STRING);

        if (idField != null) {
            LOG.debug1("Ignoring record " + this.toString());
            return false;
        }
        return true;
    }

    protected boolean isHeaderRecord()
    {

        PwsField idField = getField(V3_ID_STRING);

        if (idField != null) {
            LOG.debug1("Ignoring record " + this.toString());
            return true;
        }
        return false;
    }

    static byte[] EOF_BYTES_RAW = "PWS3-EOFPWS3-EOF".getBytes();

    protected class ItemV3 extends Item
    {
        public ItemV3(PwsFileV3 file) throws EndOfFileException, IOException
        {
            super();
            try {
                rawData = file.readBlock();
            }
            catch (EndOfFileException eofe) {
                data = new byte[32]; // to hold closing HMAC
                file.readBytes(data);
                byte[] hash = file.hasher.doFinal();
                if (!Util.bytesAreEqual(data, hash)) {
                    LOG.error("HMAC record did not match. File may have been tampered");
                    throw new IOException(
                           "HMAC record did not match. File has been tampered");
                }
                throw eofe;
            }

            length = Util.getIntFromByteArray(rawData, 0);
            type = rawData[4] & 0x000000ff; // rest of header is now random data
            data = new byte[length];
            byte[] remainingDataInRecord = Util.getBytes(rawData, 5, 11);
            if (length <= 11) {
                Util.copyBytes(Util.getBytes(remainingDataInRecord, 0, length),
                               data);
            }
            else if (length > 11) {
                int bytesToRead = length - 11;
                int blocksToRead = bytesToRead / file.getBlockSize();

                // if blocksToRead doesn't fit neatly into current block
                // size, add an extra block for the remaining bytes
                if (bytesToRead % file.getBlockSize() != 0)
                    blocksToRead++;

                byte[] remainingRecords = new byte[0];
                for (int i = 0; i < blocksToRead; i++) {
                    byte[] nextBlock = new byte[file.getBlockSize()];
                    file.readDecryptedBytes(nextBlock);
                    if (i == blocksToRead - 1) {
                        // last block, do magic
                        nextBlock =
                            Util.getBytes(nextBlock, 0, bytesToRead
                                - remainingRecords.length);
                    }
                    remainingRecords =
                        Util.mergeBytes(remainingRecords, nextBlock);
                }
                data = Util.mergeBytes(remainingDataInRecord, remainingRecords);
            }
            byte[] dataToHash = data;
            file.hasher.digest(dataToHash);

        }
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

        for (;;) {
            item = new ItemV3((PwsFileV3) file);

            if (item.getType() == END_OF_RECORD) {
                // LOG.debug2( "-- END OF RECORD --" );
                break; // out of the for loop
            }

            if (ignoreFieldTypes) {
                // header record has no valid types...
                itemVal =
                    new PwsUnknownField(item.getType(), item.getByteData());
                attributes.put(Integer.valueOf(item.getType()), itemVal);
            }
            else {

                switch (item.getType()) {
                case V3_ID_STRING:
                    // itemVal = new PwsIntegerField( item.getType(), new byte[]
                    // {3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0} );
                    itemVal = new PwsVersionField(item.getType(),
                                                  item.getByteData());
                    break;

                case UUID:
                    itemVal = new PwsUUIDField(item.getType(),
                                               item.getByteData());
                    break;

                case GROUP:
                case TITLE:
                case USERNAME:
                case NOTES:
                case PASSWORD_POLICY:
                case PASSWORD_HISTORY:
                case URL:
                case AUTOTYPE:
                case RUN_COMMAND:
                case EMAIL:
                case OWN_PASSWORD_SYMBOLS:
                case PASSWORD_POLICY_NAME:
                    itemVal = new PwsStringUnicodeField(item.getType(),
                                                        item.getByteData());
                    break;

                case PASSWORD:
                    itemVal = new PwsPasswdUnicodeField(item.getType(),
                                                        item.getByteData(),
                                                        file);
                    item.clear();
                    break;

                case CREATION_TIME:
                case PASSWORD_MOD_TIME:
                case LAST_ACCESS_TIME:
                case LAST_MOD_TIME:
                    itemVal = new PwsTimeField(item.getType(),
                                               item.getByteData());
                    break;

                case PASSWORD_LIFETIME:
                    itemVal = new PwsTimeField(item.getType(),
                                               item.getByteData());
                    break;

                case PASSWORD_EXPIRY_INTERVAL:
                    itemVal = new PwsIntegerField(item.getType(),
                                                  item.getByteData());
                    break;

                case DOUBLE_CLICK_ACTION:
                    itemVal = new PwsShortField(item.getType(),
                                                item.getByteData());
                    break;

                case PROTECTED_ENTRY:
                    itemVal = new PwsByteField(item.getType(),
                                               item.getByteData());
                    break;

                default:
                    itemVal = new PwsUnknownField(item.getType(),
                                                  item.getByteData());
                    break;
                // throw new UnimplementedConversionException();
                }
                // if ( LOG.isDebug2Enabled() ) LOG.debug2( "type=" +
                // item.getType() + " (" +
                // ((Object[])VALID_TYPES[item.getType()])[1] + "), value=\"" +
                // itemVal.toString() + "\"" );
                setField(itemVal);
            }
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
    protected void saveRecord(PwsFile file) throws IOException
    {
        LOG.debug2("----- START OF RECORD -----");
        for (Iterator<Integer> iter = getFields(); iter.hasNext();) {
            int type;
            PwsField value;

            type = iter.next().intValue();
            value = getField(type);

            if (LOG.isDebug2Enabled())
                LOG.debug2("Writing field " + type + " ("
                    + ((Object[]) VALID_TYPES[type])[1] + ") : \""
                    + value.toString() + "\"");

            writeField(file, value);

            PwsFileV3 fileV3 = (PwsFileV3) file;
            fileV3.hasher.digest(value.getBytes());
        }
        writeField(file, new PwsStringField(END_OF_RECORD, ""));
        LOG.debug2("----- END OF RECORD -----");
    }

    /**
     * Writes a single field to the file.
     *
     * @param file the file to write the field to.
     * @param field the field to be written.
     * @param type the type to write to the file instead of
     *            <code>field.getType()</code>
     * @throws IOException
     */
    @Override
    protected void writeField(PwsFile file, PwsField field, int type)
        throws IOException
    {
        byte lenBlock[];
        byte dataBlock[];

        lenBlock = new byte[5];
        dataBlock = field.getBytes();

        Util.putIntToByteArray(lenBlock, dataBlock.length, 0);
        // Util.putIntToByteArray( lenBlock, type, 4 );
        lenBlock[4] = (byte) type;

        // ensure encryption payload is equal blocks of 16
        int bytesToPad = 0;
        int calcWriteLen = lenBlock.length + dataBlock.length;
        if (calcWriteLen % 16 != 0) {
            bytesToPad = 16 - (calcWriteLen % 16);
        }

        // TODOlib put random bytes here
        dataBlock =
            Util.cloneByteArray(dataBlock, dataBlock.length + bytesToPad);

        // file.writeBytes(lenBlock);
        byte[] dataToWrite = Util.mergeBytes(lenBlock, dataBlock);

        for (int i = 0; i < (dataToWrite.length / 16); i++) {
            byte[] nextBlock = Util.getBytes(dataToWrite, i * 16, 16);
            file.writeEncryptedBytes(nextBlock);
        }

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

        for (Iterator<?> iter = getFields(); iter.hasNext();) {
            Integer key;
            String value;

            key = (Integer) iter.next();
            value = getField(key).toString();

            if (!first) {
                sb.append(", ");
            }
            first = false;

            int i = key.intValue();
            if (i <= VALID_TYPES.length) {
                sb.append(((Object[]) VALID_TYPES[i])[1]);
            }
            else {
                sb.append(key);
            }
            sb.append("=");
            sb.append(value);
        }
        sb.append(" }");

        return sb.toString();
    }

}
