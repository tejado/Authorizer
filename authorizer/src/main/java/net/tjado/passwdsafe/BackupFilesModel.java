/*
 * Copyright (©) 2023 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import net.tjado.passwdsafe.db.BackupFile;
import net.tjado.passwdsafe.db.BackupFilesDao;
import net.tjado.passwdsafe.db.PasswdSafeDb;
import net.tjado.passwdsafe.file.PasswdFileUri;
import net.tjado.passwdsafe.lib.ApiCompat;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * View model for backup files
 */
public class BackupFilesModel extends AndroidViewModel
{
    private final BackupFilesDao itsBackupFilesDao;
    private final LiveData<List<BackupFile>> itsBackupFiles;
    private final Observer<List<BackupFile>> itsVerifyObserver;

    private static final String TAG = "BackupFilesModel";

    /**
     * Constructor
     */
    public BackupFilesModel(Application app)
    {
        super(app);
        itsBackupFilesDao = PasswdSafeDb.get(app).accessBackupFiles();
        itsBackupFiles = itsBackupFilesDao.loadBackupFiles();
        itsVerifyObserver = this::verify;
        itsBackupFiles.observeForever(itsVerifyObserver);
    }

    /**
     * Get the backup files
     */
    public LiveData<List<BackupFile>> getBackupFiles()
    {
        return itsBackupFiles;
    }

    /**
     * Delete all of the backup files
     */
    public void deleteAll()
    {
        itsBackupFilesDao.deleteAll(getApplication());
    }

    /**
     * Delete a backup file
     */
    public void delete(long backupFileId)
    {
        itsBackupFilesDao.delete(backupFileId, getApplication());
    }

    @Override
    protected void onCleared()
    {
        PasswdSafeUtil.dbginfo(TAG, "Cleared");
        itsBackupFiles.removeObserver(itsVerifyObserver);
    }

    /**
     * Verify a list of backup files in the background
     */
    @UiThread
    private void verify(List<BackupFile> backupFiles)
    {
        Application app = getApplication();
        PasswdSafeApp.scheduleTask(new VerifyRunnable(backupFiles, app), app);
    }

    /**
     * Runnable to verify backup files
     */
    private static class VerifyRunnable implements Runnable
    {
        private final List<BackupFile> itsBackupFiles;
        private final Application itsApp;

        /**
         * Constructor
         */
        @UiThread
        protected VerifyRunnable(List<BackupFile> backupFiles, Application app)
        {
            itsBackupFiles = backupFiles;
            itsApp = app;
        }

        @Override
        @WorkerThread
        public void run()
        {
            PasswdSafeUtil.dbginfo(TAG, "Verify");
            if (itsBackupFiles.isEmpty()) {
                return;
            }

            ContentResolver cr = itsApp.getContentResolver();
            BackupFilesDao backupFilesDao =
                    PasswdSafeDb.get(itsApp).accessBackupFiles();
            Map<String, Boolean> checkedUris = new HashMap<>();
            for (BackupFile backup : itsBackupFiles) {
                boolean uriok;
                Boolean checkedVal = checkedUris.get(backup.fileUri);
                if (checkedVal != null) {
                    uriok = checkedVal;
                } else {
                    uriok = checkUriPerm(Uri.parse(backup.fileUri), cr);
                    checkedUris.put(backup.fileUri, uriok);
                }

                boolean fileok = BackupFilesDao.hasBackupFile(backup, itsApp);

                PasswdSafeUtil.dbginfo(TAG, "Verify %d, %s: uri %b file %b",
                                       backup.id, backup.title, uriok, fileok);
                if ((uriok != backup.hasUriPerm) ||
                    (fileok != backup.hasFile)) {
                    BackupFilesDao.Update update = new BackupFilesDao.Update();
                    update.id = backup.id;
                    update.hasUriPerm = uriok;
                    update.hasFile = fileok;
                    backupFilesDao.update(update);
                }
            }
        }

        /**
         * Check permissions on a URI
         */
        @WorkerThread
        private static boolean checkUriPerm(Uri uri, ContentResolver cr)
        {
            switch (PasswdFileUri.getUriType(uri)) {
            case FILE: {
                return true;
            }
            case SYNC_PROVIDER:
            case GENERIC_PROVIDER: {
                return checkProviderUriPerm(uri, cr);
            }
            case EMAIL:
            case BACKUP: {
                break;
            }
            }
            return false;
        }

        /**
         * Check permissions on a provider URI
         */
        @WorkerThread
        private static boolean checkProviderUriPerm(Uri uri, ContentResolver cr)
        {
            PasswdSafeUtil.dbginfo(TAG, "Checking persist perm %s", uri);

            try (Cursor cursor = cr.query(uri, null, null, null, null)) {
                int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

                if ((cursor != null) && (cursor.moveToFirst())) {
                    ApiCompat.takePersistableUriPermission(cr, uri, flags);
                    return true;
                } else {
                    ApiCompat.releasePersistableUriPermission(cr, uri, flags);
                }
            } catch (SecurityException e) {
                PasswdSafeUtil.dbginfo(TAG, e, "URI security error for %s",
                                       uri);
            } catch (Exception e) {
                Log.e(TAG, "Permission remove error: " + uri, e);
            }
            return false;
        }
    }
}
