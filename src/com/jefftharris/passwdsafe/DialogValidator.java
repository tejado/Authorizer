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
    private Activity itsActivity;
    private TextWatcher itsTextWatcher = new TextWatcher()
    {
        public void afterTextChanged(Editable s)
        {
            validate();
        }

        public void beforeTextChanged(CharSequence s, int start,
                                      int count, int after)
        {
        }

        public void onTextChanged(CharSequence s, int start,
                                  int before, int count)
        {
        }
    };

    public DialogValidator(Activity act)
    {
        itsActivity = act;
    }

    public final void registerTextView(TextView tv)
    {
        tv.addTextChangedListener(itsTextWatcher);
    }

    public final void validate()
    {
        String errorMsg = doValidation();

        TextView errorMsgView =
            (TextView)itsActivity.findViewById(R.id.error_msg);
        if (errorMsg == null) {
            errorMsgView.setVisibility(View.GONE);
        } else {
            errorMsgView.setVisibility(View.VISIBLE);

            String errorFmt =
                itsActivity.getResources().getString(R.string.error_msg);
            errorMsgView.setText(
                Html.fromHtml(String.format(errorFmt, errorMsg)));
        }

        View doneBtn = itsActivity.findViewById(R.id.done_btn);
        doneBtn.setEnabled(errorMsg == null);
    }

    protected String doValidation()
    {
        if (!GuiUtils.getTextViewStr(itsActivity, R.id.password).equals(
                GuiUtils.getTextViewStr(itsActivity, R.id.password_confirm))) {
            return "Passwords do not match";
        }
        return null;
    }
}
