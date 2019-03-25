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

/**
 * @author Kevin Preece
 */
public class PwsUUIDField extends PwsField
{
    private static final long serialVersionUID = 4760068611660875030L;

    /**
     * Constructor
     *
     * @param type  the field's type.
     * @param value the field's value.
     */
    public PwsUUIDField(int type, byte[] value)
    {
        super(type, new UUID(value));
    }

    /**
     * Constructor
     *
     * @param type  the field's type.
     * @param value the field's value.
     */
    @SuppressWarnings("SameParameterValue")
    public PwsUUIDField(int type, UUID value)
    {
        super(type, value);
    }

    /**
     * Constructor
     *
     * @param type  the field's type.
     * @param value the field's value.
     */
    public PwsUUIDField(PwsFieldType type, UUID value)
    {
        super(type, value);
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
        return ((UUID)super.getValue()).getBytes();
    }

    /**
     * Compares this <code>PwsUUIDField</code> to another returning a
     * value less than zero if <code>this</code> is "less than"
     * <code>that</code>, zero if they're equal and greater than zero if
     * <code>this</code> is "greater than" <code>that</code>.
     *
     * @param that the other field to compare to.
     * @return A value less than zero if <code>this</code> is "less than"
     * <code>that</code>, zero if they're equal and greater than zero if
     * <code>this</code> is "greater than" <code>that</code>.
     */
    public int compareTo(@NonNull Object that)
    {
        return ((UUID)this.getValue()).compareTo(
                (UUID)((PwsUUIDField)that).getValue());
    }

    /**
     * Compares this object to another <code>PwsUUIDField</code> or
     * {@link UUID} returning <code>true</code> if they're equal or
     * <code>false</code> otherwise.
     *
     * @param arg0 the other object to compare to.
     * @return <code>true</code> if they're equal or <code>false</code>
     * otherwise.
     */
    @Override
    public boolean equals(Object arg0)
    {
        if (arg0 instanceof PwsUUIDField) {
            return getValue().equals(((PwsUUIDField)arg0).getValue());
        } else if (arg0 instanceof UUID) {
            return ((UUID)getValue()).equals((UUID)arg0);
        }
        throw new ClassCastException();
    }

}
