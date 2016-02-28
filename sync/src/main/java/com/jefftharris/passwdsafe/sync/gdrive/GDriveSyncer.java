/*
 * Copyright (©) 2014-2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.gdrive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.accounts.Account;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableNotifiedException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.R;
import com.jefftharris.passwdsafe.sync.SyncUpdateHandler;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.ProviderSyncer;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;

/**
 * The Syncer class encapsulates a sync operation
 */
public class GDriveSyncer extends ProviderSyncer<Pair<Drive, String>>
{
    private final Drive itsDrive;
    private final String itsDriveToken;
    private final HashMap<String, File> itsFileCache = new HashMap<>();
    private final FileFolders itsFileFolders;

    private static final String TAG = "GDriveSyncer";

    /** Constructor */
    public GDriveSyncer(Account acct,
                        DbProvider provider,
                        SQLiteDatabase db,
                        SyncLogRecord logrec,
                        Context ctx)
    {
        super(getDriveService(acct, ctx), provider, db, logrec, ctx, TAG);
        itsDrive = itsProviderClient.first;
        itsDriveToken = itsProviderClient.second;
        itsFileFolders = new FileFolders(itsDrive, itsFileCache,
                                         new HashMap<String, FolderRefs>());
    }


    /** Sync the provider */
    public SyncUpdateHandler.GDriveState sync() throws Exception
    {
        if (itsDrive == null) {
            return SyncUpdateHandler.GDriveState.PENDING_AUTH;
        }

        SyncUpdateHandler.GDriveState syncState =
                SyncUpdateHandler.GDriveState.OK;
        try {
            List<AbstractSyncOper<Drive>> opers = null;
            try {
                itsDb.beginTransaction();
                itsLogrec.setFullSync(true);
                opers = performFullSync();
                itsDb.setTransactionSuccessful();
            } finally {
                itsDb.endTransaction();
            }

            if (opers != null) {
                for (AbstractSyncOper<Drive> oper: opers) {
                    try {
                        itsLogrec.addEntry(oper.getDescription(itsContext));
                        oper.doOper(itsDrive, itsContext);
                        try {
                            itsDb.beginTransaction();
                            oper.doPostOperUpdate(itsDb, itsContext);
                            itsDb.setTransactionSuccessful();
                        } finally {
                            itsDb.endTransaction();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Sync error for file " + oper.getFile(), e);
                        itsLogrec.addFailure(e);
                    }
                }
            }

            itsContext.getContentResolver().notifyChange(
                     PasswdSafeContract.CONTENT_URI, null, false);
        } catch (UserRecoverableAuthIOException e) {
            PasswdSafeUtil.dbginfo(TAG, e, "Recoverable google auth error");
            GoogleAuthUtil.clearToken(itsContext, itsDriveToken);
            syncState = SyncUpdateHandler.GDriveState.AUTH_REQUIRED;
        } catch (GoogleAuthIOException e) {
            Log.e(TAG, "Google auth error", e);
            GoogleAuthUtil.clearToken(itsContext, itsDriveToken);
            throw e;
        }

        return syncState;
    }


    /** Get the Google account credential */
    public static GoogleAccountCredential getAcctCredential(Context ctx)
    {
        return GoogleAccountCredential.usingOAuth2(
                ctx, Collections.singletonList(DriveScopes.DRIVE));
    }


    /** Get a file's metadata */
    public static File getFile(String id, Drive drive)
            throws IOException
    {
        return drive.files().get(id)
                .setFields(GDriveProvider.FILE_FIELDS).execute();
    }


    /** Perform a full sync of the files */
    private List<AbstractSyncOper<Drive>> performFullSync()
            throws SQLException, IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "Perform sync");
        syncDisplayName();

        HashMap<String, File> allRemFiles = new HashMap<>();
        String query =
                "not trashed" +
                " and ( mimeType = 'application/octet-stream' or " +
                "       mimeType = 'binary/octet-stream' or " +
                "       mimeType = 'application/psafe3' )" +
                " and fullText contains '.psafe3'";
        Drive.Files.List request = itsDrive.files().list()
                .setQ(query)
                .setFields("nextPageToken,files("+GDriveProvider.FILE_FIELDS+")");
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
                allRemFiles.put(file.getId(), file);
            }
            request.setPageToken(files.getNextPageToken());
        } while((request.getPageToken() != null) &&
                (request.getPageToken().length() > 0));

        return performSync(allRemFiles, true);
    }

    /**
     * Sync account display name
     */
    private void syncDisplayName() throws IOException
    {
        About about = itsDrive.about().get()
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

    /** Perform a sync of the files */
    private List<AbstractSyncOper<Drive>>
    performSync(HashMap<String, File> remfiles, boolean isAllRemoteFiles)
            throws SQLException, IOException
    {
        itsFileCache.putAll(remfiles);
        Map<String, String> fileFolders =
                itsFileFolders.computeFilesFolders(remfiles);

        List<DbFile> dbfiles = SyncDb.getFiles(itsProvider.itsId, itsDb);
        for (DbFile dbfile: dbfiles) {
            if (remfiles.containsKey(dbfile.itsRemoteId)) {
                File remfile = remfiles.get(dbfile.itsRemoteId);
                if (remfile != null) {
                    checkRemoteFileChange(dbfile, remfile, fileFolders);
                } else {
                    PasswdSafeUtil.dbginfo(TAG, "performSync remove remote %s",
                                           dbfile);
                    SyncDb.updateRemoteFileDeleted(dbfile.itsId, itsDb);
                }
                remfiles.remove(dbfile.itsRemoteId);
            } else if (isAllRemoteFiles &&
                    (dbfile.itsLocalChange != DbFile.FileChange.ADDED)) {
                PasswdSafeUtil.dbginfo(TAG, "performSync remove remote %s",
                                       dbfile);
                SyncDb.updateRemoteFileDeleted(dbfile.itsId, itsDb);
            }
        }

        for (File remfile: remfiles.values()) {
            if (remfile == null) {
                continue;
            }
            String fileId = remfile.getId();
            PasswdSafeUtil.dbginfo(TAG, "performSync add remote %s", fileId);
            SyncDb.addRemoteFile(itsProvider.itsId, fileId,
                                 remfile.getName(),
                                 fileFolders.get(fileId),
                                 remfile.getModifiedTime().getValue(),
                                 remfile.getMd5Checksum(), itsDb);
        }

        List<AbstractSyncOper<Drive>> opers = new ArrayList<>();
        dbfiles = SyncDb.getFiles(itsProvider.itsId, itsDb);
        for (DbFile dbfile: dbfiles) {
            resolveSyncOper(dbfile, opers);
        }

        return opers;
    }


    /** Check for a remote file change and update */
    private void checkRemoteFileChange(DbFile dbfile,
                                       File remfile,
                                       Map<String, String> fileFolders)
    {
        boolean changed = true;
        do {
            String remTitle = remfile.getName();
            String remFolder = fileFolders.get(dbfile.itsRemoteId);
            long remModDate = remfile.getModifiedTime().getValue();
            String remHash = remfile.getMd5Checksum();
            if (!TextUtils.equals(dbfile.itsRemoteTitle, remTitle) ||
                    !TextUtils.equals(dbfile.itsRemoteFolder, remFolder) ||
                    (dbfile.itsRemoteModDate != remModDate) ||
                    !TextUtils.equals(dbfile.itsRemoteHash, remHash) ||
                    TextUtils.isEmpty(dbfile.itsLocalFile)) {
                break;
            }

            java.io.File localFile =
                    itsContext.getFileStreamPath(dbfile.itsLocalFile);
            if (!localFile.exists()) {
                break;
            }

            changed = false;
        } while(false);

        if (!changed) {
            return;
        }

        PasswdSafeUtil.dbginfo(TAG, "performSync update remote %s", dbfile);
        SyncDb.updateRemoteFile(dbfile.itsId, dbfile.itsRemoteId,
                                remfile.getName(),
                                fileFolders.get(dbfile.itsRemoteId),
                                remfile.getModifiedTime().getValue(),
                                remfile.getMd5Checksum(), itsDb);
        switch (dbfile.itsRemoteChange) {
        case NO_CHANGE:
        case REMOVED: {
            SyncDb.updateRemoteFileChange(dbfile.itsId,
                                          DbFile.FileChange.MODIFIED, itsDb);
            break;
        }
        case ADDED:
        case MODIFIED: {
            break;
        }
        }
    }


    /** Resolve the sync operations for a file */
    private void resolveSyncOper(DbFile dbfile,
                                 List<AbstractSyncOper<Drive>> opers)
            throws SQLException
    {
        if ((dbfile.itsLocalChange != DbFile.FileChange.NO_CHANGE) ||
                (dbfile.itsRemoteChange != DbFile.FileChange.NO_CHANGE)) {
            PasswdSafeUtil.dbginfo(TAG, "resolveSyncOper %s", dbfile);
        }

        switch (dbfile.itsLocalChange) {
        case ADDED: {
            switch (dbfile.itsRemoteChange) {
            case ADDED:
            case MODIFIED: {
                logConflictFile(dbfile, true);
                splitConflictedFile(dbfile, opers);
                break;
            }
            case NO_CHANGE: {
                opers.add(new GDriveLocalToRemoteOper(dbfile, itsFileCache,
                                                      false));
                break;
            }
            case REMOVED: {
                logConflictFile(dbfile, true);
                recreateRemoteRemovedFile(dbfile, opers);
                break;
            }
            }
            break;
        }
        case MODIFIED: {
            switch (dbfile.itsRemoteChange) {
            case ADDED:
            case MODIFIED: {
                logConflictFile(dbfile, true);
                splitConflictedFile(dbfile, opers);
                break;
            }
            case NO_CHANGE: {
                opers.add(new GDriveLocalToRemoteOper(dbfile, itsFileCache,
                                                      false));
                break;
            }
            case REMOVED: {
                logConflictFile(dbfile, true);
                recreateRemoteRemovedFile(dbfile, opers);
                break;
            }
            }
            break;
        }
        case NO_CHANGE: {
            switch (dbfile.itsRemoteChange) {
            case ADDED:
            case MODIFIED: {
                opers.add(new GDriveRemoteToLocalOper(dbfile, itsFileCache));
                break;
            }
            case NO_CHANGE: {
                // Nothing
                break;
            }
            case REMOVED: {
                opers.add(new GDriveRmFileOper(dbfile));
                break;
            }
            }
            break;
        }
        case REMOVED: {
            switch (dbfile.itsRemoteChange) {
            case ADDED:
            case MODIFIED: {
                logConflictFile(dbfile, false);
                DbFile newRemfile = splitRemoteToNewFile(dbfile);
                DbFile updatedLocalFile = SyncDb.getFile(dbfile.itsId, itsDb);

                opers.add(new GDriveRemoteToLocalOper(newRemfile,
                                                      itsFileCache));
                opers.add(new GDriveRmFileOper(updatedLocalFile));
                break;
            }
            case NO_CHANGE:
            case REMOVED: {
                opers.add(new GDriveRmFileOper(dbfile));
                break;
            }
            }
            break;
        }
        }
    }


    /** Split the file.  A new added remote file is created with the remote id,
     * and the file is updated to resemble a new local file with the same id but
     * a different name indicating a conflict
     */
    private void splitConflictedFile(DbFile dbfile,
                                     List<AbstractSyncOper<Drive>> opers)
            throws SQLException
    {
        DbFile newRemfile = splitRemoteToNewFile(dbfile);
        DbFile updatedLocalFile = updateFileAsLocallyAdded(
                dbfile, itsContext.getString(R.string.conflicted_local_copy));

        opers.add(new GDriveRemoteToLocalOper(newRemfile,
                                              itsFileCache));
        opers.add(new GDriveLocalToRemoteOper(updatedLocalFile,
                                              itsFileCache, true));
    }


    /** Recreate a remotely deleted file from local updates */
    private void recreateRemoteRemovedFile(DbFile dbfile,
                                           List<AbstractSyncOper<Drive>> opers)
            throws SQLException
    {
        resetRemoteFields(dbfile);
        DbFile updatedLocalFile = updateFileAsLocallyAdded(
                dbfile, itsContext.getString(R.string.recreated_local_copy));
        opers.add(new GDriveLocalToRemoteOper(updatedLocalFile,
                itsFileCache, true));
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
    private static boolean isFolderFile(File file)
    {
        return !file.getTrashed() &&
                GDriveProvider.FOLDER_MIME.equals(file.getMimeType());
    }


    /** Get a string form for a remote file */
    private static String fileToString(File file)
    {
        if (file == null) {
            return "{null}";
        }
        return String.format(Locale.US,
                             "{id:%s, name:%s, mime:%s, md5:%s}",
                             file.getId(), file.getName(),
                             file.getMimeType(), file.getMd5Checksum());
    }

    /**
     * Retrieve a authorized service object to send requests to the Google
     * Drive API. On failure to retrieve an access token, a notification is
     * sent to the user requesting that authorization be granted for the
     * {@code https://www.googleapis.com/auth/drive} scope.
     *
     * @return An authorized service object and its auth token.
     */
    private static Pair<Drive, String> getDriveService(Account acct,
                                                       Context ctx)
    {
        Drive drive = null;
        String token = null;
        try {
            GoogleAccountCredential credential = getAcctCredential(ctx);
            credential.setBackOff(new ExponentialBackOff());
            credential.setSelectedAccountName(acct.name);

            token = GoogleAuthUtil.getTokenWithNotification(
                    ctx, acct, credential.getScope(),
                    null, PasswdSafeContract.AUTHORITY, null);

            Drive.Builder builder = new Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    JacksonFactory.getDefaultInstance(), credential);
            builder.setApplicationName(ctx.getString(R.string.app_name));
            drive = builder.build();
        } catch (UserRecoverableNotifiedException e) {
            // User notified
            PasswdSafeUtil.dbginfo(TAG, e, "User notified auth exception");
            try {
                GoogleAuthUtil.clearToken(ctx, null);
            } catch(Exception ioe) {
                Log.e(TAG, "getDriveService clear failure", e);
            }
        } catch (GoogleAuthException e) {
            // Unrecoverable
            Log.e(TAG, "Unrecoverable auth exception", e);
        }
        catch (IOException e) {
            // Transient
            PasswdSafeUtil.dbginfo(TAG, e, "Transient error");
        } catch (Exception e) {
            Log.e(TAG, "Token exception", e);
        }
        return new Pair<>(drive, token);
    }
}
