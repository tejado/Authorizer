/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.view;

import net.tjado.passwdsafe.lib.view.AbstractTextWatcher;

import android.content.Context;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

public abstract class DialogValidator
{
    /**
     * DialogValidator for alert compat dialogs
     */
    public static class AlertCompatValidator extends DialogValidator
    {
        private final androidx.appcompat.app.AlertDialog itsDialog;

        /**
         * Constructor with a specific view and optional password fields
         */
        public AlertCompatValidator(androidx.appcompat.app.AlertDialog dlg,
                                    View view, Context ctx)
        {
            super(view, ctx);
            itsDialog = dlg;
        }

        @Override
        protected final View getDoneButton()
        {
            return itsDialog.getButton(
                    androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
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
        itsErrorMsgView = (TextView)view.findViewById(net.tjado.passwdsafe.R.id.error_msg);
        itsErrorFmt = view.getResources().getString(net.tjado.passwdsafe.R.string.error_msg);
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
