/*
 * Copyright (©) 2010-2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.pwsafe.lib.file.PwsRecord;

import com.jefftharris.passwdsafe.PasswdRecord.Type;
import com.jefftharris.passwdsafe.view.PasswordVisibilityMenuHandler;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
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
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class RecordEditActivity extends AbstractRecordActivity
{
    private static final String TAG = "RecordEditActivity";

    private static final int DIALOG_NEW_GROUP = MAX_DIALOG + 1;

    private static final int MENU_TOGGLE_PASSWORD = 3;
    private static final int MENU_GENERATE_PASSWORD = 4;
    private static final int MENU_PASSWORD_PREFERENCES = 5;
    private static final int MENU_CANCEL = 6;

    private static final int CTXMENU_REMOVE = 1;
    private static final int CTXMENU_SET_PASSWORD = 2;

    private static final int RECORD_SELECTION_REQUEST = 0;

    private TreeSet<String> itsGroups =
        new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    private String itsPrevGroup;
    private HashSet<V3Key> itsRecordKeys = new HashSet<V3Key>();
    private DialogValidator itsValidator;
    private PasswdHistory itsHistory;
    private boolean itsIsV3 = false;
    private PasswdRecord.Type itsType = Type.NORMAL;
    private boolean itsTypeHasNormalPassword = true;
    private boolean itsTypeHasDetails = true;
    private PasswdRecord.Type itsOrigType = Type.NORMAL;
    private PwsRecord itsLinkRef = null;
    private ArrayList<View> itsProtectViews = new ArrayList<View>();

    private static final String LOWER_CHARS = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER_CHARS = LOWER_CHARS.toUpperCase();
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "+-=_@#$%^&;:,.<>/~\\[](){}?!|";
    private static final String EASY_LOWER_CHARS = "abcdefghijkmnopqrstuvwxyz";
    private static final String EASY_UPPER_CHARS = "ABCDEFGHJKLMNPQRTUVWXY";
    private static final String EASY_DIGITS = "346789";
    private static final String EASY_SYMBOLS = "+-=_@#$%^&<>/~\\?";

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
        itsIsV3 = fileData.isV3();
        PwsRecord record = null;
        String group = null;
        String uuid = getUUID();
        CheckBox protCb = (CheckBox)findViewById(R.id.protected_record);
        if (uuid != null) {
            record = fileData.getRecord(uuid);
            if (record == null) {
                PasswdSafeApp.showFatalMsg("Unknown record: " + uuid, this);
                return;
            }

            getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
            setText(R.id.rec_title, "Edit " + fileData.getTitle(record));
            setText(R.id.title, fileData.getTitle(record));
            group = fileData.getGroup(record);
            setText(R.id.user, fileData.getUsername(record));
            setText(R.id.notes, fileData.getNotes(record));

            if (itsIsV3) {
                setText(R.id.url, fileData.getURL(record));
                setText(R.id.email, fileData.getEmail(record));
                protCb.setChecked(fileData.isProtected(record));
            }

            itsHistory = fileData.getPasswdHistory(record);
        } else {
            setText(R.id.rec_title, "New Entry");
            setText(R.id.title, null);
            setText(R.id.user, null);
            setText(R.id.notes, null);

            if (itsIsV3) {
                setText(R.id.url, null);
                setText(R.id.email, null);
            }
        }
        if (!itsIsV3) {
            setVisibility(R.id.url_row, false);
            setVisibility(R.id.email_row, false);
            setVisibility(R.id.protected_row, false);
        }

        initTypeAndPassword(fileData, record);
        initGroup(fileData, record, group);

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
                setProtected(((CheckBox)v).isChecked());
            }
        });

        initProtViews(findViewById(R.id.base_group));
        initProtViews(findViewById(R.id.history_group));
        initProtViews(findViewById(R.id.notes_label));
        initProtViews(findViewById(R.id.notes));
        setProtected(isProtected());
        itsValidator.validate();
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
        PasswdSafeApp.dbginfo(TAG, "onResume");
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
                .setTitle(PasswdSafeApp.getAppTitle(this))
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
        default:
        {
            dialog = super.onCreateDialog(id);
            break;
        }
        }
        return dialog;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuItem item;

        menu.add(0, MENU_CANCEL, 0, R.string.cancel);
        menu.add(0, MENU_TOGGLE_PASSWORD, 0, R.string.show_password);
        menu.add(0, MENU_GENERATE_PASSWORD, 0, R.string.generate_password);

        item = menu.add(0, MENU_PASSWORD_PREFERENCES, 0,
                        R.string.password_preferences);
        item.setIcon(android.R.drawable.ic_menu_preferences);
        Intent intent = new Intent(this, Preferences.class);
        intent.putExtra(Preferences.INTENT_SCREEN,
                        Preferences.SCREEN_PASSWORD_OPTIONS);
        item.setIntent(intent);
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
            mi.setEnabled(userPassword && !isProtected());
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
        PasswdSafeApp.dbginfo(TAG, "onActivityResult data: " + data);

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
                                           PwsRecord record)
    {
        itsOrigType = Type.NORMAL;
        PwsRecord linkRef = null;
        String password = null;

        if (record != null) {
            PasswdRecord passwdRec = fileData.getPasswdRecord(record);
            itsOrigType = passwdRec.getType();

            switch (itsOrigType) {
            case NORMAL: {
                password = fileData.getPassword(record);
                break;
            }
            case ALIAS:
            case SHORTCUT: {
                linkRef = passwdRec.getRef();
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

            Button btn = (Button)findViewById(R.id.password_link_btn);
            btn.setOnClickListener(new OnClickListener()
            {
                public void onClick(View v)
                {
                    startActivityForResult(
                        new Intent(PasswdSafeApp.CHOOSE_RECORD_INTENT,
                                   getIntent().getData(),
                                   RecordEditActivity.this,
                                   RecordSelectionActivity.class),
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
        setVisibility(R.id.history_group_sep, itsTypeHasNormalPassword);
        setVisibility(R.id.history_group, itsTypeHasNormalPassword);
        itsValidator.validate();
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
        ArrayList<String> chars = new ArrayList<String>();
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        if (Preferences.getPasswordGenHexPref(prefs)) {
            chars.add(DIGITS + "abcdef");
        } else {
            if (Preferences.getPasswordGenEasyPref(prefs)) {
                if (Preferences.getPasswordGenLowerPref(prefs)) {
                    chars.add(EASY_LOWER_CHARS);
                }
                if (Preferences.getPasswordGenUpperPref(prefs)) {
                    chars.add(EASY_UPPER_CHARS);
                }
                if (Preferences.getPasswordGenDigitsPref(prefs)) {
                    chars.add(EASY_DIGITS);
                }
                if (Preferences.getPasswordGenSymbolsPref(prefs)) {
                    chars.add(EASY_SYMBOLS);
                }
            } else {
                if (Preferences.getPasswordGenLowerPref(prefs)) {
                    chars.add(LOWER_CHARS);
                }
                if (Preferences.getPasswordGenUpperPref(prefs)) {
                    chars.add(UPPER_CHARS);
                }
                if (Preferences.getPasswordGenDigitsPref(prefs)) {
                    chars.add(DIGITS);
                }
                if (Preferences.getPasswordGenSymbolsPref(prefs)) {
                    chars.add(SYMBOLS);
                }
            }
        }

        if (chars.isEmpty()) {
            return;
        }

        String charsStr =
            TextUtils.concat(chars.toArray(new CharSequence[0])).toString();
        int numChars = charsStr.length();
        StringBuilder passwd = new StringBuilder();
        int passwdLen = Preferences.getPasswordGenLengthPref(prefs);
        try {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.nextBytes(new byte[passwdLen]);

            ArrayList<String> verifyChars = new ArrayList<String>();
            do {
                verifyChars.clear();
                verifyChars.addAll(chars);
                passwd.delete(0, passwd.length());
                for (int i = 0; i < passwdLen; ++i) {
                    int charPos = random.nextInt(numChars);
                    char c = charsStr.charAt(charPos);
                    passwd.append(c);

                    if (!verifyChars.isEmpty()) {
                        Iterator<String> iter = verifyChars.iterator();
                        while (iter.hasNext()) {
                            String verifyStr = iter.next();
                            if (verifyStr.indexOf(c) != -1) {
                                iter.remove();
                            }
                        }
                    }
                }
            } while (!verifyChars.isEmpty() &&
                     (passwdLen > (chars.size() - verifyChars.size())));

            setPassword(passwd.toString());
        } catch (NoSuchAlgorithmException e) {
            PasswdSafeApp.showFatalMsg(e, this);
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

    private final void initGroup(PasswdFileData fileData, PwsRecord editRecord,
	    			 String group)
    {
        ArrayList<PwsRecord> records = fileData.getRecords();
        for (PwsRecord rec : records) {
            String grp = fileData.getGroup(rec);
            if ((grp != null) && (grp.length() != 0)) {
                itsGroups.add(grp);
            }

            if (rec != editRecord) {
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

        groupList.add("New group...");
        ArrayAdapter<String> groupAdapter =
            new ArrayAdapter<String>(this,
                                     android.R.layout.simple_spinner_item,
                                     groupList);
        groupAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item);
        Spinner s = (Spinner)findViewById(R.id.group);
        s.setAdapter(groupAdapter);
        groupAdapter.notifyDataSetChanged();
        if (groupPos != 0) {
            s.setSelection(groupPos);
        }
    }

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
            boolean historyEnabled = itsHistory.isEnabled();
            enabledCb.setChecked(historyEnabled);

            maxSize.setEnabled(historyEnabled);
            maxSizeLabel.setEnabled(historyEnabled);
            if (updateMaxSize) {
                maxSize.setText(Integer.toString(itsHistory.getMaxSize()));
            }

            ListAdapter histAdapter =
                GuiUtils.createPasswdHistoryAdapter(itsHistory, this);
            histView.setAdapter(histAdapter);
            GuiUtils.setListViewHeightBasedOnChildren(histView);
            histView.setEnabled(historyEnabled);
            // TODO: this item enable doesn't work...
            for (int i = 0; i < histAdapter.getCount(); ++i) {
                histAdapter.getView(i, null, histView).setEnabled(historyEnabled);
            }
        }

        itsValidator.validate();
    }

    /**
     * Is the record protected
     */
    private final boolean isProtected()
    {
        CheckBox cb = (CheckBox)findViewById(R.id.protected_record);
        return cb.isChecked();
    }

    /**
     * Set whether the record is protected
     */
    private final void setProtected(boolean prot)
    {
        for (View v: itsProtectViews) {
            v.setEnabled(!prot);
        }

        if (!prot) {
            historyChanged(true);
        }

        // TODO: need to invalidate options menu
        // TODO: try to disable listview items
    }

    private final void saveRecord()
    {
        PasswdFileData fileData = getPasswdFile().getFileData();
        if (fileData == null) {
            PasswdSafeApp.showFatalMsg("File closed", this);
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
            PasswdSafeApp.showFatalMsg("Unknown record: " + uuid, this);
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

            boolean updateProt = isProtected();
            if (updateProt != currProt) {
                fileData.setProtected(updateProt, record);
            }

            if (itsTypeHasNormalPassword) {
                if (isPasswdHistoryUpdated(currHistory)) {
                    if (itsHistory != null) {
                        itsHistory.adjustEntriesToMaxSize();
                    }
                    fileData.setPasswdHistory(itsHistory, record);
                }
            } else {
                if (currHistory != null) {
                    fileData.setPasswdHistory(null, record);
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
        }

        try {
            if (newRecord) {
                fileData.addRecord(record);
            }
        } catch (Exception e) {
            PasswdSafeApp.showFatalMsg(e, "Error saving record: " + e, this);
            finish();
            return;
        }

        if (newRecord || record.isModified()) {
            saveFile();
        } else {
            finish();
        }
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
        String str = GuiUtils.getTextViewStr(this, R.id.history_max_size);
        try {
            int size = Integer.parseInt(str);
            return size;
        } catch (NumberFormatException e) {
            return -1;
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
            itsValidator.registerTextView(tv);
            break;
        }

        return tv;
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
