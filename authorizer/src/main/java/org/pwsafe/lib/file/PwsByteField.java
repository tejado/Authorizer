/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import androidx.annotation.NonNull;

/**
 * Provides a wrapper for fields that hold a byte value. Byte values
 * are stored in the database in little-endian order and are converted to and
 * from this format on writing and reading.
 *
 * @author Jeff Harris
 */
public class PwsByteField extends PwsField
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs the object
     *
     * @param type  the field type.  Values depend on the version of the file
     *              being read.
     * @param value the byte array holding the byte value.
     * @throws IndexOutOfBoundsException If <code>value.length</code> &lt; 1.
     */
    public PwsByteField(int type, byte[] value)
    {
        super(type, value[0]);
    }

    /**
     * Constructs the object
     *
     * @param type  the field type.  Values depend on the version of the file
     *              being read.
     * @param value the byte value.
     */
    public PwsByteField(int type, byte value)
    {
        super(type, value);
    }

    /**
     * Returns this byte as an array of bytes.  The returned array will have
     * a length of PwsFile.BLOCK_LENGTH and is thus suitable to be written to
     * the database.
     *
     * @return a byte array containing the field's byte value.
     * @see org.pwsafe.lib.file.PwsField#getBytes()
     */
    @Override
    public byte[] getBytes()
    {
        byte value;
        byte retval[];

        value = (Byte)super.getValue();
        // Force a size of 1, otherwise it would be set to a size of blocklength
        retval = new byte[1];
        retval[0] = value;
        return retval;
    }

    /**
     * Compares this fileld to another <code>PwsByteField</code>.  The return
     * value is less than zero if this is less than <code>other</code>, zero if
     * they are equal and greater than zero if this is greater than
     * <code>other</code>.
     *
     * @param other the other field to compare to.
     * @return An integer indicating whether this field's value is &lt;, = or
     * &gt; <code>other</code>'s value.
     * @throws ClassCastException if <code>other</code> is not a
     * <code>PwsByteField</code>.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(@NonNull Object other)
    {
        return ((Byte)getValue()).compareTo((Byte)((PwsByteField)other)
                .getValue());
    }

    /**
     * Compares this field's value to another <code>PwsByteField</code> or an
     * Byte.  It returns <code>true</code> if they are equal and
     * <code>false</code> if they're unequal.
     *
     * @param arg0 the object to compare with.
     * @return <code>true</code> if the values of this and arg0 are equal,
     * <code>false</code> if they're unequal.
     * @throws ClassCastException if <code>other</code> is neither a
     * <code>PwsByteField</code> nor an <code>Byte</code>.
     */
    @Override
    public boolean equals(Object arg0)
    {
        if (arg0 instanceof PwsByteField) {
            return getValue().equals(((PwsByteField)arg0).getValue());
        } else if (arg0 instanceof Byte) {
            return getValue().equals(arg0);
        }
        throw new ClassCastException();
    }
}
