/*
 * Copyright (©) 2022 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.exception;

import androidx.annotation.NonNull;

import org.pwsafe.lib.file.PwsRecord;

import java.util.List;

/**
 * An exception from loading a record
 */
public class RecordLoadException extends Exception
{
    private static final long serialVersionUID = 1L;

    public final @NonNull PwsRecord itsRecord;
    public final @NonNull List<Throwable> itsErrors;

    public RecordLoadException(@NonNull PwsRecord record,
                               @NonNull List<Throwable> errors)
    {
        super("Error reading record: " + record);
        itsRecord = record;
        itsErrors = errors;
    }
}
