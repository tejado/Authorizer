/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import org.pwsafe.lib.file.PwsRecord;

public class PasswdRecord
{
    public enum Type
    {
        NORMAL, ALIAS, SHORTCUT
    }

    private final PwsRecord itsRecord;
    private final Type itsType;
    private final PwsRecord itsRef;

    public PasswdRecord(PwsRecord rec, PasswdFileData fileData)
    {
        itsRecord = rec;

        PwsRecord ref = null;
        Type type = Type.NORMAL;
        if (fileData.isV3()) {
            String passwd = fileData.getPassword(rec);
            if (passwd != null) {
                if (passwd.startsWith("[[") && passwd.endsWith("]]")) {
                    ref = lookupRef(passwd, fileData);
                    if (ref != null) {
                        type = Type.ALIAS;
                    }
                } else if (passwd.startsWith("[~") && passwd.endsWith("~]")) {
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

    public PwsRecord getRecord()
    {
        return itsRecord;
    }

    public Type getType()
    {
        return itsType;
    }

    public PwsRecord getRef()
    {
        return itsRef;
    }

    private PwsRecord lookupRef(String passwd, PasswdFileData fileData)
    {
        PwsRecord ref = null;

        // TODO: test passords that look like aliases/shortcuts but aren't

        // Passwd in the form of [[<uuid>]] or [~<uuid>~]. Check for a real
        // entry. The <uuid> is a string of hex digits and needs to be converted
        // to the format used by the UUID class
        if (passwd.length() == 36) {
            StringBuilder sb = new StringBuilder(38);
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
            ref = fileData.getRecord(sb.toString());
            for (int i = 0; i < sb.length(); ++i) {
                sb.setCharAt(i, '\0');
            }
        }

        return ref;
    }
}
