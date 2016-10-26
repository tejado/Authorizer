/*
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import org.pwsafe.lib.exception.EndOfFileException;
import org.pwsafe.lib.exception.UnsupportedFileVersionException;

import java.io.IOException;

/**
 * Encapsulates version 2 PasswordSafe files.
 *
 * @author Kevin Preece
 */
public class PwsFileV2 extends PwsFileV1V2
{
    /**
     * The PasswordSafe database version number that this class supports.
     */
    public static final int VERSION = 2;

    /**
     * The string that identifies a database as V2 rather than V1
     */
    public static final String ID_STRING = " !!!Version 2 File Format!!! " +
                                           "Please upgrade to PasswordSafe 2.0" +
					   " or later";

    /**
     * Use of this constructor to load a PasswordSafe database is STRONGLY
     * discouraged since it's use ties the caller to a particular file version.
     * </p><p>
     * <b>N.B. </b>this constructor's visibility may be reduced in future
     * releases.
     * </p>
     *
     * @param storage the password file storage
     * @param passwd   the passphrase for the database.
     * @param encoding the password encoding
     * @throws EndOfFileException
     * @throws IOException
     * @throws UnsupportedFileVersionException
     */
    public PwsFileV2(PwsStorage storage,
                     Owner<PwsPassword>.Param passwd, String encoding)
            throws EndOfFileException, IOException,
                   UnsupportedFileVersionException
    {
        super(storage, passwd, encoding);
    }

    /**
     * Returns the major version number for the file.
     *
     * @return The major version number for the file.
     */
    @Override
    public int getFileVersionMajor()
    {
        return VERSION;
    }

    /**
     * Allocates a new, empty record unowned by any file.  The record
     * type is {@link PwsRecordV2}.
     *
     * @return A new empty record
     * @see org.pwsafe.lib.file.PwsFile#newRecord()
     */
    @Override
    public PwsRecord newRecord()
    {
        return new PwsRecordV2();
    }

    /**
     * Reads the extra header present in version 2 files.
     *
     * @param file the file to read the header from.
     * @throws EndOfFileException              If end of file is reached.
     * @throws IOException                     If an error occurs whilst
     * reading.
     * @throws UnsupportedFileVersionException If the header is not a
     * valid V2 header.
     */
    @Override
    protected void readExtraHeader(PwsFile file)
            throws EndOfFileException, IOException,
                   UnsupportedFileVersionException
    {
        PwsRecordV1 hdr;

        hdr = new PwsRecordV1();
        hdr.loadRecord(file);

        if (!hdr.getField(PwsRecordV1.TITLE).equals(ID_STRING)) {
            throw new UnsupportedFileVersionException();
        }
    }

    /**
     * Writes the extra version 2 header.
     *
     * @param file the file to write the header to.
     * @throws IOException if an error occurs whilst writing the header.
     */
    @Override
    protected void writeExtraHeader(PwsFile file)
            throws IOException
    {
        PwsRecordV1 hdr;

        hdr = new PwsRecordV1();

        hdr.setField(
                new PwsStringField(PwsRecordV1.TITLE, PwsFileV2.ID_STRING));
        hdr.setField(new PwsPasswdField(PwsRecordV1.PASSWORD, "2.0", this));

        hdr.saveRecord(file);
    }

    /**
     * @see org.pwsafe.lib.file.PwsFile#getBlockSize()
     */
    @Override
    protected int getBlockSize()
    {
        return 8;
    }
}
