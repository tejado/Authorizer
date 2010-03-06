/*
 * Copyright (©) 2009-2010 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;

import org.pwsafe.lib.exception.EndOfFileException;
import org.pwsafe.lib.exception.InvalidPassphraseException;
import org.pwsafe.lib.exception.UnsupportedFileVersionException;
import org.pwsafe.lib.file.PwsField;
import org.pwsafe.lib.file.PwsFile;
import org.pwsafe.lib.file.PwsFileFactory;
import org.pwsafe.lib.file.PwsFileV3;
import org.pwsafe.lib.file.PwsRecord;
import org.pwsafe.lib.file.PwsRecordV3;

public class PasswdFileData
{
    public File itsFile;
    public PwsFile itsPwsFile;
    private final HashMap<String, PwsRecord> itsRecordsByUUID =
        new HashMap<String, PwsRecord>();

    private static final String TAG = "PasswdFileData";

    public PasswdFileData(File file, StringBuilder passwd)
        throws Exception
    {
        itsFile= file;
        loadFile(passwd);
   }

    public void close()
    {
        itsFile= null;
        itsPwsFile.dispose();
        itsRecordsByUUID.clear();
    }

    public HashMap<String, PwsRecord> getRecordsByUUID()
    {
        return itsRecordsByUUID;
    }

    public PwsRecord getRecord(String uuid)
    {
        return itsRecordsByUUID.get(uuid);
    }

    public final String getEmail(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.EMAIL);
    }

    public final String getGroup(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.GROUP);
    }

    public final String getNotes(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.NOTES);
    }

    public final String getPassword(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.PASSWORD);
    }

    public final String getTitle(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.TITLE);
    }

    public final String getUsername(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.USERNAME);
    }

    public final String getURL(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.URL);
    }

    public final String getUUID(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.UUID);
    }

    public final String getPasswdExpiryTime(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.PASSWORD_LIFETIME);
    }

    private final String getField(PwsRecord rec, int fieldId)
    {
        if (itsPwsFile == null) {
            return "";
        }

        if (itsPwsFile.getFileVersionMajor() != PwsFileV3.VERSION) {
            // TODO: convert field id
            fieldId = -1;
        }

        if (fieldId == -1) {
            return "(unsupported)";
        }

        PwsField field = rec.getField(fieldId);
        if (field == null) {
            return null;
        }
        return field.toString();
    }

    private void loadFile(StringBuilder passwd)
        throws IOException, NoSuchAlgorithmException,
            EndOfFileException, InvalidPassphraseException,
            UnsupportedFileVersionException
    {
        PasswdSafeApp.dbginfo(TAG, "before load file");
        itsPwsFile = PwsFileFactory.loadFile(itsFile.getAbsolutePath(), passwd);
        passwd.delete(0, passwd.length());
        passwd = null;
        PasswdSafeApp.dbginfo(TAG, "after load file");

        Iterator<PwsRecord> recIter = itsPwsFile.getRecords();
        while (recIter.hasNext()) {
            PwsRecord rec = recIter.next();
            String uuid = getUUID(rec);
            if (uuid != null) {
                itsRecordsByUUID.put(uuid, rec);
            }
        }

        PasswdSafeApp.dbginfo(TAG, "file loaded");
    }

}
