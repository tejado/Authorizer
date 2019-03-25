/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.file;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.pwsafe.lib.file.PwsRecord;

import androidx.annotation.NonNull;
import android.text.TextUtils;


public class PasswdRecord
{
    public enum Type
    {
        NORMAL, ALIAS, SHORTCUT
    }

    private static final String ALIAS_OPEN = "[[";
    private static final String ALIAS_CLOSE = "]]";
    private static final String SHORTCUT_OPEN = "[~";
    private static final String SHORTCUT_CLOSE = "~]";

    private final PwsRecord itsRecord;
    private final String itsUUID;
    private Type itsType;
    private PwsRecord itsRef;
    private final ArrayList<PwsRecord> itsRefsToRecord = new ArrayList<>();
    private PasswdPolicy itsPasswdPolicy;
    private PasswdExpiration itsPasswdExpiry;

    public PasswdRecord(PwsRecord rec, PasswdFileData fileData)
    {
        itsRecord = rec;
        itsUUID = fileData.getUUID(rec);
        passwordChanged(fileData);
        passwdPolicyChanged(fileData);
        passwdExpiryChanged(fileData);
    }

    public PwsRecord getRecord()
    {
        return itsRecord;
    }

    /** Get the unique identifier of the record */
    public String getUUID()
    {
        return itsUUID;
    }

    public Type getType()
    {
        return itsType;
    }

    public PwsRecord getRef()
    {
        return itsRef;
    }

    public void addRefToRecord(PwsRecord ref)
    {
        itsRefsToRecord.add(ref);
    }

    public void removeRefToRecord(PwsRecord ref)
    {
        itsRefsToRecord.remove(ref);
    }

    public List<PwsRecord> getRefsToRecord()
    {
        return itsRefsToRecord;
    }

    public void passwordChanged(PasswdFileData fileData)
    {
        PwsRecord ref = null;
        Type type = Type.NORMAL;
        if (fileData.isV3()) {
            String passwd = fileData.getPassword(itsRecord);
            if (passwd != null) {
                if (passwd.startsWith(ALIAS_OPEN) &&
                    passwd.endsWith(ALIAS_CLOSE)) {
                    ref = lookupRef(passwd, fileData);
                    if (ref != null) {
                        type = Type.ALIAS;
                    }
                } else if (passwd.startsWith(SHORTCUT_OPEN) &&
                           passwd.endsWith(SHORTCUT_CLOSE)) {
                    ref = lookupRef(passwd, fileData);
                    if (ref != null) {
                        type = Type.SHORTCUT;
                    }
                }
            }
        }
        itsType = type;
        itsRef = ref;
    }

    /**
     * Get the password for the record
     */
    public String getPassword(@NonNull PasswdFileData fileData)
    {
        switch (itsType) {
        case NORMAL: {
            return fileData.getPassword(itsRecord);
        }
        case ALIAS:
        case SHORTCUT: {
            return fileData.getPassword(itsRef);
        }
        }
        return null;
    }

    /** Notification that the password policy has changed */
    public void passwdPolicyChanged(PasswdFileData fileData)
    {
        itsPasswdPolicy = fileData.getPasswdPolicy(itsRecord);
    }

    /** Get the record's password policy */
    public PasswdPolicy getPasswdPolicy()
    {
        return itsPasswdPolicy;
    }

    /** Notification that the password expiration has changed */
    public void passwdExpiryChanged(PasswdFileData fileData)
    {
        itsPasswdExpiry = fileData.getPasswdExpiry(itsRecord);
    }

    /** Get the record's password expiration */
    public PasswdExpiration getPasswdExpiry()
    {
        return itsPasswdExpiry;
    }

    /** Get an identifier for a record from its naming fields */
    public static String getRecordId(String group, String title,
                                     String username)
    {
        StringBuilder id = new StringBuilder();

        if (!TextUtils.isEmpty(group)) {
            id.append(group.replace(".", " / "));
            id.append(" / ");
        }
        id.append(title);
        if (!TextUtils.isEmpty(username)) {
            id.append(" [");
            id.append(username);
            id.append("]");
        }
        return id.toString();
    }

    public static String uuidToPasswd(String uuid, Type type)
    {
        StringBuilder sb = new StringBuilder(36);

        switch (type) {
        case NORMAL: {
            break;
        }
        case ALIAS: {
            sb.append(ALIAS_OPEN);
            break;
        }
        case SHORTCUT: {
            sb.append(SHORTCUT_OPEN);
            break;
        }
        }

        switch (type) {
        case NORMAL: {
            sb.append(uuid);
            break;
        }
        case ALIAS:
        case SHORTCUT: {
            if (uuid.length() == 38) {
                sb.append(uuid, 1, 1 + 8);
                sb.append(uuid, 10, 10 + 4);
                sb.append(uuid, 15, 15 + 4);
                sb.append(uuid, 20, 20 + 4);
                sb.append(uuid, 25, 25 + 12);
            }
            break;
        }
        }

        switch (type) {
        case NORMAL: {
            break;
        }
        case ALIAS: {
            sb.append(ALIAS_CLOSE);
            break;
        }
        case SHORTCUT: {
            sb.append(SHORTCUT_CLOSE);
            break;
        }
        }

        return sb.toString();
    }

    private PwsRecord lookupRef(String passwd, PasswdFileData fileData)
    {
        PwsRecord ref = null;

        // Passwd in the form of [[<uuid>]] or [~<uuid>~]. Check for a real
        // entry. The <uuid> is a string of hex digits and needs to be converted
        // to the format used by the UUID class
        if (passwd.length() == 36) {
            StringBuilder sb = new StringBuilder(36 - 4 + 6);
            sb.append('{');
            sb.append(passwd, 2, 2 + 8);
            sb.append('-');
            sb.append(passwd, 10, 10 + 4);
            sb.append('-');
            sb.append(passwd, 14, 14 + 4);
            sb.append('-');
            sb.append(passwd, 18, 18 + 4);
            sb.append('-');
            sb.append(passwd, 22, 22 + 12);
            sb.append('}');
            ref = fileData.getRecord(sb.toString().toLowerCase(Locale.US));
            for (int i = 0; i < sb.length(); ++i) {
                sb.setCharAt(i, '\0');
            }
        }

        return ref;
    }
}
