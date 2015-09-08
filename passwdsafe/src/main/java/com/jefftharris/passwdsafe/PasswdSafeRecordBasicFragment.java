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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

import org.pwsafe.lib.file.PwsRecord;


/**
 * Fragment for showing basic fields of a password record
 */
public class PasswdSafeRecordBasicFragment extends Fragment
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
    private View itsUserRow;
    private TextView itsUser;
    private Listener itsListener;

    public static PasswdSafeRecordBasicFragment newInstance(String recUuid)
    {
        Bundle args = new Bundle();
        args.putString("recUuid", recUuid);
        PasswdSafeRecordBasicFragment frag = new PasswdSafeRecordBasicFragment();
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
        setHasOptionsMenu(true);
        View root = inflater.inflate(R.layout.fragment_passwdsafe_record_basic,
                                     container, false);
        itsUserRow = root.findViewById(R.id.user_row);
        itsUser = (TextView)root.findViewById(R.id.user);

        return root;
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.fragment_passwdsafe_record_basic, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_copy_user: {
            PasswdSafeUtil.copyToClipboard(itsUser.getText().toString(),
                                           getActivity());
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    /**
     * Refresh the view
     */
    private void refresh()
    {
        if (!isAdded() || (itsListener == null)) {
            return;
        }

        PasswdFileData fileData = itsListener.getFileData();
        if (fileData == null) {
            return;
        }

        PwsRecord rec = fileData.getRecord(itsRecUuid);
        if (rec == null) {
            return;
        }

        setFieldText(itsUser, itsUserRow, fileData.getUsername(rec));
    }

    /**
     * Set the value of a text field.  The field's row is visible if the text
     * isn't null.
     */
    private void setFieldText(TextView field, View fieldRow, String text)
    {
        field.setText(text);

        if (fieldRow != null) {
            fieldRow.setVisibility((text != null) ? View.VISIBLE : View.GONE);
        }
    }
}
