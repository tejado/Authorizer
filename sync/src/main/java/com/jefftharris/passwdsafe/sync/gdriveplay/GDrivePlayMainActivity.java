/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.gdriveplay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.OpenFileActivityBuilder;
import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.sync.R;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;

/**
 *  Activity for accessing Google Drive
 */
public class GDrivePlayMainActivity extends FragmentActivity
    implements FilesFragment.Listener,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener
{
    public static final String INTENT_PROVIDER_URI = "provider_uri";
    public static final String INTENT_PROVIDER_ACCT = "provider_acct";
    public static final String INTENT_PROVIDER_DISPLAY = "provider_display";

    private static final String TAG = "GDrivePlayMainActivity";
    private static final int GDRIVE_RESOLUTION_RC = 1;
    private static final int ADD_FILE_RC = 2;

    private Uri itsProviderUri;
    private GoogleApiClient itsClient;
    private List<DriveId> itsAddFiles = new ArrayList<>();

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle args)
    {
        super.onCreate(args);
        setContentView(R.layout.activity_gdrive_play_main);

        itsProviderUri = getIntent().getParcelableExtra(INTENT_PROVIDER_URI);
        String acctId = getIntent().getStringExtra(INTENT_PROVIDER_ACCT);
        String display = getIntent().getStringExtra(INTENT_PROVIDER_DISPLAY);
        if ((itsProviderUri == null) || (acctId == null) ||
                (display == null)) {
            Log.e(TAG, "Required args missing");
            finish();
            return;
        }

        itsClient = GDrivePlayProvider.createClient(this, acctId,
                                                    this, this);

        if (args == null) {
            Fragment files = FilesFragment.newInstance(itsProviderUri);
            FragmentManager mgr = getSupportFragmentManager();
            mgr.beginTransaction().add(R.id.content, files).commit();
        }

        updateConnected(false);
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onStart()
     */
    @Override
    protected void onStart()
    {
        super.onStart();
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        if (itsClient != null) {
            itsClient.connect();
        }
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        PasswdSafeUtil.dbginfo(TAG, "onActivityResult req %d res %d data %s",
                               requestCode, resultCode, data);
        switch (requestCode) {
        case GDRIVE_RESOLUTION_RC: {
            if (resultCode == RESULT_OK) {
                itsClient.connect();
            } else {
                finish();
            }
            break;
        }
        case ADD_FILE_RC: {
            if (resultCode != RESULT_OK) {
                break;
            }
            DriveId driveId = data.getParcelableExtra(
                    OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
            PasswdSafeUtil.dbginfo(TAG, "open file id: %s", driveId);
            itsAddFiles.add(driveId);
        }
        }
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onPause()
     */
    @Override
    protected void onPause()
    {
        if (itsClient != null) {
            itsClient.disconnect();
        }
        super.onPause();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_gdrive_play_main, menu);

        MenuItem item = menu.findItem(R.id.menu_sync);
        MenuItemCompat.setShowAsAction(item,
                                       MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        MenuItem item = menu.findItem(R.id.menu_sync);
        item.setEnabled(itsClient.isConnected());

        return super.onPrepareOptionsMenu(menu);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_sync: {
            PasswdSafeUtil.dbginfo(TAG, "onOptionsItemSelected start sync");
            PendingResult<Status> rc = Drive.DriveApi.requestSync(itsClient);
            rc.setResultCallback(new ResultCallback<Status>()
                    {
                        @Override
                        public void onResult(Status status)
                        {
                            // TODO play: user notification?
                            PasswdSafeUtil.dbginfo(TAG, "sync done: %s",
                                                   status);
                        }
                    });
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    /* (non-Javadoc)
     * @see com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener#onConnectionFailed(com.google.android.gms.common.ConnectionResult)
     */
    @Override
    public void onConnectionFailed(ConnectionResult result)
    {
        PasswdSafeUtil.dbginfo(TAG, "onConnectionFailed %s", result);
        if (!result.hasResolution()) {
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(),
                                                  this, 0).show();
            return;
        }
        try {
            result.startResolutionForResult(this, GDRIVE_RESOLUTION_RC);
        } catch (SendIntentException e) {
            Log.e(TAG, "Error starting resolution", e);
        }
    }

    /* (non-Javadoc)
     * @see com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks#onConnected(android.os.Bundle)
     */
    @Override
    public void onConnected(Bundle hint)
    {
        PasswdSafeUtil.dbginfo(TAG, "onConnected %s", hint);
        updateConnected(true);
    }

    /* (non-Javadoc)
     * @see com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks#onConnectionSuspended(int)
     */
    @Override
    public void onConnectionSuspended(int cause)
    {
        PasswdSafeUtil.dbginfo(TAG, "onConnectionSuspended %d", cause);
        updateConnected(false);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.gdriveplay.FilesFragment.Listener#addFile()
     */
    public void addFile()
    {
        IntentSender sender = Drive.DriveApi.newOpenFileActivityBuilder()
                .setMimeType(new String[] { "application/octet-stream",
                                            "application/psafe3" })
                .build(itsClient);
        try {
            startIntentSenderForResult(sender, ADD_FILE_RC, null, 0, 0, 0);
        } catch (SendIntentException e) {
            PasswdSafeUtil.showFatalMsg(e, this);
        }
    }

    /** Update the connected state of the GDrive client */
    private void updateConnected(boolean connected)
    {
        GuiUtils.invalidateOptionsMenu(this);

        if (connected) {
            if (!itsAddFiles.isEmpty()) {
                /* Start task to add any pending new sync files */
                new AddFileTask(itsProviderUri, itsClient, this).execute(
                        itsAddFiles.toArray(new DriveId[itsAddFiles.size()]));
                itsAddFiles.clear();
            }
        }
    }

    /** Background task to add new sync files to the provider */
    private static class AddFileTask extends AsyncTask<DriveId, Void, Object>
    {
        private final Uri itsProviderUri;
        private final GoogleApiClient itsClient;
        private final Activity itsActivity;

        public AddFileTask(Uri providerUri,
                           GoogleApiClient client,
                           Activity act)
        {
            itsProviderUri = providerUri;
            itsClient = client;
            itsActivity = act;
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
         */
        @Override
        protected Object doInBackground(DriveId... params)
        {
            SyncDb syncDb = SyncDb.acquire();
            try {
                SQLiteDatabase db = syncDb.beginTransaction();
                for (DriveId id: params) {
                    DriveFile file = Drive.DriveApi.getFile(itsClient, id);
                    Metadata meta = getFileMeta(file);
                    String title = meta.getTitle();
                    Date modTime = meta.getModifiedDate();
                    String folders = computeFolders(file);

                    PasswdSafeUtil.dbginfo(TAG,
                                           "Add file %s, mod %s, folder %s",
                                           title, modTime, folders);

                    // TODO play: check for duplicates

                    long providerId =
                            PasswdSafeContract.Providers.getId(itsProviderUri);
                    long fileId = SyncDb.addRemoteFile(
                            providerId, id.encodeToString(), title, folders,
                            modTime.getTime(), null, db);
                    SyncDb.updateLocalFile(fileId, null, title, folders,
                            modTime.getTime(), db);
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "Error adding file", e);
                return e;
            } finally {
                syncDb.endTransactionAndRelease();
            }

            itsActivity.getContentResolver().notifyChange(itsProviderUri,
                                                          null, false);
            return null;
        }

        @Override
        protected void onPostExecute(Object rc)
        {
            if (rc instanceof Exception) {
                PasswdSafeUtil.showFatalMsg((Exception)rc, itsActivity);
            }
        }

        /** Compute the folder names for the file */
        private String computeFolders(DriveFile file) throws IOException
        {
            // TODO play: will folders work given app scope?
            ArrayList<String> folders = new ArrayList<>();

            for (DriveId parent: getParents(file)) {
                traceParentRefs(parent, "", folders);
            }

            Collections.sort(folders);
            return TextUtils.join(", ", folders);
        }

        /** Get the parent folder IDs for the file */
        private List<DriveId> getParents(DriveResource file)
            throws IOException
        {
            ArrayList<DriveId> parents = new ArrayList<>();

            PendingResult<DriveApi.MetadataBufferResult> metaPend =
                    file.listParents(itsClient);
            DriveApi.MetadataBufferResult metaRc =
                    metaPend.await(10, TimeUnit.SECONDS);
            if (!metaRc.getStatus().isSuccess()) {
                throw new IOException("Error retrieving parents: " +
                                      metaRc.getStatus().getStatusMessage());
            }
            MetadataBuffer metaBuffer = metaRc.getMetadataBuffer();
            try {
                for (Metadata meta: metaBuffer) {
                    if (meta.isFolder()) {
                        parents.add(meta.getDriveId());
                    }
                }
            } finally {
                metaBuffer.release();
            }
            return parents;
        }

        /** Recursively trace the parent references for their paths */
        private void traceParentRefs(DriveId parentId,
                                     String suffix,
                                     ArrayList<String> folders)
            throws IOException
        {
            DriveFolder parent = Drive.DriveApi.getFolder(itsClient, parentId);
            Metadata meta = getFileMeta(parent);
            List<DriveId> grandparents = getParents(parent);
            if (grandparents.isEmpty()) {
                suffix = meta.getTitle() + suffix;
                folders.add(suffix);
            } else {
                suffix = "/" + meta.getTitle() + suffix;
                for (DriveId grandparent: grandparents) {
                    traceParentRefs(grandparent, suffix, folders);
                }
            }
        }

        /** Get the metadata for a file */
        private Metadata getFileMeta(DriveResource file) throws IOException
        {
            PendingResult<DriveResource.MetadataResult> metaPend =
                    file.getMetadata(itsClient);
            DriveResource.MetadataResult metaRc =
                    metaPend.await(10, TimeUnit.SECONDS);
            if (!metaRc.getStatus().isSuccess()) {
                throw new IOException(
                        "Error retrieving metadata: " +
                        metaRc.getStatus().getStatusMessage());
            }
            return metaRc.getMetadata();
        }
    }
}
