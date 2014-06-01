/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.util.ArrayList;

import org.pwsafe.lib.file.PwsRecord;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdLocation;

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

        /** Update the view for the location in the password file */
        void updateLocationView(PasswdLocation location);
    }

    // TODO: When viewing record, should menu_close item close file or view
    // of record

    private PasswdLocation itsLocation;
    private Listener itsListener;


    /** Create a new instance */
    public static PasswdSafeRecordFragment newInstance(PasswdLocation location)
    {
        PasswdSafeRecordFragment frag = new PasswdSafeRecordFragment();
        Bundle args = new Bundle();
        args.putStringArrayList("groups", location.getGroups());
        args.putString("record", location.getRecord());
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
        ArrayList<String> groups = null;
        String record = null;
        if (args != null) {
            groups = args.getStringArrayList("groups");
            record = args.getString("record");
        }
        itsLocation = new PasswdLocation(groups, record);
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
        PwsRecord rec = fileData.getRecord(itsLocation.getRecord());
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


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onResume()
     */
    @Override
    public void onResume()
    {
        super.onResume();
        itsListener.updateLocationView(itsLocation);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onCreateOptionsMenu(android.view.Menu, android.view.MenuInflater)
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.fragment_passwdsafe_record, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_edit: {
            // TODO: impl
            break;
        }
        case R.id.menu_delete: {
            // TODO: impl
            break;
        }
        case R.id.menu_toggle_password: {
            // TODO: impl
            break;
        }
        case R.id.menu_copy_user: {
            // TODO: impl
            break;
        }
        case R.id.menu_copy_password: {
            // TODO: impl
            break;
        }
        case R.id.menu_copy_notes: {
            // TODO: impl
            break;
        }
        case R.id.menu_toggle_monospace: {
            // TODO: impl
            break;
        }
        case R.id.menu_toggle_wrap_notes: {
            // TODO: impl
            break;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
        return true;
    }


}
