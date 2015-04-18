/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import java.io.File;

import android.database.sqlite.SQLiteDatabase;

/**
 * Provides common Provider operations for local file management
 */
public abstract class AbstractProvider implements Provider
{
    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#insertLocalFile(long, java.lang.String, android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public long insertLocalFile(long providerId,
                                String title,
                                SQLiteDatabase db)
            throws Exception
    {
        long id = SyncDb.addLocalFile(providerId, title,
                                      System.currentTimeMillis(), db);
        requestSync(false);
        return id;
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#updateLocalFile(com.jefftharris.passwdsafe.sync.lib.DbFile, java.lang.String, java.io.File, android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void updateLocalFile(DbFile file,
                                String localFileName,
                                File localFile,
                                SQLiteDatabase db)
            throws Exception
    {
        SyncDb.updateLocalFile(file.itsId, localFileName,
                               file.itsLocalTitle, file.itsLocalFolder,
                               localFile.lastModified(), db);
        switch (file.itsLocalChange) {
        case NO_CHANGE:
        case REMOVED: {
            SyncDb.updateLocalFileChange(file.itsId, DbFile.FileChange.MODIFIED,
                                         db);
            break;
        }
        case ADDED:
        case MODIFIED: {
            break;
        }
        }
        requestSync(false);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#deleteLocalFile(com.jefftharris.passwdsafe.sync.lib.DbFile, android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void deleteLocalFile(DbFile file, SQLiteDatabase db)
            throws Exception
    {
        SyncDb.updateLocalFileDeleted(file.itsId, db);
        requestSync(false);
    }
}
