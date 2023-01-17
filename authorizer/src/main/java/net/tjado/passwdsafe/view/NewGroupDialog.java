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
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import net.tjado.passwdsafe.R;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.view.AbstractDialogClickListener;
import net.tjado.passwdsafe.lib.view.GuiUtils;

/**
 * Dialog to select a new group
 */
public class NewGroupDialog extends DialogFragment
{
    /**
     * Listener interface for the owning fragment
     */
    public interface Listener
    {
        void handleNewGroup(String newGroup);
    }

    /**
     * Create a new instance
     */
    public static NewGroupDialog newInstance()
    {
        return new NewGroupDialog();
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
     */
    @Override
    public @NonNull
    Dialog onCreateDialog(Bundle savedInstanceState)
    {
        LayoutInflater factory = LayoutInflater.from(getContext());
        @SuppressLint("InflateParams")
        final View view = factory.inflate(R.layout.new_group, null);
        AbstractDialogClickListener dlgClick =
                new AbstractDialogClickListener()
                {
                    @Override
                    public void onOkClicked(DialogInterface dialog)
                    {
                        EditText newGroup = view.findViewById(R.id.new_group);
                        Listener listener = (Listener)getTargetFragment();
                        if (listener != null) {
                            listener.handleNewGroup(
                                    newGroup.getText().toString());
                        }
                    }

                    @Override
                    public void onCancelClicked()
                    {
                        Listener listener = (Listener)getTargetFragment();
                        if (listener != null) {
                            listener.handleNewGroup(null);
                        }
                    }
                };

        Context ctx = requireContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx)
                .setTitle(PasswdSafeUtil.getAppTitle(ctx))
                .setView(view)
                .setPositiveButton(R.string.ok, dlgClick)
                .setNegativeButton(R.string.cancel, dlgClick)
                .setOnCancelListener(dlgClick);
        final AlertDialog alertDialog = builder.create();
        TextView tv = view.findViewById(R.id.new_group);
        GuiUtils.setupFormKeyboard(tv, tv, getContext(), () -> {
            Button btn = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (btn.isEnabled()) {
                btn.performClick();
            }
        });

        // show keyboard per default
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return alertDialog;
    }
}
