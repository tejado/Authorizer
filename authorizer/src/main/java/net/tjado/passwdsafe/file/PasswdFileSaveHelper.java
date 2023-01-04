/*
 * Copyright (©) 2017 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.file;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import net.tjado.passwdsafe.Preferences;
import net.tjado.passwdsafe.db.BackupFilesDao;
import net.tjado.passwdsafe.db.PasswdSafeDb;
import net.tjado.passwdsafe.pref.FileBackupPref;

import org.pwsafe.lib.file.PwsStorage;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A PwsStorage save helper for files */
public class PasswdFileSaveHelper implements PwsStorage.SaveHelper
{
    private final Context itsContext;

    private static final String TAG = "PasswdFileSaveHelper";

    /**
     * Constructor
     */
    public PasswdFileSaveHelper(Context context)
    {
        itsContext = context;
    }

    /** Get the save context */
    public Context getContext()
    {
        return itsContext;
    }

    @Override
    public String getSaveFileName(File file, boolean isV3)
    {
        String name = file.getName();
        Pattern pat = Pattern.compile("^(.*)_\\d{8}_\\d{6}\\.ibak$");
        Matcher match = pat.matcher(name);
        if (match.matches()) {
            name = match.group(1);
            if (isV3) {
                name += ".psafe3";
            } else {
                name += ".dat";
            }
        }
        return name;
    }

    public void createBackup(Uri fileUri, String identifier)
    {
        BackupFilesDao backupFiles =
                PasswdSafeDb.get(itsContext).accessBackupFiles();
        SharedPreferences prefs = Preferences.getSharedPrefs(itsContext);
        FileBackupPref backupPref = Preferences.getFileBackupPref(prefs);
        backupFiles.insert(fileUri, identifier, backupPref, itsContext,
                           itsContext.getContentResolver());
    }


    @Override
    public void createBackupFile(File fromFile, File toFile)
            throws IOException
    {
        createBackup(Uri.fromFile(toFile), toFile.getName());

        SharedPreferences prefs = Preferences.getSharedPrefs(itsContext);
        FileBackupPref backupPref = Preferences.getFileBackupPref(prefs);

        File dir = toFile.getParentFile();
        String fileName = toFile.getName();
        int dotpos = fileName.lastIndexOf('.');
        if (dotpos != -1) {
            fileName = fileName.substring(0, dotpos);
        }

        final Pattern pat = Pattern.compile(
                "^" + Pattern.quote(fileName) + "_\\d{8}_\\d{6}\\.ibak$");
        File[] backupFiles = null;
        if (dir != null) {
            backupFiles = dir.listFiles(
                    f -> f.isFile() && pat.matcher(f.getName()).matches());
        }
        if (backupFiles != null) {
            Arrays.sort(backupFiles);

            int numBackups = backupPref.getNumBackups();
            if (numBackups > 0) {
                --numBackups;
            }
            for (int i = 0, numFiles = backupFiles.length;
                 numFiles > numBackups; ++i, --numFiles) {
                if (!backupFiles[i].equals(fromFile)) {
                    if (!backupFiles[i].delete()) {
                        Log.e(TAG, "Error removing backup: " + backupFiles[i]);
                    }
                }
            }
        }

        if (backupPref != FileBackupPref.BACKUP_NONE) {
            SimpleDateFormat bakTime =
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
            String bakName =
                    fileName + "_" + bakTime.format(new Date()) + ".ibak";
            File bakFile = new File(dir, bakName);
            if (!toFile.renameTo(bakFile)) {
                throw new IOException("Can not create backup file: " +
                                      bakFile);
            }
        }
    }
}
