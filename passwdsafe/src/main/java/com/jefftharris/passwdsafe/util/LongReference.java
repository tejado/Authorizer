/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.util;

/**
 * The LongReference class holds a long that can be modified as a method
 * parameter
 */
public class LongReference
{
    public long itsValue;

    /** Constructor */
    public LongReference(long l)
    {
        itsValue = l;
    }
}
