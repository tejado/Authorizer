/*
 * Copyright (©) 2010-2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import com.jefftharris.passwdsafe.R;

public abstract class DialogValidator
{
    /**
     * DialogValidator for alert dialogs
     */
    public static class AlertValidator extends DialogValidator
    {
        private final AlertDialog itsDialog;

        /**
         * Constructor with a specific view and password fields
         */
        public AlertValidator(AlertDialog dlg, View view, Activity act)
        {
            this(dlg, view, act, true);
        }

        /**
         * Constructor with a specific view and optional password fields
         */
        public AlertValidator(AlertDialog dlg, View view, Activity act,
                              boolean hasPasswords)
        {
            super(view, act, hasPasswords);
            itsDialog = dlg;
        }

        @Override
        protected final View getDoneButton()
        {
            return itsDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        }

        /**
         * Get the alert dialog
         */
        protected final AlertDialog getDialog()
        {
            return itsDialog;
        }
    }


    /**
     * DialogValidator for fragments
     */
    public static class FragmentValidator extends DialogValidator
    {
        private final View itsDoneButton;

        /** Constructor */
        public FragmentValidator(View rootView, View doneButton,
                                 boolean hasPasswords, Context ctx)
        {
            super(rootView, ctx, hasPasswords);
            itsDoneButton = doneButton;
        }

        @Override
        protected final View getDoneButton()
        {
            return itsDoneButton;
        }
    }

    private final Context itsContext;
    private TextView itsPassword = null;
    private TextView itsPasswordConfirm = null;
    private boolean itsAllowEmptyPassword = false;
    private TextView itsErrorMsgView;
    private String itsErrorFmt;
    private TextWatcher itsTextWatcher = new AbstractTextWatcher()
    {
        public final void afterTextChanged(Editable s)
        {
            validate();
        }
    };

    /**
     * Constructor with the activity as the view and password fields
     */
    public DialogValidator(Activity act)
    {
        itsContext = act;
        itsPassword = (TextView) act.findViewById(R.id.password);
        registerTextView(itsPassword);
        itsPasswordConfirm = (TextView)act.findViewById(R.id.password_confirm);
        registerTextView(itsPasswordConfirm);
        itsErrorMsgView = (TextView)act.findViewById(R.id.error_msg);
        itsErrorFmt = act.getResources().getString(R.string.error_msg);
    }

    /**
     * Constructor with a specific view and optional password fields
     */
    public DialogValidator(View view, Context ctx, boolean hasPasswords)
    {
        itsContext = ctx;
        if (hasPasswords) {
            itsPassword = (TextView) view.findViewById(R.id.password);
            registerTextView(itsPassword);
            itsPasswordConfirm =
                (TextView)view.findViewById(R.id.password_confirm);
            registerTextView(itsPasswordConfirm);
        }
        itsErrorMsgView = (TextView)view.findViewById(R.id.error_msg);
        itsErrorFmt = view.getResources().getString(R.string.error_msg);
    }


    public final void registerTextView(TextView tv)
    {
        tv.addTextChangedListener(itsTextWatcher);
    }

    public void reset()
    {
        if (itsPassword != null) {
            itsPassword.setText("");
            itsPasswordConfirm.setText("");
        }
        validate();
    }

    public final void validate()
    {
        String errorMsg = doValidation();
        boolean isError = (errorMsg != null);
        if (!isError) {
            itsErrorMsgView.setVisibility(View.GONE);
        } else {
            itsErrorMsgView.setVisibility(View.VISIBLE);
            itsErrorMsgView.setText(
                Html.fromHtml(String.format(itsErrorFmt, errorMsg)));
        }

        getDoneButton().setEnabled(!isError);
    }

    protected abstract View getDoneButton();

    protected final TextView getPassword()
    {
        return itsPassword;
    }

    protected String doValidation()
    {
        if (itsPassword != null) {
            String passwd = itsPassword.getText().toString();
            if (!itsAllowEmptyPassword && (passwd.length() == 0)) {
                return getString(R.string.empty_password);
            }
            if (!passwd.equals(itsPasswordConfirm.getText().toString())) {
                return getString(R.string.passwords_do_not_match);
            }
        }
        return null;
    }

    protected final String getString(int id)
    {
        return itsContext.getString(id);
    }

    protected final String getString(int id, Object... args)
    {
        return itsContext.getString(id, args);
    }

    /** Get the validator's context */
    protected final Context getContext()
    {
        return itsContext;
    }

    protected final void setAllowEmptyPassword(boolean allow)
    {
        itsAllowEmptyPassword = allow;
    }
}
