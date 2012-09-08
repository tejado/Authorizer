/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.util;

/**
 * Generic pair class
 */
public class Pair<T, U>
{
    public T first;
    public U second;

    /** Constructor */
    public Pair(T t, U u)
    {
        first = t;
        second = u;
    }

    /** Convert the object to a string */
    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        str.append("[[").append(first).append("], [");
        str.append(second).append("]]");
        return str.toString();
    }
}
