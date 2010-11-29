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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.pwsafe.lib.UUID;
import org.pwsafe.lib.Util;
import org.pwsafe.lib.exception.EndOfFileException;
import org.pwsafe.lib.exception.InvalidPassphraseException;
import org.pwsafe.lib.exception.PasswordSafeException;
import org.pwsafe.lib.exception.UnsupportedFileVersionException;
import org.pwsafe.lib.file.PwsField;
import org.pwsafe.lib.file.PwsFieldTypeV2;
import org.pwsafe.lib.file.PwsFieldTypeV3;
import org.pwsafe.lib.file.PwsFile;
import org.pwsafe.lib.file.PwsFileFactory;
import org.pwsafe.lib.file.PwsFileStorage;
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
import org.pwsafe.lib.file.PwsUnknownField;

import android.content.Context;
import android.os.Build;
import android.util.Log;

public class PasswdFileData
{
    private File itsFile;
    private PwsFile itsPwsFile;
    private final HashMap<String, PwsRecord> itsRecordsByUUID =
        new HashMap<String, PwsRecord>();
    private final ArrayList<PwsRecord> itsRecords = new ArrayList<PwsRecord>();

    private static final String TAG = "PasswdFileData";

    private static final int FIELD_UNSUPPORTED = -1;
    private static final int FIELD_NOT_PRESENT = -2;

    public PasswdFileData(File file)
    {
        itsFile = file;
    }

    public void load(StringBuilder passwd)
        throws IOException, NoSuchAlgorithmException,
            EndOfFileException, InvalidPassphraseException,
            UnsupportedFileVersionException
    {
        PasswdSafeApp.dbginfo(TAG, "before load file");
        itsPwsFile = PwsFileFactory.loadFile(itsFile.getAbsolutePath(), passwd);
        if (!itsFile.canWrite()) {
            itsPwsFile.setReadOnly(true);
        }
        finishOpenFile(passwd);
    }

    public void createNewFile(StringBuilder passwd, Context context)
        throws IOException, NoSuchAlgorithmException
    {
        itsPwsFile = PwsFileFactory.newFile();
        itsPwsFile.setPassphrase(passwd);
        itsPwsFile.setStorage(new PwsFileStorage(itsFile.getAbsolutePath()));
        setSaveHdrFields(context);
        itsPwsFile.save();
        finishOpenFile(passwd);
    }

    public void save(Context context)
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

            setSaveHdrFields(context);
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

    public final void changePasswd(StringBuilder passwd)
    {
        itsPwsFile.setPassphrase(passwd);
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

    public final String getOpenPasswordEncoding()
    {
        return (itsPwsFile != null) ? itsPwsFile.getOpenPasswordEncoding() :
            null;
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

    public final String getHdrVersion()
    {
        return getHdrField(PwsRecordV3.HEADER_VERSION);
    }

    public final String getHdrLastSaveUser()
    {
        return getHdrField(PwsRecordV3.HEADER_LAST_SAVE_USER);
    }

    public final void setHdrLastSaveUser(String user)
    {
        setHdrField(PwsRecordV3.HEADER_LAST_SAVE_USER, user);
    }

    public final String getHdrLastSaveHost()
    {
        return getHdrField(PwsRecordV3.HEADER_LAST_SAVE_HOST);
    }

    public final void setHdrLastSaveHost(String host)
    {
        setHdrField(PwsRecordV3.HEADER_LAST_SAVE_HOST, host);
    }

    public final String getHdrLastSaveApp()
    {
        return getHdrField(PwsRecordV3.HEADER_LAST_SAVE_WHAT);
    }

    public final void setHdrLastSaveApp(String app)
    {
        setHdrField(PwsRecordV3.HEADER_LAST_SAVE_WHAT, app);
    }

    public final String getHdrLastSaveTime()
    {
        return getHdrField(PwsRecordV3.HEADER_LAST_SAVE_TIME);
    }

    public final void setHdrLastSaveTime(Date date)
    {
        setHdrField(PwsRecordV3.HEADER_LAST_SAVE_TIME, date);
    }

    private final void setSaveHdrFields(Context context)
    {
        setHdrLastSaveApp(PasswdSafeApp.getAppTitle(context) +
                          " " +
                          PasswdSafeApp.getAppVersion(context));
        setHdrLastSaveUser("User");
        setHdrLastSaveHost(Build.MODEL);
        setHdrLastSaveTime(new Date());

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
                // No real UUID field for V1, so just use the phantom one
                fieldId = PwsRecordV1.UUID;
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

        return doGetFieldStr(rec, fieldId);
    }

    private final String getHdrField(int fieldId)
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
        case PwsFileV1.VERSION:
        {
            fieldId = FIELD_NOT_PRESENT;
            break;
        }
        default:
        {
            fieldId = FIELD_UNSUPPORTED;
            break;
        }
        }

        if (isV3()) {
            PwsRecord rec = ((PwsFileV3)itsPwsFile).getHeaderRecord();
            switch (fieldId)
            {
            case PwsRecordV3.HEADER_VERSION:
            {
                return String.format("%d.%02d", 3, getHdrMinorVersion(rec));
            }
            case PwsRecordV3.HEADER_LAST_SAVE_TIME:
            {
                PwsField time = doGetField(rec, fieldId);
                if (time == null) {
                    return null;
                }
                byte[] bytes = time.getBytes();
                if (bytes.length == 8)
                {
                    byte[] binbytes = new byte[4];
                    Util.putIntToByteArray(binbytes,
                                           hexBytesToInt(bytes, bytes.length),
                                           0);
                    bytes = binbytes;
                }
                Date d = new Date(Util.getMillisFromByteArray(bytes, 0));
                return d.toString();
            }
            case PwsRecordV3.HEADER_LAST_SAVE_USER:
            {
                PwsField field = doGetField(rec, fieldId);
                if (field != null) {
                    return field.toString();
                }

                return getHdrLastSaveWhoField(rec, true);
            }
            case PwsRecordV3.HEADER_LAST_SAVE_HOST:
            {
                PwsField field = doGetField(rec, fieldId);
                if (field != null) {
                    return field.toString();
                }

                return getHdrLastSaveWhoField(rec, false);
            }
            case PwsRecordV3.HEADER_LAST_SAVE_WHO:
            case PwsRecordV3.HEADER_LAST_SAVE_WHAT:
            {
                return doGetFieldStr(rec, fieldId);
            }
            default:
            {
                return null;
            }
            }
        } else {
            return null;
        }
    }

    private final void setHdrField(int fieldId, Object value)
    {
        if (itsPwsFile == null) {
            return;
        }

        if (isV3()) {
            PwsRecord rec = ((PwsFileV3)itsPwsFile).getHeaderRecord();
            switch (fieldId)
            {
            case PwsRecordV3.HEADER_LAST_SAVE_TIME:
            {
                long timeVal = ((Date)value).getTime();
                byte[] newbytes;
                int minor = getHdrMinorVersion(rec);
                if (minor >= 2) {
                    newbytes = new byte[4];
                    Util.putMillisToByteArray(newbytes, timeVal, 0);
                } else {
                    int secs = (int) (timeVal / 1000);
                    String str = String.format("%08x", secs);
                    newbytes = str.getBytes();
                }
                rec.setField(new PwsUnknownField(fieldId, newbytes));
                break;
            }
            case PwsRecordV3.HEADER_LAST_SAVE_WHAT:
            {
                rec.setField(
                    new PwsUnknownField(PwsRecordV3.HEADER_LAST_SAVE_WHAT,
                                        value.toString().getBytes()));
                break;
            }
            case PwsRecordV3.HEADER_LAST_SAVE_USER:
            {
                int minor = getHdrMinorVersion(rec);
                if (minor >= 2) {
                    rec.setField(
                        new PwsUnknownField(PwsRecordV3.HEADER_LAST_SAVE_USER,
                                            value.toString().getBytes()));
                } else {
                    setHdrLastSaveWhoField(rec, value.toString(),
                                           getHdrLastSaveWhoField(rec, false));
                }
                break;
            }
            case PwsRecordV3.HEADER_LAST_SAVE_HOST:
            {
                int minor = getHdrMinorVersion(rec);
                if (minor >= 2) {
                    rec.setField(
                        new PwsUnknownField(PwsRecordV3.HEADER_LAST_SAVE_HOST,
                                            value.toString().getBytes()));
                } else {
                    setHdrLastSaveWhoField(rec,
                                           getHdrLastSaveWhoField(rec, true),
                                           value.toString());
                }
                break;
            }
            default:
            {
                break;
            }
            }
        }
    }

    private final String doGetFieldStr(PwsRecord rec, int fieldId)
    {
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

    private static final PwsField doGetField(PwsRecord rec, int fieldId)
    {
        switch (fieldId)
        {
        case FIELD_UNSUPPORTED:
        case FIELD_NOT_PRESENT:
        {
            return null;
        }
        default:
        {
            return rec.getField(fieldId);
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

    private final void finishOpenFile(StringBuilder passwd)
    {
        for (int i = 0; i < passwd.length(); ++i) {
            passwd.setCharAt(i, '\0');
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

    private static final int getHdrMinorVersion(PwsRecord rec)
    {
        PwsField ver = doGetField(rec, PwsRecordV3.HEADER_VERSION);
        if (ver == null) {
            return -1;
        }
        byte[] bytes = ver.getBytes();
        if ((bytes == null) || (bytes.length == 0)) {
            return -1;
        }
        return bytes[0];
    }

    private static final String getHdrLastSaveWhoField(PwsRecord rec,
                                                       boolean isUser)
    {
        PwsField field = doGetField(rec, PwsRecordV3.HEADER_LAST_SAVE_WHO);
        if (field == null) {
            return null;
        }

        byte[] whoBytes = field.getBytes();
        if (whoBytes.length < 4) {
            Log.e(TAG, "Invalid who length: " + whoBytes.length);
            return null;
        }
        int len = hexBytesToInt(whoBytes, 4);

        if ((len + 4) > whoBytes.length) {
            Log.e(TAG, "Invalid user length: " + (len + 4));
            return null;
        }

        if (isUser) {
            return new String(whoBytes, 4, len);
        } else {
            return new String(whoBytes, len + 4, whoBytes.length - len - 4);
        }
    }


    private static final void setHdrLastSaveWhoField(PwsRecord rec,
                                                     String user, String host)
    {
        StringBuilder who = new StringBuilder();
        who.append(String.format("%04x", user.length()));
        who.append(user);
        who.append(host);
        rec.setField(new PwsUnknownField(PwsRecordV3.HEADER_LAST_SAVE_WHO,
                                         who.toString().getBytes()));
    }


    private static final int hexBytesToInt(byte[] bytes, int len)
    {
        int i = 0;
        for (int pos = 0; pos < len; ++pos) {
            i <<= 4;
            i |= Character.digit(bytes[pos], 16);
        }
        return i;
    }
}
