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

import org.pwsafe.lib.UUID;
import org.pwsafe.lib.exception.EndOfFileException;
import org.pwsafe.lib.exception.InvalidPassphraseException;
import org.pwsafe.lib.exception.PasswordSafeException;
import org.pwsafe.lib.exception.UnsupportedFileVersionException;
import org.pwsafe.lib.file.PwsField;
import org.pwsafe.lib.file.PwsFieldTypeV2;
import org.pwsafe.lib.file.PwsFieldTypeV3;
import org.pwsafe.lib.file.PwsFile;
import org.pwsafe.lib.file.PwsFileFactory;
import org.pwsafe.lib.file.PwsFileV1;
import org.pwsafe.lib.file.PwsFileV2;
import org.pwsafe.lib.file.PwsFileV3;
import org.pwsafe.lib.file.PwsRecord;
import org.pwsafe.lib.file.PwsRecordV1;
import org.pwsafe.lib.file.PwsRecordV2;
import org.pwsafe.lib.file.PwsRecordV3;
import org.pwsafe.lib.file.PwsStringField;
import org.pwsafe.lib.file.PwsStringUnicodeField;
import org.pwsafe.lib.file.PwsUUIDField;

public class PasswdFileData
{
    public File itsFile;
    public PwsFile itsPwsFile;
    private final HashMap<String, PwsRecord> itsRecordsByUUID =
        new HashMap<String, PwsRecord>();
    private final ArrayList<PwsRecord> itsRecords = new ArrayList<PwsRecord>();

    private static final String TAG = "PasswdFileData";

    private static final int FIELD_UNSUPPORTED = -1;
    private static final int FIELD_NOT_PRESENT = -2;

    public PasswdFileData(File file, StringBuilder passwd)
        throws Exception
    {
        itsFile = file;
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
        itsFile = null;
        itsPwsFile.dispose();
        itsPwsFile = null;
        itsRecordsByUUID.clear();
        itsRecords.clear();
    }

    public ArrayList<PwsRecord> getRecords()
    {
        return itsRecords;
    }

    public PwsRecord getRecord(String uuid)
    {
        return itsRecordsByUUID.get(uuid);
    }

    public PwsRecord createRecord()
    {
        if (itsPwsFile != null) {
            return itsPwsFile.newRecord();
        } else {
            return null;
        }
    }

    public final void addRecord(PwsRecord rec)
        throws PasswordSafeException
    {
        if (itsPwsFile != null) {
            itsPwsFile.add(rec);
            indexRecords();
        }
    }

    public final boolean removeRecord(PwsRecord rec)
    {
        if (itsPwsFile != null) {
            String recuuid = getUUID(rec);
            if (recuuid == null) {
                return false;
            }

            for (int i = 0; i < itsRecords.size(); ++i) {
                PwsRecord r = itsRecords.get(i);
                String ruuid = getUUID(r);
                if (recuuid.equals(ruuid)) {
                    boolean rc = itsPwsFile.removeRecord(i);
                    if (rc) {
                        indexRecords();
                    }
                    return rc;
                }
            }
        }
        return false;
    }

    public final File getFile()
    {
        return itsFile;
    }

    public final boolean canEdit()
    {
        return (itsPwsFile != null) &&
               !itsPwsFile.isReadOnly() &&
               ((itsPwsFile.getFileVersionMajor() == PwsFileV3.VERSION) ||
                (itsPwsFile.getFileVersionMajor() == PwsFileV2.VERSION));
    }

    public final boolean isV3()
    {
        return (itsPwsFile != null) &&
            (itsPwsFile.getFileVersionMajor() == PwsFileV3.VERSION);
    }

    public final boolean isV2()
    {
        return (itsPwsFile != null) &&
            (itsPwsFile.getFileVersionMajor() == PwsFileV2.VERSION);
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
                if ((str != null) && (str.length() != 0)) {
                    field = new PwsStringUnicodeField(fieldId, str);
                }
                break;
            }
            default:
            {
                fieldId = FIELD_UNSUPPORTED;
                break;
            }
            }
            break;
        }
        case PwsFileV2.VERSION:
        {
            switch (fieldId)
            {
            case PwsRecordV3.GROUP:
            case PwsRecordV3.NOTES:
            case PwsRecordV3.PASSWORD:
            case PwsRecordV3.TITLE:
            case PwsRecordV3.USERNAME:
            {
                if ((str != null) && (str.length() != 0)) {
                    field = new PwsStringField(fieldId, str);
                }
                break;
            }
            default:
            {
                fieldId = FIELD_UNSUPPORTED;
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

        if (fieldId != FIELD_UNSUPPORTED) {
            if (field != null) {
                rec.setField(field);
            } else {
                rec.removeField(fieldId);
            }
        }
    }

    private void loadFile(StringBuilder passwd)
        throws IOException, NoSuchAlgorithmException,
            EndOfFileException, InvalidPassphraseException,
            UnsupportedFileVersionException
    {
        PasswdSafeApp.dbginfo(TAG, "before load file");
        itsPwsFile = PwsFileFactory.loadFile(itsFile.getAbsolutePath(), passwd);
        if (!itsFile.canWrite()) {
            itsPwsFile.setReadOnly(true);
        }
        passwd.delete(0, passwd.length());
        passwd = null;
        PasswdSafeApp.dbginfo(TAG, "after load file");
        indexRecords();
        PasswdSafeApp.dbginfo(TAG, "file loaded");
    }

    private final void indexRecords()
    {
        itsRecords.clear();
        itsRecordsByUUID.clear();
        Iterator<PwsRecord> recIter = itsPwsFile.getRecords();
        for (int idx = 0; recIter.hasNext(); ++idx) {
            PwsRecord rec = recIter.next();
            String uuid = getUUID(rec);
            if (uuid == null) {
                // Add a UUID field for records without one.  The record will
                // not be marked as modified unless the user manually edits it.
                PwsUUIDField uuidField =
                    new PwsUUIDField(isV2() ?
                                     PwsFieldTypeV2.UUID : PwsFieldTypeV3.UUID,
                                     new UUID());
                boolean modified = rec.isModified();
                rec.setField(uuidField);
                if (!modified) {
                    rec.resetModified();
                }
                uuid = uuidField.toString();
            }

            itsRecords.add(rec);
            itsRecordsByUUID.put(uuid, rec);
        }
    }
}
