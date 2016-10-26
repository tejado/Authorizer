/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib;

/**
 * The ObjectHolder class is used to encapsulate a variable passed out of an
 * inner class
 */
public final class ObjectHolder<T>
{
    private T itsObj;

    /**
     * Default constructor
     */
    public ObjectHolder()
    {
        itsObj = null;
    }

    /**
     * Value constructor
     */
    public ObjectHolder(T obj)
    {
        itsObj = obj;
    }

    /**
     * Get the object
     */
    public final T get()
    {
        return itsObj;
    }

    /**
     * Set the object
     */
    public final void set(T obj)
    {
        itsObj = obj;
    }
}
