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
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Recent file database entry
 */
@Entity(tableName = RecentFile.TABLE)
public class RecentFile
{
    public static final String TABLE = "files";
    public static final String COL_ID = BaseColumns._ID;
    public static final String COL_TITLE = "title";
    public static final String COL_URI = "uri";
    public static final String COL_DATE = "date";

    /**
     * Unique id for the record.  The field is a Long for migration as the field
     * is not marked NOT NULL
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = COL_ID)
    public final Long id;

    /**
     * The title of the recent file
     */
    @ColumnInfo(name = COL_TITLE)
    @NonNull
    public final String title;

    /**
     * The file URI
     */
    @ColumnInfo(name = COL_URI)
    @NonNull
    public final String uri;

    /**
     * The last access date
     */
    @ColumnInfo(name = COL_DATE)
    public long date;

    /**
     * Constructor from database entry
     */
    RecentFile(long id, @NonNull String title, @NonNull String uri, long date)
    {
        this.id = id;
        this.title = title;
        this.uri = uri;
        this.date = date;
    }

    /**
     * Constructor for a new recent file
     */
    @Ignore
    public RecentFile(@NonNull Uri uri, @NonNull String title)
    {
        this.id = null;
        this.title = title;
        this.uri = uri.toString();
        this.date = System.currentTimeMillis();
    }
}
