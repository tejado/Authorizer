/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * An read-only implementation of PwsStorage that reads from an input stream
 */
public class PwsStreamStorage implements PwsStorage
{
    private SaveHelper itsSaveHelper;
    private BufferedInputStream itsLoadStream;
    private byte[] itsLoadBytes;
    private final String itsIdentifier;

    public PwsStreamStorage(String identifier, InputStream stream)
    {
        itsIdentifier = identifier;
        itsLoadStream = new BufferedInputStream(stream);
    }

    public byte[] openForLoad(int headerLen) throws IOException
    {
        itsLoadStream.mark(headerLen);
        byte[] bytes = new byte[headerLen];
        int offset = 0;
        int numread;
        while (offset < bytes.length) {
            numread = itsLoadStream.read(bytes, offset,
                                         bytes.length - offset);
            if (numread < 0) {
                throw new IOException("Error reading header from " +
                                      itsIdentifier);
            }
            offset += numread;
        }
        itsLoadStream.reset();
        return bytes;
    }

    public byte[] load() throws IOException
    {
        if (itsLoadBytes == null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int numread;

            for (;;) {
                numread = itsLoadStream.read(buf);
                if (numread > 0) {
                    bos.write(buf, 0, numread);
                } else if (numread < 0) {
                    break;
                }
            }
            closeAfterLoad();
            itsLoadBytes = bos.toByteArray();
        }
        return itsLoadBytes;
    }

    public void closeAfterLoad() throws IOException
    {
        itsLoadBytes = null;
        if (itsLoadStream != null) {
            try {
                itsLoadStream.close();
            } finally {
                itsLoadStream = null;
            }
        }
    }

    public boolean save(byte[] data, boolean isV3)
    {
        // Can't save
        return false;
    }

    public String getIdentifier()
    {
        return itsIdentifier;
    }

    public Date getModifiedDate()
    {
        return null;
    }

    public void setSaveHelper(SaveHelper helper)
    {
        itsSaveHelper = helper;
    }

    protected SaveHelper getSaveHelper()
    {
        return itsSaveHelper;
    }
}
