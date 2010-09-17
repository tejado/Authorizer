/*
 * Copyright (©) 2010 Jeff Harris <jefftharris@gmail.com>
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
import java.util.TreeSet;

import org.pwsafe.lib.file.PwsRecord;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.text.method.TransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class RecordEditActivity extends AbstractRecordActivity
{
    private static final String TAG = "RecordEditActivity";

    private static final int DIALOG_NEW_GROUP = MAX_DIALOG + 1;

    private static final int MENU_TOGGLE_PASSWORD = 3;
    private static final int MENU_GENERATE_PASSWORD = 4;

    private TreeSet<String> itsGroups =
        new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    private String itsPrevGroup;
    private HashSet<V3Key> itsRecordKeys = new HashSet<V3Key>();
    private DialogValidator itsValidator;
    private boolean isPasswordShown = false;

    private static final String LOWER_CHARS = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER_CHARS = LOWER_CHARS.toUpperCase();
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "+-=_@#$%^&;:,.<>/~\\[](){}?!|";
    private static final String EASY_LOWER_CHARS = "abcdefghijkmnopqrstuvwxyz";
    private static final String EASY_UPPER_CHARS = "ABCDEFGHJKLMNPQRTUVWXY";
    private static final String EASY_DIGITS = "346789";
    private static final String EASY_SYMBOLS = "+-=_@#$%^&<>/~\\?";

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
        PwsRecord record = null;
        String group = null;
        String uuid = getUUID();
        if (uuid != null) {
            record = fileData.getRecord(uuid);
            if (record == null) {
                PasswdSafeApp.showFatalMsg("Unknown record: " + uuid, this);
                return;
            }

            setText(R.id.rec_title, "Edit " + fileData.getTitle(record));
            setText(R.id.title, fileData.getTitle(record));
            group = fileData.getGroup(record);
            setText(R.id.user, fileData.getUsername(record));
            setText(R.id.notes, fileData.getNotes(record));

            String password = fileData.getPassword(record);
            setText(R.id.password, password);
            setText(R.id.password_confirm, password);

            if (fileData.isV3()) {
                setText(R.id.url, fileData.getURL(record));
                setText(R.id.email, fileData.getEmail(record));
            } else {
                hideRow(R.id.url_row);
                hideRow(R.id.email_row);
            }
        } else {
            setText(R.id.rec_title, "New Entry");
            setText(R.id.title, null);
            setText(R.id.user, null);
            setText(R.id.password, null);
            setText(R.id.password_confirm, null);
            setText(R.id.notes, null);

            if (fileData.isV3()) {
                setText(R.id.url, null);
                setText(R.id.email, null);
            } else {
                hideRow(R.id.url_row);
                hideRow(R.id.email_row);
            }
        }

        initGroup(fileData, record, group);

        Button button = (Button)findViewById(R.id.done_btn);
        button.setOnClickListener(new OnClickListener()
        {
            public final void onClick(View v)
            {
                saveRecord();
            }
        });

        button = (Button)findViewById(R.id.cancel_btn);
        button.setOnClickListener(new OnClickListener()
        {
            public final void onClick(View v)
            {
                finish();
            }
        });

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
                .setMessage("Enter new group:")
                .setView(view)
                .setPositiveButton("Ok", dlgClick)
                .setNegativeButton("Cancel", dlgClick)
                .setOnCancelListener(dlgClick);
            dialog = builder.create();
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
        menu.add(0, MENU_TOGGLE_PASSWORD, 0, R.string.show_password);
        menu.add(0, MENU_GENERATE_PASSWORD, 0, R.string.generate_password);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        MenuItem mi = menu.findItem(MENU_TOGGLE_PASSWORD);
        if (mi != null) {
            mi.setTitle(isPasswordShown ? R.string.hide_password :
                        R.string.show_password);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case MENU_TOGGLE_PASSWORD:
        {
            setPasswordVisibility(
                !isPasswordShown,
                (TextView)findViewById(R.id.password),
                (TextView)findViewById(R.id.password_confirm));
            return true;
        }
        case MENU_GENERATE_PASSWORD:
        {
            ArrayList<String> chars = new ArrayList<String>();
            SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(this);
            if (PasswdSafeApp.getPasswordGenHexPref(prefs)) {
                chars.add(DIGITS + "abcdef");
            } else {
                if (PasswdSafeApp.getPasswordGenEasyPref(prefs)) {
                    if (PasswdSafeApp.getPasswordGenLowerPref(prefs)) {
                        chars.add(EASY_LOWER_CHARS);
                    }
                    if (PasswdSafeApp.getPasswordGenUpperPref(prefs)) {
                        chars.add(EASY_UPPER_CHARS);
                    }
                    if (PasswdSafeApp.getPasswordGenDigitsPref(prefs)) {
                        chars.add(EASY_DIGITS);
                    }
                    if (PasswdSafeApp.getPasswordGenSymbolsPref(prefs)) {
                        chars.add(EASY_SYMBOLS);
                    }
                } else {
                    if (PasswdSafeApp.getPasswordGenLowerPref(prefs)) {
                        chars.add(LOWER_CHARS);
                    }
                    if (PasswdSafeApp.getPasswordGenUpperPref(prefs)) {
                        chars.add(UPPER_CHARS);
                    }
                    if (PasswdSafeApp.getPasswordGenDigitsPref(prefs)) {
                        chars.add(DIGITS);
                    }
                    if (PasswdSafeApp.getPasswordGenSymbolsPref(prefs)) {
                        chars.add(SYMBOLS);
                    }
                }
            }

            String charsStr =
                TextUtils.concat(chars.toArray(new CharSequence[0])).toString();
            int numChars = charsStr.length();
            StringBuilder passwd = new StringBuilder();
            int passwdLen = PasswdSafeApp.getPasswordGenLengthPref(prefs);
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

                // TODO: Test one selected group and passwd size of 1, 2, 3
                // TODO: Prefs screen

                TextView passwdField =
                    (TextView)findViewById(R.id.password);
                TextView confirmField =
                    (TextView)findViewById(R.id.password_confirm);
                passwdField.setText(passwd);
                confirmField.setText(passwd);
                setPasswordVisibility(true, passwdField, confirmField);
            } catch (NoSuchAlgorithmException e) {
                PasswdSafeApp.showFatalMsg(e, this);
            }
            return true;
        }
        default:
        {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    private final void setPasswordVisibility(boolean visible,
                                             TextView passwdField,
                                             TextView confirmField)
    {
        isPasswordShown = visible;
        TransformationMethod tm;
        if (isPasswordShown) {
            tm = SingleLineTransformationMethod.getInstance();
        } else {
            tm = PasswordTransformationMethod.getInstance();
        }
        passwdField.setTransformationMethod(tm);
        confirmField.setTransformationMethod(tm);
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

        updateStr = getUpdatedField(fileData.getPassword(record),
                                    R.id.password);
        if (updateStr != null) {
            fileData.setPassword(updateStr, record);
        }

        updateStr = getUpdatedField(fileData.getNotes(record), R.id.notes);
        if (updateStr != null) {
            fileData.setNotes(updateStr, record);
        }

        if (fileData.isV3()) {
            updateStr = getUpdatedField(fileData.getURL(record), R.id.url);
            if (updateStr != null) {
                fileData.setURL(updateStr, record);
            }

            updateStr = getUpdatedField(fileData.getEmail(record), R.id.email);
            if (updateStr != null) {
                fileData.setEmail(updateStr, record);
            }
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

    private final void setText(int id, String text)
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
    }

    private final void hideRow(int rowId)
    {
        View row = findViewById(rowId);
        row.setVisibility(View.GONE);
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
                return "Empty title";
            }

            V3Key key =
                new V3Key(title,
                          GuiUtils.getSpinnerStr(RecordEditActivity.this,
                                                 R.id.group),
                          GuiUtils.getTextViewStr(RecordEditActivity.this,
                                                  R.id.user));
            if (itsRecordKeys.contains(key)) {
                return "Duplicate entry";
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
