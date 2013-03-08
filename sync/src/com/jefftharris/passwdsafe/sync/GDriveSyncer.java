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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
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
    private final SyncDb itsSyncDb;

    /** Constructor */
    public GDriveSyncer(Context context,
                        ContentProviderClient provider,
                        Account account)
    {
        itsContext = context;
        itsProvider = provider;
        itsAccount = account;
        itsDrive = getDriveService();
        itsSyncDb = new SyncDb(itsContext);
        Log.i(TAG, "GDriveSyncer");
    }


    /** Add a provider for an account */
    public static void addProvider(Account account, SyncDb db)
        throws SQLException
    {
        Log.i(TAG, "Add provider: " + account);
        db.addProvider(account.name);
    }


    /** Delete the provider for the account */
    public static void deleteProvider(Account account, SyncDb db, Context ctx)
        throws SQLException
    {
        Log.i(TAG, "Delete provider: " + account);
        List<String> localFilesToRemove = new ArrayList<String>();
        Cursor cursor = db.getFiles(account.name);
        try {
            for (boolean more = cursor.moveToFirst(); more;
                more = cursor.moveToNext()) {
                String fileId = cursor.getString(1);
                localFilesToRemove.add(fileId);
            }
        } finally {
            cursor.close();
        }

        for (String fileId: localFilesToRemove) {
            ctx.deleteFile(fileId);
        }
        db.deleteProvider(account.name);
    }


    /** Perform synchronization */
    public void performSync()
    {
        if (itsDrive == null) {
            return;
        }

        Log.i(TAG, "Performing sync for " + itsAccount.name);
        try {
            long changeId = itsSyncDb.getProviderSyncChange(itsAccount.name);
            Log.i(TAG, "largest change " + changeId);
            long newChangeId = -1;
            if (changeId == -1) {
                newChangeId = performFullSync();
            } else {
                // TODO: use same db txn for whole sync
                newChangeId = performSyncSince(changeId);
            }
            if (changeId != newChangeId) {
                itsSyncDb.setProviderSyncChange(itsAccount.name, newChangeId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Sync error", e);
        }
        Log.i(TAG, "Sync finished for " + itsAccount.name);
    }

    /** Close the syncer */
    public void close()
    {
        itsSyncDb.close();
    }

    /** Perform a full sync of the files */
    private long performFullSync()
        throws SQLException, IOException
    {
        Log.i(TAG, "Perform full sync");
        About about = itsDrive.about().get()
            .setFields(ABOUT_CHANGE_ID).execute();
        long largestChangeId = about.getLargestChangeId();

        // TODO: filter on mime types
        // TODO: .dat files?
        // TODO: only get needed fields
        HashMap<String, File> allFiles = new HashMap<String, File>();
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
                //Log.i(TAG, "File: " + file);
                allFiles.put(file.getId(), file);
            }
            request.setPageToken(files.getNextPageToken());
        } while((request.getPageToken() != null) &&
                (request.getPageToken().length() > 0));

        HashMap<Long, String> localFilesToRemove = new HashMap<Long, String>();
        Cursor cursor = itsSyncDb.getFiles(itsAccount.name);
        try {
            for (boolean more = cursor.moveToFirst(); more;
                 more = cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String fileId = cursor.getString(1);
                File file = allFiles.get(fileId);
                if (file != null) {
                    String title = cursor.getString(2);
                    long modDate = cursor.getLong(3);
                    mergeFile(file, id, title, modDate);
                    allFiles.remove(fileId);
                } else {
                    PasswdSafeUtil.dbginfo(TAG,
                                           "performFullSync remove local %s",
                                           fileId);
                    localFilesToRemove.put(id, fileId);
                }
            }
        } finally {
            cursor.close();
        }

        for (String fileId: localFilesToRemove.values()) {
            itsContext.deleteFile(fileId);
        }
        itsSyncDb.removeFiles(localFilesToRemove.keySet());
        insertNewDriveFiles(allFiles.values());

        return largestChangeId;
    }

    /** Perform a sync of files since the given change id */
    private long performSyncSince(long changeId)
        throws SQLException, IOException
    {
        // TODO: dbginfo in file
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

        HashMap<Long, String> localFilesToRemove = new HashMap<Long, String>();
        Cursor cursor = itsSyncDb.getFiles(itsAccount.name);
        try {
            for (boolean more = cursor.moveToFirst(); more;
                 more = cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String fileId = cursor.getString(1);
                String title = cursor.getString(2);
                long modDate = cursor.getLong(3);

                if (changedFiles.containsKey(fileId)) {
                    File file = changedFiles.get(fileId);
                    if (file != null) {
                        mergeFile(file, id, title, modDate);
                    } else {
                        PasswdSafeUtil.dbginfo(TAG,
                                               "performSyncSince remove local %s",
                                               fileId);
                        localFilesToRemove.put(id, fileId);
                    }
                    changedFiles.remove(fileId);
                } else {
                    String localFileName = cursor.getString(4);
                    java.io.File localFile =
                        itsContext.getFileStreamPath(fileId);
                    if ((localFileName == null) || (!localFile.isFile())) {
                        File file = itsDrive.files().get(fileId).execute();
                        modDate = -1;
                        mergeFile(file, id, title, modDate);
                    }
                    // TODO: Update remote from local changes??
                    // TODO: delete remote files if local deleted
                }

            }
        } finally {
            cursor.close();
        }

        for (String fileId: localFilesToRemove.values()) {
            itsContext.deleteFile(fileId);
        }
        itsSyncDb.removeFiles(localFilesToRemove.keySet());
        insertNewDriveFiles(changedFiles.values());

        return changeId;
    }


    /** Merge a local and remote file */
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


    /** Insert new files from the drive */
    void insertNewDriveFiles(Collection<File> files)
        throws SQLException, IOException
    {
        for (File file: files) {
            if (file == null) {
                continue;
            }
            String fileId = file.getId();
            PasswdSafeUtil.dbginfo(TAG, "insertNewDriveFiles %s", fileId);
            String localFileName = downloadFile(file);
            itsSyncDb.addFile(itsAccount.name, localFileName,
                              fileId, file.getTitle(),
                              file.getModifiedDate().getValue());
        }
    }


    /** Download a file */
    String downloadFile(File file)
    {
        String url = file.getDownloadUrl();
        if ((url == null) || (url.length() <= 0)) {
            return null;
        }

        PasswdSafeUtil.dbginfo(TAG, "downloadFile %s from %s",
                               file.getId(), url);
        String localFileName = null;
        try {
            GenericUrl downloadUrl = new GenericUrl(url);
            OutputStream os = null;
            try {
                os = new BufferedOutputStream(
                    itsContext.openFileOutput(file.getId(),
                                              Context.MODE_PRIVATE));
                Drive.Files.Get get = itsDrive.files().get(file.getId());
                MediaHttpDownloader dl = get.getMediaHttpDownloader();
                // TODO: get listener to work?
                /*
                dl.setProgressListener(new MediaHttpDownloaderProgressListener()
                    {
                        @Override
                        public void progressChanged(MediaHttpDownloader dl)
                            throws IOException
                        {
                            switch (dl.getDownloadState()) {
                            case NOT_STARTED: {
                                PasswdSafeUtil.dbginfo(TAG,
                                                       "downloadFile not start");
                                break;
                            }
                            case MEDIA_IN_PROGRESS: {
                                PasswdSafeUtil.dbginfo(TAG,
                                                       "downloadFile %s",
                                                       dl.getProgress());
                                break;
                            }
                            case MEDIA_COMPLETE: {
                                PasswdSafeUtil.dbginfo(TAG,
                                                       "downloadFile complete");
                                break;
                            }
                            }
                        }
                    });
                    */
                dl.setDirectDownloadEnabled(true);
                dl.download(downloadUrl, os);
            } finally {
                if (os != null) {
                    os.close();
                }
            }

            java.io.File localFile = itsContext.getFileStreamPath(file.getId());
            localFile.setLastModified(file.getModifiedDate().getValue());
            localFileName = localFile.getAbsolutePath();
        } catch (IOException e) {
            itsContext.deleteFile(file.getId());
            Log.e(TAG, "Sync failed to download " + file.getTitle(), e);
        }
        return localFileName;
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
    private Drive getDriveService()
    {
        Drive drive = null;
        try {
            GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(itsContext,
                                                    DriveScopes.DRIVE);
          credential.setSelectedAccountName(itsAccount.name);
          // Trying to get a token right away to see if we are authorized
          credential.getToken();
          drive = new Drive.Builder(AndroidHttp.newCompatibleTransport(),
                                    new GsonFactory(), credential).build();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get token");
            // If the Exception is User Recoverable, we display a notification
            // that will trigger the intent to fix the issue.
            if (e instanceof UserRecoverableAuthException) {
                UserRecoverableAuthException exception =
                    (UserRecoverableAuthException) e;
                NotificationManager notificationManager = (NotificationManager)
                    itsContext.getSystemService(Context.NOTIFICATION_SERVICE);
                Intent authorizationIntent = exception.getIntent();
                authorizationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_FROM_BACKGROUND);
                PendingIntent pendingIntent =
                    PendingIntent.getActivity(itsContext, 0,
                                              authorizationIntent, 0);
                // TODO: resource strs
                Notification notification = new NotificationCompat.Builder(itsContext)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setTicker("Permission requested")
                    .setContentTitle("Permission requested")
                    .setContentText("for account " + itsAccount.name)
                    .setContentIntent(pendingIntent).setAutoCancel(true).build();
                notificationManager.notify(0, notification);
            } else {
                e.printStackTrace();
            }
        }
        return drive;
    }
}
