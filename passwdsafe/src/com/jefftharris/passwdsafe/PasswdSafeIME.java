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
import android.widget.Button;

/**
 *  Input method for selecting fields from a record
 *
 *  @author Jeff Harris
 */
public class PasswdSafeIME extends InputMethodService
{
    /* (non-Javadoc)
     * @see android.inputmethodservice.InputMethodService#onCreateInputView()
     */
    @Override
    public View onCreateInputView()
    {
        View v = getLayoutInflater().inflate(R.layout.input_method, null);

        //TextView tv = (TextView)v.findViewById(R.id.file);
        //PasswdSafeApp app = getPasswdSafeApp();

        Button btn = (Button)v.findViewById(R.id.user);
        btn.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
            }
        });

        btn = (Button)v.findViewById(R.id.password);
        btn.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
            }
        });

        return v;
    }

    /** Get the PasswdSafeApp */
//    private final PasswdSafeApp getPasswdSafeApp()
//    {
//        return (PasswdSafeApp)getApplication();
//    }
}
