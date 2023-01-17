/*
 * Copyright (©) 2023 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.db;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import net.tjado.passwdsafe.lib.PasswdSafeUtil;

/**
 * PasswdSafe database
 */
@Database(entities = {BackupFile.class, RecentFile.class, SavedPassword.class},
          version = 3)
public abstract class PasswdSafeDb extends RoomDatabase
{
    private static final String TAG = "PasswdSafeDb";

    private static volatile PasswdSafeDb INSTANCE;

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database)
        {
            // Do nothing, the 2->3 migration does all the work
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db)
        {
            // V1 and 2 do not use Room, so a migration to V3 is needed to setup
            // Room.  Use the previous migrations to create any tables from all
            // previous database formats to V3.

            PasswdSafeUtil.dbginfo(TAG, "Migrate v2->v3");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + BackupFile.TABLE + " (" +
                       BackupFile.COL_ID + " INTEGER PRIMARY KEY " +
                       "AUTOINCREMENT NOT NULL, " +
                       BackupFile.COL_TITLE + " TEXT NOT NULL, " +
                       BackupFile.COL_FILE_URI + " TEXT NOT NULL, " +
                       BackupFile.COL_DATE + " INTEGER NOT NULL, " +
                       BackupFile.COL_HAS_FILE +
                       " INTEGER NOT NULL DEFAULT 1, " +
                       BackupFile.COL_HAS_URI_PERM +
                       " INTEGER NOT NULL DEFAULT 1" +
                       ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + RecentFile.TABLE + " (" +
                       RecentFile.COL_ID + " INTEGER PRIMARY KEY," +
                       RecentFile.COL_TITLE + " TEXT NOT NULL," +
                       RecentFile.COL_URI + " TEXT NOT NULL," +
                       RecentFile.COL_DATE + " INTEGER NOT NULL" +
                       ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS " +
                       SavedPassword.TABLE + " (" +
                       SavedPassword.COL_ID + " INTEGER PRIMARY KEY," +
                       SavedPassword.COL_URI + " TEXT NOT NULL UNIQUE," +
                       SavedPassword.COL_PROVIDER_URI + " TEXT NOT NULL," +
                       SavedPassword.COL_DISPLAY_NAME + " TEXT NOT NULL," +
                       SavedPassword.COL_IV + " TEXT NOT NULL," +
                       SavedPassword.COL_ENC_PASSWD + " TEXT NOT NULL" +
                       ");");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS " +
                       "`index_saved_passwords_uri` ON " +
                       "`" + SavedPassword.TABLE + "` (" +
                       "`" + SavedPassword.COL_URI + "`)");
        }
    };

    /**
     * Get a reference to the database
     */
    public static PasswdSafeDb get(final Context ctx)
    {
        if (INSTANCE == null) {
            synchronized (PasswdSafeDb.class) {
                if (INSTANCE == null) {
                    PasswdSafeUtil.dbginfo(TAG, "Create");
                    INSTANCE = Room.databaseBuilder(ctx.getApplicationContext(),
                                                    PasswdSafeDb.class,
                                                    "recent_files.db")
                                   .allowMainThreadQueries()
                                   .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                                   .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Access the backup files
     */
    public abstract BackupFilesDao accessBackupFiles();

    /**
     * Access the recent files
     */
    public abstract RecentFilesDao accessRecentFiles();

    /**
     * Access the saved passwords
     */
    public abstract SavedPasswordsDao accessSavedPasswords();
}
