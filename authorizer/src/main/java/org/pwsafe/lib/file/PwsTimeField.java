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

import java.util.Date;

/**
 * @author Kevin Preece
 */
public class PwsTimeField extends PwsField
{
    private static final long serialVersionUID = -3091539688166386331L;

    /**
     * Constructor
     *
     * @param type  the field's type.
     * @param value the field's value.
     */
    public PwsTimeField(int type, byte[] value)
    {
        super(type, new Date(Util.getMillisFromByteArray(value, 0)));
    }

    /**
     * Constructor
     *
     * @param type  the field's type.
     * @param aDate the field's value.
     */
    public PwsTimeField(int type, Date aDate)
    {
        super(type, aDate);
    }

    /**
     * Constructor
     *
     * @param type  the field's type.
     * @param aDate the field's value.
     */
    @SuppressWarnings("SameParameterValue")
    public PwsTimeField(PwsFieldType type, Date aDate)
    {
        super(type, aDate);
    }

    /**
     * Returns the field's value as a byte array.
     *
     * @return A byte array containing the field's data.
     * @see org.pwsafe.lib.file.PwsField#getBytes()
     */
    @Override
    public byte[] getBytes()
    {
        long value = ((Date)getValue()).getTime();

        // Force a size of 4, otherwise it would be set to a size of
        // blocklength
        byte[] retval = new byte[4];
        Util.putMillisToByteArray(retval, value, 0);
        return retval;
    }

    /**
     * Compares this <code>PwsTimeField</code> to another returning a
     * value less than zero if <code>this</code> is "less than"
     * <code>that</code>, zero if they're equal and greater
     * than zero if <code>this</code> is "greater than" <code>that</code>.
     *
     * @param that the other field to compare to.
     * @return A value less than zero if <code>this</code> is "less than"
     * <code>that</code>, zero if they're equal and greater than zero if
     * <code>this</code> is "greater than" <code>that</code>.
     */
    public int compareTo(@NonNull Object that)
    {
        return ((Date)this.getValue()).compareTo(
                (Date)((PwsTimeField)that).getValue());
    }

    /**
     * Compares this object to another <code>PwsTimeField</code> or
     * <code>java.util.Date</code> returning <code>true</code> if they're equal
     * or <code>false</code> otherwise.
     *
     * @param arg0 the other object to compare to.
     * @return <code>true</code> if they're equal or <code>false</code>
     * otherwise.
     */
    @Override
    public boolean equals(Object arg0)
    {
        if (arg0 instanceof PwsTimeField) {
            return getValue().equals(((PwsTimeField)arg0).getValue());
        } else if (arg0 instanceof Date) {
            return getValue().equals(arg0);
        }
        throw new ClassCastException();
    }

}
