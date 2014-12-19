/**
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.gdriveplay;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.sync.R;
import com.jefftharris.passwdsafe.sync.lib.NewAccountTask;

/**
 * The AccountLinker class manages a new connection to a GDrive account
 */
public class AccountLinker
    implements GoogleApiClient.ConnectionCallbacks,
               GoogleApiClient.OnConnectionFailedListener
{
    private static final String TAG = "AccountLinker";

    private enum State
    {
        CHOOSING_ACCOUNT,
        CONNECTING,
        CONNECTED,
        FINISHED
    }

    private final Activity itsActivity;
    private final int itsRequestCode;
    private AccountLinker.State itsState = State.CHOOSING_ACCOUNT;
    private GoogleApiClient itsClient;
    private String itsAcctName = null;

    /** Constructor */
    public AccountLinker(Activity act, int requestCode)
    {
        itsActivity = act;
        itsRequestCode = requestCode;
        evalState();
    }

    /* (non-Javadoc)
     * @see com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener#onConnectionFailed(com.google.android.gms.common.ConnectionResult)
     */
    @Override
    public void onConnectionFailed(ConnectionResult result)
    {
        PasswdSafeUtil.dbginfo(TAG, "onConnectionFailed resolution %b, %s",
                               result.hasResolution(), result);
        if (!result.hasResolution()) {
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(),
                                                  itsActivity, 0).show();
            itsState = State.FINISHED;
            sendActivityResult(Activity.RESULT_CANCELED);
            return;
        }
        try {
            result.startResolutionForResult(itsActivity, itsRequestCode);
        } catch (SendIntentException e) {
            Log.e(TAG, "Error starting resolution", e);
            itsState = State.FINISHED;
            sendActivityResult(Activity.RESULT_CANCELED);
        }
    }

    /* (non-Javadoc)
     * @see com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks#onConnected(android.os.Bundle)
     */
    @Override
    public void onConnected(Bundle hint)
    {
        PasswdSafeUtil.dbginfo(TAG, "onConnected hint %s, acct %s",
                               hint, itsAcctName);
        itsState = State.CONNECTED;
        sendActivityResult(Activity.RESULT_OK);
    }

    /* (non-Javadoc)
     * @see com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks#onConnectionSuspended(int)
     */
    @Override
    public void onConnectionSuspended(int cause)
    {
        PasswdSafeUtil.dbginfo(TAG, "onConnectionSuspended cause %d", cause);
    }

    /** Handle an activity result to further the state of the linker */
    public Pair<Boolean, NewAccountTask>
    handleActivityResult(int activityResult,
                         Intent activityData,
                         Uri providerAcctUri)
    {
        PasswdSafeUtil.dbginfo(
                TAG, "handleActivityResult state %s rc %d, data %s",
                itsState, activityResult, activityData);
        boolean finished = true;
        NewAccountTask task = null;
        switch (itsState) {
        case CHOOSING_ACCOUNT: {
            if (activityData == null) {
                itsState = State.FINISHED;
                break;
            }

            Bundle b = activityData.getExtras();
            String acctName = b.getString(AccountManager.KEY_ACCOUNT_NAME);
            PasswdSafeUtil.dbginfo(TAG, "Selected account: %s", acctName);
            if (TextUtils.isEmpty(acctName)) {
                itsState = State.FINISHED;
                break;
            }
            itsAcctName = acctName;
            itsState = State.CONNECTING;
            finished = false;
            evalState();
            break;
        }
        case CONNECTING: {
            if (activityResult == Activity.RESULT_OK) {
                finished = false;
                evalState();
            } else {
                itsState = State.FINISHED;
            }
            break;
        }
        case CONNECTED: {
            // TODO: use updated play services and acct id for unique value
            // with name as display name
            task = new NewAccountTask(providerAcctUri, itsAcctName,
                                      ProviderType.GDRIVE_PLAY,
                                      itsActivity);
            break;
        }
        case FINISHED: {
            break;
        }
        }
        return new Pair<Boolean, NewAccountTask>(finished, task);
    }

    /** Disconnect the linker and shut down */
    public void disconnect()
    {
        if (itsClient != null) {
            itsClient.disconnect();
        }
        itsState = State.FINISHED;
    }

    /** Evaluate the state of the linker */
    private void evalState()
    {
        switch (itsState) {
        case CHOOSING_ACCOUNT: {
            Intent intent = AccountPicker.newChooseAccountIntent(
                    null, null,
                    new String[] { GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE },
                    true, null, null, null, null);
            try {
                itsActivity.startActivityForResult(intent, itsRequestCode);
            } catch (ActivityNotFoundException e) {
                String msg = itsActivity.getString(
                        R.string.google_acct_not_available);
                Log.e(TAG, msg, e);
                PasswdSafeUtil.showErrorMsg(msg, itsActivity);
            }
            break;
        }
        case CONNECTING: {
            if (itsClient == null) {
                itsClient = GDrivePlayProvider.createClient(itsActivity, itsAcctName,
                                         this, this);
            }
            itsClient.connect();
            break;
        }
        case CONNECTED:
        case FINISHED: {
            break;
        }
        }
    }

    /**
     * Send an event to the activity to call back to the linker with a result
     * code
     */
    private void sendActivityResult(int rc)
    {
        PendingIntent intent = itsActivity.createPendingResult(
                itsRequestCode, new Intent(), PendingIntent.FLAG_ONE_SHOT);
        try {
            intent.send(rc);
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "intent send failed", e);
        }
    }
}