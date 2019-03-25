/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.view;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import net.tjado.passwdsafe.R;

/**
 * Dialog to confirm a prompt
 */
public class ConfirmPromptDialog extends AppCompatDialogFragment
    implements CompoundButton.OnCheckedChangeListener,
               DialogInterface.OnCancelListener,
               DialogInterface.OnClickListener
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
    {
        /** Handle when a prompt was confirmed */
        void promptConfirmed(Bundle confirmArgs);

        /** Handle when a prompt was canceled */
        void promptCanceled();
    }

    private CheckBox itsConfirmCb;
    private AlertDialog itsDialog;
    private Listener itsListener;

    /**
     * Create a new instance
     */
    public static ConfirmPromptDialog newInstance(String title,
                                                  String prompt,
                                                  String confirm,
                                                  Bundle confirmArgs)
    {
        ConfirmPromptDialog dialog = new ConfirmPromptDialog();
        Bundle args = new Bundle();
        args.putString("title", title);
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
        String titleStr = args.getString("title");
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

        setCancelable(true);
        AlertDialog.Builder alert = new AlertDialog.Builder(ctx)
            .setTitle(titleStr)
            .setMessage(promptStr)
            .setView(dlgView)
            .setPositiveButton(confirmStr, this)
            .setNegativeButton(R.string.cancel, this);
        itsDialog = alert.create();
        return itsDialog;
    }

    @Override
    public void onAttach(Context ctx)
    {
        super.onAttach(ctx);
        Fragment frag = getTargetFragment();
        if (frag != null) {
            itsListener = (Listener)frag;
        } else {
            itsListener = (Listener)ctx;
        }
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
        if (itsListener == null) {
            return;
        }

        Bundle confirmArgs = getArguments().getBundle("confirmArgs");
        switch (which) {
        case AlertDialog.BUTTON_POSITIVE: {
            itsListener.promptConfirmed(confirmArgs);
            break;
        }
        case AlertDialog.BUTTON_NEGATIVE: {
            itsListener.promptCanceled();
            break;
        }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        validate();
    }

    @Override
    public void onCancel(DialogInterface dialog)
    {
        super.onCancel(dialog);
        if (itsListener != null) {
            itsListener.promptCanceled();
        }
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
