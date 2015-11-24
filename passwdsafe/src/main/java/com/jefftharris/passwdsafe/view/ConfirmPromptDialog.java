/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.view;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.jefftharris.passwdsafe.R;

/**
 * Dialog to confirm a prompt
 */
public class ConfirmPromptDialog extends AppCompatDialogFragment
    implements CompoundButton.OnCheckedChangeListener,
               DialogInterface.OnClickListener
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
    {
        /** Handle when a prompt was confirmed */
        void promptConfirmed(Bundle confirmArgs);
    }

    private CheckBox itsConfirmCb;
    private AlertDialog itsDialog;
    private Listener itsListener;

    /**
     * Create a new instance
     */
    public static ConfirmPromptDialog newInstance(String prompt,
                                                  String confirm,
                                                  Bundle confirmArgs)
    {
        ConfirmPromptDialog dialog = new ConfirmPromptDialog();
        Bundle args = new Bundle();
        args.putString("prompt", prompt);
        args.putString("confirm", confirm);
        args.putBundle("confirmArgs", confirmArgs);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Bundle args = getArguments();
        String promptStr = args.getString("prompt");
        String confirmStr = args.getString("confirm");
        if (TextUtils.isEmpty(confirmStr)) {
            confirmStr = getString(R.string.ok);
        }

        Context ctx = getContext();
        LayoutInflater factory = LayoutInflater.from(ctx);
        @SuppressLint("InflateParams")
        View dlgView = factory.inflate(R.layout.confirm_prompt, null);

        itsConfirmCb = (CheckBox)dlgView.findViewById(R.id.confirm);
        itsConfirmCb.setOnCheckedChangeListener(this);

        AlertDialog.Builder alert = new AlertDialog.Builder(ctx)
            .setTitle(promptStr)
            .setView(dlgView)
            .setPositiveButton(confirmStr, this)
            .setNegativeButton(R.string.cancel, null);
        itsDialog = alert.create();
        return itsDialog;
    }

    @Override
    public void onAttach(Context ctx)
    {
        super.onAttach(ctx);
        itsListener = (Listener)ctx;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        validate();
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        itsListener = null;
    }

    @Override
    public void onClick(DialogInterface dialog, int which)
    {
        if ((which == AlertDialog.BUTTON_POSITIVE) && (itsListener != null)) {
            Bundle confirmArgs = getArguments().getBundle("confirmArgs");
            itsListener.promptConfirmed(confirmArgs);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        validate();
    }

    /**
     * Validate the prompt
     */
    private void validate()
    {
        Button okBtn = itsDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        okBtn.setEnabled(itsConfirmCb.isChecked());
    }
}
