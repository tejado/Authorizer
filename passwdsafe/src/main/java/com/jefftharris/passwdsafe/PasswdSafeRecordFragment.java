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
import android.widget.TabHost;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.view.PasswdLocation;

import org.pwsafe.lib.file.PwsRecord;


/**
 * Fragment showing a password record
 */
public class PasswdSafeRecordFragment extends Fragment
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
    {
        /** Get the file data */
        PasswdFileData getFileData();

        /** Update the view for the location in the password file */
        void updateLocationView(PasswdLocation location);
    }

    private PasswdLocation itsLocation;
    private TextView itsTitle;
    private Listener itsListener;

    /**
     * Create a new instance
     */
    public static PasswdSafeRecordFragment newInstance(PasswdLocation location)
    {
        PasswdSafeRecordFragment frag = new PasswdSafeRecordFragment();
        Bundle args = new Bundle();
        args.putParcelable("location", location);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        PasswdLocation location;
        if (args != null) {
            location = args.getParcelable("location");
        } else {
            location = new PasswdLocation();
        }
        itsLocation = location;
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
        View root = inflater.inflate(R.layout.fragment_passwdsafe_record,
                                     container, false);
        itsTitle = (TextView)root.findViewById(R.id.title);
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        TabHost tabHost = (TabHost)getView();
        if (tabHost == null) {
            return;
        }

        tabHost.setup();
        tabHost.addTab(tabHost.newTabSpec("basic")
                              .setIndicator(getString(R.string.basic))
                              .setContent(R.id.basic_tab));
        tabHost.addTab(tabHost.newTabSpec("password")
                              .setIndicator(getString(R.string.password))
                              .setContent(R.id.password_tab));
        tabHost.addTab(tabHost.newTabSpec("notes")
                              .setIndicator(getString(R.string.notes))
                              .setContent(R.id.notes_tab));
        tabHost.setCurrentTab(0);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        itsListener.updateLocationView(itsLocation);
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
    public void refresh()
    {
        if (!isAdded() || (itsListener == null)) {
            return;
        }

        PasswdFileData fileData = itsListener.getFileData();
        if (fileData == null) {
            return;
        }

        PwsRecord rec = fileData.getRecord(itsLocation.getRecord());
        if (rec == null) {
            return;
        }

        itsTitle.setText(fileData.getTitle(rec));
    }
}
