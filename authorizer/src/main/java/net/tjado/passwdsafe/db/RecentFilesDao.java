/*
 * Copyright (©) 2023 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.db;


import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import net.tjado.passwdsafe.lib.ApiCompat;

import java.util.List;

/**
 * Access to the recent files database
 */
@Dao
public abstract class RecentFilesDao
{
    private static final int NUM_RECENT_FILES = 10;

    /**
     * Get a cursor of the recent files ordered by date
     */
    @Query("SELECT * FROM " + RecentFile.TABLE +
           " ORDER BY " + RecentFile.COL_DATE + " DESC")
    public abstract Cursor getOrderedByDateCursor();

    /**
     * Insert or update a recent file
     */
    @Transaction
    public void insertOrUpdate(Uri uri, String title)
    {
        String uristr = uri.toString();
        RecentFile currFile = getByUri(uristr);
        if (currFile == null) {
            currFile = new RecentFile(uri, title);
            insert(currFile);
        } else {
            currFile.date = System.currentTimeMillis();
            update(currFile);
        }

        List<RecentFile> files = getOrderedByDate();
        if (files.size() > NUM_RECENT_FILES) {
            for (RecentFile file :
                    files.subList(NUM_RECENT_FILES, files.size())) {
                delete(file);
            }
        }
    }

    /**
     * Update the timestamp for a file
     */
    public void touchFile(Uri uri)
    {
        updateTimestamp(uri.toString(), System.currentTimeMillis());
    }

    /**
     * Remove a recent file
     */
    @Query("DELETE FROM " + RecentFile.TABLE + " WHERE " +
           RecentFile.COL_URI + " = :uri")
    public abstract void removeUri(String uri);

    /**
     * Delete all recent files
     */
    @Query("DELETE FROM " + RecentFile.TABLE)
    public abstract void deleteAll();

    /** Update an opened storage access file */
    public static void updateOpenedSafFile(Uri uri, int flags, Context ctx)
    {
        ContentResolver cr = ctx.getContentResolver();
        ApiCompat.takePersistableUriPermission(cr, uri, flags);
    }

    /** Get the display name of a storage access file */
    public static String getSafDisplayName(Uri uri, Context ctx)
    {
        ContentResolver cr = ctx.getContentResolver();
        Cursor cursor = cr.query(uri, null, null, null, null);
        try {
            if ((cursor != null) && (cursor.moveToFirst())) {
                return cursor.getString(cursor.getColumnIndexOrThrow(
                        OpenableColumns.DISPLAY_NAME));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * Query all recent files ordered by date
     */
    @Query("SELECT * FROM " + RecentFile.TABLE +
           " ORDER BY " + RecentFile.COL_DATE + " DESC")
    protected abstract List<RecentFile> getOrderedByDate();

    /**
     * Query a recent file by URI
     */
    @Query("SELECT * FROM " + RecentFile.TABLE +
           " WHERE " + RecentFile.COL_URI + " = :uri")
    protected abstract RecentFile getByUri(String uri);

    /**
     * Insert a recent file
     */
    @Insert
    protected abstract void insert(RecentFile file);

    /**
     * Update a recent file
     */
    @Update
    protected abstract void update(RecentFile file);

    /**
     * Update the timestamp on a file
     */
    @Query("UPDATE " + RecentFile.TABLE +
           " SET " + RecentFile.COL_DATE + " = :date" +
           " WHERE " + RecentFile.COL_URI + " = :uri")
    protected abstract void updateTimestamp(String uri, long date);

    /**
     * Delete a recent file
     */
    @Delete
    protected abstract void delete(RecentFile file);
}
