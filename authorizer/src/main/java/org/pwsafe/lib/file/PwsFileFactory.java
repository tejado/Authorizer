/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import org.pwsafe.lib.Log;
import org.pwsafe.lib.Util;
import org.pwsafe.lib.crypto.BlowfishPwsECB;
import org.pwsafe.lib.crypto.SHA1;
import org.pwsafe.lib.exception.EndOfFileException;
import org.pwsafe.lib.exception.InvalidPassphraseException;
import org.pwsafe.lib.exception.PasswordSafeException;
import org.pwsafe.lib.exception.UnsupportedFileVersionException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * This is a singleton factory class used to load a PasswordSafe file.  It is
 * able to determine which version of the file format the file has and
 * returns the correct subclass of {@link PwsFile}
 *
 * @author Kevin Preece
 */
public class PwsFileFactory
{
    private static final Log LOG = Log
            .getInstance(PwsFileFactory.class.getPackage().getName());

    private static final int MAX_HEADER_LEN = PwsFile.STUFF_LENGTH +
                                              PwsFile.HASH_LENGTH;

    /**
     * Private for the singleton pattern.
     */
    private PwsFileFactory()
    {
    }

    /**
     * Verifies that <code>passwdParam</code> is actually the passphrase
     * for the file.  It returns normally if everything is OK or
     * {@link InvalidPassphraseException} if the passphrase is incorrect.
     *
     * @param header      the file header bytes
     * @param passwdParam the file's passphrase.
     * @return the password encoding
     * @throws InvalidPassphraseException If the passphrase is not the
     * correct one for the file.
     */
    private static String checkPassword(byte[] header,
                                        Owner<PwsPassword>.Param passwdParam)
            throws InvalidPassphraseException
    {
        byte[] phash;
        String encoding = null;

        byte[] stuff = Util.getBytes(header, 0, PwsFile.STUFF_LENGTH);
        byte[] fhash = Util.getBytes(header, PwsFile.STUFF_LENGTH,
                                     PwsFile.HASH_LENGTH);
        byte[] fudged = new byte[PwsFile.STUFF_LENGTH + 2];
        System.arraycopy(stuff, 0, fudged, 0, PwsFile.STUFF_LENGTH);

        boolean validPassword = false;
        for (String charset : PwsFile.getPasswordEncodings()) {
            try {
                phash = genRandHash(passwdParam, charset, fudged);
            } catch (UnsupportedEncodingException e) {
                // Skip this charset
                continue;
            }

            if (Util.bytesAreEqual(fhash, phash)) {
                validPassword = true;
                encoding = charset;
                break;
            }
        }

        if (!validPassword) {
            throw new InvalidPassphraseException();
        }

        return encoding;
    }

    static byte[] genRandHash(Owner<PwsPassword>.Param passwdParam,
                              byte[] stuff)
    {
        try {
            Owner<PwsPassword> passwd = passwdParam.use();
            try {
                return genRandHash(passwd.pass(), null, stuff);
            } finally {
                passwd.close();
            }
        } catch (UnsupportedEncodingException e) {
            return new byte[0];
        }
    }

    /**
     * Generates a checksum from the passphrase and some random bytes.
     *
     * @param passwdParam the passphrase.
     * @param charEnc     the passphrase charset encoding
     * @param stuff       the random bytes.
     * @return the generated checksum.
     */
    private static byte[] genRandHash(Owner<PwsPassword>.Param passwdParam,
                                      String charEnc,
                                      byte[] stuff)
            throws UnsupportedEncodingException
    {
        SHA1 md;
        BlowfishPwsECB ecb;
        byte[] digest;
        byte[] tmp;

        Owner<PwsPassword> passwd = passwdParam.use();
        try {
            byte[] pw = passwd.get().getBytes(charEnc);
            md = new SHA1();
            md.update(stuff, 0, stuff.length);
            md.update(pw, 0, pw.length);
            digest = md.getDigest();
        } finally {
            passwd.close();
        }

        try {
            ecb = new BlowfishPwsECB(digest);
            tmp = Util.cloneByteArray(stuff, 8);

            Util.bytesToLittleEndian(tmp);

            for (int ii = 0; ii < 1000; ++ii) {
                ecb.encrypt(tmp);
            }

            Util.bytesToLittleEndian(tmp);
            tmp = Util.cloneByteArray(tmp, 10);

            md.clear();
            md.update(tmp, 0, tmp.length);

        } catch (PasswordSafeException e) {
            LOG.error(e.getMessage()); // This should not happen!
        }
        return md.getDigest();
    }

    /**
     * Loads a Password Safe file.  It returns the appropriate subclass of
     * {@link PwsFile}.
     *
     * @param filename the name of the file to open
     * @param passwd   the passphrase for the file
     * @return The correct subclass of {@link PwsFile} for the file.
     * @throws EndOfFileException
     * @throws FileNotFoundException
     * @throws InvalidPassphraseException
     * @throws IOException
     * @throws UnsupportedFileVersionException
     */
    public static PwsFile loadFile(String filename,
                                   Owner<PwsPassword>.Param passwd)
            throws EndOfFileException, InvalidPassphraseException, IOException,
                   UnsupportedFileVersionException
    {
        PwsStorage storage = new PwsFileStorage(filename, filename);
        return loadFromStorage(storage, passwd);
    }

    /**
     * Loads a Password Safe file.  It returns the appropriate subclass of
     * {@link PwsFile}.
     *
     * @param storage the password storage
     * @param passwd  the passphrase for the file
     * @return The correct subclass of {@link PwsFile} for the file.
     * @throws EndOfFileException
     * @throws InvalidPassphraseException
     * @throws IOException
     * @throws UnsupportedFileVersionException
     */
    public static PwsFile loadFromStorage(PwsStorage storage,
                                          Owner<PwsPassword>.Param passwd)
            throws EndOfFileException, InvalidPassphraseException, IOException,
                   UnsupportedFileVersionException
    {
        PwsFile file;
        try {
            byte[] header = storage.openForLoad(MAX_HEADER_LEN);

            // First check for a v3 file...
            byte[] first4Bytes = Util.getBytes(header, 0, 4);
            if (Util.bytesAreEqual("PWS3".getBytes(), first4Bytes)) {
                file = new PwsFileV3(storage, passwd);
                file.readAll();
                file.close();
                return file;
            }

            PwsRecordV1 rec;
            String encoding = checkPassword(header, passwd);
            file = new PwsFileV1(storage, passwd, encoding);
            rec = (PwsRecordV1)file.readRecord();
            file.close();

            // TODOlib what can we do about this?
            // it will probably be fooled if someone is daft enough to create
            // a V1 file with the
            // title of the first record set to the value of PwsFileV2
		// .ID_STRING!

            if (rec.getField(PwsRecordV1.TITLE).equals(PwsFileV2.ID_STRING)) {
                file = new PwsFileV2(storage, passwd, encoding);
            } else {
                file = new PwsFileV1(storage, passwd, encoding);
            }
            file.readAll();
            file.close();
            return file;
        } finally {
            try {
                storage.closeAfterLoad();
            } catch (IOException ioe) {
                LOG.error("Error closing file " + storage.getIdentifier(),
                          ioe);
            }
        }
    }

    /**
     * Creates a new, empty PasswordSafe database in memory.  The database will
     * always be the latest version supported by this library which for this
     * release is version 3.
     *
     * @return A new empty PasswordSafe database.
     */
    public static PwsFile newFile()
    {
        return new PwsFileV3();
    }

}
