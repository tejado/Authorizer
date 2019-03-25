/*
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import androidx.annotation.NonNull;

import org.pwsafe.lib.Log;
import org.pwsafe.lib.Util;
import org.pwsafe.lib.exception.EndOfFileException;
import org.pwsafe.lib.exception.UnsupportedFileVersionException;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * This abstract class implements the common features of PasswordSafe records.
 * </p>
 * <p>
 * When a record is added to a file it becomes "owned" by that file. Records can
 * only be owned by one file at a time and an exception will be thrown if an
 * attempt is made to add it to another file.
 *
 * @author Kevin Preece
 */
public abstract class PwsRecord implements Comparable<Object>, Serializable
{
    private static final long serialVersionUID = 1L;

    private static final Log LOG = Log.getInstance(
            PwsRecord.class.getPackage().getName());

    /**
     * The default character set used for <code>byte[]</code> to
     * <code>String</code> conversions.
     */
    protected static final String DEFAULT_CHARSET = "ISO-8859-1";

    private boolean modified = false;
    private boolean isLoaded = false;
    protected final Map<Integer, PwsField> attributes = new TreeMap<>();
    private final Object ValidTypes[];

    protected boolean ignoreFieldTypes = false;

    /**
     * A holder class for all the data about a single field. It holds the
     * field's length, data and, for those formats that use it, the field's
     * type.
     */
    protected class Item
    {
        protected byte[] rawData;
        protected byte[] data;
        protected int length;
        protected int type;

        /**
         * No args constructor helps subclassing.
         */
        protected Item()
        {

        }

        /**
         * Reads a single item of data from the file.
         *
         * @param file the file the data should be read from.
         * @throws EndOfFileException
         * @throws IOException
         */
        public Item(PwsFile file) throws EndOfFileException,
                                         IOException
        {
            rawData = file.readBlock();
            length = Util.getIntFromByteArray(rawData, 0);
            //type	= Util.getIntFromByteArray( RawData, 4 );
            type = rawData[4] & 0x000000ff; // rest of header is now random data
            try {
                data = PwsFile.allocateBuffer(length);
            } catch (OutOfMemoryError e) {
                throw new IOException(
                        "Out of memory.  Record length too long: " +
                        length);
            }

            file.readDecryptedBytes(data);
        }

        /**
         * Gets this items data as an array of bytes.
         *
         * @return This items data as an array of bytes.
         */
        public byte[] getByteData()
        {
            if (length != data.length) {
                return Util.cloneByteArray(data, length);
            }
            return data;
        }

        /**
         * Gets this items data as a <code>String</code>. The byte array is
         * converted to a <code>String</code> using
         * <code>DEFAULT_CHARSET</code>
         * as the encoding.
         *
         * @return The item data as a <code>String</code>.
         */
        public String getData()
        {
            try {
                // Use ISO-8859-1 because we may have some
                // values outside the
                // valid ASCII
                // range.
                //
                // TODOlib This needs to be reviewed if the format ever
		    // changes to
                // unicode

                return new String(data, 0, length, DEFAULT_CHARSET);
            } catch (UnsupportedEncodingException e) {
                // Should never get here since all Java implementations must
                // support the above charset.
                return new String(data, 0, length);
            }
        }

        /**
         * Returns this items type. For V1 files this will always be zero.
         *
         * @return This items type.
         */
        public int getType()
        {
            return type;
        }

        /**
         * Returns details about this field as a <code>String</code>
         * suitable
         * for debugging.
         *
         * @return A <code>String</code> representation of this object.
         */
        @Override
        public String toString()
        {
            StringBuilder sb;

            sb = new StringBuilder();
            sb.append("{ type=");
            sb.append(type);
            sb.append(", data=\"");
            sb.append(getData());
            sb.append("\" }");

            return sb.toString();
        }

        public final void clear()
        {
            Arrays.fill(data, (byte)0);
            Arrays.fill(rawData, (byte)0);
            data = new byte[0];
            rawData = new byte[0];
            length = 0;
        }
    }

    /**
     * Simple constructor. Used when creating a new record to add to a file.
     *
     * @param validTypes an array of valid field types.
     */
    PwsRecord(Object[] validTypes)
    {
        super();

        ValidTypes = validTypes;
    }

    /**
     * This constructor is called when a record is to be read from the
     * database.
     *
     * @param owner      the file that data is to be read from and which
     *                   "owns" this record.
     * @param validTypes an array of valid field types.
     * @throws EndOfFileException
     * @throws IOException
     */
    PwsRecord(PwsFile owner, Object[] validTypes) throws
                                                  EndOfFileException,
                                                  IOException
    {
        super();

        ValidTypes = validTypes;

        loadRecord(owner);

        isLoaded = true;
    }

    /**
     * Special constructor for use when ignoring field types.
     *
     * @param owner            the file that data is to be read from and
     *                         which "owns" this record.
     * @param validTypes       an array of valid field types.
     * @param ignoreFieldTypes true if all fields types should be ignored,
     *                                false otherwise
     * @throws EndOfFileException
     * @throws IOException
     */
    protected PwsRecord(PwsFile owner, Object[] validTypes,
                        boolean ignoreFieldTypes)
            throws EndOfFileException, IOException
    {
        super();

        ValidTypes = validTypes;
        this.ignoreFieldTypes = ignoreFieldTypes;

        loadRecord(owner);

        isLoaded = true;

    }

    /**
     * Special constructor for use when ignoring field types.
     *
     * @param validTypes       an array of valid field types.
     * @param ignoreFieldTypes true if all fields types should be ignored,
     *                         false otherwise
     */
    protected PwsRecord(Object[] validTypes, boolean ignoreFieldTypes)
    {
        super();

        ValidTypes = validTypes;
        this.ignoreFieldTypes = ignoreFieldTypes;
    }

    // *************************************************************************
    // * Abstract methods
    // *************************************************************************

    /**
     * Compares this record to another returning a value that is less than
     * zero if this record is "less than" <code>other</code>, zero if they are
     * "equal", or greater than zero if this record is "greater than"
     * <code>other</code>.
     *
     * @param other the record to compare this record to.
     * @return A value &lt; zero if this record is "less than"
     * <code>other</code>, zero if they're equal and &gt; zero if this
     * record is "greater than" <code>other</code>.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public abstract int compareTo(@NonNull Object other);

    /**
     * Compares this record to another returning <code>true</code> if
     * they're equal and <code>false</code> if they're unequal.
     *
     * @param other the record this one is compared to.
     * @return <code>true</code> if the records are equal,
     * <code>false</code> if
     * they're unequal.
     */
    @Override
    public abstract boolean equals(Object other);

    /**
     * Validates the record, returning <code>true</code> if it's valid or
     * <code>false</code> if unequal.
     *
     * @return <code>true</code> if it's valid or <code>false</code> if unequal.
     */
    protected abstract boolean isValid();

    /**
     * Loads this record from <code>file</code>.
     *
     * @param file the file to load the record from.
     * @throws EndOfFileException
     * @throws IOException
     */
    protected abstract void loadRecord(PwsFile file) throws
                                                     EndOfFileException,
                                                     IOException;

    /**
     * Saves this record to <code>file</code>.
     *
     * @param file the file to save the record to.
     * @throws IOException
     */
    protected abstract void saveRecord(PwsFile file) throws IOException;

    /**
     * Provide subclasses a means to handle unknown field values not
     * included in ValidTypes. Used by PWSRecordV3. Defaults to false.
     *
     * @return false
     */
    protected boolean allowUnknownFieldTypes()
    {
        return false;
    }

    // *************************************************************************
    // * Class methods
    // *************************************************************************

    /**
     * Gets the value of a field. See the subclass documentation for valid
     * values for <code>type</code>.
     *
     * @param aType the field to get.
     * @return The value of the field.
     */
    public final PwsField getField(int aType)
    {
        return getField(Integer.valueOf(aType));
    }

    /**
     * Gets the value of a field. See the subclass documentation for valid
     * values for <code>type</code>.
     *
     * @param aType the field to get.
     * @return The value of the field.
     */
    protected final PwsField getField(Integer aType)
    {
        return attributes.get(aType);
    }

    /**
     * Returns an <code>Iterator</code> that returns the field types (but
     * not the values) that have been stored. Use one of the
     * <code>getField</code> methods to get the value. The iterators
     * <code>next()</code> method returns an <code>Integer</code>
     *
     * @return An <code>Iterator</code> over the stored field codes.
     */
    protected Iterator<Integer> getFields()
    {
        return attributes.keySet().iterator();
    }

    /**
     * Returns <code>true</code> if the record has been modified or
     * <code>false</code> if not.
     *
     * @return <code>true</code> if the record has been modified or
     * <code>false</code> if not.
     */
    public boolean isModified()
    {
        return modified;
    }

    /**
     * Read a record from the given file.
     *
     * @param file the file to read the record from.
     * @return The record that was read.
     * @throws EndOfFileException
     * @throws IOException
     * @throws UnsupportedFileVersionException
     */
    public static PwsRecord read(PwsFile file)
            throws EndOfFileException, IOException,
                   UnsupportedFileVersionException
    {
        switch (file.getFileVersionMajor()) {
        case PwsFileV1.VERSION:
            return new PwsRecordV1(file);

        case PwsFileV2.VERSION:
            return new PwsRecordV2(file);

        case PwsFileV3.VERSION:
            return new PwsRecordV3(file);
        }
        throw new UnsupportedFileVersionException();
    }

    /**
     * Resets the modified flag.
     */
    public void resetModified()
    {
        modified = false;
    }

    /**
     * Mark the record as loaded
     */
    public final void setLoaded()
    {
        isLoaded = true;
    }

    /**
     * Sets a field on this record from <code>value</code>.
     *
     * @param value the field to set
     * @throws IllegalArgumentException if value is not the correct type
     * for the file.
     */
    public void setField(PwsField value)
    {
        int theType;

        theType = value.getType();

        if (ignoreFieldTypes) {
            attributes.put(theType, value);
            setModified();
            return;
        }

        // try a shortcut first
        if (theType < ValidTypes.length) {
            if ((Integer)((Object[])ValidTypes[theType])[0] == theType) {
                Class<? extends PwsField> cl = value.getClass();

                if (cl == (((Object[])ValidTypes[theType])[2])) {
                    attributes.put(theType, value);
                    setModified();
                    return;
                }
            }
        }
        // no chance, iterate over all values
        for (Object ValidType : ValidTypes) {
            int vType;

            vType = (Integer)((Object[])ValidType)[0];

            if (vType == theType) {
                Class<? extends PwsField> cl = value.getClass();

                if (cl == (((Object[])ValidType)[2])) {
                    attributes.put(theType, value);
                    setModified();
                    return;
                }
            }
        }
        // before giving up, check if unknown fields are allowed
        if (allowUnknownFieldTypes()) {
            LOG.warn("Adding unknown field of type " + theType +
                     ", class " + value.getClass() +
                     " - maybe a new version is needed?");
            attributes.put(theType, value);
            setModified();
        } else {
            throw new IllegalArgumentException(
                    "Invalid type: " + theType);
        }
    }

    /**
     * Remove a field from this record
     *
     * @param type The type of field to remove
     */
    public void removeField(int type)
    {
        PwsField field = attributes.remove(type);
        if (field != null) {
            setModified();
        }
    }

    /**
     * Sets the modified flag on this record, and also on the file this record
     * belongs to.
     */
    private void setModified()
    {
        if (isLoaded) {
            modified = true;
        }
    }

    /**
     * Writes a single field to the file.
     *
     * @param file  the file to write the field to.
     * @param field the field to be written.
     * @param aType the type to write to the file instead of <code>field
     *                     .getType()</code>
     * @throws IOException
     */
    protected void writeField(PwsFile file, PwsField field, int aType)
            throws IOException
    {
        byte lenBlock[];
        byte dataBlock[];

        lenBlock = new byte[PwsFile.calcBlockLength(8)];
        dataBlock = field.getBytes();

        Util.putIntToByteArray(lenBlock, dataBlock.length, 0);
        Util.putIntToByteArray(lenBlock, aType, 4);
        // TODOlib put random bytes here

        dataBlock = Util.cloneByteArray(dataBlock, PwsFile
                .calcBlockLength(dataBlock.length));

        file.writeEncryptedBytes(lenBlock);
        file.writeEncryptedBytes(dataBlock);
    }

    /**
     * Writes a single field to the file.
     *
     * @param file  the file to write the field to.
     * @param field the field to be written.
     * @throws IOException
     */
    protected void writeField(PwsFile file, PwsField field) throws IOException
    {
        writeField(file, field, field.getType());
    }
}
