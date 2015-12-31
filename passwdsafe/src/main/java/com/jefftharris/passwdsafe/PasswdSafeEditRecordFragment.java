/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jefftharris.passwdsafe.file.HeaderPasswdPolicies;
import com.jefftharris.passwdsafe.file.PasswdExpiration;
import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdHistory;
import com.jefftharris.passwdsafe.file.PasswdPolicy;
import com.jefftharris.passwdsafe.file.PasswdRecord;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.Utils;
import com.jefftharris.passwdsafe.lib.view.AbstractTextWatcher;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.util.ObjectHolder;
import com.jefftharris.passwdsafe.util.Pair;
import com.jefftharris.passwdsafe.view.DatePickerDialogFragment;
import com.jefftharris.passwdsafe.view.NewGroupDialog;
import com.jefftharris.passwdsafe.view.PasswdLocation;
import com.jefftharris.passwdsafe.view.PasswdPolicyEditDialog;
import com.jefftharris.passwdsafe.view.PasswdPolicyView;
import com.jefftharris.passwdsafe.view.PasswordVisibilityMenuHandler;
import com.jefftharris.passwdsafe.view.TextInputUtils;
import com.jefftharris.passwdsafe.view.TimePickerDialogFragment;

import org.pwsafe.lib.exception.PasswordSafeException;
import org.pwsafe.lib.file.PwsRecord;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;


/**
 * Fragment for editing a password record
 */
public class PasswdSafeEditRecordFragment
        extends AbstractPasswdSafeLocationFragment
                        <PasswdSafeEditRecordFragment.Listener>
        implements NewGroupDialog.Listener,
                   View.OnClickListener,
                   View.OnLongClickListener,
                   TimePickerDialogFragment.Listener,
                   DatePickerDialogFragment.Listener,
                   PasswdPolicyEditDialog.Listener,
                   AdapterView.OnItemSelectedListener
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
            extends AbstractPasswdSafeFileDataFragment.Listener
    {
        /** Update the view for editing a record */
        void updateViewEditRecord(PasswdLocation location);

        /** Finish editing a record */
        void finishEditRecord(boolean save, PasswdLocation newLocation);
    }

    private Validator itsValidator = new Validator();
    private final TreeSet<String> itsGroups =
            new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private int itsPrevGroupPos = -1;
    private final HashSet<RecordKey> itsRecordKeys = new HashSet<>();
    private final ArrayList<View> itsProtectViews = new ArrayList<>();
    private String itsUuid;
    private boolean itsIsProtected = false;
    private PasswdRecord.Type itsRecType = PasswdRecord.Type.NORMAL;
    private boolean itsTypeHasNormalPassword = true;
    private boolean itsTypeHasDetails = true;
    private PasswdRecord.Type itsRecOrigType = PasswdRecord.Type.NORMAL;
    private String itsReferencedRecUuid = null;
    private List<PasswdPolicy> itsPolicies;
    private PasswdPolicy itsOrigPolicy;
    private PasswdPolicy itsCurrPolicy;
    private PasswdExpiration itsOrigExpiry;
    private PasswdExpiration.Type itsExpiryType = PasswdExpiration.Type.NEVER;
    private Calendar itsExpiryDate;
    private PasswdHistory itsHistory;
    private boolean itsIsV3 = false;
    // UI fields
    private View itsTypeGroup;
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
    private View itsPasswordLabel;
    private View itsPasswordFields;
    private TextView itsPasswordCurrent;
    private TextInputLayout itsPasswordInput;
    private TextView itsPassword;
    private TextInputLayout itsPasswordConfirmInput;
    private TextView itsPasswordConfirm;
    private Spinner itsPolicy;
    private Button itsPolicyEditBtn;
    private PasswdPolicyView itsPasswdPolicyView;
    private View itsExpireGroup;
    private Spinner itsExpire;
    private View itsExpireDateFields;
    private TextView itsExpireDateTime;
    private TextView itsExpireDateDate;
    private View itsExpireDateError;
    private View itsExpireIntervalFields;
    private TextInputLayout itsExpireIntervalInput;
    private TextView itsExpireInterval;
    private CheckBox itsExpireIntervalRecurring;
    private View itsHistoryGroup;
    private Button itsHistoryAddRemoveBtn;
    private CheckBox itsHistoryEnabled;
    private TextInputLayout itsHistoryMaxSizeInput;
    private TextView itsHistoryMaxSize;
    private ListView itsHistoryList;
    private View itsNotesLabel;
    private TextView itsNotes;

    private static final String TAG = "PasswdSafeEditRecordFragment";

    private static final int RECORD_SELECTION_REQUEST = 0;

    private static final String STATE_EXPIRY_DATE = "expiryDate";
    private static final String STATE_HISTORY = "history";
    private static final String STATE_PROTECTED = "protected";
    private static final String STATE_REFERENCED_RECORD = "referencedRecord";

     // Constants must match record_type strings
    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_ALIAS = 1;
    private static final int TYPE_SHORTCUT = 2;

    /**
     * Create a new instance
     */
    public static PasswdSafeEditRecordFragment newInstance(
            PasswdLocation location)
    {
        PasswdSafeEditRecordFragment frag = new PasswdSafeEditRecordFragment();
        frag.setArguments(createArgs(location));
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        View rootView = inflater.inflate(
                R.layout.fragment_passwdsafe_edit_record, container, false);
        itsTypeGroup = rootView.findViewById(R.id.type_group);
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

        // Password
        itsPasswordLabel = rootView.findViewById(R.id.password_label);
        itsPasswordFields = rootView.findViewById(R.id.password_fields);
        itsPasswordCurrent = (TextView)
                rootView.findViewById(R.id.password_current);
        itsPasswordInput = (TextInputLayout)
                rootView.findViewById(R.id.password_input);
        itsPassword = (TextView)rootView.findViewById(R.id.password);
        View passwordVisibility =
                rootView.findViewById(R.id.password_visibility);
        passwordVisibility.setOnClickListener(this);
        passwordVisibility.setOnLongClickListener(this);
        View passwordGenerate =
                rootView.findViewById(R.id.password_generate);
        passwordGenerate.setOnClickListener(this);
        passwordGenerate.setOnLongClickListener(this);
        itsValidator.registerTextView(itsPassword);
        itsPasswordConfirmInput = (TextInputLayout)
                rootView.findViewById(R.id.password_confirm_input);
        itsPasswordConfirm = (TextView)
                rootView.findViewById(R.id.password_confirm);
        itsValidator.registerTextView(itsPasswordConfirm);

        // Password policy
        itsPolicy = (Spinner)rootView.findViewById(R.id.policy);
        itsPolicy.setOnItemSelectedListener(this);
        itsPasswdPolicyView = (PasswdPolicyView)
                rootView.findViewById(R.id.policy_view);
        itsPolicyEditBtn = (Button)rootView.findViewById(R.id.policy_edit);
        itsPolicyEditBtn.setOnClickListener(this);

        // Password expiration
        itsExpireGroup = rootView.findViewById(R.id.expire_group);
        itsExpire = (Spinner)rootView.findViewById(R.id.expire_choice);
        itsExpire.setOnItemSelectedListener(this);
        itsExpireDateFields = rootView.findViewById(R.id.expire_date_fields);
        itsExpireDateTime = (TextView)
                rootView.findViewById(R.id.expire_date_time);
        itsExpireDateTime.setOnClickListener(this);
        itsExpireDateDate = (TextView)
                rootView.findViewById(R.id.expire_date_date);
        itsExpireDateDate.setOnClickListener(this);
        itsExpireDateError = rootView.findViewById(R.id.expire_date_error);
        itsExpireIntervalFields =
                rootView.findViewById(R.id.expire_interval_fields);
        itsExpireIntervalInput = (TextInputLayout)
                rootView.findViewById(R.id.expire_interval_val_input);
        itsExpireInterval = (TextView)
                rootView.findViewById(R.id.expire_interval_val);
        itsValidator.registerTextView(itsExpireInterval);
        itsExpireIntervalRecurring = (CheckBox)
                rootView.findViewById(R.id.expire_interval_recurring);

        // Password history
        itsHistoryGroup = rootView.findViewById(R.id.history_group);
        itsHistoryAddRemoveBtn = (Button)
                rootView.findViewById(R.id.history_addremove);
        itsHistoryAddRemoveBtn.setOnClickListener(this);
        itsHistoryEnabled = (CheckBox)
                rootView.findViewById(R.id.history_enabled);
        itsHistoryEnabled.setOnClickListener(this);
        itsHistoryMaxSizeInput = (TextInputLayout)
                rootView.findViewById(R.id.history_max_size_input);
        itsHistoryMaxSize = (TextView)
                rootView.findViewById(R.id.history_max_size);
        itsHistoryMaxSize.addTextChangedListener(
                new AbstractTextWatcher()
                {
                    @Override
                    public void afterTextChanged(Editable s)
                    {
                        historyMaxSizeChanged();
                    }
                });
        itsHistoryList = (ListView)rootView.findViewById(R.id.history);
        registerForContextMenu(itsHistoryList);

        // Notes
        itsNotesLabel = rootView.findViewById(R.id.notes_label);
        itsNotes = (TextView)rootView.findViewById(R.id.notes);
        PasswdSafeRecordNotesFragment.setNotesOptions(itsNotes, getActivity());

        initProtViews(rootView);
        initialize();
        return rootView;
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState)
    {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState == null) {
            return;
        }

        if (savedInstanceState.containsKey(STATE_PROTECTED)) {
            itsIsProtected = savedInstanceState.getBoolean(STATE_PROTECTED,
                                                           itsIsProtected);
            updateProtected();
        }

        if (savedInstanceState.containsKey(STATE_EXPIRY_DATE)) {
            itsExpiryDate.setTimeInMillis(
                    savedInstanceState.getLong(
                            STATE_EXPIRY_DATE,
                            itsExpiryDate.getTimeInMillis()));
            updatePasswdExpiryDate();
        }

        if (savedInstanceState.containsKey(STATE_HISTORY)) {
            String historyStr = savedInstanceState.getString(STATE_HISTORY);
            itsHistory = (historyStr != null) ?
                    new PasswdHistory(historyStr) : null;
            historyChanged(true);
        }

        if (savedInstanceState.containsKey(STATE_REFERENCED_RECORD)) {
            final String ref =
                    savedInstanceState.getString(STATE_REFERENCED_RECORD);
            // Delay setting the link reference to allow type selection change
            // to occur which clears the reference
            View root = getView();
            if (root != null) {
                root.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        setLinkRefUuid(ref);
                    }
                });
            }
        }

        GuiUtils.invalidateOptionsMenu(getActivity());
    }

    @Override
    public void onResume()
    {
        super.onResume();
        getListener().updateViewEditRecord(getLocation());
        itsValidator.validate();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        if (itsExpiryDate != null) {
            outState.putLong(STATE_EXPIRY_DATE,
                             itsExpiryDate.getTimeInMillis());
        }
        outState.putString(STATE_HISTORY,
                           (itsHistory != null) ? itsHistory.toString() : null);
        outState.putBoolean(STATE_PROTECTED, itsIsProtected);
        outState.putString(STATE_REFERENCED_RECORD, itsReferencedRecUuid);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        GuiUtils.setKeyboardVisible(itsTitle, getContext(), false);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);

        MenuItem item = menu.findItem(R.id.menu_save);
        if (item != null) {
            item.setEnabled(itsValidator.isValid());
        }

        item = menu.findItem(R.id.menu_protect);
        if (item != null) {
            updateProtectedMenu(item);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_save: {
            saveRecord();
            return true;
        }
        case R.id.menu_protect: {
            itsIsProtected = !itsIsProtected;
            updateProtectedMenu(item);
            updateProtected();
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);

        if ((v == itsHistoryList) && itsHistoryList.isEnabled()) {
            AdapterView.AdapterContextMenuInfo info =
                    (AdapterView.AdapterContextMenuInfo)menuInfo;
            List<PasswdHistory.Entry> passwds = itsHistory.getPasswds();
            if ((info.position >= 0) && (info.position < passwds.size())) {
                menu.setHeaderTitle(passwds.get(info.position).getPasswd());
                getActivity().getMenuInflater().inflate(
                        R.menu.fragment_passwdsafe_edit_record_history, menu);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

        switch (item.getItemId()) {
        case R.id.menu_history_remove: {
            List<PasswdHistory.Entry> passwds = itsHistory.getPasswds();
            if ((info.position >= 0) && (info.position < passwds.size())) {
                passwds.remove(info.position);
                historyChanged(true);
            }
            return true;
        }
        case R.id.menu_history_set_password: {
            List<PasswdHistory.Entry> passwds = itsHistory.getPasswds();
            if ((info.position >= 0) && (info.position < passwds.size())) {
                setPassword(passwds.get(info.position).getPasswd());
            }
            return true;
        }
        default: {
            return super.onContextItemSelected(item);
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
        case R.id.expire_date_date: {
            DatePickerDialogFragment picker =
                    DatePickerDialogFragment.newInstance(
                            itsExpiryDate.get(Calendar.YEAR),
                            itsExpiryDate.get(Calendar.MONTH),
                            itsExpiryDate.get(Calendar.DAY_OF_MONTH));
            picker.setTargetFragment(this, 0);
            picker.show(getFragmentManager(), "datePicker");
            break;
        }
        case R.id.expire_date_time: {
            TimePickerDialogFragment picker =
                    TimePickerDialogFragment.newInstance(
                            itsExpiryDate.get(Calendar.HOUR_OF_DAY),
                            itsExpiryDate.get(Calendar.MINUTE));
            picker.setTargetFragment(this, 0);
            picker.show(getFragmentManager(), "timePicker");
            break;
        }
        case R.id.history_addremove: {
            if (itsHistory == null) {
                itsHistory = new PasswdHistory();
            } else {
                itsHistory = null;
            }
            historyChanged(true);
            break;
        }
        case R.id.history_enabled: {
            if (itsHistory != null) {
                itsHistory.setEnabled(!itsHistory.isEnabled());
            }
            historyChanged(true);
            break;
        }
        case R.id.link_ref: {
            Intent intent = new Intent(PasswdSafeApp.CHOOSE_RECORD_INTENT,
                                       getActivity().getIntent().getData(),
                                       getContext(),
                                       LauncherRecordShortcuts.class);
            // Do not allow mixed alias and shortcut references to a
            // record to work around a bug in Password Safe that does
            // not allow both
            switch (itsRecType) {
            case NORMAL: {
                break;
            }
            case ALIAS: {
                intent.putExtra(LauncherRecordShortcuts.FILTER_NO_SHORTCUT,
                                true);
                break;
            }
            case SHORTCUT: {
                intent.putExtra(LauncherRecordShortcuts.FILTER_NO_ALIAS, true);
                break;
            }
            }

            startActivityForResult(intent, RECORD_SELECTION_REQUEST);
            break;
        }
        case R.id.password_generate: {
            if (itsCurrPolicy != null) {
                try {
                    setPassword(itsCurrPolicy.generate());
                } catch (Exception e) {
                    PasswdSafeUtil.showFatalMsg(e, getActivity());
                }
            }
            break;
        }
        case R.id.password_visibility: {
            boolean visible = GuiUtils.isPasswordVisible(itsPassword);
            setPasswordVisibility(!visible);
            break;
        }
        case R.id.policy_edit: {
            PasswdPolicyEditDialog dlg =
                    PasswdPolicyEditDialog.newInstance(itsCurrPolicy);
            dlg.setTargetFragment(this, 0);
            dlg.show(getFragmentManager(), "PasswdPolicyEditDialog");
            break;
        }
        }
    }

    @Override
    public boolean onLongClick(View v)
    {
        switch (v.getId()) {
        case R.id.password_visibility: {
            int msg = GuiUtils.isPasswordVisible(itsPassword) ?
                    R.string.hide_passwords : R.string.show_passwords;
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            return true;
        }
        case R.id.password_generate: {
            Toast.makeText(getContext(), R.string.generate_password,
                           Toast.LENGTH_SHORT).show();
            return true;
        }
        }
        return false;
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
        case R.id.policy: {
            selectPolicy((PasswdPolicy)spinnerView.getSelectedItem());
            break;
        }
        case R.id.expire_choice: {
            updatePasswdExpiryChoice(
                    PasswdExpiration.Type.fromStrIdx(position));
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
        case R.id.policy: {
            selectPolicy(null);
            break;
        }
        case R.id.expire_choice: {
            updatePasswdExpiryChoice(PasswdExpiration.Type.NEVER);
            break;
        }
        }
    }

    @Override
    public void handleTimePicked(int hourOfDay, int minute)
    {
        itsExpiryDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
        itsExpiryDate.set(Calendar.MINUTE, minute);
        itsExpiryDate.set(Calendar.SECOND, 0);
        updatePasswdExpiryDate();
    }

    @Override
    public void handleDatePicked(int year, int monthOfYear, int dayOfMonth)
    {
        itsExpiryDate.set(Calendar.YEAR, year);
        itsExpiryDate.set(Calendar.MONTH, monthOfYear);
        itsExpiryDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        updatePasswdExpiryDate();
    }

    @Override
    public void handlePolicyEditComplete(PasswdPolicy oldPolicy,
                                         PasswdPolicy newPolicy)
    {
        if (oldPolicy != null) {
            itsPolicies.remove(oldPolicy);
        }
        itsPolicies.add(newPolicy);
        updatePasswdPolicies(newPolicy);
    }

    @Override
    public boolean isDuplicatePolicy(String name)
    {
        // Shouldn't ever be called as record policy
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        PasswdSafeUtil.dbginfo(TAG, "onActivityResult data: %s", data);

        if ((requestCode == RECORD_SELECTION_REQUEST) &&
            (resultCode == Activity.RESULT_OK)) {
            setLinkRefUuid(data.getStringExtra(PasswdSafeApp.RESULT_DATA_UUID));
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void doOnCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.fragment_passwdsafe_edit_record, menu);
    }

    /**
     * Initialize the view
     */
    private void initialize()
    {
        useRecordFile(new RecordFileUser()
        {
            @Override
            public void useFile(@Nullable RecordInfo info,
                                @NonNull PasswdFileData fileData)
            {
                PwsRecord record;
                String group;
                if (info != null) {
                    record = info.itsRec;
                    itsUuid = fileData.getUUID(record);
                    itsIsV3 = fileData.isV3();
                    itsTitle.setText(fileData.getTitle(record));
                    group = fileData.getGroup(record);
                    itsUser.setText(fileData.getUsername(record));
                    itsHistory = fileData.getPasswdHistory(record);
                    itsNotes.setText(fileData.getNotes(record));

                    if (itsIsV3) {
                        itsUrl.setText(fileData.getURL(record));
                        itsEmail.setText(fileData.getEmail(record));
                        itsIsProtected = fileData.isProtected(record);
                        historyChanged(true);
                    } else {
                        GuiUtils.setVisible(itsUrlInput, false);
                        GuiUtils.setVisible(itsEmailInput, false);
                        GuiUtils.setVisible(itsHistoryGroup, false);
                    }
                } else {
                    record = null;
                    itsUuid = null;
                    itsIsV3 = fileData.isV3();
                    group = getLocation().getRecordGroup();
                }
                initGroup(group, fileData, record);
                initTypeAndPassword(info);
                initPasswdPolicy(info, fileData);
                initPasswdExpiry(info);
            }
        });
        updateProtected();
        itsValidator.validate();
    }

    /**
     * Initialize the group in the view
     */
    private void initGroup(@Nullable String initialGroup, 
                           @NonNull PasswdFileData fileData,
                           @Nullable PwsRecord editRec)
    {
        for (PwsRecord rec: fileData.getRecords()) {
            String group = fileData.getGroup(rec);
            if (!TextUtils.isEmpty(group)) {
                itsGroups.add(group);
            }

            if (rec != editRec) {
                RecordKey key = new RecordKey(fileData.getTitle(rec), group,
                                              fileData.getUsername(rec));
                itsRecordKeys.add(key);
            }
        }

        itsPrevGroupPos = updateGroups(initialGroup);
    }

    /**
     * Initialize the type and password
     */
    private void initTypeAndPassword(@Nullable RecordInfo info)
    {
        String password = null;
        PwsRecord linkRef = null;
        if (info != null) {
            itsRecOrigType = info.itsPasswdRec.getType();
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
        } else {
            itsRecOrigType = PasswdRecord.Type.NORMAL;
        }

        setType(itsRecOrigType, true);
        itsPasswordCurrent.setText(password);
        itsPassword.setText(password);
        itsPasswordConfirm.setText(password);
        PasswordVisibilityMenuHandler.set(itsPassword, itsPasswordCurrent,
                                          itsPasswordConfirm);

        if (itsIsV3) {
            setLinkRef(linkRef, (info != null) ? info.itsFileData : null);
        } else {
            GuiUtils.setVisible(itsTypeGroup, false);
        }
    }

    /**
     * Initialize the password policy
     */
    private void initPasswdPolicy(@Nullable RecordInfo info,
                                  @NonNull PasswdFileData fileData)
    {
        if (info != null) {
            itsOrigPolicy = info.itsPasswdRec.getPasswdPolicy();
        } else {
            itsOrigPolicy = null;
        }

        itsPolicies = new ArrayList<>();
        PasswdSafeApp app = (PasswdSafeApp)getActivity().getApplication();
        PasswdPolicy defPolicy = app.getDefaultPasswdPolicy();
        itsPolicies.add(defPolicy);
        HeaderPasswdPolicies hdrPolicies = fileData.getHdrPasswdPolicies();
        if (hdrPolicies != null) {
            for (HeaderPasswdPolicies.HdrPolicy hdrPolicy:
                    hdrPolicies.getPolicies()) {
                itsPolicies.add(hdrPolicy.getPolicy());
            }
        }

        PasswdPolicy customPolicy = null;
        if (itsIsV3) {
            String customName = getString(R.string.record_policy);
            if ((itsOrigPolicy != null) &&
                (itsOrigPolicy.getLocation() == PasswdPolicy.Location.RECORD)) {
                customPolicy = new PasswdPolicy(customName, itsOrigPolicy);
            } else {
                customPolicy = new PasswdPolicy(customName,
                                                PasswdPolicy.Location.RECORD);
            }
            itsPolicies.add(customPolicy);
        }

        itsPasswdPolicyView.setGenerateEnabled(false);

        PasswdPolicy selPolicy;
        if (itsOrigPolicy != null) {
            if (itsOrigPolicy.getLocation() ==
                PasswdPolicy.Location.RECORD_NAME) {
                if (hdrPolicies != null) {
                    selPolicy = hdrPolicies.getPasswdPolicy(
                            itsOrigPolicy.getName());
                } else {
                    selPolicy = null;
                }
            } else {
                selPolicy = customPolicy;
            }
        } else {
            selPolicy = defPolicy;
        }

        updatePasswdPolicies(selPolicy);
    }

    /**
     * Initialize the password expiration
     */
    @SuppressLint("SetTextI18n")
    private void initPasswdExpiry(@Nullable RecordInfo info)
    {
        if (itsIsV3) {
            itsOrigExpiry =
                    (info != null) ? info.itsPasswdRec.getPasswdExpiry() : null;

            itsExpiryDate = Calendar.getInstance();
            int interval;
            boolean recurring;
            PasswdExpiration.Type expireType;
            if (itsOrigExpiry == null) {
                expireType = PasswdExpiration.Type.NEVER;
                interval = PasswdExpiration.INTERVAL_DEFAULT;
                recurring = false;
            } else {
                if ((itsOrigExpiry.itsInterval != 0) &&
                    itsOrigExpiry.itsIsRecurring) {
                    expireType = PasswdExpiration.Type.INTERVAL;
                    interval = itsOrigExpiry.itsInterval;
                    recurring = true;
                } else {
                    expireType = PasswdExpiration.Type.DATE;
                    interval = PasswdExpiration.INTERVAL_DEFAULT;
                    recurring = false;
                }
                itsExpiryDate.setTime(itsOrigExpiry.itsExpiration);
            }
            updatePasswdExpiryDate();

            itsExpire.setSelection(expireType.itsStrIdx);
            updatePasswdExpiryChoice(expireType);
            itsExpireInterval.setText(Integer.toString(interval));
            itsExpireIntervalRecurring.setChecked(recurring);
        } else {
            GuiUtils.setVisible(itsExpireGroup, false);
        }
    }

    /**
     * Update the password policies
     */
    private void updatePasswdPolicies(PasswdPolicy selPolicy)
    {
        Collections.sort(itsPolicies);
        setSpinnerItems(itsPolicy, itsPolicies);
        int selItem = itsPolicies.indexOf(selPolicy);
        if (selItem < 0) {
            selItem = 0;
        }
        itsPolicy.setSelection(selItem);
    }

    /**
     * Select a password policy
     */
    private void selectPolicy(PasswdPolicy policy)
    {
        itsCurrPolicy = policy;
        itsPasswdPolicyView.showPolicy(itsCurrPolicy, -1);
        boolean canEdit =
                (itsCurrPolicy != null) &&
                (itsCurrPolicy.getLocation() == PasswdPolicy.Location.RECORD);
        GuiUtils.setVisible(itsPolicyEditBtn, canEdit);
    }

    /**
     * Update fields based on the password expiration choice changing
     */
    private void updatePasswdExpiryChoice(PasswdExpiration.Type type)
    {
        itsExpiryType = type;
        GuiUtils.setVisible(itsExpireDateFields,
                            itsExpiryType == PasswdExpiration.Type.DATE);
        GuiUtils.setVisible(itsExpireIntervalFields,
                            itsExpiryType == PasswdExpiration.Type.INTERVAL);
        itsValidator.validate();
    }

    /**
     * Update fields after the password expiration date changes
     */
    private void updatePasswdExpiryDate()
    {
        long expiryDate = itsExpiryDate.getTimeInMillis();
        Context ctx = getContext();
        itsExpireDateTime.setText(Utils.formatDate(expiryDate, ctx,
                                                   true, false, false));
        itsExpireDateDate.setText(Utils.formatDate(expiryDate, ctx,
                                                   false, true, false));
        itsValidator.validate();
    }

    /**
     * Update the view when the history changes
     */
    @SuppressLint("SetTextI18n")
    private void historyChanged(boolean updateMaxSize)
    {
        boolean historyExists = (itsHistory != null);
        itsHistoryAddRemoveBtn.setText(
                getString(historyExists ? R.string.remove : R.string.add));

        GuiUtils.setVisible(itsHistoryEnabled, historyExists);
        GuiUtils.setVisible(itsHistoryMaxSize, historyExists);
        GuiUtils.setVisible(itsHistoryList, historyExists);

        if (historyExists) {
            boolean historyEnabled = itsHistory.isEnabled() && !itsIsProtected;
            itsHistoryEnabled.setChecked(historyEnabled);
            itsHistoryMaxSize.setEnabled(historyEnabled);
            if (updateMaxSize) {
                itsHistoryMaxSize.setText(
                        Integer.toString(itsHistory.getMaxSize()));
            }

            ListAdapter histAdapter = PasswdHistory.createAdapter(
                    itsHistory, historyEnabled, true, getContext());
            itsHistoryList.setAdapter(histAdapter);
            GuiUtils.setListViewHeightBasedOnChildren(itsHistoryList);
            itsHistoryList.setEnabled(historyEnabled);
        }

        itsValidator.validate();
    }

    /**
     * Update the view when the history max size changes
     */
    private void historyMaxSizeChanged()
    {
        if (itsHistory != null) {
            int maxSize = getHistMaxSize();
            itsHistory.setMaxSize(maxSize);
            historyChanged(false);
        }
    }

    /**
     * Get the history max size
     */
    private int getHistMaxSize()
    {
        return getTextFieldInt(itsHistoryMaxSize, -1);
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
        GuiUtils.setVisible(itsUrlInput, itsIsV3 && itsTypeHasDetails);
        GuiUtils.setVisible(itsEmailInput, itsIsV3 && itsTypeHasDetails);
        GuiUtils.setVisible(itsPasswordLabel, itsTypeHasNormalPassword);
        GuiUtils.setVisible(itsPasswordFields, itsTypeHasNormalPassword);
        GuiUtils.setVisible(itsNotesLabel, itsTypeHasDetails);
        GuiUtils.setVisible(itsNotes, itsTypeHasDetails);

        itsValidator.validate();

        if (!init) {
            // Clear link on type change in case it is no longer valid
            setLinkRef(null, null);
        }
    }

    /**
     * Set the link to another record
     */
    private void setLinkRef(PwsRecord ref, PasswdFileData fileData)
    {
        String id;
        if (ref != null) {
            itsReferencedRecUuid = fileData.getUUID(ref);
            id = fileData.getId(ref);
        } else {
            itsReferencedRecUuid = null;
            id = "";
        }
        itsLinkRef.setText(id);
        itsValidator.validate();
    }

    /**
     * Set the link to another record by UUID
     */
    private void setLinkRefUuid(final String refUuid)
    {
        useRecordFile(new RecordFileUser()
        {
            @Override
            public void useFile(@Nullable RecordInfo info,
                                @NonNull PasswdFileData fileData)
            {
                setLinkRef(fileData.getRecord(refUuid), fileData);
            }
        });
    }

    /**
     * Set the visibility of the password fields
     */
    private void setPasswordVisibility(boolean visible)
    {
        GuiUtils.setPasswordVisible(itsPasswordCurrent, visible);
        GuiUtils.setPasswordVisible(itsPassword, visible);
        GuiUtils.setPasswordVisible(itsPasswordConfirm, visible);
    }

    /**
     * Set the password
     */
    private void setPassword(String password)
    {
        try {
            itsValidator.setPaused(true);
            itsPassword.setText(password);
            itsPasswordConfirm.setText(password);
        } finally {
            itsValidator.setPaused(false);
        }
        setPasswordVisibility(true);
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
     * Initialize the list of protected views
     */
    private void initProtViews(View v)
    {
        if ((v instanceof Spinner) || (v instanceof TextInputLayout) ||
            (v instanceof EditText) || (v instanceof Button)) {
            itsProtectViews.add(v);
        } else {
            switch (v.getId()) {
            case R.id.expire_date_date:
            case R.id.expire_date_time:
            case R.id.link_ref:
            case R.id.password_current:
            case R.id.password_generate: {
                itsProtectViews.add(v);
                break;
            }
            }
        }

        if (v instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)v;
            for (int i = 0; i < group.getChildCount(); ++i) {
                initProtViews(group.getChildAt(i));
            }
        }
    }

    /**
     * Update the UI when the protected state changes
     */
    private void updateProtected()
    {
        for (View v: itsProtectViews) {
            v.setEnabled(!itsIsProtected);
        }
        historyChanged(true);
    }

    /**
     * Update the menu item for protected
     */
    private void updateProtectedMenu(MenuItem protItem)
    {
        if (itsIsV3) {
            protItem.setChecked(itsIsProtected);
            protItem.setIcon(itsIsProtected ? R.drawable.ic_action_lock :
                                     R.drawable.ic_action_lock_open);
        } else {
            protItem.setVisible(false);
        }
    }

    /**
     * Save the record
     */
    private void saveRecord()
    {
        final ObjectHolder<Pair<Boolean, PasswdLocation>> rc =
                new ObjectHolder<>();
        useRecordFile(new RecordFileUser()
        {
            @Override
            public void useFile(@Nullable RecordInfo info,
                                @NonNull PasswdFileData fileData)
            {
                rc.set(updateSaveRecord(info, fileData));
            }
        });
        if (rc.get() != null) {
            getListener().finishEditRecord(rc.get().first, rc.get().second);
        }
    }

    /**
     * Save the updated fields in the record
     */
    private Pair<Boolean, PasswdLocation>
    updateSaveRecord(@Nullable RecordInfo info,
                     @NonNull PasswdFileData fileData)
    {
        PwsRecord record;
        boolean newRecord;
        if (info != null) {
            record = info.itsRec;
            newRecord = false;
        } else {
            record = fileData.createRecord();
            record.setLoaded();
            newRecord = true;
        }

        String updateStr;
        updateStr = getUpdatedField(fileData.getTitle(record), itsTitle);
        if (updateStr != null) {
            fileData.setTitle(updateStr, record);
        }

        updateStr = getUpdatedGroup(fileData.getGroup(record));
        if (updateStr != null) {
            fileData.setGroup(updateStr, record);
        }

        updateStr = getUpdatedField(fileData.getUsername(record), itsUser);
        if (updateStr != null) {
            fileData.setUsername(updateStr, record);
        }

        String currNotes = fileData.getNotes(record);
        if (itsTypeHasDetails) {
            updateStr = getUpdatedField(currNotes, itsNotes);
            if (updateStr != null) {
                fileData.setNotes(updateStr, record);
            }
        } else {
            if (currNotes != null) {
                fileData.setNotes(null, record);
            }
        }

        if (itsIsV3) {
            String currUrl = fileData.getURL(record);
            String currEmail = fileData.getEmail(record);
            PasswdHistory currHistory = fileData.getPasswdHistory(record);
            if (itsTypeHasDetails) {
                updateStr = getUpdatedField(currUrl, itsUrl);
                if (updateStr != null) {
                    fileData.setURL(updateStr, record);
                }

                updateStr = getUpdatedField(currEmail, itsEmail);
                if (updateStr != null) {
                    fileData.setEmail(updateStr, record);
                }
            } else {
                if (currUrl != null) {
                    fileData.setURL(null, record);
                }
                if (currEmail != null) {
                    fileData.setEmail(null, record);
                }
            }

            if (itsIsProtected != fileData.isProtected(record)) {
                fileData.setProtected(itsIsProtected, record);
            }

            Pair<Boolean, PasswdPolicy> updatePolicy = getUpdatedPolicy();
            if (updatePolicy.first) {
                fileData.setPasswdPolicy(updatePolicy.second, record);
            }

            if (itsTypeHasNormalPassword) {
                if (!PasswdHistory.isEqual(itsHistory, currHistory)) {
                    if (itsHistory != null) {
                        itsHistory.adjustEntriesToMaxSize();
                    }
                    fileData.setPasswdHistory(itsHistory, record, true);
                }
            } else {
                if (currHistory != null) {
                    fileData.setPasswdHistory(null, record, true);
                }
            }
        }

        // Update password after history so update is shown in new history
        String currPasswd = fileData.getPassword(record);
        String newPasswd;
        if (itsTypeHasNormalPassword) {
            newPasswd = getUpdatedField(currPasswd, itsPassword);
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
            newPasswd = PasswdRecord.uuidToPasswd(itsReferencedRecUuid,
                                                  itsRecType);
            if (newPasswd.equals(currPasswd)) {
                newPasswd = null;
            }
        }
        if (newPasswd != null) {
            fileData.setPassword(currPasswd, newPasswd, record);
            if (!itsTypeHasNormalPassword) {
                fileData.clearPasswdLastModTime(record);
            }
        }

        if (itsIsV3) {
            // Update expiration dates after password so changes in expiration
            // overwrite basic expiration updates when the password changes.
            Pair<Boolean, PasswdExpiration> updateExpiry = getUpdatedExpiry();
            if (updateExpiry.first) {
                fileData.setPasswdExpiry(updateExpiry.second, record);
            }
        }

        if (newRecord) {
            try {
                fileData.addRecord(record);
            } catch (PasswordSafeException e) {
                PasswdSafeUtil.showFatalMsg(e, "Error saving record: " + e,
                                            getActivity());
                return null;
            }
        }

        GuiUtils.setKeyboardVisible(itsTitle, getContext(), false);

        return new Pair<>(newRecord || record.isModified(),
                          new PasswdLocation(record, fileData));
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
     * Get the password policy that may have been updated
     */
    private Pair<Boolean, PasswdPolicy> getUpdatedPolicy()
    {
        PasswdPolicy.Location origLoc = PasswdPolicy.Location.DEFAULT;
        if (itsOrigPolicy != null) {
            origLoc = itsOrigPolicy.getLocation();
        }

        PasswdPolicy.Location currLoc = PasswdPolicy.Location.DEFAULT;
        if (itsCurrPolicy != null) {
            currLoc = itsCurrPolicy.getLocation();
        }

        boolean policyChanged = false;
        switch (origLoc) {
        case DEFAULT: {
            if (currLoc != origLoc) {
                policyChanged = true;
            }
            break;
        }
        case HEADER:
        case RECORD_NAME: {
            switch (currLoc) {
            case DEFAULT:
            case RECORD: {
                policyChanged = true;
                break;
            }
            case HEADER:
            case RECORD_NAME: {
                if (!itsOrigPolicy.getName().equals(itsCurrPolicy.getName())) {
                    policyChanged = true;
                }
                break;
            }
            }
            break;
        }
        case RECORD: {
            switch (currLoc) {
            case DEFAULT:
            case HEADER:
            case RECORD_NAME: {
                policyChanged = true;
                break;
            }
            case RECORD: {
                if (!itsOrigPolicy.recordPolicyEquals(itsCurrPolicy)) {
                    policyChanged = true;
                }
                break;
            }
            }
            break;
        }
        }

        PasswdPolicy updatedPolicy = null;
        if (policyChanged && (currLoc != PasswdPolicy.Location.DEFAULT)) {
            updatedPolicy = itsCurrPolicy;
        }

        return new Pair<>(policyChanged, updatedPolicy);
    }

    /**
     * Get the password expiration that may have been updated
     */
    private Pair<Boolean, PasswdExpiration> getUpdatedExpiry()
    {
        // Get the updated expiration
        PasswdExpiration updatedExpiry = null;
        switch (itsRecType) {
        case NORMAL: {
            switch (itsExpiryType) {
            case NEVER: {
                updatedExpiry = null;
                break;
            }
            case DATE: {
                updatedExpiry = new PasswdExpiration(itsExpiryDate.getTime(),
                                                     0, false);
                break;
            }
            case INTERVAL: {
                int interval = getTextFieldInt(
                        itsExpireInterval, PasswdExpiration.INTERVAL_DEFAULT);
                long exp = System.currentTimeMillis();
                exp += (long)interval * DateUtils.DAY_IN_MILLIS;
                exp -= (exp % DateUtils.MINUTE_IN_MILLIS);
                Date expiry = new Date(exp);

                if (itsExpireIntervalRecurring.isChecked()) {
                    updatedExpiry = new PasswdExpiration(expiry, interval,
                                                         true);
                } else {
                    updatedExpiry = new PasswdExpiration(expiry, 0, false);
                }
                break;
            }
            }
            break;
        }
        case ALIAS:
        case SHORTCUT: {
            updatedExpiry = null;
            break;
        }
        }

        return new Pair<>(!PasswdExpiration.isEqual(itsOrigExpiry,
                                                    updatedExpiry),
                          updatedExpiry);
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
     * Get the integer value of a field
     */
    private static int getTextFieldInt(TextView tv, int defaultValue)
    {
        try {
            return Integer.parseInt(tv.getText().toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Validator
     */
    private class Validator extends AbstractTextWatcher
    {
        private boolean itsIsValid = false;
        private boolean itsIsPaused = false;

        /**
         * Register a text view with the validator
         */
        public final void registerTextView(TextView field)
        {
            field.addTextChangedListener(this);
        }

        /**
         * Set whether the validator is paused.  Validation will be performed
         * if not paused.
         */
        public final void setPaused(boolean paused)
        {
            itsIsPaused = paused;
            if (!paused) {
                validate();
            }
        }

        /**
         * Validate
         */
        public final void validate()
        {
            if (itsIsPaused) {
                return;
            }

            String typeError = null;
            switch (itsRecType) {
            case NORMAL: {
                break;
            }
            case ALIAS: {
                if (itsReferencedRecUuid == null) {
                    typeError = getString(R.string.no_alias_chosen);
                    break;
                }
                if (TextUtils.equals(itsReferencedRecUuid, itsUuid)) {
                    typeError = getString(R.string.alias_to_same_record);
                }
                break;
            }
            case SHORTCUT: {
                if (itsReferencedRecUuid == null) {
                    typeError = getString(R.string.no_shortcut_chosen);
                    break;
                }
                if (TextUtils.equals(itsReferencedRecUuid, itsUuid)) {
                    typeError = getString(R.string.shortcut_to_same_record);
                }
                break;
            }
            }
            GuiUtils.setVisible(itsTypeError, (typeError != null));
            itsTypeError.setText(typeError);

            boolean valid = (typeError == null);
            valid &= !TextInputUtils.setTextInputError(validateTitle(),
                                                       itsTitleInput);
            valid &= !TextInputUtils.setTextInputError(validatePassword(),
                                                       itsPasswordInput);
            valid &= !TextInputUtils.setTextInputError(
                    validatePasswordConfirm(), itsPasswordConfirmInput);

            if (itsIsV3) {
                boolean invalidExpiryDate = false;
                switch (itsExpiryType) {
                case NEVER: {
                    break;
                }
                case DATE: {
                    long now = System.currentTimeMillis();
                    long expiry = itsExpiryDate.getTimeInMillis();
                    invalidExpiryDate = (expiry < now);
                    break;
                }
                case INTERVAL: {
                    valid &= !TextInputUtils.setTextInputError(
                            validateExpiryInterval(), itsExpireIntervalInput);
                    break;
                }
                }
                GuiUtils.setVisible(itsExpireDateError, invalidExpiryDate);
                valid &= !invalidExpiryDate;
            }

            boolean invalidHistory = false;
            if (itsHistory != null) {
                int histMaxSize = getHistMaxSize();
                if ((histMaxSize < PasswdHistory.MAX_SIZE_MIN) ||
                    (histMaxSize > PasswdHistory.MAX_SIZE_MAX)) {
                    invalidHistory = true;
                }
            }
            valid &= !TextInputUtils.setTextInputError(
                    invalidHistory ?
                            getString(R.string.invalid_history_max_size,
                                      PasswdHistory.MAX_SIZE_MIN,
                                      PasswdHistory.MAX_SIZE_MAX) : null,
                    itsHistoryMaxSizeInput);

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
         * Validate the password field
         * @return error message if invalid; null if valid
         */
        private String validatePassword()
        {
            switch (itsRecType) {
            case NORMAL: {
                if (itsPassword.getText().length() == 0) {
                    return getString(R.string.empty_password);
                }
                break;
            }
            case ALIAS:
            case SHORTCUT: {
                break;
            }
            }
            return null;
        }

        /**
         * Validate the password confirm field
         * @return error message if invalid; null if valid
         */
        private String validatePasswordConfirm()
        {
            switch (itsRecType) {
            case NORMAL: {
                if (!TextUtils.equals(itsPassword.getText(),
                                      itsPasswordConfirm.getText())) {
                    return getString(R.string.passwords_do_not_match);
                }
                break;
            }
            case ALIAS:
            case SHORTCUT: {
                break;
            }
            }
            return null;
        }

        /**
         * Validate the password expiration field
         * @return error message if invalid; null if valid
         */
        private String validateExpiryInterval()
        {
            int interval = getTextFieldInt(itsExpireInterval, -1);
            return ((interval < PasswdExpiration.VALID_INTERVAL_MIN) ||
                    (interval > PasswdExpiration.VALID_INTERVAL_MAX)) ?
                    getString(R.string.password_expiration_invalid_interval) :
                    null;
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
