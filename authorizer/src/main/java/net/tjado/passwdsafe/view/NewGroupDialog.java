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
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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
                        EditText newGroup = (EditText)
                                view.findViewById(R.id.new_group);
                        ((Listener)getTargetFragment()).handleNewGroup(
                                newGroup.getText().toString());
                    }

                    @Override
                    public void onCancelClicked()
                    {
                        ((Listener)getTargetFragment()).handleNewGroup(null);
                    }
                };

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(PasswdSafeUtil.getAppTitle(getContext()))
                .setView(view)
                .setPositiveButton(R.string.ok, dlgClick)
                .setNegativeButton(R.string.cancel, dlgClick)
                .setOnCancelListener(dlgClick);
        final AlertDialog alertDialog = builder.create();
        TextView tv = (TextView)view.findViewById(R.id.new_group);
        GuiUtils.setupFormKeyboard(tv, tv, getContext(), new Runnable()
        {
            @Override
            public void run()
            {
                Button btn = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (btn.isEnabled()) {
                    btn.performClick();
                }
            }
        });
        return alertDialog;
    }
}
