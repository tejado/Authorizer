/*
 * Copyright (©) 2014-2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.gdrive;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.SyncUpdateHandler;
import com.jefftharris.passwdsafe.sync.lib.AbstractLocalToRemoteSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer;
import com.jefftharris.passwdsafe.sync.lib.AbstractRemoteToLocalSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractRmSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.ProviderRemoteFile;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;

/**
 * The Syncer class encapsulates a sync operation
 */
public class GDriveSyncer extends AbstractProviderSyncer<Drive>
{
    private final HashMap<String, File> itsFileCache = new HashMap<>();
    private final FileFolders itsFileFolders;
    private SyncUpdateHandler.GDriveState itsSyncState =
            SyncUpdateHandler.GDriveState.OK;

    private static final String TAG = "GDriveSyncer";

    // TODO: test file in multiple dirs
    // TODO: optimize FileFolders for single file at a time?
    // TODO: remove concerns about file renames and purging empty folders from FileFolders
    // TODO: local to remote force insert used? needed?

    /** Constructor */
    public GDriveSyncer(Drive drive,
                        DbProvider provider,
                        SQLiteDatabase db,
                        SyncLogRecord logrec,
                        Context ctx)
    {
        super(drive, provider, db, logrec, ctx, TAG);
        itsFileFolders = new FileFolders(itsProviderClient, itsFileCache,
                                         new HashMap<String, FolderRefs>());
    }

    /** Get the sync state */
    public SyncUpdateHandler.GDriveState getSyncState()
    {
        return itsSyncState;
    }

    /** Get a file's metadata */
    public static File getFile(String id, Drive drive)
            throws IOException
    {
        return drive.files().get(id)
                .setFields(GDriveProvider.FILE_FIELDS).execute();
    }

    @Override
    protected List<AbstractSyncOper<Drive>> performSync()
            throws IOException, SQLException
    {
        if (itsProviderClient == null) {
            itsSyncState = SyncUpdateHandler.GDriveState.PENDING_AUTH;
            return null;
        }

        syncDisplayName();
        HashMap<String, ProviderRemoteFile> driveFiles = getDriveFiles();
        updateDbFiles(driveFiles);
        return resolveSyncOpers();
    }

    @Override
    protected AbstractLocalToRemoteSyncOper<Drive> createLocalToRemoteOper(
            DbFile dbfile)
    {
        return new GDriveLocalToRemoteOper(dbfile, false);
    }

    @Override
    protected AbstractRemoteToLocalSyncOper<Drive> createRemoteToLocalOper(
            DbFile dbfile)
    {
        return new GDriveRemoteToLocalOper(dbfile, itsFileCache);
    }

    @Override
    protected AbstractRmSyncOper<Drive> createRmFileOper(DbFile dbfile)
    {
        return new GDriveRmFileOper(dbfile);
    }

    /** Get the Google Drive files */
    private HashMap<String, ProviderRemoteFile> getDriveFiles()
            throws IOException
    {
        HashMap<String, ProviderRemoteFile> driveFiles = new HashMap<>();

        String query =
                "not trashed" +
                " and ( mimeType = 'application/octet-stream' or " +
                "       mimeType = 'binary/octet-stream' or " +
                "       mimeType = 'application/psafe3' )" +
                " and fullText contains '.psafe3'";
        Drive.Files.List request =
                itsProviderClient.files().list()
                                 .setQ(query)
                                 .setFields("nextPageToken,files(" +
                                            GDriveProvider.FILE_FIELDS + ")");
        do {
            FileList files = request.execute();
            PasswdSafeUtil.dbginfo(TAG, "num files: %d",
                                   files.getFiles().size());
            for (File file: files.getFiles()) {
                if (!isSyncFile(file)) {
                    if (isFolderFile(file)) {
                        PasswdSafeUtil.dbginfo(TAG, "isdir %s", file);
                    }
                    continue;
                }
                PasswdSafeUtil.dbginfo(TAG, "File %s", fileToString(file));
                GDriveProviderFile driveFile = new GDriveProviderFile(
                        file, itsFileFolders.computeFileFolders(file));
                driveFiles.put(driveFile.getRemoteId(), driveFile);
            }
            request.setPageToken(files.getNextPageToken());
        } while(!TextUtils.isEmpty(request.getPageToken()));

        return driveFiles;
    }

    /**
     * Sync account display name
     */
    private void syncDisplayName() throws IOException
    {
        About about = itsProviderClient.about().get()
                                       .setFields(GDriveProvider.ABOUT_FIELDS)
                                       .execute();
        String displayName = null;
        if (about.getUser() != null) {
            displayName = about.getUser().getDisplayName();
        }
        PasswdSafeUtil.dbginfo(TAG, "user %s", displayName);
        if (!TextUtils.equals(itsProvider.itsDisplayName, displayName)) {
            SyncDb.updateProviderDisplayName(itsProvider.itsId, displayName,
                                             itsDb);
        }
    }

    /** Should the file be synced */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isSyncFile(File file)
    {
        if (isFolderFile(file) || file.getTrashed()) {
            return false;
        }
        String ext = file.getFileExtension();
        return (ext != null) && ext.equals("psafe3");
    }


    /** Is the file a folder */
    public static boolean isFolderFile(File file)
    {
        return !file.getTrashed() &&
                GDriveProvider.FOLDER_MIME.equals(file.getMimeType());
    }


    /** Get a string form for a remote file */
    public static String fileToString(File file)
    {
        if (file == null) {
            return "{null}";
        }
        return String.format(Locale.US,
                             "{id:%s, name:%s, mime:%s, md5:%s, mod:%d}",
                             file.getId(), file.getName(),
                             file.getMimeType(), file.getMd5Checksum(),
                             file.getModifiedTime().getValue());
    }
}
