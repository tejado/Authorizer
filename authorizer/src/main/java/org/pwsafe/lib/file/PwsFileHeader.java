/*
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import org.pwsafe.lib.Util;
import org.pwsafe.lib.exception.EndOfFileException;

import java.io.IOException;

/**
 * This class encapsulates the header fields of a PasswordSafe database.  The
 * header comprises:
 * </p><p>
 * <tt>
 * <pre>
 * +--------+-----------+-----------------------------------------------+
 * | Length | Name      | Description                                   |
 * +--------+-----------+-----------------------------------------------+
 * |      8 | RandStuff | Random bytes                                  |
 * |     20 | RandHash  | Random hash                                   |
 * |     20 | Salt      | Salt                                          |
 * |      8 | IpThing   | Initial vector for decryption                 |
 * +--------+-----------+-----------------------------------------------+
 * </pre>
 * </tt>
 * </p>
 *
 * @author Kevin Preece
 */
public class PwsFileHeader
{
    private final byte[] RandStuff = new byte[8];
    private byte[] RandHash = new byte[20];
    private final byte[] Salt = new byte[20];
    private final byte[] IpThing = new byte[8];

    /**
     * Constructs the PasswordSafe file header by reading the header data
     * from <code>file</code>.
     *
     * @param file the file to read the header from.
     * @throws IOException        If an error occurs whilst reading from
     * the file.
     * @throws EndOfFileException If end of file is reached before reading
     * all the data.
     */
    public PwsFileHeader(PwsFile file)
            throws IOException, EndOfFileException
    {
        file.readBytes(RandStuff);
        file.readBytes(RandHash);
        file.readBytes(Salt);
        file.readBytes(IpThing);
    }

    /**
     * Gets a copy of IpThing - the initial vector for encryption/decryption.
     *
     * @return A copy of IpThing
     */
    public byte[] getIpThing()
    {
        return Util.cloneByteArray(IpThing);
    }

    /**
     * Gets a copy of Salt.
     *
     * @return a copy of Salt.
     */
    public byte[] getSalt()
    {
        return Util.cloneByteArray(Salt);
    }

    /**
     * Write the header to the file.
     *
     * @param file the file to write the header to.
     * @throws IOException
     */
    public void save(PwsFile file)
            throws IOException
    {
        Owner<PwsPassword> passwd = file.getPassphrase();
        try {
            update(passwd.pass());
        } finally {
            passwd.close();
        }

        file.writeBytes(RandStuff);
        file.writeBytes(RandHash);
        file.writeBytes(Salt);
        file.writeBytes(IpThing);
    }

    /**
     * Updates the header ready for saving.
     *
     * @param passwd the passphrase to be used to encrypt the database.
     */
    private void update(Owner<PwsPassword>.Param passwd)
    {
        byte temp[];

        Util.newRandBytes(RandStuff);
        temp = Util.cloneByteArray(RandStuff, 10);
        RandHash = PwsFileFactory.genRandHash(passwd, temp);
        Util.newRandBytes(Salt);
        Util.newRandBytes(IpThing);
    }
}
