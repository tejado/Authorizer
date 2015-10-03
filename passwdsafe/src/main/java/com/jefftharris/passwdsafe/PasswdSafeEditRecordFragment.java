/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdRecord;
import com.jefftharris.passwdsafe.lib.view.AbstractTextWatcher;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.view.PasswdLocation;

import org.pwsafe.lib.file.PwsRecord;


/**
 * Fragment for editing a password record
 */
public class PasswdSafeEditRecordFragment extends Fragment
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
    {
        /** Get the file data */
        PasswdFileData getFileData();

        /** Update the view for editing a record */
        void updateViewEditRecord(PasswdLocation location);

        /** Is the navigation drawer open */
        boolean isNavDrawerOpen();

        /** Finish editing a record */
        void finishEditRecord(boolean save);
    }

    private PasswdLocation itsLocation;
    private Listener itsListener;
    private Validator itsValidator = new Validator();
    private TextInputLayout itsTitleInput;
    private TextView itsTitle;
    private TextView itsUrl;
    private TextView itsEmail;

    // TODO: if pending changes, warn on navigation
    // TODO: v2 support
    // TODO: protected flag

    /**
     * Create a new instance
     */
    public static PasswdSafeEditRecordFragment newInstance(
            PasswdLocation location)
    {
        PasswdSafeEditRecordFragment frag = new PasswdSafeEditRecordFragment();
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
    public void onAttach(Context ctx)
    {
        super.onAttach(ctx);
        itsListener = (Listener)ctx;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        View rootView = inflater.inflate(
                R.layout.fragment_passwdsafe_edit_record, container, false);
        itsTitleInput =
                (TextInputLayout)rootView.findViewById(R.id.title_input);
        itsTitle = (TextView)rootView.findViewById(R.id.title);
        itsValidator.registerTextView(itsTitle);
        itsUrl = (TextView)rootView.findViewById(R.id.url);
        itsEmail = (TextView)rootView.findViewById(R.id.email);

        initialize();
        return rootView;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        itsListener.updateViewEditRecord(itsLocation);
        refresh();
        itsValidator.validate();
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
        if ((itsListener != null) && !itsListener.isNavDrawerOpen()) {
            inflater.inflate(R.menu.fragment_passwdsafe_edit_record, menu);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);

        MenuItem item = menu.findItem(R.id.menu_done);
        boolean valid = itsValidator.isValid();
        item.setEnabled(valid);
        item.getIcon().setAlpha(valid ? 255 : 130);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_done: {
            saveRecord();
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    /**
     * Initialize the view
     */
    private void initialize()
    {
        RecordInfo info = getRecordInfo();
        if (info == null) {
            return;
        }

        itsTitle.setText(info.itsFileData.getTitle(info.itsRec));
        itsUrl.setText(info.itsFileData.getURL(info.itsRec));
        itsEmail.setText(info.itsFileData.getEmail(info.itsRec));
    }

    /**
     * Refresh the view
     */
    private void refresh()
    {
        // TODO: refresh needed?
        /*
        if (!isAdded() || (itsListener == null)) {
            return;
        }
        */
    }

    /**
     * Save the record
     */
    private void saveRecord()
    {
        RecordInfo info = getRecordInfo();
        if (info == null) {
            return;
        }

        PwsRecord record = info.itsRec;

        String updateStr;
        updateStr = getUpdatedField(info.itsFileData.getTitle(record),
                                    itsTitle);
        if (updateStr != null) {
            info.itsFileData.setTitle(updateStr, record);
        }

        updateStr = getUpdatedField(info.itsFileData.getURL(record), itsUrl);
        if (updateStr != null) {
            info.itsFileData.setURL(updateStr, record);
        }

        updateStr = getUpdatedField(info.itsFileData.getEmail(record),
                                    itsEmail);
        if (updateStr != null) {
            info.itsFileData.setEmail(updateStr, record);
        }

        GuiUtils.setKeyboardVisible(itsTitle, getContext(), false);
        itsListener.finishEditRecord(record.isModified());
    }

    /**
     * Get the updated value from a text field.  Return null if no changes.
     */
    private String getUpdatedField(String currVal, TextView field)
    {
        if (currVal == null) {
            currVal = "";
        }
        String newVal = field.getText().toString();
        if (newVal.equals(currVal)) {
            newVal = null;
        }
        return newVal;
    }

    /**
     * Get the record information
     */
    protected RecordInfo getRecordInfo()
    {
        // TODO: pull record info into base class to share with AbstractPasswdSafeRecordFragment

        if (isAdded() && (itsListener != null)) {
            PasswdFileData fileData = itsListener.getFileData();
            if (fileData != null) {
                PwsRecord rec = fileData.getRecord(itsLocation.getRecord());
                if (rec != null) {
                    PasswdRecord passwdRec = fileData.getPasswdRecord(rec);
                    if (passwdRec != null) {
                        return new RecordInfo(rec, passwdRec, fileData);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Wrapper class for record information
     */
    protected static class RecordInfo
    {
        public final PwsRecord itsRec;
        public final PasswdRecord itsPasswdRec;
        public final PasswdFileData itsFileData;

        /**
         * Constructor
         */
        public RecordInfo(@NonNull PwsRecord rec,
                          @NonNull PasswdRecord passwdRec,
                          @NonNull PasswdFileData fileData)
        {
            itsRec = rec;
            itsPasswdRec = passwdRec;
            itsFileData = fileData;
        }
    }

    /**
     * Validator
     */
    private class Validator extends AbstractTextWatcher
    {
        private boolean itsIsValid = false;

        /**
         * Register a text view with the validator
         */
        public final void registerTextView(TextView field)
        {
            field.addTextChangedListener(this);
        }

        /**
         * Validate
         */
        public final void validate()
        {
            boolean valid = true;
            CharSequence title = itsTitle.getText();
            if (title.length() == 0) {
                valid = false;
                itsTitleInput.setError(getString(R.string.empty_title));
            } else {
                itsTitleInput.setError(null);
                itsTitleInput.setErrorEnabled(false);
            }

            if (valid != itsIsValid) {
                itsIsValid = valid;
                GuiUtils.invalidateOptionsMenu(getActivity());
            }
        }

        /**
         * Is valid
         */
        public final boolean isValid()
        {
            return itsIsValid;
        }

        @Override
        public final void afterTextChanged(Editable s)
        {
            validate();
        }
    }
}
