/*
 * Copyright (©) 2010 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.app.Activity;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

public abstract class DialogValidator
{
    private TextView itsPassword;
    private TextView itsPasswordConfirm;
    private TextView itsErrorMsgView;
    private String itsErrorFmt;
    private TextWatcher itsTextWatcher = new AbstractTextWatcher()
    {
        public final void afterTextChanged(Editable s)
        {
            validate();
        }
    };

    public DialogValidator(Activity act)
    {
        itsPassword = (TextView) act.findViewById(R.id.password);
        registerTextView(itsPassword);
        itsPasswordConfirm = (TextView)act.findViewById(R.id.password_confirm);
        registerTextView(itsPasswordConfirm);
        itsErrorMsgView = (TextView)act.findViewById(R.id.error_msg);
        itsErrorFmt = act.getResources().getString(R.string.error_msg);
    }

    public DialogValidator(View view)
    {
        itsPassword = (TextView) view.findViewById(R.id.password);
        registerTextView(itsPassword);
        itsPasswordConfirm = (TextView)view.findViewById(R.id.password_confirm);
        registerTextView(itsPasswordConfirm);
        itsErrorMsgView = (TextView)view.findViewById(R.id.error_msg);
        itsErrorFmt = view.getResources().getString(R.string.error_msg);
    }

    public final void registerTextView(TextView tv)
    {
        tv.addTextChangedListener(itsTextWatcher);
    }

    public final void reset()
    {
        itsPassword.setText("");
        itsPasswordConfirm.setText("");
        validate();
    }

    public final void validate()
    {
        String errorMsg = doValidation();

        if (errorMsg == null) {
            itsErrorMsgView.setVisibility(View.GONE);
        } else {
            itsErrorMsgView.setVisibility(View.VISIBLE);
            itsErrorMsgView.setText(
                Html.fromHtml(String.format(itsErrorFmt, errorMsg)));
        }

        getDoneButton().setEnabled(errorMsg == null);
    }

    protected abstract View getDoneButton();

    protected final TextView getPassword()
    {
        return itsPassword;
    }

    protected String doValidation()
    {
        if (!itsPassword.getText().toString().equals(
             itsPasswordConfirm.getText().toString())) {
            return "Passwords do not match";
        }
        return null;
    }
}
