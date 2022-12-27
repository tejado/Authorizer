/*
 * Copyright (©) 2022 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.util;

import org.pwsafe.lib.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * The ClearingByteArrayOutputStream class is a {@link ByteArrayOutputStream}
 * which clears its buffer when closed
 */
public final class ClearingByteArrayOutputStream extends ByteArrayOutputStream
{
    /**
     * Constructor
     */
    public ClearingByteArrayOutputStream()
    {
    }

    @Override
    public void close() throws IOException
    {
        super.close();
        Util.clearArray(buf);
        Runtime.getRuntime().gc();
    }
}
