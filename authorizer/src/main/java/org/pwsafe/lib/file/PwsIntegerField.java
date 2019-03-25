/*
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import androidx.annotation.NonNull;

import org.pwsafe.lib.Util;

/**
 * Provides a wrapper for fields that hold an integer value such as the date
 * and time fields.  Integer values are stored in the database in little-endian
 * order and are converted to and from this format on writing and reading.
 *
 * @author Kevin Preece
 */
public class PwsIntegerField extends PwsField
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs the object
     *
     * @param type  the field type.  Values depend on the version of the file
     *              being read.
     * @param value the byte array holding the integer value.
     * @throws IndexOutOfBoundsException If <code>value.length</code> &lt; 4.
     */
    public PwsIntegerField(int type, byte[] value)
    {
        super(type, Util.getIntFromByteArray(value, 0));
    }

    /**
     * Constructor
     */
    public PwsIntegerField(int type, Integer value)
    {
        super(type, value);
    }

    /**
     * Constructs the object
     *
     * @param type  the field type.  Values depend on the version of the
     *                     file being read.
     * @param value the byte array holding the integer value.
     * @throws IndexOutOfBoundsException If <code>value.length</code>
     * &lt; 4.
     */
    public PwsIntegerField(PwsFieldType type, byte[] value)
    {
        super(type, Util.getIntFromByteArray(value, 0));
    }

    /**
     * Returns this integer as an array of bytes.  The returned array will
     * have a length of PwsFile.BLOCK_LENGTH and is thus suitable to be written
     * to the database.
     *
     * @return a byte array containing the field's integer value.
     * @see org.pwsafe.lib.file.PwsField#getBytes()
     */
    @Override
    public byte[] getBytes()
    {
        int value;
        byte retval[];

        value = (Integer)super.getValue();
        // Force a size of 4, otherwise it would be set to a size of blocklength
        retval = new byte[4];

        Util.putIntToByteArray(retval, value, 0);

        return retval;
    }

    /**
     * Compares this fileld to another <code>PwsIntegerField</code>.  The
     * return value is less than zero if this is less than <code>other</code>,
     * zero if they are equal and greater than zero if this is greater than
     * <code>other</code>.
     *
     * @param other the other field to compare to.
     * @return An integer indicating whether this field's value is &lt;, =
     * or &gt; <code>other</code>'s value.
     * @throws ClassCastException if <code>other</code> is not a
     * <code>PwsIntegerField</code>.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(@NonNull Object other)
    {
        return ((Integer)getValue())
                .compareTo((Integer)((PwsIntegerField)other).getValue());
    }

    /**
     * Compares this field's value to another <code>PwsIntegerField</code>
     * or an Integer.  It returns <code>true</code> if they are equal and
     * <code>false</code> if they're unequal.
     *
     * @param arg0 the object to compare with.
     * @return <code>true</code> if the values of this and arg0 are equal,
     * <code>false</code> if they're unequal.
     * @throws ClassCastException if <code>other</code> is neither a
     * <code>PwsIntegerField</code> nor an <code>Integer</code>.
     */
    @Override
    public boolean equals(Object arg0)
    {
        if (arg0 instanceof PwsIntegerField) {
            return getValue().equals(((PwsIntegerField)arg0).getValue());
        } else if (arg0 instanceof Integer) {
            return getValue().equals(arg0);
        }
        throw new ClassCastException();
    }
}
