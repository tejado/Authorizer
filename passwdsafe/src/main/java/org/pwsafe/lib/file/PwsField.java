/*
 * $Id: PwsField.java 400 2009-09-07 21:37:16Z roxon $
 *
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import java.io.Serializable;


/**
 * A record in PasswordSafe consists of a number of fields.  In V1.7 files
 * all fields are strings but in V2.0 they can be of type String, UUID, time_t
 * (a 32-bit integer holding the number of milliseconds since 00:00:00 on 1st
 * January 1970) or integer.
 * </p><p>
 * This class is abstract because each subclass, although sharing similar
 * features, has unique characteristics that are best handled with
 * polymorphism.  This allows easier support for new field types should new
 * ones be introduced in later versions of the program.
 * </p>
 *
 * @author Kevin Preece
 */
public abstract class PwsField implements Comparable<Object>, Serializable
{
    private Object Value = null;
    private final int Type;

    /**
     * Creates the field object.
     *
     * @param type  the field type.
     * @param value the field value.
     */
    protected PwsField(int type, Object value)
    {
        super();

        Type = type;
        Value = value;
    }

    /**
     * Creates the field object.
     *
     * @param type  the field type.
     * @param value the field value.
     */
    protected PwsField(PwsFieldType type, Object value)
    {
        super();

        Type = type.getId();
        Value = value;
    }

    /**
     * Converts this field into an array of bytes suitable for writing to
     * a PasswordSafe file.
     *
     * @return The field as a byte array.
     */
    public abstract byte[] getBytes();

    /**
     * Returns the field type given when the object was created.  The
     * field type is specific to the subclass of {@link PwsRecord} that this
     * field belongs to.
     *
     * @return the field type.
     */
    public int getType()
    {
        return Type;
    }

    /**
     * Returns the field value in its native form.
     *
     * @return The field value in its native form.
     */
    public Object getValue()
    {
        return Value;
    }

    /**
     * Returns a hash code for this object.
     *
     * @return the hash code for the object.
     */
    @Override
    public int hashCode()
    {
        return Value.hashCode();
    }

    /**
     * Returns the string value of the field.
     *
     * @return The string value of the field.
     */
    @Override
    public String toString()
    {
        return Value.toString();
    }
}
