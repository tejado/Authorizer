/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import java.io.IOException;

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

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

    /** Constructor */
    public GDriveSyncer(Context context,
                        ContentProviderClient provider,
                        Account account)
    {
        itsContext = context;
        itsProvider = provider;
        itsAccount = account;
        itsDrive = getDriveService();
        Log.i(TAG, "GDriveSyncer");
    }

    /** Perform synchronization */
    public void performSync()
    {
        if (itsDrive == null) {
            return;
        }

        Log.i(TAG, "Performing sync for " + itsAccount.name);
        // TODO: do sync

        performFullSync();
    }


    /** Perform a full sync of the files */
    private void performFullSync()
    {
        long largestChangeId = -1;
        try {
            About about = itsDrive.about().get()
                .setFields(ABOUT_CHANGE_ID).execute();
            largestChangeId = about.getLargestChangeId();
        }
        catch (IOException e) {
            Log.e(TAG, "Failed to get change ID", e);
        }
        Log.i(TAG, "largest change " + largestChangeId);

        try {
            // TODO: filter on mime types
            // TODO: .dat files?
            Files.List request =
                itsDrive.files().list().setQ("not trashed");
            do {
                FileList files = request.execute();
                Log.i(TAG, "num files: " + files.getItems().size());
                for (File file: files.getItems()) {
                    String ext = file.getFileExtension();
                    if ((ext == null) || (!ext.equals("psafe3"))) {
                        continue;
                    }
                    Log.i(TAG, "File id: " + file.getId() + ", title: " +
                          file.getTitle() + ", mime: " + file.getMimeType());
                    //Log.i(TAG, "File: " + file);
                }
                request.setPageToken(files.getNextPageToken());
            } while((request.getPageToken() != null) &&
                    (request.getPageToken().length() > 0));
        }
        catch (IOException e) {
            Log.e(TAG, "Error getting files", e);
        }
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
