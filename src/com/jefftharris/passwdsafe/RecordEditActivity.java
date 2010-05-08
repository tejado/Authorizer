/*
 * Copyright (©) 2010 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeSet;

import org.pwsafe.lib.file.PwsRecord;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
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

public class RecordEditActivity extends Activity
{
    private static final String TAG = "RecordEditActivity";

    private static final int DIALOG_PROGRESS = 0;
    private static final int DIALOG_NEW_GROUP = 1;

    private ActivityPasswdFile itsFile;
    private String itsUUID;
    private SaveTask itsSaveTask;
    private TreeSet<String> itsGroups =
        new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    private String itsPrevGroup;

    private TextWatcher itsTextWatcher = new TextWatcher()
    {
        public void afterTextChanged(Editable s)
        {
            validate();
        }

        public void beforeTextChanged(CharSequence s, int start,
                                      int count, int after)
        {
        }

        public void onTextChanged(CharSequence s, int start,
                                  int before, int count)
        {
        }
    };

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        PasswdSafeApp.dbginfo(TAG, "onCreate intent:" + getIntent());

        File file = new File(intent.getData().getPath());
        itsUUID = intent.getData().getQueryParameter("rec");

        PasswdSafeApp app = (PasswdSafeApp)getApplication();
        itsFile = app.accessPasswdFile(file, this);
        PasswdFileData fileData = itsFile.getFileData();
        if (fileData == null) {
            PasswdSafeApp.showFatalMsg("File not open: " + file, this);
            return;
        }

        setContentView(R.layout.record_edit);
        setTitle(PasswdSafeApp.getAppFileTitle(itsFile, this));

        String group = null;
        if (itsUUID != null) {
            PwsRecord record = fileData.getRecord(itsUUID);
            if (record == null) {
                PasswdSafeApp.showFatalMsg("Unknown record: " + itsUUID, this);
                return;
            }

            setText(R.id.rec_title, "Edit " + fileData.getTitle(record));
            setText(R.id.title, fileData.getTitle(record));
            group = fileData.getGroup(record);
            setText(R.id.url, fileData.getURL(record));
            setText(R.id.email, fileData.getEmail(record));
            setText(R.id.user, fileData.getUsername(record));
            setText(R.id.notes, fileData.getNotes(record));

            String password = fileData.getPassword(record);
            setText(R.id.password, password);
            setText(R.id.password_confirm, password);
        } else {
            setText(R.id.rec_title, "New Entry");
            setText(R.id.title, null);
            setText(R.id.url, null);
            setText(R.id.email, null);
            setText(R.id.user, null);
            setText(R.id.notes, null);
            setText(R.id.password, null);
            setText(R.id.password_confirm, null);
        }

        initGroup(fileData, group);

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

        validate();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        PasswdSafeApp.dbginfo(TAG, "onPause");
        if (itsSaveTask != null) {
            try {
                itsSaveTask.get();
            } catch (Exception e) {
                PasswdSafeApp.showFatalMsg(e.toString(), this);
            }
            itsSaveTask = null;
            removeDialog(DIALOG_PROGRESS);
        }
        itsFile.resumeFileTimer();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        PasswdSafeApp.dbginfo(TAG, "onResume");
        itsFile.pauseFileTimer();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog = null;
        switch (id) {
        case DIALOG_PROGRESS:
        {
            ProgressDialog dlg = new ProgressDialog(this);
            dlg.setTitle(PasswdSafeApp.getAppTitle(this));
            dlg.setMessage("Saving " +
                           itsFile.getFileData().getFile().getName() + "...");
            dlg.setIndeterminate(true);
            dlg.setCancelable(false);
            dialog = dlg;
            break;
        }
        case DIALOG_NEW_GROUP:
        {
            LayoutInflater factory = LayoutInflater.from(this);
            final View view = factory.inflate(R.layout.new_group, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(PasswdSafeApp.getAppTitle(this))
                .setMessage("Enter new group:")
                .setView(view)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        EditText newGroup = (EditText)
                           view.findViewById(R.id.new_group);
                        setGroup(newGroup.getText().toString());
                    }
                })
                .setNegativeButton("Cancel",
                                   new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        setGroup(itsPrevGroup);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener()
                {
                    public void onCancel(DialogInterface dialog)
                    {
                        setGroup(itsPrevGroup);
                    }
                });
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

    private final void initGroup(PasswdFileData fileData, String group)
    {
        ArrayList<PwsRecord> records = fileData.getRecords();
        for (PwsRecord rec : records) {
            String grp = fileData.getGroup(rec);
            if ((grp != null) && (grp.length() != 0)) {
                itsGroups.add(grp);
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
        if (groupPos != 0) {
            s.setSelection(groupPos);
        }
    }

    private final void validate()
    {
        String errorMsg = null;
        do {
            if (getTextViewStr(R.id.title).length() == 0) {
                errorMsg = "Empty title";
                break;
            }

            if (!getTextViewStr(R.id.password).equals(
                 getTextViewStr(R.id.password_confirm))) {
                errorMsg = "Passwords do not match";
            }
        } while(false);

        TextView errorMsgView = (TextView)findViewById(R.id.error_msg);
        if (errorMsg == null) {
            errorMsgView.setVisibility(View.GONE);
        } else {
            errorMsgView.setVisibility(View.VISIBLE);

            String errorFmt = getResources().getString(R.string.error_msg);
            errorMsgView.setText(
                Html.fromHtml(String.format(errorFmt, errorMsg)));
        }

        View doneBtn = findViewById(R.id.done_btn);
        doneBtn.setEnabled(errorMsg == null);
    }

    private final void saveRecord()
    {
        PasswdFileData fileData = itsFile.getFileData();
        if (fileData == null) {
            PasswdSafeApp.showFatalMsg("File closed", this);
            finish();
            return;
        }

        PwsRecord record = null;
        boolean newRecord = false;
        if (itsUUID != null) {
            record = fileData.getRecord(itsUUID);
        } else {
            newRecord = true;
            record = fileData.createRecord();
        }
        if (record == null) {
            PasswdSafeApp.showFatalMsg("Unknown record: " + itsUUID, this);
            finish();
            return;
        }

        // TODO for v3 (at least) title,group, and user are primary keys for
        // entries
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

        updateStr = getUpdatedField(fileData.getURL(record), R.id.url);
        if (updateStr != null) {
            fileData.setURL(updateStr, record);
        }

        updateStr = getUpdatedField(fileData.getEmail(record), R.id.email);
        if (updateStr != null) {
            fileData.setEmail(updateStr, record);
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

        try {
            if (newRecord) {
                fileData.addRecord(record);
            }
        } catch (Exception e) {
            PasswdSafeApp.showFatalMsg("Error saving record: " + e, this);
            finish();
            return;
        }

        if (newRecord || record.isModified()) {
            PasswdSafeApp.dbginfo(TAG, "saving");
                // TODO update header fields for last save info??
                // TODO save unknown fields/records
            showDialog(DIALOG_PROGRESS);
            itsSaveTask = new SaveTask();
            itsSaveTask.execute();
        } else {
            finish();
        }
    }

    private final String getUpdatedField(String currStr, int viewId)
    {
        return getUpdatedField(currStr, getTextViewStr(viewId));
    }

    private final String getTextViewStr(int viewId)
    {
        TextView tv = (TextView)findViewById(viewId);
        return tv.getText().toString();
    }

    private final String getUpdatedSpinnerField(String currStr, int viewId)
    {
        Spinner s = (Spinner)findViewById(viewId);
        return getUpdatedField(currStr, s.getSelectedItem().toString());
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
        case R.id.password:
        case R.id.password_confirm:
        case R.id.title:
            tv.addTextChangedListener(itsTextWatcher);
            break;
        }
    }

    private final class SaveTask extends AsyncTask<Void, Void, Object>
    {
        @Override
        protected Object doInBackground(Void... params)
        {
            try {
                itsFile.save();
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Object result)
        {
            removeDialog(DIALOG_PROGRESS);
            if (result instanceof Exception) {
                PasswdSafeApp.showFatalMsg(((Exception)result).toString(),
                                           RecordEditActivity.this);
            } else {
                setResult(PasswdSafeApp.RESULT_MODIFIED);
            }
            itsSaveTask = null;
            finish();
        }
    }
}
