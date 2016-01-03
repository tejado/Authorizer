/*
 * Copyright (©) 2010-2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.view;

import com.jefftharris.passwdsafe.R;
import com.jefftharris.passwdsafe.lib.view.AbstractTextWatcher;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

public abstract class DialogValidator
{
    /**
     * DialogValidator for alert dialogs
     */
    public static class AlertValidator extends DialogValidator
    {
        private final AlertDialog itsDialog;

        /**
         * Constructor with a specific view and optional password fields
         */
        public AlertValidator(AlertDialog dlg, View view, Context ctx)
        {
            super(view, ctx);
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
     * DialogValidator for alert compat dialogs
     */
    public static class AlertCompatValidator extends DialogValidator
    {
        private final android.support.v7.app.AlertDialog itsDialog;

        /**
         * Constructor with a specific view and optional password fields
         */
        public AlertCompatValidator(android.support.v7.app.AlertDialog dlg,
                                    View view, Context ctx)
        {
            super(view, ctx);
            itsDialog = dlg;
        }

        @Override
        protected final View getDoneButton()
        {
            return itsDialog.getButton(
                    android.support.v7.app.AlertDialog.BUTTON_POSITIVE);
        }

    }

    private final Context itsContext;
    private final TextView itsErrorMsgView;
    private final String itsErrorFmt;
    private final TextWatcher itsTextWatcher = new AbstractTextWatcher()
    {
        public final void afterTextChanged(Editable s)
        {
            validate();
        }
    };

    /**
     * Constructor with a specific view and optional password fields
     */
    protected DialogValidator(View view, Context ctx)
    {
        itsContext = ctx;
        itsErrorMsgView = (TextView)view.findViewById(R.id.error_msg);
        itsErrorFmt = view.getResources().getString(R.string.error_msg);
    }


    public final void registerTextView(TextView tv)
    {
        tv.addTextChangedListener(itsTextWatcher);
    }

    public void reset()
    {
        validate();
    }

    public final void validate()
    {
        String errorMsg = doValidation();
        boolean isError = (errorMsg != null);
        if (itsErrorMsgView != null) {
            if (!isError) {
                itsErrorMsgView.setVisibility(View.GONE);
            } else {
                itsErrorMsgView.setVisibility(View.VISIBLE);
                itsErrorMsgView.setText(
                        Html.fromHtml(String.format(itsErrorFmt, errorMsg)));
            }
        }

        getDoneButton().setEnabled(!isError);
    }

    protected abstract View getDoneButton();

    protected String doValidation()
    {
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

}
