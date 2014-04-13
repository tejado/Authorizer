/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import org.pwsafe.lib.file.PwsRecord;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileData;

/**
 *  Fragment containing the details of a record
 */
public class PasswdSafeRecordFragment extends Fragment
{
    /** Listener interface for owning activity */
    public interface Listener
    {
        /** Get the file data */
        PasswdFileData getFileData();
    }


    private String itsUuid;
    private Listener itsListener;


    /** Create a new instance */
    public static PasswdSafeRecordFragment newInstance(String uuid)
    {
        PasswdSafeRecordFragment frag = new PasswdSafeRecordFragment();
        Bundle args = new Bundle();
        args.putString("uuid", uuid);
        frag.setArguments(args);
        return frag;
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        itsUuid = (args != null) ? args.getString("uuid") : null;
    }


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
        View root = inflater.inflate(R.layout.fragment_passwdsafe_record,
                                     container, false);

        PasswdFileData fileData = itsListener.getFileData();
        if (fileData == null) {
            return root;
        }
        PwsRecord rec = fileData.getRecord(itsUuid);
        if (rec == null) {
            return root;
        } else {
            // TODO: for keyboard
            //getPasswdFile().setLastViewedRecord(uuid);
        }

        TextView tv = (TextView)root.findViewById(R.id.title);
        tv.setText(fileData.getTitle(rec));

        return root;
    }
}
