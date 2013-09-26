/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

/**
 *  Input method for selecting fields from a record
 *
 *  @author Jeff Harris
 */
public class PasswdSafeIME extends InputMethodService
{
    private static final String TAG = "PasswdSafeIME";

    /* (non-Javadoc)
     * @see android.inputmethodservice.InputMethodService#onCreateInputView()
     */
    @Override
    public View onCreateInputView()
    {
        View v = getLayoutInflater().inflate(R.layout.input_method, null);

        TextView tv = (TextView)v.findViewById(R.id.file);
        PasswdSafeApp app = getPasswdSafeApp();
        PasswdFileData fileData = app.accessOpenFileData();
        if (fileData != null) {
            tv.setText(fileData.getUri().toString());
        } else {
            tv.setText("null data");
        }

        Button btn = (Button)v.findViewById(R.id.user);
        btn.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                PasswdSafeUtil.dbginfo(TAG, "user click");
                sendText("USER");
            }
        });

        btn = (Button)v.findViewById(R.id.password);
        btn.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                PasswdSafeUtil.dbginfo(TAG, "password click");
                sendText("PASSWORD");
            }
        });

        return v;
    }

    /* (non-Javadoc)
     * @see android.inputmethodservice.InputMethodService#onEvaluateFullscreenMode()
     */
    @Override
    public boolean onEvaluateFullscreenMode()
    {
        // Don't want to enter full-screen mode as not a real keyboard
        return false;
    }

    /** Send text to the connected editor */
    private final void sendText(String str)
    {
        InputConnection conn = getCurrentInputConnection();
        if (conn != null) {
            conn.commitText(str, 1);
        }
    }

    /** Get the PasswdSafeApp */
    private final PasswdSafeApp getPasswdSafeApp()
    {
        return (PasswdSafeApp)getApplication();
    }
}
