/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import java.util.Locale;

import android.database.Cursor;

/**
 *  Entry in the files table
 */
public class DbFile
{
    public final long itsId;
    public final String itsLocalFile;
    public final String itsLocalTitle;
    public final long itsLocalModDate;
    public final boolean itsIsLocalDeleted;
    public final String itsRemoteId;
    public final String itsRemoteTitle;
    public final long itsRemoteModDate;
    public final boolean itsIsRemoteDeleted;

    public static final String[] QUERY_FIELDS = {
        SyncDb.DB_COL_FILES_ID,
        SyncDb.DB_COL_FILES_LOCAL_FILE,
        SyncDb.DB_COL_FILES_LOCAL_TITLE,
        SyncDb.DB_COL_FILES_LOCAL_MOD_DATE,
        SyncDb.DB_COL_FILES_LOCAL_DELETED,
        SyncDb.DB_COL_FILES_REMOTE_ID,
        SyncDb.DB_COL_FILES_REMOTE_TITLE,
        SyncDb.DB_COL_FILES_REMOTE_MOD_DATE,
        SyncDb.DB_COL_FILES_REMOTE_DELETED };

    /** Constructor */
    public DbFile(Cursor cursor)
    {
        itsId = cursor.getLong(0);
        itsLocalFile = cursor.getString(1);
        itsLocalTitle = cursor.getString(2);
        itsLocalModDate = cursor.getLong(3);
        itsIsLocalDeleted = cursor.getInt(4) != 0;
        itsRemoteId = cursor.getString(5);
        itsRemoteTitle = cursor.getString(6);
        itsRemoteModDate = cursor.getLong(7);
        itsIsRemoteDeleted = cursor.getInt(8) != 0;
    }

    @Override
    public String toString()
    {
        return String.format(Locale.US,
                "{id:%d, local:{title:%s, file:%s, mod:%d, del:%b}, " +
                "remote:{id:%s, title:'%s', mod:%d, del:%b}}",
                itsId, itsLocalTitle, itsLocalFile, itsLocalModDate,
                itsIsLocalDeleted, itsRemoteId, itsRemoteTitle,
                itsRemoteModDate, itsIsRemoteDeleted);
    }
}
