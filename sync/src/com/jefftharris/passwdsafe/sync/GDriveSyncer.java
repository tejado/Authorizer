/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 *  The GDriveSyncer class syncs password files from Google Drive
 */
public class GDriveSyncer
{
    private static final String TAG = "GDriveSyncer";

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
        Log.i(TAG, "Performing sync for " + itsAccount.name);
        // TODO: do sync
    }


    /**
     * Retrieve a authorized service object to send requests to the Google Drive
     * API. On failure to retrieve an access token, a notification is sent to the
     * user requesting that authorization be granted for the
     * {@code https://www.googleapis.com/auth/drive.file} scope.
     *
     * @return An authorized service object.
     */
    private Drive getDriveService()
    {
        Drive drive = null;
        try {
            GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(itsContext,
                                                    DriveScopes.DRIVE_FILE);
          credential.setSelectedAccountName(itsAccount.name);
          // Trying to get a token right away to see if we are authorized
          credential.getToken();
          drive = new Drive.Builder(AndroidHttp.newCompatibleTransport(),
                                    new GsonFactory(), credential).build();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get token");
            // If the Exception is User Recoverable, we display a notification that will trigger the
            // intent to fix the issue.
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
