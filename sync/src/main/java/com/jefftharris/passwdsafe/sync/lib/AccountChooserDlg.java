/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;

import com.jefftharris.passwdsafe.lib.view.AbstractDialogClickListener;
import com.jefftharris.passwdsafe.sync.R;

/**
 * The AccountChooserDlg allows the user to choose an account of a given type
 */
public class AccountChooserDlg extends DialogFragment
{
    private static final String TAG = "AccountChooserDlg";

    /** Create a new instance of the dialog */
    public static AccountChooserDlg newInstance(
            @SuppressWarnings("SameParameterValue") String accountType,
            int requestCode, String noAccountsMsg)
    {
        AccountChooserDlg dialog = new AccountChooserDlg();
        Bundle args = new Bundle();
        args.putString("accountType", accountType);
        args.putInt("requestCode", requestCode);
        args.putString("noAccountsMsg", noAccountsMsg);
        dialog.setArguments(args);
        return dialog;
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
     */
    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Bundle args = getArguments();
        String accountType = args.getString("accountType");

        AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.choose_account);

        AccountManager acctMgr = AccountManager.get(getActivity());
        Account[] accts = acctMgr.getAccountsByType(accountType);
        if (accts.length > 0) {
            final String[] names = new String[accts.length];
            for (int i = 0; i < accts.length; ++i) {
                names[i] = accts[i].name;
            }
            builder.setItems(names, new OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    onAccountSelected(names[which]);
                }
            });
        } else {
            builder.setMessage(args.getString("noAccountsMsg"));
        }

        AbstractDialogClickListener clickListener =
                new AbstractDialogClickListener()
        {
            @Override
            public void onCancelClicked(DialogInterface dialog)
            {
                onAccountSelected(null);
            }
        };
        builder.setNegativeButton(R.string.cancel, clickListener);
        builder.setOnCancelListener(clickListener);

        return builder.create();
    }


    /** Handle a selected account */
    private void onAccountSelected(String accountName)
    {
        Bundle args = getArguments();
        int requestCode = args.getInt("requestCode");

        int result;
        Intent intent = new Intent();
        if (accountName != null) {
            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName);
            result = Activity.RESULT_OK;
        } else {
            result = Activity.RESULT_CANCELED;
        }

        PendingIntent pendIntent = getActivity().createPendingResult(
                requestCode, intent, PendingIntent.FLAG_ONE_SHOT);
        try {
            pendIntent.send(result);
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "intent send failed", e);
        }
    }
}
