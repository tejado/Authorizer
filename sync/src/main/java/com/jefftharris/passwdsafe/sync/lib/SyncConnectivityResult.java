/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

/**
 * Contains the result of a sync connectivity check
 */
public class SyncConnectivityResult
{
    private final String itsDisplayName;

    /**
     * Constructor
     */
    public SyncConnectivityResult(String displayName)
    {
        itsDisplayName = displayName;
    }

    /**
     * Get the display name
     */
    public String getDisplayName()
    {
        return itsDisplayName;
    }
}
