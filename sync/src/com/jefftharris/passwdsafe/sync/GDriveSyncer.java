/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

import android.accounts.Account;
import android.content.ContentProviderClient;
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
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Changes;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

/**
 *  The GDriveSyncer class syncs password files from Google Drive
 */
public class GDriveSyncer
{
    private static final String TAG = "GDriveSyncer";

    private static final String ABOUT_CHANGE_ID = "largestChangeId";

    private final Context itsContext;
    private final ContentProviderClient itsProvider;
    private final Account itsAccount;
    private final Drive itsDrive;
    private final String itsDriveToken;
    private final SyncDb itsSyncDb;

    /** Constructor */
    public GDriveSyncer(Context context,
                        ContentProviderClient provider,
                        Account account)
    {
        itsContext = context;
        itsProvider = provider;
        itsAccount = account;
        Pair<Drive, String> driveInfo = getDriveService();
        itsDrive = driveInfo.first;
        itsDriveToken = driveInfo.second;
        itsSyncDb = new SyncDb(itsContext);
        Log.i(TAG, "GDriveSyncer");
    }


    /** Add a provider for an account */
    public static void addProvider(Account account, SyncDb db, Context ctx)
        throws SQLException
    {
        Log.i(TAG, "Add provider: " + account);
        db.addProvider(account.name);
        ctx.getContentResolver().notifyChange(
                PasswdSafeContract.Providers.CONTENT_URI, null);
    }


    /** Delete the provider for the account */
    public static void deleteProvider(Account account, SyncDb syncDb,
                                      Context ctx)
        throws SQLException
    {
        Log.i(TAG, "Delete provider: " + account);

        SQLiteDatabase db = syncDb.getDb();
        try {
            db.beginTransaction();
            List<SyncDb.DbFile> dbfiles = syncDb.getFiles(account.name, db);
            for (SyncDb.DbFile dbfile: dbfiles) {
                ctx.deleteFile(dbfile.itsLocalFile);
            }
            syncDb.deleteProvider(account.name, db);

            try {
                GoogleAccountCredential credential =
                        GoogleAccountCredential.usingOAuth2(ctx,
                                                            DriveScopes.DRIVE);
                String token = GoogleAuthUtil.getToken(ctx, account.name,
                                                       credential.getScope());
                PasswdSafeUtil.dbginfo(TAG, "Remove token for %s: %s",
                                       account.name, token);
                if (token != null) {
                    GoogleAuthUtil.invalidateToken(ctx, token);
                }
            } catch (Exception e) {
                PasswdSafeUtil.dbginfo(TAG, e, "No auth token for %s",
                                       account.name);
            }

            ctx.getContentResolver().notifyChange(
                    PasswdSafeContract.CONTENT_URI, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Close the syncer */
    public void close()
    {
        itsSyncDb.close();
    }


    /** Perform synchronization */
    public void performSync()
    {
        if (itsDrive == null) {
            return;
        }

        Log.i(TAG, "Performing sync for " + itsAccount.name);

        SQLiteDatabase db = itsSyncDb.getDb();
        try {
            db.beginTransaction();
            long changeId = itsSyncDb.getProviderSyncChange(itsAccount.name,
                                                            db);
            Log.i(TAG, "largest change " + changeId);
            long newChangeId = -1;
            if (changeId == -1) {
                newChangeId = performFullSync(db);
            } else {
                newChangeId = performSyncSince(changeId, db);
            }
            if (changeId != newChangeId) {
                itsSyncDb.setProviderSyncChange(itsAccount.name,
                                                newChangeId, db);
            }

            itsContext.getContentResolver().notifyChange(
                     PasswdSafeContract.CONTENT_URI, null, false);
            db.setTransactionSuccessful();
        } catch (UserRecoverableAuthIOException e) {
            PasswdSafeUtil.dbginfo(TAG, e, "Recoverable google auth error");
            GoogleAuthUtil.invalidateToken(itsContext, itsDriveToken);
        } catch (GoogleAuthIOException e) {
            Log.e(TAG, "Google auth error", e);
            GoogleAuthUtil.invalidateToken(itsContext, itsDriveToken);
        } catch (Exception e) {
            Log.e(TAG, "Sync error", e);
        } finally {
            db.endTransaction();
        }
        Log.i(TAG, "Sync finished for " + itsAccount.name);
    }

    // TODO: filter on mime types
    // TODO: .dat files?
    // TODO: only get needed fields


    /** Perform a full sync of the files */
    private long performFullSync(SQLiteDatabase db)
            throws SQLException, IOException
    {
        Log.i(TAG, "Perform full sync");
        About about = itsDrive.about().get()
                .setFields(ABOUT_CHANGE_ID).execute();
        long largestChangeId = about.getLargestChangeId();

        HashMap<String, File> allRemFiles = new HashMap<String, File>();
        Files.List request = itsDrive.files().list().setQ("not trashed");
        do {
            FileList files = request.execute();
            Log.i(TAG, "num files: " + files.getItems().size());
            for (File file: files.getItems()) {
                if (!isSyncFile(file)) {
                    continue;
                }
                Log.i(TAG, "File id: " + file.getId() + ", title: " +
                    file.getTitle() + ", mime: " + file.getMimeType());
                allRemFiles.put(file.getId(), file);
            }
            request.setPageToken(files.getNextPageToken());
        } while((request.getPageToken() != null) &&
                (request.getPageToken().length() > 0));

        performSync(allRemFiles, db);
        return largestChangeId;
    }


    /** Perform a sync of files since the given change id */
    private long performSyncSince(long changeId, SQLiteDatabase db)
        throws SQLException, IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "performSyncSince %d", changeId);
        HashMap<String, File> changedFiles = new HashMap<String, File>();
        Changes.List request =
            itsDrive.changes().list().setStartChangeId(changeId + 1);
        do {
            ChangeList changes = request.execute();
            long changesLargestId = changes.getLargestChangeId().longValue();

            for (Change change: changes.getItems()) {
                File file = change.getFile();
                if (change.getDeleted() || !isSyncFile(file)) {
                    file = null;
                }
                changedFiles.put(change.getFileId(), file);
                PasswdSafeUtil.dbginfo(TAG, "performSyncSince changed %s: %s",
                                       change.getFileId(), file);
            }

            if (changesLargestId > changeId) {
                changeId = changesLargestId;
            }
            request.setPageToken(changes.getNextPageToken());
        } while((request.getPageToken() != null) &&
                (request.getPageToken().length() > 0));

        performSync(changedFiles, db);
        return changeId;
    }


    /** Perform a sync of the files */
    private void performSync(HashMap<String, File> remfiles, SQLiteDatabase db)
            throws SQLException
    {
        HashMap<String, File> fileCache = new HashMap<String, File>(remfiles);
        List<SyncDb.DbFile> dbfiles = itsSyncDb.getFiles(itsAccount.name, db);
        for (SyncDb.DbFile dbfile: dbfiles) {
            if (remfiles.containsKey(dbfile.itsRemoteId)) {
                File remfile = remfiles.get(dbfile.itsRemoteId);
                if (remfile != null) {
                    PasswdSafeUtil.dbginfo(TAG, "performSync update remote %s",
                                           dbfile.itsRemoteId);
                    itsSyncDb.updateRemoteFile(
                            dbfile.itsId, dbfile.itsRemoteId,
                            remfile.getTitle(),
                            remfile.getModifiedDate().getValue(), db);
                } else {
                    PasswdSafeUtil.dbginfo(TAG, "performSync remove remote %s",
                                           dbfile.itsRemoteId);
                    itsSyncDb.updateRemoteFileDeleted(dbfile.itsId, db);
                }
                remfiles.remove(dbfile.itsRemoteId);
            }
        }

        for (File remfile: remfiles.values()) {
            if (remfile == null) {
                continue;
            }
            String fileId = remfile.getId();
            PasswdSafeUtil.dbginfo(TAG, "performSync add remote %s", fileId);
            itsSyncDb.addRemoteFile(itsAccount.name, fileId, remfile.getTitle(),
                                    remfile.getModifiedDate().getValue(), db);
        }

        dbfiles = itsSyncDb.getFiles(itsAccount.name, db);
        for (SyncDb.DbFile dbfile: dbfiles) {
            try {
                if (isRemoteNewer(dbfile)) {
                    if (dbfile.itsIsRemoteDeleted) {
                        removeFile(dbfile, db);
                    } else if (dbfile.itsIsLocalDeleted) {
                        // TODO: conflict?
                    } else {
                        syncRemoteToLocal(dbfile, fileCache, db);
                    }
                } else if (dbfile.itsLocalModDate > dbfile.itsRemoteModDate) {
                    if (dbfile.itsIsLocalDeleted) {
                        removeFile(dbfile, db);
                    } else if (dbfile.itsIsRemoteDeleted) {
                        // TODO: conflict?
                    } else {
                        syncLocalToRemote(dbfile, fileCache, db);
                    }
                } else if (dbfile.itsIsRemoteDeleted ||
                        dbfile.itsIsLocalDeleted) {
                    removeFile(dbfile, db);
                }
            } catch (IOException e) {
                Log.e(TAG, "Sync error for file " + dbfile, e);
            }
        }
    }


    /** Is the remote file considered newer than the local */
    private boolean isRemoteNewer(SyncDb.DbFile dbfile)
    {
        if (dbfile.itsRemoteModDate > dbfile.itsLocalModDate) {
            return true;
        }
        if (TextUtils.isEmpty(dbfile.itsLocalFile)) {
            return true;
        }
        java.io.File localFile =
                itsContext.getFileStreamPath(dbfile.itsLocalFile);
        if (!localFile.exists()) {
            return true;
        }
        return false;
    }


    /** Sync a remote file to local */
    private void syncRemoteToLocal(SyncDb.DbFile dbfile,
                                   HashMap<String, File> fileCache,
                                   SQLiteDatabase db)
            throws SQLException, IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "syncRemoteToLocal %s", dbfile);
        File file = fileCache.get(dbfile.itsRemoteId);
        if (file == null) {
            file = itsDrive.files().get(dbfile.itsRemoteId).execute();
        }
        String localFile = "syncfile-" + Long.toString(dbfile.itsId);
        try {
            if (downloadFile(file, localFile)) {
                itsSyncDb.updateLocalFile(dbfile.itsId, localFile,
                                          dbfile.itsRemoteTitle,
                                          dbfile.itsRemoteModDate, db);
            }
        } catch (SQLException e) {
            itsContext.deleteFile(localFile);
            throw e;
        }
    }


    /** Sync a local file to remote */
    private void syncLocalToRemote(SyncDb.DbFile dbfile,
                                   HashMap<String, File> fileCache,
                                   SQLiteDatabase db)
            throws SQLException, IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "syncLocalToRemote %s", dbfile);

        File file;
        boolean isInsert;
        if (TextUtils.isEmpty(dbfile.itsRemoteId)) {
            file = new File();
            file.setDescription("Password Safe file");
            file.setMimeType("application/x-psafe3");
            isInsert = true;
        } else {
            file = fileCache.get(dbfile.itsRemoteId);
            if (file == null) {
                file = itsDrive.files().get(dbfile.itsRemoteId).execute();
            }
            isInsert = false;
        }

        file.setTitle(dbfile.itsLocalTitle);
        java.io.File localFile =
                itsContext.getFileStreamPath(dbfile.itsLocalFile);
        FileContent fileMedia = new FileContent(file.getMimeType(), localFile);
        if (isInsert) {
            file = itsDrive.files().insert(file, fileMedia).execute();
        } else {
            file = itsDrive.files().update(dbfile.itsRemoteId, file,
                                           fileMedia).execute();
        }

        String title = file.getTitle();
        long modDate = file.getModifiedDate().getValue();
        itsSyncDb.updateRemoteFile(dbfile.itsId, file.getId(),
                                   title, modDate, db);
        itsSyncDb.updateLocalFile(dbfile.itsId, dbfile.itsLocalFile,
                                  title, modDate, db);
        localFile.setLastModified(modDate);
    }


    /** Remove a local and/or remote file */
    private void removeFile(SyncDb.DbFile dbfile, SQLiteDatabase db)
            throws SQLException, IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "removeFile %s", dbfile);
        if (dbfile.itsLocalFile != null) {
            itsContext.deleteFile(dbfile.itsLocalFile);
        }

        if (!dbfile.itsIsRemoteDeleted) {
            itsDrive.files().trash(dbfile.itsRemoteId).execute();
        }

        itsSyncDb.removeFile(dbfile.itsId, db);
    }


    /** Merge a local and remote file */
    /*
    void mergeFile(File file,
                   long localId,
                   String localTitle,
                   long localModDate)
        throws SQLException, IOException
    {
        String fileId = file.getId();
        long fileModDate = file.getModifiedDate().getValue();
        // TODO: moving file to different folder doesn't update modDate

        PasswdSafeUtil.dbginfo(
            TAG, "mergeFile %s, title %s, file date %d, local date %d",
            fileId, file.getTitle(), fileModDate, localModDate);
        if (fileModDate > localModDate) {
            PasswdSafeUtil.dbginfo(TAG, "mergeFile update local %s", fileId);
            String localFileName = downloadFile(file);
            itsSyncDb.updateFile(localId, localFileName,
                                 file.getTitle(), fileModDate);
        } else if (localModDate > fileModDate) {
            // TODO: update remote file
            PasswdSafeUtil.dbginfo(TAG, "mergeFile update remote %s", fileId);
            File updateFile = (File)file.clone();
            updateFile.setTitle(localTitle);
            File updatedFile = itsDrive.files().update(fileId, file).execute();
            itsSyncDb.updateFile(localId, null, updatedFile.getTitle(),
                                 updatedFile.getModifiedDate().getValue());
        }
    }
    */


    /** Download a file */
    private boolean downloadFile(File file, String localFileName)
    {
        String url = file.getDownloadUrl();
        if ((url == null) || (url.length() <= 0)) {
            return false;
        }

        PasswdSafeUtil.dbginfo(TAG, "downloadFile %s from %s",
                               file.getId(), url);
        try {
            GenericUrl downloadUrl = new GenericUrl(url);
            OutputStream os = null;
            try {
                os = new BufferedOutputStream(
                    itsContext.openFileOutput(localFileName,
                                              Context.MODE_PRIVATE));
                Drive.Files.Get get = itsDrive.files().get(file.getId());
                MediaHttpDownloader dl = get.getMediaHttpDownloader();
                dl.setDirectDownloadEnabled(true);
                dl.download(downloadUrl, os);
            } finally {
                if (os != null) {
                    os.close();
                }
            }

            java.io.File localFile =
                    itsContext.getFileStreamPath(localFileName);
            localFile.setLastModified(file.getModifiedDate().getValue());
        } catch (IOException e) {
            itsContext.deleteFile(localFileName);
            Log.e(TAG, "Sync failed to download " + file.getTitle(), e);
            return false;
        }
        return true;
    }

    /** Should the file be synced */
    private static boolean isSyncFile(File file)
    {
        if (file.getLabels().getTrashed()) {
            return false;
        }
        String ext = file.getFileExtension();
        return (ext != null) && ext.equals("psafe3");
    }

    /**
     * Retrieve a authorized service object to send requests to the Google Drive
     * API. On failure to retrieve an access token, a notification is sent to
     * the user requesting that authorization be granted for the
     * {@code https://www.googleapis.com/auth/drive} scope.
     *
     * @return An authorized service object.
     */
    private Pair<Drive, String> getDriveService()
    {
        Drive drive = null;
        String token = null;
        try {
            GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(itsContext,
                                                    DriveScopes.DRIVE);
            credential.setSelectedAccountName(itsAccount.name);

            token = GoogleAuthUtil.getTokenWithNotification(
                itsContext, itsAccount.name, credential.getScope(),
                null, PasswdSafeContract.AUTHORITY, null);

            drive = new Drive.Builder(AndroidHttp.newCompatibleTransport(),
                                      new GsonFactory(), credential).build();
        } catch (UserRecoverableNotifiedException e) {
            // User notified
            PasswdSafeUtil.dbginfo(TAG, e, "User notified auth exception");
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
        return new Pair<Drive, String>(drive, token);
    }
}
