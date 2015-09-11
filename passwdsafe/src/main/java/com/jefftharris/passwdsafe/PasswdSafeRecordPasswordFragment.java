/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
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
import android.view.ViewGroup;

import com.jefftharris.passwdsafe.file.PasswdFileData;


/**
 * Fragment for showing password-specific fields of a password record
 */
public class PasswdSafeRecordPasswordFragment extends Fragment
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
    {
        /** Get the file data */
        PasswdFileData getFileData();
    }

    private String itsRecUuid;
    private Listener itsListener;

    /**
     * Create a new instance of the fragment
     */
    public static PasswdSafeRecordPasswordFragment newInstance(String recUuid)
    {
        Bundle args = new Bundle();
        args.putString("recUuid", recUuid);
        PasswdSafeRecordPasswordFragment frag =
                new PasswdSafeRecordPasswordFragment();
        frag.setArguments(args);
        return frag;

    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            itsRecUuid = args.getString("recUuid");
        }
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        itsListener = (Listener)activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_passwdsafe_record_password,
                                container, false);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        refresh();
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        itsListener = null;
    }

    /**
     * Refresh the view
     */
    private void refresh()
    {

    }
}
