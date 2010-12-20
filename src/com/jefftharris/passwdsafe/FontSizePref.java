/*
 * Copyright (©) 2009-2010 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

public enum FontSizePref
{
    NORMAL ("Normal"),
    SMALL ("Small");

    private String itsDisplayName;

    private FontSizePref(String displayName)
    {
        itsDisplayName = displayName;
    }

    public final String getDisplayName()
    {
        return itsDisplayName;
    }
}