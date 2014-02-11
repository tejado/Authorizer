/*
 * Copyright (©) 2010-2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import org.pwsafe.lib.file.PwsRecord;

import com.jefftharris.passwdsafe.file.HeaderPasswdPolicies;
import com.jefftharris.passwdsafe.file.PasswdExpiration;
import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdHistory;
import com.jefftharris.passwdsafe.file.PasswdPolicy;
import com.jefftharris.passwdsafe.file.PasswdRecord;
import com.jefftharris.passwdsafe.file.PasswdRecord.Type;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.Utils;
import com.jefftharris.passwdsafe.lib.view.AbstractDialogClickListener;
import com.jefftharris.passwdsafe.util.Pair;
import com.jefftharris.passwdsafe.view.AbstractTextWatcher;
import com.jefftharris.passwdsafe.view.DialogValidator;
import com.jefftharris.passwdsafe.view.GuiUtils;
import com.jefftharris.passwdsafe.view.PasswdPolicyEditDialog;
import com.jefftharris.passwdsafe.view.PasswdPolicyView;
import com.jefftharris.passwdsafe.view.PasswordVisibilityMenuHandler;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.AdapterView.OnItemSelectedListener;

public class RecordEditActivity extends AbstractRecordActivity
    implements PasswdPolicyEditDialog.Editor
{
    private static final String TAG = "RecordEditActivity";

    private static final int DIALOG_NEW_GROUP = MAX_DIALOG + 1;
    private static final int DIALOG_EDIT_POLICY = MAX_DIALOG + 2;
    private static final int DIALOG_PASSWD_EXPIRY_TIME = MAX_DIALOG + 3;
    private static final int DIALOG_PASSWD_EXPIRY_DATE = MAX_DIALOG + 4;

    private static final int MENU_TOGGLE_PASSWORD = 3;
    private static final int MENU_GENERATE_PASSWORD = 4;
    private static final int MENU_CANCEL = 5;

    private static final int CTXMENU_REMOVE = 1;
    private static final int CTXMENU_SET_PASSWORD = 2;

    private static final int RECORD_SELECTION_REQUEST = 0;

    /** Insert intent extra field for the group of the new record */
    public static final String INSERT_INTENT_EXTRA_GROUP = "group";

    private TreeSet<String> itsGroups =
        new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    private String itsPrevGroup;
    private HashSet<V3Key> itsRecordKeys = new HashSet<V3Key>();
    private DialogValidator itsValidator;
    private PasswdPolicyEditDialog itsPolicyEditDialog;
    private List<PasswdPolicy> itsPolicies;
    private PasswdPolicy itsOrigPolicy;
    private PasswdPolicy itsCurrPolicy;
    private PasswdExpiration itsOrigExpiry;
    private PasswdExpiration.Type itsExpiryType = PasswdExpiration.Type.NEVER;
    private Calendar itsExpiryDate;
    private PasswdHistory itsHistory;
    private boolean itsIsV3 = false;
    private PasswdRecord.Type itsType = Type.NORMAL;
    private boolean itsTypeHasNormalPassword = true;
    private boolean itsTypeHasDetails = true;
    private PasswdRecord.Type itsOrigType = Type.NORMAL;
    private PwsRecord itsLinkRef = null;
    private ArrayList<View> itsProtectViews = new ArrayList<View>();
    private boolean itsIsProtected = false;

    // Constants must match record_type strings
    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_ALIAS = 1;
    private static final int TYPE_SHORTCUT = 2;


    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.record_edit);
        itsValidator = new Validator();

        PasswdFileData fileData = getPasswdFile().getFileData();
        if (fileData == null) {
            PasswdSafeUtil.showFatalMsg("File not open", this);
            return;
        }

        itsIsV3 = fileData.isV3();
        itsIsProtected = false;
        PasswdRecord passwdRecord = null;
        String group = null;
        String uuid = getUUID();
        CheckBox protCb = (CheckBox)findViewById(R.id.protected_record);
        if (uuid != null) {
            PwsRecord record = fileData.getRecord(uuid);
            if (record == null) {
                PasswdSafeUtil.showFatalMsg("Unknown record: " + uuid, this);
                return;
            }
            passwdRecord = fileData.getPasswdRecord(record);
            if (passwdRecord == null) {
                PasswdSafeUtil.showFatalMsg("Unknown passwd record: " + uuid,
                                           this);
                return;
            }

            getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
            setText(R.id.rec_title, getString(R.string.edit_item,
                                              fileData.getTitle(record)));
            setText(R.id.title, fileData.getTitle(record));
            group = fileData.getGroup(record);
            setText(R.id.user, fileData.getUsername(record));
            setText(R.id.notes, fileData.getNotes(record));

            if (itsIsV3) {
                setText(R.id.url, fileData.getURL(record));
                setText(R.id.email, fileData.getEmail(record));
                itsIsProtected = fileData.isProtected(record);
                protCb.setChecked(itsIsProtected);
            }

            itsHistory = fileData.getPasswdHistory(record);
        } else {
            setText(R.id.rec_title, getString(R.string.new_entry));
            setText(R.id.title, null);
            setText(R.id.user, null);
            setText(R.id.notes, null);

            if (itsIsV3) {
                setText(R.id.url, null);
                setText(R.id.email, null);
            }

            Intent newIntent = getIntent();
            group = newIntent.getStringExtra(INSERT_INTENT_EXTRA_GROUP);
        }
        if (!itsIsV3) {
            setVisibility(R.id.url_row, false);
            setVisibility(R.id.email_row, false);
            setVisibility(R.id.protected_row, false);
        }

        initTypeAndPassword(fileData, passwdRecord);
        initGroup(fileData, passwdRecord, group);
        initPasswdPolicy(fileData, passwdRecord);
        initPasswdExpiry(fileData, passwdRecord);

        if (itsIsV3) {
            TextView tv = (TextView)findViewById(R.id.history_max_size);
            tv.addTextChangedListener(new AbstractTextWatcher()
            {
                public void afterTextChanged(Editable s)
                {
                    if (itsHistory != null) {
                        int maxSize = getHistMaxSize();
                        itsHistory.setMaxSize(maxSize);
                        historyChanged(false);
                    }
                }
            });
            View view = findViewById(R.id.history);
            registerForContextMenu(view);
            historyChanged(true);
        } else {
            findViewById(R.id.history_group_sep).setVisibility(View.GONE);
            findViewById(R.id.history_group).setVisibility(View.GONE);
        }

        Button button = (Button)findViewById(R.id.done_btn);
        button.setOnClickListener(new OnClickListener()
        {
            public final void onClick(View v)
            {
                saveRecord();
            }
        });

        button = (Button)findViewById(R.id.history_addremove);
        button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                if (itsHistory == null) {
                    itsHistory = new PasswdHistory();
                } else {
                    itsHistory = null;
                }
                historyChanged(true);
            }
        });

        CheckBox cb = (CheckBox)findViewById(R.id.history_enabled);
        cb.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                if (itsHistory != null) {
                    itsHistory.setEnabled(!itsHistory.isEnabled());
                }
                historyChanged(true);
            }
        });

        protCb.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                itsIsProtected = ((CheckBox)v).isChecked();
                updateProtected();
            }
        });

        initProtViews(findViewById(R.id.base_group));
        initProtViews(findViewById(R.id.policy_label));
        initProtViews(findViewById(R.id.policy_group));
        initProtViews(findViewById(R.id.policy_view));
        initProtViews(findViewById(R.id.expire_label));
        initProtViews(findViewById(R.id.expire_choice));
        initProtViews(findViewById(R.id.expire_date_fields));
        initProtViews(findViewById(R.id.expire_interval_fields));
        initProtViews(findViewById(R.id.history_group));
        initProtViews(findViewById(R.id.notes_label));
        initProtViews(findViewById(R.id.notes));
        updateProtected();
        itsValidator.validate();
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.view.PasswdPolicyEditDialog.Editor#onPolicyEditComplete(com.jefftharris.passwdsafe.file.PasswdPolicy, com.jefftharris.passwdsafe.file.PasswdPolicy)
     */
    public void onPolicyEditComplete(PasswdPolicy oldPolicy,
                                     PasswdPolicy newPolicy)
    {
        if (oldPolicy != null) {
            itsPolicies.remove(oldPolicy);
        }
        itsPolicies.add(newPolicy);
        updatePasswdPolicies(newPolicy);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.view.PasswdPolicyEditDialog.Editor#isDuplicatePolicy(java.lang.String)
     */
    public boolean isDuplicatePolicy(String name)
    {
        // Shouldn't ever be called
        return true;
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        ActivityPasswdFile passwdFile = getPasswdFile();
        if (passwdFile != null) {
            passwdFile.resumeFileTimer();
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        PasswdSafeUtil.dbginfo(TAG, "onResume");
        ActivityPasswdFile passwdFile = getPasswdFile();
        if (passwdFile != null) {
            passwdFile.pauseFileTimer();
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog = null;
        switch (id) {
        case DIALOG_NEW_GROUP:
        {
            LayoutInflater factory = LayoutInflater.from(this);
            final View view = factory.inflate(R.layout.new_group, null);
            AbstractDialogClickListener dlgClick =
                new AbstractDialogClickListener()
            {
                @Override
                public void onOkClicked(DialogInterface dialog)
                {
                    EditText newGroup = (EditText)
                        view.findViewById(R.id.new_group);
                    setGroup(newGroup.getText().toString());
                }

                @Override
                public void onCancelClicked(DialogInterface dialog)
                {
                    setGroup(itsPrevGroup);
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(PasswdSafeUtil.getAppTitle(this))
                .setMessage(R.string.enter_net_group)
                .setView(view)
                .setPositiveButton(R.string.ok, dlgClick)
                .setNegativeButton(R.string.cancel, dlgClick)
                .setOnCancelListener(dlgClick);
            AlertDialog alertDialog = builder.create();
            TextView tv = (TextView)view.findViewById(R.id.new_group);
            GuiUtils.setupDialogKeyboard(alertDialog, tv, tv, this);
            dialog = alertDialog;
            break;
        }
        case DIALOG_EDIT_POLICY: {
            itsPolicyEditDialog = new PasswdPolicyEditDialog(this);
            dialog = itsPolicyEditDialog.create(itsCurrPolicy, this);
            break;
        }
        case DIALOG_PASSWD_EXPIRY_TIME: {
            dialog = new TimePickerDialog(
                this,
                new TimePickerDialog.OnTimeSetListener()
                {
                    public void onTimeSet(TimePicker view,
                                          int hourOfDay, int minute)
                    {
                        itsExpiryDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        itsExpiryDate.set(Calendar.MINUTE, minute);
                        itsExpiryDate.set(Calendar.SECOND, 0);
                        updatePasswdExpiryDate();
                    }
                },
                itsExpiryDate.get(Calendar.HOUR_OF_DAY),
                itsExpiryDate.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(this));
            break;
        }
        case DIALOG_PASSWD_EXPIRY_DATE: {
            dialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener()
                {
                    public void onDateSet(DatePicker view,
                                          int year, int monthOfYear,
                                          int dayOfMonth)
                    {
                        itsExpiryDate.set(Calendar.YEAR, year);
                        itsExpiryDate.set(Calendar.MONTH, monthOfYear);
                        itsExpiryDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updatePasswdExpiryDate();
                    }
                },
                itsExpiryDate.get(Calendar.YEAR),
                itsExpiryDate.get(Calendar.MONTH),
                itsExpiryDate.get(Calendar.DAY_OF_MONTH));
            break;
        }
        default:
        {
            dialog = super.onCreateDialog(id);
            break;
        }
        }
        return dialog;
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
     */
    @SuppressWarnings("deprecation")
    @Override
    protected void onPrepareDialog(int id, Dialog dialog)
    {
        switch (id) {
        case DIALOG_EDIT_POLICY: {
            itsPolicyEditDialog.reset();
            break;
        }
        case DIALOG_PASSWD_EXPIRY_TIME: {
            TimePickerDialog dlg = (TimePickerDialog)dialog;
            dlg.updateTime(itsExpiryDate.get(Calendar.HOUR_OF_DAY),
                           itsExpiryDate.get(Calendar.MINUTE));
            break;
        }
        case DIALOG_PASSWD_EXPIRY_DATE: {
            DatePickerDialog dlg = (DatePickerDialog)dialog;
            dlg.updateDate(itsExpiryDate.get(Calendar.YEAR),
                           itsExpiryDate.get(Calendar.MONTH),
                           itsExpiryDate.get(Calendar.DAY_OF_MONTH));
            break;
        }
        default: {
            super.onPrepareDialog(id, dialog);
            break;
        }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, MENU_CANCEL, 0, R.string.cancel);
        menu.add(0, MENU_TOGGLE_PASSWORD, 0, R.string.show_password);
        menu.add(0, MENU_GENERATE_PASSWORD, 0, R.string.generate_password);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        boolean userPassword = true;
        switch (itsType) {
        case NORMAL: {
            userPassword = true;
            break;
        }
        case ALIAS:
        case SHORTCUT: {
            userPassword = false;
            break;
        }
        }

        MenuItem mi = menu.findItem(MENU_TOGGLE_PASSWORD);
        if (mi != null) {
            mi.setEnabled(userPassword);
            TextView tv = (TextView)findViewById(R.id.password);
            boolean visible = GuiUtils.isPasswordVisible(tv);
            mi.setTitle(visible ? R.string.hide_passwords :
                        R.string.show_passwords);
        }

        mi = menu.findItem(MENU_GENERATE_PASSWORD);
        if (mi != null) {
            mi.setEnabled(userPassword && !itsIsProtected);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case MENU_TOGGLE_PASSWORD:
        {
            TextView passwdField = (TextView)findViewById(R.id.password);
            setPasswordVisibility(
                !GuiUtils.isPasswordVisible(passwdField),
                passwdField,
                (TextView)findViewById(R.id.password_confirm));
            break;
        }
        case MENU_GENERATE_PASSWORD:
        {
            generatePassword();
            break;
        }
        case MENU_CANCEL:
        {
            finish();
            break;
        }
        default:
        {
            return super.onOptionsItemSelected(item);
        }
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);

        ListView histView = (ListView)findViewById(R.id.history);
        if ((v == histView) && histView.isEnabled()) {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
            List<PasswdHistory.Entry> passwds = itsHistory.getPasswds();
            if ((info.position >= 0) && (info.position < passwds.size())) {
                menu.setHeaderTitle(passwds.get(info.position).getPasswd());
                menu.add(0, CTXMENU_REMOVE, 0, R.string.remove);
                menu.add(0, CTXMENU_SET_PASSWORD, 0, R.string.set_password);
            }
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterContextMenuInfo info =
            (AdapterContextMenuInfo)item.getMenuInfo();

        switch (item.getItemId()) {
        case CTXMENU_REMOVE:
        {
            List<PasswdHistory.Entry> passwds = itsHistory.getPasswds();
            if ((info.position >= 0) && (info.position < passwds.size())) {
                passwds.remove(info.position);
                historyChanged(true);
            }
            return true;
        }
        case CTXMENU_SET_PASSWORD:
        {
            List<PasswdHistory.Entry> passwds = itsHistory.getPasswds();
            if ((info.position >= 0) && (info.position < passwds.size())) {
                setPassword(passwds.get(info.position).getPasswd());
            }
            return true;
        }
        default:
        {
            return super.onContextItemSelected(item);
        }
        }
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        PasswdSafeUtil.dbginfo(TAG, "onActivityResult data: %s", data);

        if ((requestCode == RECORD_SELECTION_REQUEST) &&
            (resultCode == RESULT_OK)) {
            String uuid = data.getStringExtra(PasswdSafeApp.RESULT_DATA_UUID);
            PasswdFileData fileData = getPasswdFile().getFileData();
            if (fileData == null) {
                return;
            }
            setLinkRef(fileData.getRecord(uuid), fileData);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    private final void initTypeAndPassword(PasswdFileData fileData,
                                           PasswdRecord record)
    {
        itsOrigType = Type.NORMAL;
        PwsRecord linkRef = null;
        String password = null;

        if (record != null) {
            itsOrigType = record.getType();

            switch (itsOrigType) {
            case NORMAL: {
                password = fileData.getPassword(record.getRecord());
                break;
            }
            case ALIAS:
            case SHORTCUT: {
                linkRef = record.getRef();
                break;
            }
            }
        }

        if (itsIsV3) {
            Spinner typeSpin = (Spinner)findViewById(R.id.type);
            typeSpin.setOnItemSelectedListener(new OnItemSelectedListener()
            {
                public void onItemSelected(AdapterView<?> parent, View arg1,
                                           int position, long id)
                {
                    PasswdRecord.Type type = Type.NORMAL;
                    switch (position) {
                    case TYPE_NORMAL: {
                        type = Type.NORMAL;
                        break;
                    }
                    case TYPE_ALIAS: {
                        type = Type.ALIAS;
                        break;
                    }
                    case TYPE_SHORTCUT: {
                        type = Type.SHORTCUT;
                        break;
                    }
                    }
                    setType(type, false);
                }

                public void onNothingSelected(AdapterView<?> arg0)
                {
                    setType(Type.NORMAL, false);
                }
            });

            View passwdLink = findViewById(R.id.password_link);
            passwdLink.setOnClickListener(new OnClickListener()
            {
                public void onClick(View v)
                {
                    Intent intent =
                        new Intent(PasswdSafeApp.CHOOSE_RECORD_INTENT,
                                   getIntent().getData(),
                                   RecordEditActivity.this,
                                   RecordSelectionActivity.class);
                    // Do not allow mixed alias and shortcut references to a
                    // record to work around a bug in Password Safe that does
                    // not allow both
                    switch (itsType) {
                    case NORMAL: {
                        break;
                    }
                    case ALIAS: {
                        intent.putExtra(
                            RecordSelectionActivity.FILTER_NO_SHORTCUT, true);
                        break;
                    }
                    case SHORTCUT: {
                        intent.putExtra(
                            RecordSelectionActivity.FILTER_NO_ALIAS, true);
                        break;
                    }
                    }

                    startActivityForResult(intent,
                                           RECORD_SELECTION_REQUEST);
                }
            });
        } else {
            setVisibility(R.id.type_row, false);
            setVisibility(R.id.password_link_row, false);
        }

        setType(itsOrigType, true);
        TextView passwdField = setText(R.id.password, password);
        TextView confirmField = setText(R.id.password_confirm, password);
        PasswordVisibilityMenuHandler.set(passwdField, confirmField);
        setLinkRef(linkRef, fileData);
    }

    private final void setType(PasswdRecord.Type type, boolean init)
    {
        if ((type == itsType) && !init) {
            return;
        }
        // Prev type needs to be updated before setting spinner to prevent
        // recursion
        itsType = type;
        GuiUtils.invalidateOptionsMenu(this);

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
            Spinner typeSpin = (Spinner) findViewById(R.id.type);
            typeSpin.setSelection(pos);
        }

        itsTypeHasNormalPassword = true;
        itsTypeHasDetails = true;
        int passwordLinkLabel = 0;
        switch (type) {
        case NORMAL: {
            itsTypeHasNormalPassword = true;
            itsTypeHasDetails = true;
            break;
        }
        case ALIAS: {
            itsTypeHasNormalPassword = false;
            itsTypeHasDetails = true;
            passwordLinkLabel = R.string.alias_base_record_label;
            break;
        }
        case SHORTCUT: {
            itsTypeHasNormalPassword = false;
            itsTypeHasDetails = false;
            passwordLinkLabel = R.string.shortcut_base_record_label;
            break;
        }
        }
        setVisibility(R.id.password_row, itsTypeHasNormalPassword);
        setVisibility(R.id.password_confirm_row, itsTypeHasNormalPassword);
        setVisibility(R.id.password_link_row, !itsTypeHasNormalPassword);
        if (passwordLinkLabel != 0) {
            TextView tv = (TextView)findViewById(R.id.password_link_label);
            tv.setText(passwordLinkLabel);
        }
        setVisibility(R.id.url_row, itsIsV3 && itsTypeHasDetails);
        setVisibility(R.id.email_row, itsIsV3 && itsTypeHasDetails);
        setVisibility(R.id.notes_sep, itsTypeHasDetails);
        setVisibility(R.id.notes_label, itsTypeHasDetails);
        setVisibility(R.id.notes, itsTypeHasDetails);
        setVisibility(R.id.policy_sep, itsTypeHasNormalPassword);
        setVisibility(R.id.policy_label, itsTypeHasNormalPassword);
        setVisibility(R.id.policy_group, itsTypeHasNormalPassword);
        setVisibility(R.id.policy_view, itsTypeHasNormalPassword);
        setVisibility(R.id.expire_sep, itsTypeHasNormalPassword);
        setVisibility(R.id.expire_label, itsTypeHasNormalPassword);
        setVisibility(R.id.expire_choice, itsTypeHasNormalPassword);
        setVisibility(R.id.expire_date_fields, itsTypeHasNormalPassword);
        setVisibility(R.id.expire_interval_fields, itsTypeHasNormalPassword);
        setVisibility(R.id.history_group_sep, itsTypeHasNormalPassword);
        setVisibility(R.id.history_group, itsTypeHasNormalPassword);
        itsValidator.validate();

        if (!init) {
            // Clear link on type change in case it is no longer valid
            setLinkRef(null, null);
        }
    }

    private void setLinkRef(PwsRecord ref, PasswdFileData fileData)
    {
        itsLinkRef = ref;
        String id = "";
        if (itsLinkRef != null) {
            id = fileData.getId(itsLinkRef);
        }
        TextView tv = (TextView)findViewById(R.id.password_link);
        tv.setText(id);
        itsValidator.validate();
    }

    private final void setPasswordVisibility(boolean visible,
                                             TextView passwdField,
                                             TextView confirmField)
    {
        GuiUtils.setPasswordVisible(passwdField, visible);
        GuiUtils.setPasswordVisible(confirmField, visible);
    }

    private final void generatePassword()
    {
        if (itsCurrPolicy != null) {
            try {
                String passwd = itsCurrPolicy.generate();
                setPassword(passwd);
            } catch (Exception e) {
                PasswdSafeUtil.showFatalMsg(e, this);
            }
        }
    }

    private final void setPassword(String passwd)
    {
        TextView passwdField = (TextView)findViewById(R.id.password);
        TextView confirmField = (TextView)findViewById(R.id.password_confirm);
        passwdField.setText(passwd);
        confirmField.setText(passwd);
        setPasswordVisibility(true, passwdField, confirmField);
    }

    private final void initGroup(PasswdFileData fileData,
                                 PasswdRecord editRecord,
	    			 String group)
    {
        PwsRecord editRec =
            (editRecord != null) ? editRecord.getRecord() : null;
        ArrayList<PwsRecord> records = fileData.getRecords();
        for (PwsRecord rec : records) {
            String grp = fileData.getGroup(rec);
            if ((grp != null) && (grp.length() != 0)) {
                itsGroups.add(grp);
            }

            if (rec != editRec) {
                V3Key key = new V3Key(fileData.getTitle(rec), grp,
                                      fileData.getUsername(rec));
                itsRecordKeys.add(key);
            }
        }

        itsPrevGroup = group;
        updateGroups(group);
        Spinner s = (Spinner)findViewById(R.id.group);
        s.setOnItemSelectedListener(new OnItemSelectedListener()
        {
            @SuppressWarnings("deprecation")
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id)
            {
                Adapter adapter = parent.getAdapter();
                if (position == (adapter.getCount() - 1)) {
                    showDialog(DIALOG_NEW_GROUP);
                } else {
                    itsPrevGroup = parent.getSelectedItem().toString();
                    itsValidator.validate();
                }
            }

            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });
    }

    private final void setGroup(String newGroup)
    {
        if ((newGroup != null) && (newGroup.length() != 0)) {
            itsGroups.add(newGroup);
        }
        itsPrevGroup = newGroup;
        updateGroups(newGroup);
        itsValidator.validate();
    }

    private final void updateGroups(String selGroup)
    {
        ArrayList<String> groupList =
            new ArrayList<String>(itsGroups.size() + 2);
        groupList.add("");
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
        Spinner s = setSpinnerItems(R.id.group, groupList);
        if (groupPos != 0) {
            s.setSelection(groupPos);
        }
    }

    /** Initialize the password policy */
    private final void initPasswdPolicy(PasswdFileData fileData,
                                        PasswdRecord record)
    {
        itsOrigPolicy = null;
        if (record != null) {
            itsOrigPolicy = record.getPasswdPolicy();
        }

        itsPolicies = new ArrayList<PasswdPolicy>();
        PasswdPolicy defPolicy = getPasswdSafeApp().getDefaultPasswdPolicy();
        itsPolicies.add(defPolicy);
        HeaderPasswdPolicies hdrPolicies =
            fileData.getHdrPasswdPolicies();
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

        PasswdPolicyView view =
            (PasswdPolicyView)findViewById(R.id.policy_view);
        view.setGenerateEnabled(false);

        Button editBtn = (Button)findViewById(R.id.policy_edit);
        editBtn.setOnClickListener(new OnClickListener()
        {
            @SuppressWarnings("deprecation")
            public void onClick(View v)
            {
                removeDialog(DIALOG_EDIT_POLICY);
                showDialog(DIALOG_EDIT_POLICY);
            }
        });

        Spinner spin = (Spinner)findViewById(R.id.policy);
        spin.setOnItemSelectedListener(new OnItemSelectedListener()
        {
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id)
            {
                selectPolicy((PasswdPolicy)parent.getSelectedItem());
            }

            public void onNothingSelected(AdapterView<?> arg0)
            {
                selectPolicy(null);
            }
        });

        PasswdPolicy selPolicy = null;
        if (itsOrigPolicy != null) {
            if (itsOrigPolicy.getLocation() ==
                PasswdPolicy.Location.RECORD_NAME) {
                selPolicy =
                    hdrPolicies.getPasswdPolicy(itsOrigPolicy.getName());
            } else {
                selPolicy = customPolicy;
            }
        } else {
            selPolicy = defPolicy;
        }

        updatePasswdPolicies(selPolicy);
    }

    /** Update the password policies */
    private final void updatePasswdPolicies(PasswdPolicy selPolicy)
    {
        Collections.sort(itsPolicies);
        Spinner spin = setSpinnerItems(R.id.policy, itsPolicies);

        int selItem = itsPolicies.indexOf(selPolicy);
        if (selItem < 0) {
            selItem = 0;
        }
        spin.setSelection(selItem);

    }

    /** Select a password policy */
    private final void selectPolicy(PasswdPolicy policy)
    {
        itsCurrPolicy = policy;
        PasswdPolicyView view =
            (PasswdPolicyView)findViewById(R.id.policy_view);
        view.showPolicy(itsCurrPolicy, -1);

        View editBtn = findViewById(R.id.policy_edit);
        boolean canEdit = (itsCurrPolicy != null) &&
            (itsCurrPolicy.getLocation() == PasswdPolicy.Location.RECORD);
        editBtn.setVisibility(canEdit ? View.VISIBLE : View.GONE);
    }

    /** Initialize password expiration fields */
    private final void initPasswdExpiry(PasswdFileData fileData,
                                        PasswdRecord passwdRecord)
    {
        if (!itsIsV3) {
            setVisibility(R.id.expire_sep, false);
            setVisibility(R.id.expire_label, false);
            setVisibility(R.id.expire_choice, false);
            setVisibility(R.id.expire_date_fields, false);
            setVisibility(R.id.expire_interval_fields, false);
            return;
        }

        if (passwdRecord == null) {
            itsOrigExpiry = null;
        } else {
            itsOrigExpiry = passwdRecord.getPasswdExpiry();
        }

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

        OnClickListener dateListener = new OnClickListener()
        {
            @SuppressWarnings("deprecation")
            public void onClick(View v)
            {
                switch (v.getId()) {
                case R.id.expire_date_time: {
                    showDialog(DIALOG_PASSWD_EXPIRY_TIME);
                    break;
                }
                case R.id.expire_date_date: {
                    showDialog(DIALOG_PASSWD_EXPIRY_DATE);
                    break;
                }
                }
            }
        };
        TextView tv = (TextView)findViewById(R.id.expire_date_time);
        tv.setOnClickListener(dateListener);
        tv = (TextView)findViewById(R.id.expire_date_date);
        tv.setOnClickListener(dateListener);
        updatePasswdExpiryDate();

        setText(R.id.expire_interval_val, Integer.toString(interval));
        CheckBox cb = (CheckBox)findViewById(R.id.expire_interval_recurring);
        cb.setChecked(recurring);

        Spinner expireTypeSpin = (Spinner)findViewById(R.id.expire_choice);
        expireTypeSpin.setSelection(expireType.itsStrIdx);
        updatePasswdExpiryChoice(expireType);
        expireTypeSpin.setOnItemSelectedListener(new OnItemSelectedListener()
        {
            public void onItemSelected(AdapterView<?> parent, View arg1,
                                       int position, long id)
            {
                updatePasswdExpiryChoice(
                    PasswdExpiration.Type.fromStrIdx(position));

            }

            public void onNothingSelected(AdapterView<?> parent)
            {
                updatePasswdExpiryChoice(PasswdExpiration.Type.NEVER);
            }
        });
    }

    /** Update fields based on the password expiration choice changing */
    private final void updatePasswdExpiryChoice(PasswdExpiration.Type type)
    {
        itsExpiryType = type;
        setVisibility(R.id.expire_date_fields,
                      itsExpiryType == PasswdExpiration.Type.DATE);
        setVisibility(R.id.expire_interval_fields,
                      itsExpiryType == PasswdExpiration.Type.INTERVAL);
        itsValidator.validate();
    }

    /** Update fields after the password expiration date changes */
    private final void updatePasswdExpiryDate()
    {
        long expiryDate = itsExpiryDate.getTimeInMillis();
        setText(R.id.expire_date_time,
                Utils.formatDate(expiryDate, this, true, false, false));
        setText(R.id.expire_date_date,
                Utils.formatDate(expiryDate, this, false, true, false));
        itsValidator.validate();
    }

    /**
     * Update the view when the history changes
     */
    private final void historyChanged(boolean updateMaxSize)
    {
        boolean historyExists = (itsHistory != null);
        int visibility = historyExists ? View.VISIBLE : View.GONE;
        Button addRemoveBtn = (Button)findViewById(R.id.history_addremove);
        addRemoveBtn.setText(
            getString(historyExists ? R.string.remove : R.string.add));

        CheckBox enabledCb = (CheckBox)findViewById(R.id.history_enabled);
        enabledCb.setVisibility(visibility);

        TextView maxSize = (TextView)findViewById(R.id.history_max_size);
        maxSize.setVisibility(visibility);
        View maxSizeLabel = findViewById(R.id.history_max_size_label);
        maxSizeLabel.setVisibility(visibility);

        ListView histView = (ListView)findViewById(R.id.history);
        histView.setVisibility(visibility);

        if (historyExists) {
            boolean historyEnabled = itsHistory.isEnabled() && !itsIsProtected;
            enabledCb.setChecked(historyEnabled);

            maxSize.setEnabled(historyEnabled);
            maxSizeLabel.setEnabled(historyEnabled);
            if (updateMaxSize) {
                maxSize.setText(Integer.toString(itsHistory.getMaxSize()));
            }

            ListAdapter histAdapter =
                GuiUtils.createPasswdHistoryAdapter(itsHistory, this,
                                                    historyEnabled);
            histView.setAdapter(histAdapter);
            GuiUtils.setListViewHeightBasedOnChildren(histView);
            histView.setEnabled(historyEnabled);
        }

        itsValidator.validate();
    }

    /**
     * Update the UI when the protected state changes
     */
    private final void updateProtected()
    {
        for (View v: itsProtectViews) {
            v.setEnabled(!itsIsProtected);
        }
        historyChanged(true);
        GuiUtils.invalidateOptionsMenu(this);
    }

    private final void saveRecord()
    {
        PasswdFileData fileData = getPasswdFile().getFileData();
        if (fileData == null) {
            PasswdSafeUtil.showFatalMsg("File closed", this);
            finish();
            return;
        }

        PwsRecord record = null;
        boolean newRecord = false;
        String uuid = getUUID();
        if (uuid != null) {
            record = fileData.getRecord(uuid);
        } else {
            newRecord = true;
            record = fileData.createRecord();
            record.setLoaded();
        }
        if (record == null) {
            PasswdSafeUtil.showFatalMsg("Unknown record: " + uuid, this);
            finish();
            return;
        }

        String updateStr;
        updateStr = getUpdatedField(fileData.getTitle(record), R.id.title);
        if (updateStr != null) {
            fileData.setTitle(updateStr, record);
        }

        updateStr = getUpdatedSpinnerField(fileData.getGroup(record),
                                           R.id.group);
        if (updateStr != null) {
            fileData.setGroup(updateStr, record);
        }

        updateStr = getUpdatedField(fileData.getUsername(record), R.id.user);
        if (updateStr != null) {
            fileData.setUsername(updateStr, record);
        }

        String currNotes = fileData.getNotes(record);
        if (itsTypeHasDetails) {
            updateStr = getUpdatedField(currNotes, R.id.notes);
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
            boolean currProt = fileData.isProtected(record);
            PasswdHistory currHistory = fileData.getPasswdHistory(record);

            if (itsTypeHasDetails) {
                updateStr = getUpdatedField(currUrl, R.id.url);
                if (updateStr != null) {
                    fileData.setURL(updateStr, record);
                }

                updateStr = getUpdatedField(currEmail, R.id.email);
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

            boolean updateProt = itsIsProtected;
            if (updateProt != currProt) {
                fileData.setProtected(updateProt, record);
            }

            Pair<Boolean, PasswdPolicy> updatePolicy = getUpdatedPolicy();
            PasswdSafeUtil.dbginfo(TAG, "updatePolicy: %s", updatePolicy);
            if (updatePolicy.first) {
                fileData.setPasswdPolicy(updatePolicy.second, record);
            }

            if (itsTypeHasNormalPassword) {
                if (isPasswdHistoryUpdated(currHistory)) {
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
        String newPasswd = null;
        if (itsTypeHasNormalPassword) {
            newPasswd = getUpdatedField(currPasswd, R.id.password);
            switch (itsOrigType) {
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
            newPasswd = PasswdRecord.uuidToPasswd(fileData.getUUID(itsLinkRef),
                                                  itsType);
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

        try {
            if (newRecord) {
                fileData.addRecord(record);
            }
        } catch (Exception e) {
            PasswdSafeUtil.showFatalMsg(e, "Error saving record: " + e, this);
            finish();
            return;
        }

        if (newRecord || record.isModified()) {
            saveFile();
        } else {
            finish();
        }
    }

    /** Get the password expiration that may have been updated */
    private final Pair<Boolean, PasswdExpiration> getUpdatedExpiry()
    {
        // Get the updated expiration
        PasswdExpiration updatedExpiry = null;
        switch (itsType) {
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
                int interval =
                    getIntegerTextField(R.id.expire_interval_val,
                                        PasswdExpiration.INTERVAL_DEFAULT);
                long exp = System.currentTimeMillis();
                exp += (long)interval * DateUtils.DAY_IN_MILLIS;
                exp -= (exp % DateUtils.MINUTE_IN_MILLIS);
                Date expiry = new Date(exp);

                CheckBox cb =
                    (CheckBox)findViewById(R.id.expire_interval_recurring);
                if (cb.isChecked()) {
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

        // Determine if it has changed
        boolean changed = false;
        do {
            if (((itsOrigExpiry == null) && (updatedExpiry != null)) ||
                ((itsOrigExpiry != null) && (updatedExpiry == null))) {
                changed = true;
                break;
            }

            if ((itsOrigExpiry == null) && (updatedExpiry == null)) {
                break;
            }

            if ((itsOrigExpiry.itsExpiration == null) ||
                (updatedExpiry.itsExpiration == null)) {
                changed = true;
                break;
            }

            if (!itsOrigExpiry.itsExpiration.equals(
                     updatedExpiry.itsExpiration)) {
                changed = true;
                break;
            }

            if ((itsOrigExpiry.itsInterval != updatedExpiry.itsInterval) ||
                (itsOrigExpiry.itsIsRecurring !=
                 updatedExpiry.itsIsRecurring)) {
                changed = true;
                break;
            }

        } while(false);

        return new Pair<Boolean, PasswdExpiration>(changed, updatedExpiry);
    }

    /** Get the password policy that may have been updated */
    private final Pair<Boolean, PasswdPolicy> getUpdatedPolicy()
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

        return new Pair<Boolean, PasswdPolicy>(policyChanged, updatedPolicy);
    }

    private final String getUpdatedField(String currStr, int viewId)
    {
        return getUpdatedField(currStr,
                               GuiUtils.getTextViewStr(this, viewId));
    }

    private final String getUpdatedSpinnerField(String currStr, int viewId)
    {
        return getUpdatedField(currStr,
                               GuiUtils.getSpinnerStr(this, viewId));
    }

    private final String getUpdatedField(String currStr, String newStr)
    {
        if (currStr == null) {
            currStr = "";
        }

        if (newStr.equals(currStr)) {
            newStr = null;
        }

        return newStr;
    }

    private final int getHistMaxSize()
    {
        return getIntegerTextField(R.id.history_max_size, -1);
    }

    /** Get the value of a text field as an integer */
    private final int getIntegerTextField(int fieldId, int defaultValue)
    {
        String str = GuiUtils.getTextViewStr(this, fieldId);
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private final boolean isPasswdHistoryUpdated(PasswdHistory currHistory)
    {
        if (itsHistory != null) {
            CheckBox enabledCb = (CheckBox)findViewById(R.id.history_enabled);
            itsHistory.setEnabled(enabledCb.isChecked());
            itsHistory.setMaxSize(getHistMaxSize());
        }

        if (((currHistory == null) && (itsHistory != null)) ||
            ((currHistory != null) && (itsHistory == null))) {
            return true;
        } else if ((currHistory == null) && (itsHistory == null)) {
            return false;
        } else {
            return !itsHistory.equals(currHistory);
        }
    }

    private final TextView setText(int id, String text)
    {
        TextView tv = (TextView)findViewById(id);
        if (text != null) {
            tv.setText(text);
        }

        switch (id)
        {
        case R.id.title:
        case R.id.user:
        case R.id.expire_interval_val:
            itsValidator.registerTextView(tv);
            break;
        }

        return tv;
    }

    /** Set the items in a spinner */
    private final Spinner setSpinnerItems(int id, List<?> items)
    {
        ArrayAdapter<Object> adapter =
            new ArrayAdapter<Object>(this,
                                     android.R.layout.simple_spinner_item,
                                     Collections.unmodifiableList(items));
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item);
        Spinner s = (Spinner)findViewById(id);
        s.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        return s;
    }

    /**
     * Initialize the list of views which are enabled based on whether the
     * record is protected
     */
    private final void initProtViews(View v)
    {
        if (v.getId() == R.id.protected_record) {
            // Don't include the protected checkbox itself
            return;
        }

        if ((v instanceof TextView) ||
            (v instanceof Spinner) ||
            (v instanceof AbsListView)) {
            itsProtectViews.add(v);
        }

        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup)v;
            for (int i = 0; i < vg.getChildCount(); ++i) {
                initProtViews(vg.getChildAt(i));
            }
        }
    }

    private final void setVisibility(int viewId, boolean visible)
    {
        View v = findViewById(viewId);
        v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private class Validator extends DialogValidator
    {
        public Validator()
        {
            super(RecordEditActivity.this);
        }

        @Override
        protected final View getDoneButton()
        {
            return RecordEditActivity.this.findViewById(R.id.done_btn);
        }

        @Override
        protected final String doValidation()
        {
            String title =
                GuiUtils.getTextViewStr(RecordEditActivity.this, R.id.title);
            if (title.length() == 0) {
                return getString(R.string.empty_title);
            }

            setAllowEmptyPassword(itsType != Type.NORMAL);

            V3Key key =
                new V3Key(title,
                          GuiUtils.getSpinnerStr(RecordEditActivity.this,
                                                 R.id.group),
                          GuiUtils.getTextViewStr(RecordEditActivity.this,
                                                  R.id.user));
            if (itsRecordKeys.contains(key)) {
                return getString(R.string.duplicate_entry);
            }

            if (itsHistory != null) {
                int histMaxSize = getHistMaxSize();
                if ((histMaxSize < PasswdHistory.MAX_SIZE_MIN) ||
                    (histMaxSize > PasswdHistory.MAX_SIZE_MAX)) {
                    return getString(R.string.invalid_history_max_size);
                }
            }

            switch (itsType) {
            case NORMAL: {
                break;
            }
            case ALIAS: {
                if (itsLinkRef == null) {
                    return getString(R.string.no_alias_chosen);
                }
                break;
            }
            case SHORTCUT: {
                if (itsLinkRef == null) {
                    return getString(R.string.no_shortcut_chosen);
                }
                break;
            }
            }

            if (itsIsV3) {
                switch (itsExpiryType) {
                case NEVER: {
                    break;
                }
                case DATE: {
                    long now = System.currentTimeMillis();
                    long expiry = itsExpiryDate.getTimeInMillis();
                    if (expiry < now) {
                        return getString(R.string.password_expiration_in_past);
                    }
                    break;
                }
                case INTERVAL: {
                    int interval = getIntegerTextField(R.id.expire_interval_val,
                                                       -1);
                    if ((interval < PasswdExpiration.VALID_INTERVAL_MIN) ||
                        (interval > PasswdExpiration.VALID_INTERVAL_MAX)) {
                        return getString(
                            R.string.password_expiration_invalid_interval);
                    }
                    break;
                }
                }
            }

            return super.doValidation();
        }
    }

    private static class V3Key
    {
        private String itsTitle;
        private String itsGroup;
        private String itsUser;

        public V3Key(String title, String group, String user)
        {
            itsTitle = (title != null) ? title : "";
            itsGroup = (group != null) ? group : "";
            itsUser = (user != null) ? user : "";
        }

        @Override
        public final boolean equals(Object o)
        {
            if (o instanceof V3Key) {
                V3Key key = (V3Key)o;
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
