/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.gdriveplay;

import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
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

    private static final String TAG = "GDrivePlayMainActivity";
    private static final int REQUEST_CODE_RESOLUTION = 1;

    GoogleApiClient itsClient;
    boolean itsIsUserCanceled = false;

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle args)
    {
        super.onCreate(args);
        setContentView(R.layout.activity_gdrive_play_main);

        if (args == null) {
            Uri uri = getIntent().getParcelableExtra(INTENT_PROVIDER_URI);
            Fragment files = FilesFragment.newInstance(uri);
            FragmentManager mgr = getSupportFragmentManager();
            mgr.beginTransaction().add(R.id.content, files).commit();
        }
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onStart()
     */
    @Override
    protected void onStart()
    {
        super.onStart();
        itsIsUserCanceled = false;
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        if (itsClient == null) {
            itsClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        }
        if (!itsIsUserCanceled) {
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
        PasswdSafeUtil.dbginfo(TAG, "onActivityResult req %d res %d",
                               requestCode, resultCode);
        switch (requestCode) {
        case REQUEST_CODE_RESOLUTION: {
            switch (resultCode) {
            case RESULT_OK: {
                itsClient.connect();
                break;
            }
            default: {
                itsIsUserCanceled = true;
                break;
            }
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
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
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
    }

    /* (non-Javadoc)
     * @see com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks#onConnectionSuspended(int)
     */
    @Override
    public void onConnectionSuspended(int cause)
    {
        PasswdSafeUtil.dbginfo(TAG, "onConnectionSuspended %d", cause);
    }
}
