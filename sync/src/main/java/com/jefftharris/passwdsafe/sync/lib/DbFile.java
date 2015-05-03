/*
 * Copyright (©) 2013-2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import java.util.Locale;

import android.database.Cursor;
import android.text.TextUtils;

/**
 *  Entry in the files table
 */
public class DbFile
{
    /** Type of change on a file that needs to be resolved */
    public enum FileChange
    {
        NO_CHANGE,
        ADDED,
        MODIFIED,
        REMOVED;

        /** Get the FileChange from its string stored in the database */
        public static FileChange fromDbStr(String str)
        {
            if (TextUtils.isEmpty(str)) {
                return NO_CHANGE;
            }
            return valueOf(str);
        }

        /** Get the string form of the FileChange for storage in the database */
        public static String toDbStr(FileChange change)
        {
            if (change == NO_CHANGE) {
                return null;
            }
            return change.name();
        }
    }

    public final long itsId;
    public final String itsLocalFile;
    public final String itsLocalTitle;
    public final long itsLocalModDate;
    public final boolean itsIsLocalDeleted;
    public final String itsLocalFolder;
    public final FileChange itsLocalChange;
    public final String itsRemoteId;
    public final String itsRemoteTitle;
    public final long itsRemoteModDate;
    public final boolean itsIsRemoteDeleted;
    public final String itsRemoteFolder;
    public final FileChange itsRemoteChange;
    public final String itsRemoteHash;

    public static final String[] QUERY_FIELDS = {
        SyncDb.DB_COL_FILES_ID,
        SyncDb.DB_COL_FILES_LOCAL_FILE,
        SyncDb.DB_COL_FILES_LOCAL_TITLE,
        SyncDb.DB_COL_FILES_LOCAL_MOD_DATE,
        SyncDb.DB_COL_FILES_LOCAL_DELETED,
        SyncDb.DB_COL_FILES_LOCAL_FOLDER,
        SyncDb.DB_COL_FILES_LOCAL_CHANGE,
        SyncDb.DB_COL_FILES_REMOTE_ID,
        SyncDb.DB_COL_FILES_REMOTE_TITLE,
        SyncDb.DB_COL_FILES_REMOTE_MOD_DATE,
        SyncDb.DB_COL_FILES_REMOTE_DELETED,
        SyncDb.DB_COL_FILES_REMOTE_FOLDER,
        SyncDb.DB_COL_FILES_REMOTE_CHANGE,
        SyncDb.DB_COL_FILES_REMOTE_HASH };

    /** Constructor */
    public DbFile(Cursor cursor)
    {
        itsId = cursor.getLong(0);
        itsLocalFile = cursor.getString(1);
        itsLocalTitle = cursor.getString(2);
        itsLocalModDate = cursor.getLong(3);
        itsIsLocalDeleted = cursor.getInt(4) != 0;
        itsLocalFolder = cursor.getString(5);
        itsLocalChange = FileChange.fromDbStr(cursor.getString(6));
        itsRemoteId = cursor.getString(7);
        itsRemoteTitle = cursor.getString(8);
        itsRemoteModDate = cursor.getLong(9);
        itsIsRemoteDeleted = cursor.getInt(10) != 0;
        itsRemoteFolder = cursor.getString(11);
        itsRemoteChange = FileChange.fromDbStr(cursor.getString(12));
        itsRemoteHash = cursor.getString(13);
    }

    /** Get the local title and folder */
    public String getLocalTitleAndFolder()
    {
        StringBuilder str = new StringBuilder(itsLocalTitle);
        if (!TextUtils.isEmpty(itsLocalFolder)) {
            str.append(" [").append(itsLocalFolder).append("]");
        }
        return str.toString();
    }

    /** Get the remote title and folder */
    public String getRemoteTitleAndFolder()
    {
        StringBuilder str = new StringBuilder(itsRemoteTitle);
        if (!TextUtils.isEmpty(itsRemoteFolder)) {
            str.append(" [").append(itsRemoteFolder).append("]");
        }
        return str.toString();
    }

    @Override
    public String toString()
    {
        return String.format(Locale.US,
                "{id:%d, " +
                "local:{title:%s, folder:%s, file:%s, " +
                    "mod:%d, del:%b, ch:%s}, " +
                "remote:{id:%s, title:'%s', folder:%s, " +
                    "mod:%d, hash:%s, del:%b, ch:%s}}",
                itsId, itsLocalTitle, itsLocalFolder, itsLocalFile,
                itsLocalModDate, itsIsLocalDeleted, itsLocalChange,
                itsRemoteId, itsRemoteTitle, itsRemoteFolder,
                itsRemoteModDate, itsRemoteHash, itsIsRemoteDeleted,
                itsRemoteChange);
    }
}
