/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib;

/**
 *  The DocumentsContractCompat class provides a compatibility interface for
 *  DocumentsContract
 */
public interface DocumentsContractCompat
{
    /** Intent action for opening a document available on API 19 */
    String INTENT_ACTION_OPEN_DOCUMENT = "android.intent.action.OPEN_DOCUMENT";

    String INTENT_ACTION_CREATE_DOCUMENT =
            "android.intent.action.CREATE_DOCUMENT";

    /** Initial URI for the documents chooser UI */
    String EXTRA_INITIAL_URI = "android.provider.extra.INITIAL_URI";

    /** Prompt in the documents chooser UI */
    String EXTRA_PROMPT = "android.provider.extra.PROMPT";

    /** Show advanced mode for the documents chooser UI.  The internal storage
     * should be visible. */
    String EXTRA_SHOW_ADVANCED = "android.provider.extra.SHOW_ADVANCED";

    /** Column for DocumentsContract.Document.COLUMN_DOCUMENT_ID available on
     * API 19 */
    String COLUMN_DOCUMENT_ID = "document_id";

    /** Column for DocumentsContract.Document.COLUMN_FLAGS available on
     * API 19 */
    String COLUMN_FLAGS = "flags";

    /** Bit field in flags for whether a document is writable available on
     * API 19 */
    int FLAG_SUPPORTS_WRITE = 0x02;

    /** Bit field in flags for whether a document is deletable available on
     * API 19 */
    int FLAG_SUPPORTS_DELETE = 0x04;
}
