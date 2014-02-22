/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

/**
 *  Fragment containing the initial view of the PasswdSafe app
 */
public class PasswdSafeMainFragment extends Fragment implements OnClickListener
{
    /** Listener interface for owning activity */
    public interface Listener
    {
        /** Choose the file to open or create */
        public void chooseOpenFile();
    }


    private Listener itsListener;


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onAttach(android.app.Activity)
     */
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        itsListener = (Listener)activity;
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        View root = inflater.inflate(R.layout.fragment_passwdsafe_main,
                                     container, false);

        Button btn = (Button)root.findViewById(R.id.btn_file);
        btn.setOnClickListener(this);
        btn = (Button)root.findViewById(R.id.btn_prefs);
        btn.setOnClickListener(this);

        return root;
    }


    /* (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
        case R.id.btn_file: {
            itsListener.chooseOpenFile();
            break;
        }
        case R.id.btn_prefs: {
            // TODO: show preferences
            break;
        }
        }
    }
}
