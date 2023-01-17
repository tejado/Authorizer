/*
 * Copyright (c) 2008-2009 David Muller
 * <roxon@users.sourceforge.net>. All rights reserved. Use of the code is
 * allowed under the Artistic License 2.0 terms, as specified in the LICENSE
 * file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import androidx.annotation.NonNull;

import org.pwsafe.lib.Log;
import org.pwsafe.lib.UUID;
import org.pwsafe.lib.Util;
import org.pwsafe.lib.exception.EndOfFileException;
import org.pwsafe.lib.exception.RecordLoadException;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;

/**
 * Support for new v3 Record type.
 *
 * @author Glen Smith (based on Kevin's code for V2 records)
 */
@SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
public class PwsRecordV3 extends PwsRecord
{
    private static final long serialVersionUID = -3160317668375599155L;

    private static final Log LOG = Log.getInstance(Objects.requireNonNull(
            PwsRecordV3.class.getPackage()).getName());

    /**
     * Constant for the version 3 ID string field.
     */
    public static final int V3_ID_STRING = 0;

    /**
     * Minor version for PasswordSafe 3.25 with protected entry support
     */
    public static final byte DB_FMT_MINOR_3_25 = 8;

    /**
     * Minor version for PasswordSafe 3.28 with password policy support
     */
    public static final byte DB_FMT_MINOR_3_28 = 10;

    /**
     * Minor version for PasswordSafe 3.30
     */
    public static final byte DB_FMT_MINOR_3_30 = 0x0D;

    /**
     * Minor version of the max supported database format
     */
    public static final byte DB_FMT_MINOR_VERSION = DB_FMT_MINOR_3_30;

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
     * Password expiration interval
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

    /**
     * Own symbols for password
     */
    public static final int OWN_PASSWORD_SYMBOLS = 22;

    /**
     * Shift double-click action
     */
    public static final int SHIFT_DOUBLE_CLICK_ACTION = 23;

    /**
     * Password policy name
     */
    public static final int PASSWORD_POLICY_NAME = 24;

    /**
     * Entry keyboard shortcut
     */
    public static final int ENTRY_KEYBOARD_SHORTCUT = 25;

    /**
     * Icon (not compliant to original Pws file format)
     */
    public static final int ICON = 191;

    /**
     * OTP Token (not compliant to original Pws file format)
     */
    public static final int OTP = 192;

    /**
     * Autotype Delimiter (not compliant to original Pws file format)
     */
    public static final int AUTOTYPE_DELIMITER = 193;

    /**
     * Autotype Return Suffix (not compliant to original Pws file format)
     */
    public static final int AUTOTYPE_RETURNSUFFIX = 194;

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

    /**
     * Header named password policies
     */
    public static final int HEADER_NAMED_PASSWORD_POLICIES = 16;

    /**
     * Header for Yubico
     */
    @SuppressWarnings("unused")
    public static final int HEADER_YUBICO = 0x12;

    /**
     * Constant for the end of record marker field.
     */
    public static final int END_OF_RECORD = 255;

    /**
     * All the valid type codes.
     */
    private static final Object[] VALID_TYPES = new Object[] {
            new Object[]{V3_ID_STRING,
                         "V3_ID_STRING", PwsVersionField.class},
            new Object[]{UUID,
                         "UUID", PwsUUIDField.class},
            new Object[]{GROUP,
                         "GROUP", PwsStringUnicodeField.class},
            new Object[]{TITLE,
                         "TITLE", PwsStringUnicodeField.class},
            new Object[]{USERNAME,
                         "USERNAME", PwsStringUnicodeField.class},
            new Object[]{NOTES,
                         "NOTES", PwsStringUnicodeField.class},
            new Object[]{PASSWORD,
                         "PASSWORD", PwsPasswdUnicodeField.class},
            new Object[]{CREATION_TIME,
                         "CREATION_TIME", PwsTimeField.class},
            new Object[]{PASSWORD_MOD_TIME,
                         "PASSWORD_MOD_TIME", PwsTimeField.class},
            new Object[]{LAST_ACCESS_TIME,
                         "LAST_ACCESS_TIME", PwsTimeField.class},
            new Object[]{PASSWORD_LIFETIME,
                         "PASSWORD_LIFETIME", PwsTimeField.class},
            new Object[]{PASSWORD_POLICY_DEPRECATED,
                         "PASSWORD_POLICY_OLD",
                         PwsStringUnicodeField.class},
            new Object[]{LAST_MOD_TIME,
                         "LAST_MOD_TIME", PwsTimeField.class},
            new Object[]{URL,
                         "URL", PwsStringUnicodeField.class},
            new Object[]{AUTOTYPE,
                         "AUTOTYPE", PwsStringUnicodeField.class},
            new Object[]{PASSWORD_HISTORY,
                         "PASSWORD_HISTORY", PwsStringUnicodeField.class},
            new Object[]{PASSWORD_POLICY,
                         "PASSWORD_POLICY", PwsStringUnicodeField.class},
            new Object[]{PASSWORD_EXPIRY_INTERVAL,
                         "PASSWORD_EXPIRY_INTERVAL", PwsIntegerField.class},
            new Object[]{RUN_COMMAND,
                         "RUN_COMMAND", PwsStringUnicodeField.class},
            new Object[]{DOUBLE_CLICK_ACTION,
                         "DOUBLE_CLICK_ACTION", PwsShortField.class},
            new Object[]{EMAIL,
                         "EMAIL", PwsStringUnicodeField.class},
            new Object[]{PROTECTED_ENTRY,
                         "PROTECTED_ENTRY", PwsByteField.class},
            new Object[]{OWN_PASSWORD_SYMBOLS,
                         "OWN_PASSWORD_SYMBOLS", PwsStringUnicodeField.class},
            new Object[]{SHIFT_DOUBLE_CLICK_ACTION,
                         "SHIFT_DOUBLE_CLICK_ACTION", PwsShortField.class},
            new Object[]{PASSWORD_POLICY_NAME,
                         "PASSWORD_POLICY_NAME", PwsStringUnicodeField.class},
            new Object[]{ENTRY_KEYBOARD_SHORTCUT,
                         "ENTRY_KEYBOARD_SHORTCUT", PwsIntegerField.class},
            new Object[]{ICON,
                         "ICON", PwsStringUnicodeField.class},
            new Object[]{OTP,
                         "OTP", PwsStringUnicodeField.class},
            new Object[]{AUTOTYPE_DELIMITER,
                         "AUTOTYPE_DELIMITER", PwsIntegerField.class},
            new Object[]{AUTOTYPE_RETURNSUFFIX,
                         "AUTOTYPE_RETURNSUFFIX", PwsIntegerField.class},
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
    PwsRecordV3(@SuppressWarnings({"UnusedParameters",
                                   "SameParameterValue"}) boolean isHeader)
    {
        super(VALID_TYPES, true);
        setField(new PwsVersionField(HEADER_VERSION,
                                     new byte[]{DB_FMT_MINOR_VERSION, 3}));
        setField(new PwsUUIDField(HEADER_UUID, new UUID()));
    }

    /**
     * Create a new record by reading it from <code>file</code>.
     *
     * @param file the file to read data from.
     * @throws EndOfFileException If end of file is reached
     * @throws IOException        If a read error occurs.
     */
    PwsRecordV3(PwsFile file) throws EndOfFileException, IOException, RecordLoadException
    {
        super(file, VALID_TYPES);
    }

    /**
     * A special version which reads and ignores all headers since they have
     * different ids to standard types.
     *
     * @param file       the file to read data from.
     * @param ignoreFieldTypes true if all fields types should be ignored,
     *                         false otherwise
     * @throws EndOfFileException If end of file is reached
     * @throws IOException        If a read error occurs.
     */
    PwsRecordV3(PwsFile file, @SuppressWarnings("SameParameterValue") boolean ignoreFieldTypes)
            throws EndOfFileException, IOException, RecordLoadException
    {
        super(file, VALID_TYPES, ignoreFieldTypes);
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
     * Compares this record to another returning a value that is less than zero
     * if this record is "less than" <code>other</code>, zero if they are
     * "equal", or greater than zero if this record is "greater than"
     * <code>other</code>.
     *
     * @param other the record to compare this record to.
     * @return A value &lt; zero if this record is "less than"
     * <code>other</code> , zero if they're equal and &gt; zero if this record
     * is "greater than" <code>other</code>.
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
     * @throws ClassCastException if <code>that</code> is not a
     *                            <code>PwsRecordV1</code>.
     */
    @Override
    public boolean equals(Object that)
    {
        if (that instanceof PwsRecordV3) {
            UUID thisUUID = (UUID)getField(UUID).getValue();
            UUID thatUUID = (UUID)((PwsRecord)that).getField(UUID).getValue();
            return thisUUID.equals(thatUUID);
        } else {
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
        PwsField idField = getField(V3_ID_STRING);
        return idField == null;
    }

    protected boolean isHeaderRecord()
    {
        PwsField idField = getField(V3_ID_STRING);
        return idField != null;
    }

    static final byte[] EOF_BYTES_RAW = "PWS3-EOFPWS3-EOF".getBytes();

    protected static class ItemV3 extends Item
    {
        public ItemV3(PwsFileV3 file) throws EndOfFileException, IOException
        {
            super();
            try {
                rawData = file.readBlock();
            } catch (EndOfFileException eofe) {
                data = new byte[32]; // to hold closing HMAC
                file.readBytes(data);
                byte[] hash = file.hasher.doFinal();
                if (!Util.bytesAreEqual(data, hash)) {
                    LOG.error("HMAC record did not match. File may have been " +
                              "tampered");
                    throw new IOException("HMAC record did not match. File " +
                                          "has been tampered");
                }
                throw eofe;
            }

            length = Util.getIntFromByteArray(rawData, 0);
            type = rawData[4] & 0x000000ff; // rest of header is now random data
            try {
                data = new byte[length];
            } catch (OutOfMemoryError e) {
                throw new IOException(
                        "Out of memory.  Record length too long: " + length);
            }
            byte[] remainingDataInRecord = Util.getBytes(rawData, 5, 11);
            if (length <= 11) {
                System.arraycopy(remainingDataInRecord, 0, data, 0, length);
            } else {
                int bytesToRead = length - 11;
                final int blockSize = file.getBlockSize();
                int blocksToRead = bytesToRead / blockSize;

                System.arraycopy(remainingDataInRecord, 0, data, 0,
                                 remainingDataInRecord.length);
                int pos = remainingDataInRecord.length;

                byte[] nextBlock = new byte[blockSize];
                int nextBlockLen = nextBlock.length;
                for (int i = 0; i < blocksToRead; i++, pos += nextBlockLen) {
                    file.readDecryptedBytes(nextBlock);
                    System.arraycopy(nextBlock, 0, data, pos, nextBlockLen);
                }

                // if blocksToRead doesn't fit neatly into current block
                // size, add an extra block for the remaining bytes
                if ((bytesToRead % blockSize) != 0) {
                    file.readDecryptedBytes(nextBlock);
                    int bytesRead = pos - remainingDataInRecord.length;
                    nextBlockLen = bytesToRead - bytesRead;
                    System.arraycopy(nextBlock, 0, data, pos, nextBlockLen);
                }
            }
            byte[] dataToHash = data;
            file.hasher.digest(dataToHash);
        }
    }

    /**
     * Initialises this record by reading its data from <code>file</code>.
     *
     * @param file the file to read the data from.
     */
    @Override
    protected void loadRecord(PwsFile file)
            throws EndOfFileException, RecordLoadException
    {
        ArrayList<Throwable> itemErrors = null;
        for (; ; ) {
            try {
                Item item = new ItemV3((PwsFileV3)file);

                if (item.getType() == END_OF_RECORD) {
                    break; // out of the for loop
                }

                PwsField itemVal;
                if (ignoreFieldTypes) {
                    // header record has no valid types...
                    itemVal = new PwsUnknownField(item.getType(),
                                                  item.getByteData());
                    attributes.put(item.getType(), itemVal);
                } else {

                    switch (item.getType()) {
                    case V3_ID_STRING:
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
                    case ICON:
                    case OTP:
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
                    case PASSWORD_LIFETIME:
                        itemVal = new PwsTimeField(item.getType(),
                                                   item.getByteData());
                        break;

                    case AUTOTYPE_DELIMITER:
                    case AUTOTYPE_RETURNSUFFIX:
                    case PASSWORD_EXPIRY_INTERVAL:
                    case ENTRY_KEYBOARD_SHORTCUT:
                        itemVal = new PwsIntegerField(item.getType(),
                                                      item.getByteData());
                        break;

                    case DOUBLE_CLICK_ACTION:
                    case SHIFT_DOUBLE_CLICK_ACTION:
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
                    }
                    setField(itemVal);
                }
            } catch (EndOfFileException eof) {
                if (itemErrors != null) {
                    throw new RecordLoadException(this, itemErrors);
                }
                throw eof;
            } catch (Throwable t) {
                if (itemErrors == null) {
                    itemErrors = new ArrayList<>();
                }
                itemErrors.add(t);
            }
        }

        if (itemErrors != null) {
            throw new RecordLoadException(this, itemErrors);
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
        for (Iterator<Integer> iter = getFields(); iter.hasNext(); ) {
            int type;
            PwsField value;

            type = iter.next();
            value = getField(type);

            writeField(file, value);

            PwsFileV3 fileV3 = (PwsFileV3)file;
            fileV3.hasher.digest(value.getBytes());
        }
        writeField(file, new PwsStringField(END_OF_RECORD, ""));
    }

    /**
     * Writes a single field to the file.
     *
     * @param file  the file to write the field to.
     * @param field the field to be written.
     * @param type  the type to write to the file instead of
     *              <code>field.getType()</code>
     * @throws IOException
     */
    @Override
    protected void writeField(PwsFile file, PwsField field, int type)
            throws IOException
    {
        byte[] lenBlock = new byte[5];
        byte[] dataBlock = field.getBytes();

        Util.putIntToByteArray(lenBlock, dataBlock.length, 0);
        lenBlock[4] = (byte)type;

        final int blockSize = 16;
        byte[] nextBlock = new byte[blockSize];

        int firstBlockLen = Math.min(dataBlock.length, 11);
        System.arraycopy(lenBlock, 0, nextBlock, 0, lenBlock.length);
        System.arraycopy(dataBlock, 0,
                         nextBlock, lenBlock.length, firstBlockLen);
        file.writeEncryptedBytes(nextBlock);

        int bytesToWrite = dataBlock.length - 11;
        if (bytesToWrite > 0) {
            int blocksToWrite = bytesToWrite / blockSize;
            int pos = 11;
            int nextBlockLen = nextBlock.length;
            for (int i = 0; i < blocksToWrite; ++i, pos += nextBlockLen) {
                System.arraycopy(dataBlock, pos, nextBlock, 0, nextBlockLen);
                file.writeEncryptedBytes(nextBlock);
            }

            if ((bytesToWrite % blockSize) != 0) {
                int bytesWritten = pos - 11;
                nextBlockLen = bytesToWrite - bytesWritten;
                System.arraycopy(dataBlock, pos, nextBlock, 0, nextBlockLen);
                Arrays.fill(nextBlock, nextBlockLen, nextBlock.length, (byte)0);
                file.writeEncryptedBytes(nextBlock);
            }
        }

    }

    /**
     * Returns a string representation of this record.
     *
     * @return A string representation of this object.
     */
    @Override
    @NonNull
    public String toString()
    {
        boolean first = true;
        final StringBuilder sb = new StringBuilder();

        sb.append("{ ");

        for (Iterator<?> iter = getFields(); iter.hasNext(); ) {
            int key;
            String value;

            key = (Integer)iter.next();
            value = getField(key).toString();

            if (!first) {
                sb.append(", ");
            }
            first = false;

            boolean showValue = true;
            if (key <= VALID_TYPES.length) {
                Object[] type = (Object[])VALID_TYPES[key];
                sb.append(type[1]);
                int typeVal = (Integer)type[0];
                showValue = (typeVal != PASSWORD) && (typeVal != NOTES);
            }
            else {
                sb.append(key);
            }
            sb.append("=");
            if (showValue) {
                sb.append(value);
            }
        }
        sb.append(" }");

        return sb.toString();
    }

}
