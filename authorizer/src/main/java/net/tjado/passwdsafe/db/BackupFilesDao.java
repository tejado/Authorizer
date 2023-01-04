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
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import net.tjado.passwdsafe.R;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.Utils;
import net.tjado.passwdsafe.pref.FileBackupPref;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Access to the backup files database
 */
@Dao
public abstract class BackupFilesDao
{
    private static final String BACKUP_FILE_PFX = "backup-";
    private static final String TAG = "BackupFilesDao";

    /**
     * Backup file update fields
     */
    public static class Update
    {
        @ColumnInfo(name = BackupFile.COL_ID)
        public long id;

        @ColumnInfo(name = BackupFile.COL_HAS_FILE)
        public boolean hasFile = true;

        @ColumnInfo(name = BackupFile.COL_HAS_URI_PERM)
        public boolean hasUriPerm = true;
    }

    /**
     * Get backup files
     */
    @Query("SELECT * FROM " + BackupFile.TABLE + " ORDER BY " +
           BackupFile.COL_DATE + " DESC")
    public abstract LiveData<List<BackupFile>> loadBackupFiles();

    /**
     * Get a backup file by its primary key
     */
    @Query("SELECT * FROM " + BackupFile.TABLE +
           " WHERE " + BackupFile.COL_ID + " = :backupFileId")
    public abstract BackupFile getBackupFile(long backupFileId);

    /**
     * Open an input stream for a backup file
     */
    public static InputStream openBackupFile(@NonNull BackupFile file,
                                             @NonNull Context ctx)
            throws FileNotFoundException
    {
        return ctx.openFileInput(getBackupFileName(file.id));
    }

    /**
     * Does a file exist for the backup
     */
    public static boolean hasBackupFile(@NonNull BackupFile file,
                                        @NonNull Context ctx)
    {
        File f = ctx.getFileStreamPath(getBackupFileName(file.id));
        return f.isFile();
    }

    /**
     * Insert a backup file
     */
    public void insert(@NonNull Uri fileUri,
                       @NonNull String title,
                       @NonNull FileBackupPref backupPref,
                       @NonNull Context ctx,
                       @NonNull ContentResolver cr)
    {
        try {
            switch (backupPref) {
            case BACKUP_1:
            case BACKUP_2:
            case BACKUP_5:
            case BACKUP_10:
            case BACKUP_ALL: {
                PasswdSafeUtil.info(TAG, "Backup %s from '%s'", title, fileUri);
                BackupFile backup = doInsert(fileUri, title, ctx, cr);
                pruneBackups(backup.fileUri, backupPref, ctx);
                break;
            }
            case BACKUP_NONE: {
                pruneBackups(fileUri.toString(), backupPref, ctx);
                break;
            }
            }
        } catch (SkipBackupException e) {
            // exception reverts the backup
        } catch (Exception e) {
            Log.e(TAG, "Error inserting backup for: " + fileUri, e);

            new Handler(Looper.getMainLooper()).post(
                    () -> Toast.makeText(ctx, R.string.backup_creation_failed,
                                         Toast.LENGTH_LONG).show());
        }
    }

    /**
     * Update a backup file
     */
    @androidx.room.Update(entity = BackupFile.class)
    public abstract void update(Update update);

    /**
     * Delete all of the backup files
     */
    public void deleteAll(Context ctx)
    {
        for (String fileName : ctx.fileList()) {
            if (fileName.startsWith(BACKUP_FILE_PFX)) {
                ctx.deleteFile(fileName);
            }
        }
        doDeleteAll();
    }

    /**
     * Delete a backup file by its primary key
     */
    public void delete(long backupFileId, Context ctx) {
        ctx.deleteFile(getBackupFileName(backupFileId));
        doDelete(backupFileId);
    }

    /**
     * Get backup files for a URI ordered by date
     */
    @Query("SELECT * FROM " + BackupFile.TABLE +
           " WHERE " + BackupFile.COL_FILE_URI + " = :fileUri ORDER BY " +
           BackupFile.COL_DATE + " DESC")
    protected abstract List<BackupFile> getBackupFilesOrderedByDate(
            String fileUri);

    /**
     * Insert a backup file implementation
     */
    @Transaction
    protected BackupFile doInsert(Uri fileUri,
                                  String title,
                                  Context ctx,
                                  ContentResolver cr) throws RuntimeException
    {
        try {
            BackupFile backup = new BackupFile(fileUri, title);
            backup.id = doInsert(backup);

            String backupFileName = getBackupFileName(backup.id);
            try (InputStream is = Objects.requireNonNull(
                    cr.openInputStream(fileUri));
                 OutputStream os = Objects.requireNonNull(
                         ctx.openFileOutput(backupFileName,
                                            Context.MODE_PRIVATE))) {
                if (Utils.copyStream(is, os) == 0) {
                    // Skip backup on an empty file which is often a new file
                    throw new SkipBackupException();
                }
            } catch (Exception e) {
                ctx.deleteFile(backupFileName);
                if (e instanceof FileNotFoundException) {
                    // Skip backup if a file can't be found, often a new sync
                    // file
                    throw new SkipBackupException();
                }
                throw e;
            }
            return backup;
        } catch (SkipBackupException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error inserting backup", e);
        }
    }

    /**
     * Insert a backup file entry implementation
     */
    @Insert
    protected abstract long doInsert(BackupFile file);

    /**
     * Delete all backup file entries implementation
     */
    @Query("DELETE FROM " + BackupFile.TABLE)
    protected abstract void doDeleteAll();

    /**
     * Delete a backup file entry implementation
     */
    @Query("DELETE FROM " + BackupFile.TABLE +
           " WHERE " + BackupFile.COL_ID + " = :backupFileId")
    protected abstract void doDelete(long backupFileId);

    /**
     * Prune the backups for the file URI
     */
    private void pruneBackups(@NonNull String backupFileUri,
                              @NonNull FileBackupPref backupPref,
                              @NonNull Context ctx)
    {
        switch (backupPref) {
        case BACKUP_NONE:
        case BACKUP_1:
        case BACKUP_2:
        case BACKUP_5:
        case BACKUP_10: {
            break;
        }
        case BACKUP_ALL: {
            return;
        }
        }

        int maxBackups = backupPref.getNumBackups();
        List<BackupFile> backups = getBackupFilesOrderedByDate(backupFileUri);
        int numBackups = 0;
        for (BackupFile pruneBackup: backups) {
            if (numBackups++ >= maxBackups) {
                PasswdSafeUtil.dbginfo(TAG, "Pruning backup %d '%s'",
                                       pruneBackup.id, pruneBackup.title);
                delete(pruneBackup.id, ctx);
            }
        }
    }

    /**
     * Get the name of a backup file
     */
    private static String getBackupFileName(long backupId)
    {
        return String.format(Locale.US, "%s%d", BACKUP_FILE_PFX, backupId);
    }

    /**
     * Exception to skip the backup and undo the file and database updates
     */
    private static class SkipBackupException extends RuntimeException
    {
    }
}
