/*
 * Copyright (©) 2009-2010 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import org.pwsafe.lib.Util;

/**
 * Provides a wrapper for fields that hold an integer value such as the date
 * and time fields.  Integer values are stored in the database in little-endian
 * order and are converted to and from this format on writing and reading.
 *
 * @author Jeff Harris
 */
public class PwsShortField extends PwsField
{
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs the object
     *
     * @param type  the field type.  Values depend on the version of the file being read.
     * @param value the byte array holding the integer value.
     *
     * @throws IndexOutOfBoundsException If <code>value.length</code> &lt; 2.
     */
    public PwsShortField( int type, byte [] value )
    {
        super( type, new Short( Util.getShortFromByteArray(value, 0) ) );
    }

    /**
     * Constructs the object
     *
     * @param type  the field type.  Values depend on the version of the file being read.
     * @param value the byte array holding the integer value.
     *
     * @throws IndexOutOfBoundsException If <code>value.length</code> &lt; 2.
     */
    public PwsShortField( PwsFieldType type, byte [] value )
    {
        super( type, new Short( Util.getShortFromByteArray(value, 0) ) );
    }

    /**
     * Returns this integer as an array of bytes.  The returned array will have
     * a length of PwsFile.BLOCK_LENGTH and is thus suitable to be written to the
     * database.
     *
     * @return a byte array containing the field's integer value.
     *
     * @see org.pwsafe.lib.file.PwsField#getBytes()
     */
    @Override
    public byte[] getBytes()
    {
        short     value;
        byte    retval[];

        value   = ((Short) super.getValue()).shortValue();
        retval  = PwsFile.allocateBuffer( 2 );

        Util.putShortToByteArray( retval, value, 0 );

        return retval;
    }

    /**
     * Compares this fileld to another <code>PwsShortField</code>.  The return
     * value is less than zero if this is less than <code>other</code>, zero if
     * they are equal and greater than zero if this is greater than
     * <code>other</code>.
     *
     * @param other the other field to compare to.
     *
     * @return An integer indicating whether this field's value is &lt;, = or &gt;
     *         <code>other</code>'s value.
     *
     * @throws ClassCastException if <code>other</code> is not a <code>PwsShortField</code>.
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( Object other )
    {
        return ((Short) getValue()).compareTo((Short) ((PwsShortField) other).getValue());
    }

    /**
     * Compares this field's value to another <code>PwsShortField</code> or an
     * Short.  It returns <code>true</code> if they are equal and <code>false</code>
     * if they're unequal.
     *
     * @param arg0 the object to compare with.
     *
     * @return <code>true</code> if the values of this and arg0 are equal,
     *         <code>false</code> if they're unequal.
     *
     * @throws ClassCastException if <code>other</code> is neither a <code>PwsShortField</code>
     *         nor an <code>Short</code>.
     */
    @Override
    public boolean equals( Object arg0 )
    {
        if ( arg0 instanceof PwsShortField )
        {
            return equals( (PwsShortField) arg0 );
        }
        else if ( arg0 instanceof Short )
        {
            return equals( (Short) arg0 );
        }
        throw new ClassCastException();
    }

    /**
     * Compares this field's value to another <code>PwsShortField</code>.  It
     * returns <code>true</code> if they are equal and <code>false</code>
     * if they're unequal.
     *
     * @param arg0 the <code>PwsShortField</code> to compare to.
     *
     * @return <code>true</code> if the values of this and arg0 are equal,
     *         <code>false</code> if they're unequal.
     *
     * @throws ClassCastException if <code>other</code> is neither a <code>PwsShortField</code>
     *         nor an <code>Short</code>.
     */
    public boolean equals( PwsShortField arg0 )
    {
        return ((Short) getValue()).equals(arg0.getValue());
    }

    /**
     * Compares this field's value to an <code>Short</code>.  It returns
     * <code>true</code> if they are equal and <code>false</code>
     * if they're unequal.
     *
     * @param arg0 the <code>Short</code> to compare to.
     *
     * @return <code>true</code> if the values of this and arg0 are equal,
     *         <code>false</code> if they're unequal.
     *
     * @throws ClassCastException if <code>other</code> is neither a <code>PwsShortField</code>
     *         nor an <code>Short</code>.
     */
    public boolean equals( Short arg0 )
    {
        return ((Short) getValue()).equals(arg0);
    }
}
