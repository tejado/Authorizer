/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.pref;

import net.tjado.passwdsafe.R;

import android.content.res.Resources;

public enum FileBackupPref
{
    // Values in their display order
    BACKUP_NONE   (0,                     "",     0),
    BACKUP_1      (1,                     "1",    1),
    BACKUP_2      (2,                     "2",    2),
    BACKUP_5      (5,                     "5",    3),
    BACKUP_10     (10,                    "10",   4),
    BACKUP_ALL    (Integer.MAX_VALUE,     "all",  5);

    private final int itsNumBackups;
    private final String itsValue;
    private final int itsDisplayNameIdx;

    FileBackupPref(int num, String value, int displayNameIdx)
    {
        itsNumBackups = num;
        itsValue = value;
        itsDisplayNameIdx = displayNameIdx;
    }

    public final int getNumBackups()
    {
        return itsNumBackups;
    }

    public final String getValue()
    {
        return itsValue;
    }

    private int getDisplayNameIdx()
    {
        return itsDisplayNameIdx;
    }

    public final String getDisplayName(Resources res)
    {
        return getDisplayNamesArray(res)[itsDisplayNameIdx];
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

    public static String[] getDisplayNames(Resources res)
    {
        String[] displayNames = getDisplayNamesArray(res);
        FileBackupPref[] prefs = values();
        String[] strs = new String[prefs.length];
        for (int i = 0; i < prefs.length; ++i) {
            strs[i] = displayNames[prefs[i].getDisplayNameIdx()];
        }
        return strs;
    }

    private static String[] getDisplayNamesArray(Resources res)
    {
        return res.getStringArray(R.array.file_backup_pref);
    }
}
