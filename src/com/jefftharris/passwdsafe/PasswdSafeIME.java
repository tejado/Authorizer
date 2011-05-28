package com.jefftharris.passwdsafe;

import android.inputmethodservice.InputMethodService;
import android.view.View;

public class PasswdSafeIME extends InputMethodService
{

    /* (non-Javadoc)
     * @see android.inputmethodservice.InputMethodService#onCreateInputView()
     */
    @Override
    public View onCreateInputView()
    {
        View v = getLayoutInflater().inflate(R.layout.passwd_safe, null);
        return v;
    }

}
