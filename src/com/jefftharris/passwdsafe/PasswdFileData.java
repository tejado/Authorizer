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
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;

import org.pwsafe.lib.exception.EndOfFileException;
import org.pwsafe.lib.exception.InvalidPassphraseException;
import org.pwsafe.lib.exception.UnsupportedFileVersionException;
import org.pwsafe.lib.file.PwsField;
import org.pwsafe.lib.file.PwsFile;
import org.pwsafe.lib.file.PwsFileFactory;
import org.pwsafe.lib.file.PwsFileV1;
import org.pwsafe.lib.file.PwsFileV2;
import org.pwsafe.lib.file.PwsFileV3;
import org.pwsafe.lib.file.PwsRecord;
import org.pwsafe.lib.file.PwsRecordV1;
import org.pwsafe.lib.file.PwsRecordV2;
import org.pwsafe.lib.file.PwsRecordV3;
import org.pwsafe.lib.file.PwsStringUnicodeField;

public class PasswdFileData
{
    private static class Record
    {
        public PwsRecord itsRecord;
        // TODO itsIndex needed??
        public int itsIndex;

        public Record(PwsRecord rec, int index)
        {
            itsRecord = rec;
            itsIndex = index;
        }
    }

    public File itsFile;
    public PwsFile itsPwsFile;
    private final HashMap<String, Record> itsRecordsByUUID =
        new HashMap<String, Record>();
    private final ArrayList<PwsRecord> itsRecords = new ArrayList<PwsRecord>();

    private static final String TAG = "PasswdFileData";

    private static final int FIELD_UNSUPPORTED = -1;
    private static final int FIELD_NOT_PRESENT = -2;

    public PasswdFileData(File file, StringBuilder passwd)
        throws Exception
    {
        itsFile= file;
        loadFile(passwd);
    }

    public void save()
        throws IOException, NoSuchAlgorithmException,
               ConcurrentModificationException
    {
        if (itsPwsFile != null) {
            for (int idx = 0; idx < itsRecords.size(); ++idx) {
                PwsRecord rec = itsRecords.get(idx);
                if (rec.isModified()) {
                    PasswdSafeApp.dbginfo(TAG, "Updating idx: " + idx);
                    itsPwsFile.set(idx, rec);
                    rec.resetModified();
                }
            }

            itsPwsFile.save();
        }
    }

    public void close()
    {
        itsFile= null;
        itsPwsFile.dispose();
        itsRecordsByUUID.clear();
        itsRecords.clear();
    }

    public ArrayList<PwsRecord> getRecords()
    {
        return itsRecords;
    }

    public PwsRecord getRecord(String uuid)
    {
        Record rec = itsRecordsByUUID.get(uuid);
        if (rec != null) {
            return rec.itsRecord;
        }
        return null;
    }

    public final File getFile()
    {
        return itsFile;
    }

    public final String getEmail(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.EMAIL);
    }

    public final void setEmail(String str, PwsRecord rec)
    {
        setField(str, rec, PwsRecordV3.EMAIL);
    }

    public final String getGroup(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.GROUP);
    }

    public final void setGroup(String str, PwsRecord rec)
    {
        setField(str, rec, PwsRecordV3.GROUP);
    }

    public final String getNotes(PwsRecord rec)
    {
        String s = getField(rec, PwsRecordV3.NOTES);
        if (s != null) {
            s = s.replace("\r\n", "\n");
        }
        return s;
    }

    public final void setNotes(String str, PwsRecord rec)
    {
        str = str.replace("\n", "\r\n");
        setField(str, rec, PwsRecordV3.NOTES);
    }

    public final String getPassword(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.PASSWORD);
    }

    public final void setPassword(String str, PwsRecord rec)
    {
        setField(str, rec, PwsRecordV3.PASSWORD);
    }

    public final String getPasswdExpiryTime(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.PASSWORD_LIFETIME);
    }

    public final String getTitle(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.TITLE);
    }

    public final void setTitle(String str, PwsRecord rec)
    {
        setField(str, rec, PwsRecordV3.TITLE);
    }

    public final String getUsername(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.USERNAME);
    }

    public final void setUsername(String str, PwsRecord rec)
    {
        setField(str, rec, PwsRecordV3.USERNAME);
    }

    public final String getURL(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.URL);
    }

    public final void setURL(String str, PwsRecord rec)
    {
        setField(str, rec, PwsRecordV3.URL);
    }

    public final String getUUID(PwsRecord rec)
    {
        return getField(rec, PwsRecordV3.UUID);
    }

    private final String getField(PwsRecord rec, int fieldId)
    {
        if (itsPwsFile == null) {
            return "";
        }

        switch (itsPwsFile.getFileVersionMajor())
        {
        case PwsFileV3.VERSION:
        {
            break;
        }
        case PwsFileV2.VERSION:
        {
            switch (fieldId)
            {
            case PwsRecordV3.GROUP:
            {
                fieldId = PwsRecordV2.GROUP;
                break;
            }
            case PwsRecordV3.NOTES:
            {
                fieldId = PwsRecordV2.NOTES;
                break;
            }
            case PwsRecordV3.PASSWORD:
            {
                fieldId = PwsRecordV2.PASSWORD;
                break;
            }
            case PwsRecordV3.PASSWORD_LIFETIME:
            {
                fieldId = PwsRecordV2.PASSWORD_LIFETIME;
                break;
            }
            case PwsRecordV3.TITLE:
            {
                fieldId = PwsRecordV2.TITLE;
                break;
            }
            case PwsRecordV3.USERNAME:
            {
                fieldId = PwsRecordV2.USERNAME;
                break;
            }
            case PwsRecordV3.UUID:
            {
                fieldId = PwsRecordV2.UUID;
                break;
            }
            case PwsRecordV3.EMAIL:
            case PwsRecordV3.URL:
            {
                fieldId = FIELD_NOT_PRESENT;
                break;
            }
            }
            break;
        }
        case PwsFileV1.VERSION:
        {
            switch (fieldId)
            {
            case PwsRecordV3.NOTES:
            {
                fieldId = PwsRecordV1.NOTES;
                break;
            }
            case PwsRecordV3.PASSWORD:
            {
                fieldId = PwsRecordV1.PASSWORD;
                break;
            }
            case PwsRecordV3.TITLE:
            {
                fieldId = PwsRecordV1.TITLE;
                break;
            }
            case PwsRecordV3.USERNAME:
            {
                fieldId = PwsRecordV1.USERNAME;
                break;
            }
            case PwsRecordV3.UUID:
            {
                // No real UUID field for V1, so just use the title which
                // should be unique
                fieldId = PwsRecordV1.TITLE;
                break;
            }
            case PwsRecordV3.EMAIL:
            case PwsRecordV3.GROUP:
            case PwsRecordV3.PASSWORD_LIFETIME:
            case PwsRecordV3.URL:
            {
                fieldId = FIELD_NOT_PRESENT;
                break;
            }
            }
            break;
        }
        default:
        {
            fieldId = FIELD_UNSUPPORTED;
            break;
        }
        }

        switch (fieldId)
        {
        case FIELD_UNSUPPORTED:
        {
            return "(unsupported)";
        }
        case FIELD_NOT_PRESENT:
        {
            return null;
        }
        default:
        {
            PwsField field = rec.getField(fieldId);
            if (field == null) {
                return null;
            }
            return field.toString();
        }
        }
    }

    private final void setField(String str, PwsRecord rec, int fieldId)
    {
        // TODO v1 and v2
        PwsField field = null;
        switch (itsPwsFile.getFileVersionMajor())
        {
        case PwsFileV3.VERSION:
        {
            switch (fieldId)
            {
            case PwsRecordV3.EMAIL:
            case PwsRecordV3.GROUP:
            case PwsRecordV3.NOTES:
            case PwsRecordV3.PASSWORD:
            case PwsRecordV3.TITLE:
            case PwsRecordV3.URL:
            case PwsRecordV3.USERNAME:
            {
                field = new PwsStringUnicodeField(fieldId, str);
                break;
            }
            }
            break;
        }
        default:
        {
            break;
        }
        }

        if (field != null) {
            rec.setField(field);
        }
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
        for (int idx = 0; recIter.hasNext(); ++idx) {
            PwsRecord rec = recIter.next();
            itsRecords.add(rec);

            String uuid = getUUID(rec);
            if (uuid != null) {
                itsRecordsByUUID.put(uuid, new Record(rec, idx));
            }
        }

        PasswdSafeApp.dbginfo(TAG, "file loaded");
    }

}
