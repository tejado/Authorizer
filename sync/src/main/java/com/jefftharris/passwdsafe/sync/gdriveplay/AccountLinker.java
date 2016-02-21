/**
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.gdriveplay;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.R;

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
        RESOLVING_CONNECTION_ERROR,
        CONNECTED,
        FINISHED
    }

    private final FragmentActivity itsActivity;
    private final int itsRequestCode;
    private final Context itsContext;
    private AccountLinker.State itsState = State.CHOOSING_ACCOUNT;
    private GoogleApiClient itsClient;
    private String itsAcctName = null;

    /** Constructor */
    public AccountLinker(FragmentActivity act, int requestCode, Context ctx)
    {
        itsActivity = act;
        itsRequestCode = requestCode;
        itsContext = ctx;
        evalState();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result)
    {
        PasswdSafeUtil.dbginfo(TAG, "onConnectionFailed resolution %b, %s",
                               result.hasResolution(), result);
        switch (itsState) {
        case CONNECTING: {
            if (result.hasResolution()) {
                try {
                    itsState = State.RESOLVING_CONNECTION_ERROR;
                    result.startResolutionForResult(itsActivity,
                                                    itsRequestCode);
                } catch (SendIntentException e) {
                    Log.e(TAG, "Error starting resolution", e);
                    itsState = State.FINISHED;
                    sendActivityResult(Activity.RESULT_CANCELED);
                }
            } else {
                itsState = State.RESOLVING_CONNECTION_ERROR;

                Bundle args = new Bundle();
                args.putInt(ErrorDialogFragment.ERROR_CODE,
                            result.getErrorCode());
                args.putInt(ErrorDialogFragment.REQUEST_CODE, itsRequestCode);
                DialogFragment dlgFrag = new ErrorDialogFragment();
                dlgFrag.setArguments(args);
                dlgFrag.show(itsActivity.getSupportFragmentManager(),
                             "errorDialog");
            }
            break;
        }
        case CHOOSING_ACCOUNT:
        case RESOLVING_CONNECTION_ERROR:
        case CONNECTED:
        case FINISHED: {
            break;
        }
        }
    }

    @Override
    public void onConnected(Bundle hint)
    {
        PasswdSafeUtil.dbginfo(TAG, "onConnected hint %s, acct %s",
                               hint, itsAcctName);
        itsState = State.CONNECTED;
        sendActivityResult(Activity.RESULT_OK);
    }

    @Override
    public void onConnectionSuspended(int cause)
    {
        PasswdSafeUtil.dbginfo(TAG, "onConnectionSuspended cause %d", cause);
    }

    /** Handle an activity result to further the state of the linker */
    public Pair<Boolean, String>
    handleActivityResult(int activityResult, Intent activityData)
    {
        PasswdSafeUtil.dbginfo(
                TAG, "handleActivityResult state %s rc %d, data %s",
                itsState, activityResult, activityData);
        boolean finished = true;
        String connectedAcctName = null;
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
        case CONNECTING:
        case RESOLVING_CONNECTION_ERROR: {
            if (activityResult == Activity.RESULT_OK) {
                itsState = State.CONNECTING;
                finished = false;
                evalState();
            } else {
                itsState = State.FINISHED;
            }
            break;
        }
        case CONNECTED: {
            connectedAcctName = itsAcctName;
            break;
        }
        case FINISHED: {
            break;
        }
        }
        return new Pair<>(finished, connectedAcctName);
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
                String msg = itsContext.getString(
                        R.string.google_acct_not_available);
                Log.e(TAG, msg, e);
                PasswdSafeUtil.showErrorMsg(msg, itsActivity);
            }
            break;
        }
        case CONNECTING: {
            if (itsClient == null) {
                itsClient = GDrivePlayProvider.createClient(
                        itsContext, itsAcctName, this, this);
            }
            if (!itsClient.isConnecting() && !itsClient.isConnected()) {
                itsClient.connect();
            }
            break;
        }
        case RESOLVING_CONNECTION_ERROR:
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
        sendActivityResult(itsActivity, itsRequestCode, rc);
    }

    /**
     * Send a result event to the given activity
     */
    private static void sendActivityResult(Activity act, int requestCode,
                                           int resultCode)
    {
        PendingIntent intent = act.createPendingResult(
                requestCode, new Intent(), PendingIntent.FLAG_ONE_SHOT);
        try {
            intent.send(resultCode);
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "intent send failed", e);
        }
    }

    /**
     * Dialog fragment for showing a connection error
     */
    public static class ErrorDialogFragment extends DialogFragment
    {
        public static final String ERROR_CODE = "errorCode";
        public static final String REQUEST_CODE = "requestCode";

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            Bundle args = getArguments();
            int errorCode = args.getInt(ERROR_CODE);
            int requestCode = args.getInt(REQUEST_CODE);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    getActivity(), errorCode, requestCode);
        }

        @Override
        public void onDismiss(DialogInterface dialog)
        {
            Bundle args = getArguments();
            int requestCode = args.getInt(REQUEST_CODE);
            sendActivityResult(getActivity(), requestCode,
                               Activity.RESULT_CANCELED);
        }
    }
}
