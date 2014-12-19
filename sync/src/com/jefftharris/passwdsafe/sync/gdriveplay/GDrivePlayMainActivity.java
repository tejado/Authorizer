/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.gdriveplay;

import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MenuItemCompat;
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
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.OpenFileActivityBuilder;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.sync.R;

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
    private static final int OPEN_RC = 2;

    private String itsAcctId;
    private String itsDisplay;
    private GoogleApiClient itsClient;

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle args)
    {
        super.onCreate(args);
        setContentView(R.layout.activity_gdrive_play_main);

        Uri uri = getIntent().getParcelableExtra(INTENT_PROVIDER_URI);
        itsAcctId = getIntent().getStringExtra(INTENT_PROVIDER_ACCT);
        itsDisplay = getIntent().getStringExtra(INTENT_PROVIDER_DISPLAY);
        if ((uri == null) || (itsAcctId == null) || (itsDisplay == null)) {
            Log.e(TAG, "Required args missing");
            finish();
            return;
        }
        if (args == null) {
            /*
            Fragment files = FilesFragment.newInstance(uri);
            FragmentManager mgr = getSupportFragmentManager();
            mgr.beginTransaction().add(R.id.content, files).commit();
            */
        }

        itsClient = GDrivePlayProvider.createClient(this, itsAcctId,
                                                    this, this);
        updateConnected();
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
        case OPEN_RC: {
            if (resultCode == RESULT_OK) {
                DriveId driveId = (DriveId) data.getParcelableExtra(
                        OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                PasswdSafeUtil.dbginfo(TAG, "open file id: %s", driveId);
            }
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
                            PasswdSafeUtil.dbginfo(TAG, "sync done: %s",
                                                   status);
                        }
                    });

            // TODO: temp
            IntentSender sender =
                    Drive.DriveApi.newOpenFileActivityBuilder()
                    .build(itsClient);
            try {
                startIntentSenderForResult(sender, OPEN_RC, null, 0, 0, 0);
            } catch (SendIntentException e) {
                PasswdSafeUtil.showFatalMsg(e, this);
            }
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
        updateConnected();
    }

    /* (non-Javadoc)
     * @see com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks#onConnectionSuspended(int)
     */
    @Override
    public void onConnectionSuspended(int cause)
    {
        PasswdSafeUtil.dbginfo(TAG, "onConnectionSuspended %d", cause);
        updateConnected();
    }

    /** Update the connected state of the GDrive client */
    private void updateConnected()
    {
        GuiUtils.invalidateOptionsMenu(this);
    }
}
