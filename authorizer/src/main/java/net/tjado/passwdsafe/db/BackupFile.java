/*
 * Copyright (©) 2023 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.db;

import android.net.Uri;
import android.provider.BaseColumns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import net.tjado.passwdsafe.lib.PasswdSafeUtil;

/**
 * Backup file database entry
 */
@Entity(tableName = BackupFile.TABLE)
public class BackupFile
{
    public static final String TABLE = "backups";
    public static final String COL_ID = BaseColumns._ID;
    public static final String COL_TITLE = "title";
    public static final String COL_FILE_URI = "fileUri";
    public static final String COL_DATE = "date";
    public static final String COL_HAS_FILE = "hasFile";
    public static final String COL_HAS_URI_PERM = "hasUriPerm";

    public static final String URL_SCHEME =
            PasswdSafeUtil.PACKAGE + ".backup";

    /**
     * Unique id for the backup file
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = COL_ID)
    public long id;

    /**
     * Title of the file
     */
    @ColumnInfo(name = COL_TITLE)
    @NonNull
    public final String title;

    /**
     * The URI of the file
     */
    @ColumnInfo(name = COL_FILE_URI)
    @NonNull
    public final String fileUri;

    /**
     * Backup date
     */
    @ColumnInfo(name = COL_DATE)
    public final long date;

    /**
     * Is there a known file for the backup
     */
    @ColumnInfo(name = COL_HAS_FILE, defaultValue = "1")
    public final boolean hasFile;

    /**
     * Is there a known URI permission for the file
     */
    @ColumnInfo(name = COL_HAS_URI_PERM, defaultValue = "1")
    public final boolean hasUriPerm;

    /**
     * Constructor from database entry
     */
    BackupFile(long id,
               @NonNull String title,
               @NonNull String fileUri,
               long date,
               boolean hasFile,
               boolean hasUriPerm)
    {
        this.id = id;
        this.title = title;
        this.fileUri = fileUri;
        this.date = date;
        this.hasFile = hasFile;
        this.hasUriPerm = hasUriPerm;
    }

    /**
     * Constructor for a new backup file
     */
    @Ignore
    public BackupFile(@NonNull Uri fileUri, @NonNull String title)
    {
        this.id = 0;
        this.title = title;
        this.fileUri = fileUri.toString();
        this.date = System.currentTimeMillis();
        this.hasFile = true;
        this.hasUriPerm = true;
    }

    /**
     * Create a Uri for the backup file
     */
    public Uri createUri()
    {
        return Uri.fromParts(URL_SCHEME, Long.toString(id), null);
    }


    @Override
    public boolean equals(@Nullable Object obj)
    {
        if (!(obj instanceof BackupFile)) {
            return false;
        }
        BackupFile backup = (BackupFile)obj;
        return (id == backup.id) && title.equals(backup.title) &&
               fileUri.equals(backup.fileUri) && (date == backup.date) &&
               (hasFile == backup.hasFile) && (hasUriPerm == backup.hasUriPerm);
    }
}
