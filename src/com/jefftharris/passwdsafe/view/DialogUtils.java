/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.view;

import com.jefftharris.passwdsafe.AbstractDialogClickListener;
import com.jefftharris.passwdsafe.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * The DialogUtils class provides utility dialogs
 */
public class DialogUtils
{
    /**
     * The DialogData class encapsulates the data returned from a dialog
     * creation method
     */
    public static class DialogData
    {
        public final Dialog itsDialog;
        public final DialogValidator itsValidator;

        public DialogData(Dialog dlg, DialogValidator validator)
        {
            itsDialog = dlg;
            itsValidator = validator;
        }

    }

    /**
     * Create a dialog used as a prompt to delete an item
     */
    public static DialogData createDeletePrompt
    (
        Activity act,
        AbstractDialogClickListener dlgClick,
        String titleStr,
        String promptStr
    )
    {
        LayoutInflater factory = LayoutInflater.from(act);
        View dlgView = factory.inflate(R.layout.delete_prompt, null);

        TextView prompt = (TextView)dlgView.findViewById(R.id.prompt);
        prompt.setText(promptStr);

        AlertDialog.Builder alert = new AlertDialog.Builder(act)
            .setTitle(titleStr)
            .setView(dlgView)
            .setPositiveButton(R.string.ok, dlgClick)
            .setNegativeButton(R.string.cancel, dlgClick)
            .setOnCancelListener(dlgClick);
        AlertDialog alertDialog = alert.create();

        final DialogValidator validator =
            new DialogValidator.AlertValidator(alertDialog, dlgView, act, false)
        {
            @Override
            public final void reset()
            {
                CheckBox confirmCb =
                    (CheckBox)getDialog().findViewById(R.id.confirm);
                confirmCb.setChecked(false);
                super.reset();
            }

            @Override
            protected final String doValidation()
            {
                CheckBox confirmCb =
                    (CheckBox)getDialog().findViewById(R.id.confirm);
                if (!confirmCb.isChecked()) {
                    return getString(R.string.check_box_to_confirm);
                }
                return super.doValidation();
            }
        };

        CheckBox confirmCb = (CheckBox)dlgView.findViewById(R.id.confirm);
        confirmCb.setOnCheckedChangeListener(new OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked)
            {
                validator.validate();
            }
        });

        return new DialogData(alertDialog, validator);
    }
}
