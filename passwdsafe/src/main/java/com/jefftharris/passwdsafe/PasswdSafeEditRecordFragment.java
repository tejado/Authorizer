/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdRecord;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.view.AbstractTextWatcher;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.view.NewGroupDialog;
import com.jefftharris.passwdsafe.view.PasswdLocation;

import org.pwsafe.lib.file.PwsRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;


/**
 * Fragment for editing a password record
 */
public class PasswdSafeEditRecordFragment extends Fragment
        implements NewGroupDialog.Listener,
                   View.OnClickListener,
                   AdapterView.OnItemSelectedListener
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
    private final TreeSet<String> itsGroups =
            new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private int itsPrevGroupPos = -1;
    private final HashSet<RecordKey> itsRecordKeys = new HashSet<>();
    private PasswdRecord.Type itsRecType = PasswdRecord.Type.NORMAL;
    private boolean itsTypeHasNormalPassword = true;
    private boolean itsTypeHasDetails = true;
    private PasswdRecord.Type itsRecOrigType = PasswdRecord.Type.NORMAL;
    private PwsRecord itsReferencedRec = null;
    private Spinner itsType;
    private TextView itsTypeError;
    private TextView itsLinkRef;
    private TextInputLayout itsTitleInput;
    private TextView itsTitle;
    private Spinner itsGroup;
    private TextView itsUser;
    private View itsUrlInput;
    private TextView itsUrl;
    private View itsEmailInput;
    private TextView itsEmail;

    private static final String TAG = "PasswdSafeEditRecordFragment";

    private static final int RECORD_SELECTION_REQUEST = 0;

     // Constants must match record_type strings
    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_ALIAS = 1;
    private static final int TYPE_SHORTCUT = 2;

    // TODO: if pending changes, warn on navigation
    // TODO: v2 support
    // TODO: protected flag
    // TODO: on new record, use current group
    // TODO: fix RecordSelectionActivity for use in choosing alias/shortcut

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
        itsType = (Spinner)rootView.findViewById(R.id.type);
        itsType.setOnItemSelectedListener(this);
        itsTypeError = (TextView)rootView.findViewById(R.id.type_error);
        itsLinkRef = (TextView)rootView.findViewById(R.id.link_ref);
        itsLinkRef.setOnClickListener(this);
        itsTitleInput =
                (TextInputLayout)rootView.findViewById(R.id.title_input);
        itsTitle = (TextView)rootView.findViewById(R.id.title);
        itsValidator.registerTextView(itsTitle);
        itsGroup = (Spinner)rootView.findViewById(R.id.group);
        itsGroup.setOnItemSelectedListener(this);
        itsUser = (TextView)rootView.findViewById(R.id.user);
        itsValidator.registerTextView(itsUser);
        itsUrlInput = rootView.findViewById(R.id.url_input);
        itsUrl = (TextView)rootView.findViewById(R.id.url);
        itsEmailInput = rootView.findViewById(R.id.email_input);
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
    public void onPause()
    {
        super.onPause();
        GuiUtils.setKeyboardVisible(itsTitle, getContext(), false);
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

    @Override
    public void handleNewGroup(String newGroup)
    {
        if (newGroup != null) {
            if (!TextUtils.isEmpty(newGroup)) {
                itsGroups.add(newGroup);
            }
            itsPrevGroupPos = updateGroups(newGroup);
            itsValidator.validate();
        }
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
        case R.id.link_ref: {
            Intent intent = new Intent(PasswdSafeApp.CHOOSE_RECORD_INTENT,
                                       getActivity().getIntent().getData(),
                                       getContext(),
                                       RecordSelectionActivity.class);
            // Do not allow mixed alias and shortcut references to a
            // record to work around a bug in Password Safe that does
            // not allow both
            switch (itsRecType) {
            case NORMAL: {
                break;
            }
            case ALIAS: {
                intent.putExtra(RecordSelectionActivity.FILTER_NO_SHORTCUT,
                                true);
                break;
            }
            case SHORTCUT: {
                intent.putExtra(RecordSelectionActivity.FILTER_NO_ALIAS, true);
                break;
            }
            }

            startActivityForResult(intent, RECORD_SELECTION_REQUEST);
            break;
        }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> spinnerView, View view,
                               int position, long id)
    {
        switch (spinnerView.getId()) {
        case R.id.type: {
            PasswdRecord.Type type = PasswdRecord.Type.NORMAL;
            switch (position) {
            case TYPE_NORMAL: {
                type = PasswdRecord.Type.NORMAL;
                break;
            }
            case TYPE_ALIAS: {
                type = PasswdRecord.Type.ALIAS;
                break;
            }
            case TYPE_SHORTCUT: {
                type = PasswdRecord.Type.SHORTCUT;
                break;
            }
            }
            setType(type, false);
            break;
        }
        case R.id.group: {
            selectGroup(position);
            break;
        }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> spinnerView)
    {
        switch (spinnerView.getId()) {
        case R.id.type: {
            setType(PasswdRecord.Type.NORMAL, false);
            break;
        }
        case R.id.group: {
            break;
        }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        PasswdSafeUtil.dbginfo(TAG, "onActivityResult data: %s", data);

        if ((requestCode == RECORD_SELECTION_REQUEST) &&
            (resultCode == Activity.RESULT_OK)) {
            String uuid = data.getStringExtra(PasswdSafeApp.RESULT_DATA_UUID);
            RecordInfo info = getRecordInfo();
            if (info == null) {
                return;
            }
            setLinkRef(info.itsFileData.getRecord(uuid), info);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
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
        itsUser.setText(info.itsFileData.getUsername(info.itsRec));
        itsUrl.setText(info.itsFileData.getURL(info.itsRec));
        itsEmail.setText(info.itsFileData.getEmail(info.itsRec));

        String group = info.itsFileData.getGroup(info.itsRec);
        initGroup(group, info);
        initTypeAndPassword(info);
    }

    /**
     * Initialize the group in the view
     */
    private void initGroup(String initialGroup, RecordInfo info)
    {
        for (PwsRecord rec: info.itsFileData.getRecords()) {
            String group = info.itsFileData.getGroup(rec);
            if (!TextUtils.isEmpty(group)) {
                itsGroups.add(group);
            }

            if (rec != info.itsRec) {
                RecordKey key = new RecordKey(
                        info.itsFileData.getTitle(rec), group,
                        info.itsFileData.getUsername(rec));
                itsRecordKeys.add(key);
            }
        }

        itsPrevGroupPos = updateGroups(initialGroup);
    }

    /**
     * Initialize the type and password
     */
    private void initTypeAndPassword(RecordInfo info)
    {
        itsRecOrigType = info.itsPasswdRec.getType();
        PwsRecord linkRef = null;
        String password = null;
        switch (itsRecOrigType) {
        case NORMAL: {
            password = info.itsFileData.getPassword(info.itsRec);
            break;
        }
        case ALIAS:
        case SHORTCUT: {
            linkRef = info.itsPasswdRec.getRef();
            break;
        }
        }

        setType(itsRecOrigType, true);
        setLinkRef(linkRef, info);
    }

    /**
     * Set the record's type
     */
    private void setType(PasswdRecord.Type type, boolean init)
    {
        if ((type == itsRecType) && !init) {
            return;
        }

        // Prev type needs to be updated before setting spinner to prevent
        // recursion
        itsRecType = type;
        GuiUtils.invalidateOptionsMenu(getActivity());

        if (init) {
            int pos = TYPE_NORMAL;
            switch (type) {
            case NORMAL: {
                pos = TYPE_NORMAL;
                break;
            }
            case ALIAS: {
                pos = TYPE_ALIAS;
                break;
            }
            case SHORTCUT: {
                pos = TYPE_SHORTCUT;
                break;
            }
            }
            itsType.setSelection(pos);
        }

        itsTypeHasNormalPassword = true;
        itsTypeHasDetails = true;
        switch (type) {
        case NORMAL: {
            itsTypeHasNormalPassword = true;
            itsTypeHasDetails = true;
            break;
        }
        case ALIAS: {
            itsTypeHasNormalPassword = false;
            itsTypeHasDetails = true;
            break;
        }
        case SHORTCUT: {
            itsTypeHasNormalPassword = false;
            itsTypeHasDetails = false;
            break;
        }
        }

        GuiUtils.setVisible(itsLinkRef, !itsTypeHasNormalPassword);
        GuiUtils.setVisible(itsUrlInput, itsTypeHasDetails);
        GuiUtils.setVisible(itsEmailInput, itsTypeHasDetails);

        itsValidator.validate();

        if (!init) {
            // Clear link on type change in case it is no longer valid
            setLinkRef(null, null);
        }
    }

    /**
     * Set the link to another record
     */
    private void setLinkRef(PwsRecord ref, RecordInfo info)
    {
        itsReferencedRec = ref;
        String id = (itsReferencedRec != null) ?
                info.itsFileData.getId(itsReferencedRec) : "";
        itsLinkRef.setText(id);
        itsValidator.validate();
    }

    /**
     * Select a new group
     */
    private void selectGroup(int position)
    {
        if (position == (itsGroup.getCount() - 1)) {
            // Set to previous group so rotations don't recreate with the new
            // group item selected
            itsGroup.setSelection(itsPrevGroupPos);
            NewGroupDialog groupDlg = NewGroupDialog.newInstance();
            groupDlg.setTargetFragment(this, 0);
            groupDlg.show(getFragmentManager(), "NewGroupDialog");
        } else {
            itsPrevGroupPos = position;
            itsValidator.validate();
        }
    }

    /**
     * Update the groups in the spinner
     * @return The selected position
     */
    private int updateGroups(String selGroup)
    {
        ArrayList<String> groupList = new ArrayList<>(itsGroups.size() + 2);
        groupList.add(getString(R.string.none_paren));
        int pos = 1;
        int groupPos = 0;
        for (String grp : itsGroups) {
            if (grp.equals(selGroup)) {
                groupPos = pos;
            }
            groupList.add(grp);
            ++pos;
        }

        groupList.add(getString(R.string.new_group_menu));
        setSpinnerItems(itsGroup, groupList);
        if (groupPos != 0) {
            itsGroup.setSelection(groupPos);
        }
        return groupPos;
    }

    /**
     * Set the items in a spinner
     */
    private void setSpinnerItems(Spinner spinner, List<?> items)
    {
        ArrayAdapter<Object> adapter =
                new ArrayAdapter<>(getContext(),
                                   android.R.layout.simple_spinner_item,
                                   Collections.unmodifiableList(items));
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        adapter.notifyDataSetChanged();
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

        updateStr = getUpdatedGroup(info.itsFileData.getGroup(record));
        if (updateStr != null) {
            info.itsFileData.setGroup(updateStr, record);
        }

        updateStr = getUpdatedField(info.itsFileData.getUsername(record),
                                    itsUser);
        if (updateStr != null) {
            info.itsFileData.setUsername(updateStr, record);
        }

        String currUrl = info.itsFileData.getURL(record);
        String currEmail = info.itsFileData.getEmail(record);
        if (itsTypeHasDetails) {
            updateStr = getUpdatedField(currUrl, itsUrl);
            if (updateStr != null) {
                info.itsFileData.setURL(updateStr, record);
            }

            updateStr = getUpdatedField(currEmail, itsEmail);
            if (updateStr != null) {
                info.itsFileData.setEmail(updateStr, record);
            }
        } else {
            if (currUrl != null) {
                info.itsFileData.setURL(null, record);
            }
            if (currEmail != null) {
                info.itsFileData.setEmail(null, record);
            }
        }

        // Update password after history so update is shown in new history
        String currPasswd = info.itsFileData.getPassword(record);
        String newPasswd;
        if (itsTypeHasNormalPassword) {
            // TODO: fix with real password
            //newPasswd = getUpdatedField(currPasswd, R.id.password);
            newPasswd = null;
            switch (itsRecOrigType) {
            case NORMAL: {
                break;
            }
            case ALIAS:
            case SHORTCUT: {
                currPasswd = null;
                break;
            }
            }
        } else {
            newPasswd = PasswdRecord.uuidToPasswd(
                    info.itsFileData.getUUID(itsReferencedRec), itsRecType);
            if (newPasswd.equals(currPasswd)) {
                newPasswd = null;
            }
        }
        if (newPasswd != null) {
            info.itsFileData.setPassword(currPasswd, newPasswd, record);
            if (!itsTypeHasNormalPassword) {
                info.itsFileData.clearPasswdLastModTime(record);
            }
        }

        GuiUtils.setKeyboardVisible(itsTitle, getContext(), false);
        itsListener.finishEditRecord(record.isModified());
    }

    /**
     * Get the updated value of the group.  Return null if no changes
     */
    private String getUpdatedGroup(String currVal)
    {
        if (currVal == null) {
            currVal = "";
        }
        String newVal = getGroupVal();
        return (newVal.equals(currVal)) ? null : newVal;
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
        return (newVal.equals(currVal)) ? null : newVal;
    }

    /**
     * Get the group value
     */
    private String getGroupVal()
    {
        return (itsGroup.getSelectedItemPosition() > 0) ?
                itsGroup.getSelectedItem().toString() : "";
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

        // TODO: refactor validation to reuse with new file fragment

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
            String typeError = null;
            switch (itsRecType) {
            case NORMAL: {
                break;
            }
            case ALIAS: {
                if (itsReferencedRec == null) {
                    typeError = getString(R.string.no_alias_chosen);
                }
                break;
            }
            case SHORTCUT: {
                if (itsReferencedRec == null) {
                    typeError = getString(R.string.no_shortcut_chosen);
                }
                break;
            }
            }
            GuiUtils.setVisible(itsTypeError, (typeError != null));
            itsTypeError.setText(typeError);

            boolean valid = (typeError == null);
            valid &= !setInputError(validateTitle(), itsTitleInput);

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

        /**
         * Validate the title field
         * @return error message if invalid; null if valid
         */
        private String validateTitle()
        {
            CharSequence title = itsTitle.getText();
            if (title.length() == 0) {
                return getString(R.string.empty_title);
            }

            RecordKey key = new RecordKey(title.toString(), getGroupVal(),
                                          itsUser.getText().toString());
            if (itsRecordKeys.contains(key)) {
                return getString(R.string.duplicate_entry);
            }

            return null;
        }

        /**
         * Set the error message on a TextInputLayout
         * @param errorMsg The error message; null if no error
         * @param field The input field
         * @return Whether there was an error
         */
        private boolean setInputError(String errorMsg, TextInputLayout field)
        {
            boolean isError = (errorMsg != null);

            // Set fields only if error changes to prevent flashing
            boolean currError = field.isErrorEnabled();
            CharSequence currErrorMsg = field.getError();
            if ((currError != isError) ||
                !TextUtils.equals(errorMsg, currErrorMsg)) {
                field.setError(errorMsg);
                // Set enabled last to remove red underline even if null msg
                field.setErrorEnabled(isError);
            }

            return isError;
        }
   }

    /**
     * Unique key fields for a record
     */
    private static class RecordKey
    {
        private final String itsTitle;
        private final String itsGroup;
        private final String itsUser;

        public RecordKey(String title, String group, String user)
        {
            itsTitle = (title != null) ? title : "";
            itsGroup = (group != null) ? group : "";
            itsUser = (user != null) ? user : "";
        }

        @Override
        public final boolean equals(Object o)
        {
            if (o instanceof RecordKey) {
                RecordKey key = (RecordKey)o;
                return itsTitle.equals(key.itsTitle) &&
                    itsGroup.equals(key.itsGroup) &&
                    itsUser.equals(key.itsUser);
            } else {
                return false;
            }
        }

        @Override
        public final int hashCode()
        {
            return itsTitle.hashCode() ^ itsGroup.hashCode() ^
            itsUser.hashCode();
        }
    }
}
