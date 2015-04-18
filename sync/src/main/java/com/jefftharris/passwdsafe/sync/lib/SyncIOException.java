/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import java.io.IOException;

/**
 * The SyncIOException class is an I/O exception with an indication whether a
 * sync should be retried
 */
public class SyncIOException extends IOException
{
    private static final long serialVersionUID = -2450506282560144437L;

    private final boolean itsIsRetry;

    public SyncIOException(String msg, Throwable cause, boolean retry)
    {
        super(msg, cause);
        itsIsRetry = retry;
    }

    /** Get whether to retry a sync */
    public final boolean isRetry()
    {
        return itsIsRetry;
    }
}
