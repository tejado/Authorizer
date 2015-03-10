/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.lib;

/**
 *  The DocumentsContractCompat class provides a compatibility interface for
 *  DocumentsContract
 */
public interface DocumentsContractCompat
{
    /** Intent action for opening a document available on API 19 */
    public static final String INTENT_ACTION_OPEN_DOCUEMENT =
            "android.intent.action.OPEN_DOCUMENT";

    /** Column for DocumentsContract.Document.COLUMN_FLAGS available on
     * API 19 */
    public static final String COLUMN_FLAGS = "flags";

    /** Bit field in flags for whether a document is writable available on
     * API 19 */
    public static final int FLAG_SUPPORTS_WRITE = 0x02;
}
