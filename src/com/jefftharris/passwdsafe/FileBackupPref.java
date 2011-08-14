/*
 * Copyright (©) 2009-2011 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

public enum FileBackupPref
{
    // Values in their display order
    BACKUP_NONE   (0,                     "",     "None"),
    BACKUP_1      (1,                     "1",    "1 backup"),
    BACKUP_2      (2,                     "2",    "2 backups"),
    BACKUP_5      (5,                     "5",    "5 backups"),
    BACKUP_10     (10,                    "10",   "10 backups"),
    BACKUP_ALL    (Integer.MAX_VALUE,     "all",  "All");

    private final int itsNumBackups;
    private final String itsValue;
    private final String itsDisplayName;

    private FileBackupPref(int num, String value, String displayName)
    {
        itsNumBackups = num;
        itsValue = value;
        itsDisplayName = displayName;
    }

    public final int getNumBackups()
    {
        return itsNumBackups;
    }

    public final String getValue()
    {
        return itsValue;
    }

    public final String getDisplayName()
    {
        return itsDisplayName;
    }

    public static FileBackupPref prefValueOf(String str)
    {
        for (FileBackupPref pref : FileBackupPref.values()) {
            if (pref.getValue().equals(str)) {
                return pref;
            }
        }
        throw new IllegalArgumentException(str);
    }

    public static String[] getValues()
    {
        FileBackupPref[] prefs = values();
        String[] strs = new String[prefs.length];
        for (int i = 0; i < prefs.length; ++i) {
            strs[i] = prefs[i].getValue();
        }
        return strs;
    }

    public static String[] getDisplayNames()
    {
        FileBackupPref[] prefs = values();
        String[] strs = new String[prefs.length];
        for (int i = 0; i < prefs.length; ++i) {
            strs[i] = prefs[i].getDisplayName();
        }
        return strs;
    }
}
