/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.jefftharris.passwdsafe.Preferences;
import com.jefftharris.passwdsafe.R;
import com.jefftharris.passwdsafe.YubikeyMgr;
import com.jefftharris.passwdsafe.file.PasswdFileUri;
import com.jefftharris.passwdsafe.lib.ApiCompat;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.view.AbstractDialogClickListener;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.util.Pair;
import com.jefftharris.passwdsafe.util.YubiState;

/**
 *  The PasswdEntryDialog encapsulates the dialog for entering the password for
 *  opening a file
 */
public class PasswdEntryDialog implements View.OnClickListener
{
    /** Interface for users of the dialog */
    public interface User
    {
        /** Handle the user clicking Ok */
        public void handleOk(StringBuilder password, boolean readonly);

        /** Handle the user clicking cancel */
        public void handleCancel();
    }

    private Activity itsActivity;
    private User itsUser;
    private AlertDialog itsDialog;
    private YubikeyMgr itsYubiMgr = null;
    private YubikeyMgr.User itsYubiUser = null;

    /** Constructor */
    public PasswdEntryDialog(Activity act, User user)
    {
        itsActivity = act;
        itsUser = user;

        if (ApiCompat.SDK_VERSION >= Build.VERSION_CODES.GINGERBREAD_MR1) {
            itsYubiMgr = new YubikeyMgr();
            itsYubiUser = new YubikeyUser();
        }
    }

    /** Create the dialog */
    public Dialog create()
    {
        LayoutInflater factory = LayoutInflater.from(itsActivity);
        View passwdView = factory.inflate(R.layout.passwd_entry, null);
        AbstractDialogClickListener dlgClick =
            new AbstractDialogClickListener()
        {
            @Override
            public void onOkClicked(DialogInterface dialog)
            {
                Dialog d = (Dialog)dialog;
                CheckBox cb = (CheckBox)d.findViewById(R.id.read_only);

                if (itsYubiMgr != null) {
                    itsYubiMgr.stop();
                }

                boolean readonly;
                if (cb.isEnabled()) {
                    readonly = cb.isChecked();
                    SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences(
                                itsActivity);
                    Preferences.setFileOpenReadOnlyPref(readonly, prefs);
                } else {
                    readonly = true;
                }

                EditText passwdInput =
                    (EditText)d.findViewById(R.id.passwd_edit);
                itsUser.handleOk(
                        new StringBuilder(passwdInput.getText().toString()),
                        readonly);
            }

            @Override
            public void onCancelClicked(DialogInterface dialog)
            {
                if (itsYubiMgr != null) {
                    itsYubiMgr.stop();
                }
                itsUser.handleCancel();
            }
        };

        TextView passwordEdit =
                (TextView)passwdView.findViewById(R.id.passwd_edit);
        PasswordVisibilityMenuHandler.set(passwordEdit);
        Spinner slotSpinner = (Spinner)passwdView.findViewById(R.id.yubi_slot);
        slotSpinner.setSelection(1);
        Button yubikey = (Button)passwdView.findViewById(R.id.yubi_start);
        yubikey.setOnClickListener(this);
        setVisibility(R.id.yubi_help_text, false, passwdView);
        View yubihelp = passwdView.findViewById(R.id.yubi_help);
        yubihelp.setOnClickListener(this);


        YubiState state = YubiState.UNAVAILABLE;
        if (itsYubiMgr != null) {
            state = itsYubiMgr.getState(itsActivity);
        }
        switch (state) {
        case UNAVAILABLE: {
            setVisibility(R.id.yubi_disabled, false, passwdView);
            setVisibility(R.id.yubi_start_fields, false, passwdView);
            setVisibility(R.id.yubi_progress_fields, false, passwdView);
            break;
        }
        case DISABLED: {
            setVisibility(R.id.yubi_disabled, true, passwdView);
            setVisibility(R.id.yubi_start_fields, false, passwdView);
            setVisibility(R.id.yubi_progress_fields, false, passwdView);
            break;
        }
        case ENABLED: {
            setVisibility(R.id.yubi_disabled, false, passwdView);
            setVisibility(R.id.yubi_start_fields, true, passwdView);
            setVisibility(R.id.yubi_progress_fields, false, passwdView);
            break;
        }
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(itsActivity)
            .setTitle(R.string.open_file_title)
            .setView(passwdView)
            .setPositiveButton(R.string.ok, dlgClick)
            .setNegativeButton(R.string.cancel, dlgClick)
            .setOnCancelListener(dlgClick);
        itsDialog = alert.create();
        GuiUtils.setupDialogKeyboard(itsDialog, passwordEdit, passwordEdit,
                                     itsActivity);
        return itsDialog;
    }

    /** Reset the dialog */
    public void reset(String name, PasswdFileUri uri)
    {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(itsActivity);
        TextView tv = (TextView)itsDialog.findViewById(R.id.file);
        tv.setText(itsActivity.getString(R.string.file_label_val, name));
        CheckBox cb = (CheckBox)itsDialog.findViewById(R.id.read_only);
        Pair<Boolean, Integer> rc = uri.isWritable();
        if (rc.first) {
            cb.setEnabled(true);
            cb.setChecked(Preferences.getFileOpenReadOnlyPref(prefs));
        } else {
            cb.setEnabled(false);
            cb.setChecked(true);
            if (rc.second != null) {
                cb.setText(String.format(
                        "%s - %s", cb.getText(),
                        itsActivity.getString(rc.second.intValue())));
            }
        }
    }

    /** Handle a pause of the activity.  Return true if the manager is active;
     *  false otherwise */
    public boolean onPause()
    {
        if (itsYubiMgr != null) {
            return itsYubiMgr.onPause();
        }
        return false;
    }

    /** Handle a new intent.  Return true if handled */
    public boolean onNewIntent(Intent intent)
    {
        if (itsYubiMgr != null) {
            return itsYubiMgr.handleKeyIntent(intent);
        }
        return false;
    }

    /** Handle a click event */
    @Override
    public void onClick(View view)
    {
        if (view.getId() == R.id.yubi_start) {
            itsYubiMgr.start(itsYubiUser);
        } else if (view.getId() == R.id.yubi_help) {
            View help = itsDialog.findViewById(R.id.yubi_help_text);
            help.setVisibility((help.getVisibility() == View.VISIBLE) ?
                                View.GONE : View.VISIBLE);
        }
    }

    /** Set visibility of a field */
    private static void setVisibility(int id, boolean visible, View parent)
    {
        View v = parent.findViewById(id);
        v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /** Set visibility of a field */
    private static void setVisibility(int id,
                                      boolean visible,
                                      AlertDialog parent)
    {
        View v = parent.findViewById(id);
        v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /** User of the YubikeyMgr */
    private class YubikeyUser implements YubikeyMgr.User
    {
        @Override
        public Activity getActivity()
        {
            return itsActivity;
        }

        @Override
        public String getUserPassword()
        {
            TextView passwordEdit =
                    (TextView)itsDialog.findViewById(R.id.passwd_edit);
            return passwordEdit.getText().toString();
        }

        @Override
        public void setHashedPassword(String password)
        {
            TextView passwordEdit =
                    (TextView)itsDialog.findViewById(R.id.passwd_edit);
            passwordEdit.setText(password);
            Button okbtn = itsDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            okbtn.performClick();
        }

        @Override
        public void handleHashException(Exception e)
        {
            PasswdSafeUtil.showFatalMsg(
                    e, itsActivity.getString(R.string.yubikey_error),
                    itsActivity);
        }

        @Override
        public int getSlotNum()
        {
            Spinner slotSpinner =
                    (Spinner)itsDialog.findViewById(R.id.yubi_slot);
            return (slotSpinner.getSelectedItemPosition() == 0) ? 1 : 2;
        }

        @Override
        public void timerTick(int totalTime, int remainingTime)
        {
            ProgressBar progress = (ProgressBar)
                    itsDialog.findViewById(R.id.yubi_progress);
            progress.setMax(totalTime);
            progress.setProgress(remainingTime);
        }

        @Override
        public void starting()
        {
            setVisibility(R.id.yubi_start_fields, false, itsDialog);
            setVisibility(R.id.yubi_progress_fields, true, itsDialog);
        }

        @Override
        public void stopped()
        {
            setVisibility(R.id.yubi_start_fields, true, itsDialog);
            setVisibility(R.id.yubi_progress_fields, false, itsDialog);
        }
    };
}
