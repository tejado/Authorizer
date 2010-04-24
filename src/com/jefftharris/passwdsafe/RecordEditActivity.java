/*
 * Copyright (©) 2010 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.File;

import org.pwsafe.lib.file.PwsRecord;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class RecordEditActivity extends Activity
{
    private static final String TAG = "RecordEditActivity";

    private static final int DIALOG_PROGRESS = 0;

    private ActivityPasswdFile itsFile;
    private String itsUUID;
    private SaveTask itsSaveTask;

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
        if (itsUUID == null) {
            // TODO edit new entry
            return;
        }

        PasswdSafeApp app = (PasswdSafeApp)getApplication();
        itsFile = app.accessPasswdFile(file, this);
        PasswdFileData fileData = itsFile.getFileData();
        if (fileData == null) {
            PasswdSafeApp.showFatalMsg("File not open: " + file, this);
            return;
        }

        PwsRecord record = fileData.getRecord(itsUUID);
        if (record == null) {
            PasswdSafeApp.showFatalMsg("Unknown record: " + itsUUID, this);
            return;
        }

        setContentView(R.layout.record_edit);
        setTitle(PasswdSafeApp.getAppFileTitle(itsFile, this));

        // TODO hide fields that aren't valid for version

        setText(R.id.rec_title, "Edit " + fileData.getTitle(record));
        setText(R.id.title, fileData.getTitle(record));
        // TODO editable combo-box for groups??
        setText(R.id.group, fileData.getGroup(record));
        setText(R.id.url, fileData.getURL(record));
        setText(R.id.email, fileData.getEmail(record));
        setText(R.id.user, fileData.getUsername(record));
        setText(R.id.notes, fileData.getNotes(record));
        // TODO password edit

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
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        itsFile.touch();
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
        default:
        {
            dialog = super.onCreateDialog(id);
            break;
        }
        }
        return dialog;
    }

    private final void validate()
    {
        String errorMsg = null;
        do {
            if (getTextViewStr(R.id.title).length() == 0) {
                errorMsg = "Empty title";
                break;
            }

            if (getTextViewStr(R.id.group).length() == 0) {
                errorMsg = "Empty group";
                break;
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
            return;
        }

        PwsRecord record = fileData.getRecord(itsUUID);
        if (record == null) {
            PasswdSafeApp.showFatalMsg("Unknown record: " + itsUUID, this);
            return;
        }

        // TODO remove empty fields, if possible??
        String updateStr;

        updateStr = getUpdatedField(fileData.getTitle(record), R.id.title);
        if (updateStr != null) {
            fileData.setTitle(updateStr, record);
        }

        updateStr = getUpdatedField(fileData.getGroup(record), R.id.group);
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

        updateStr = getUpdatedField(fileData.getNotes(record), R.id.notes);
        if (updateStr != null) {
            fileData.setNotes(updateStr, record);
        }

        if (record.isModified()) {
            Log.e("RecordEditActivity", "saving");
                // TODO update header fields for last save info??
                // TODO save unknown fields/records
            showDialog(DIALOG_PROGRESS);
            itsSaveTask = new SaveTask();
            itsSaveTask.execute(fileData);
        } else {
            finish();
        }
    }

    private final String getUpdatedField(String currStr, int viewId)
    {
        if (currStr == null) {
            currStr = "";
        }

        String newStr = getTextViewStr(viewId);
        if (newStr.equals(currStr)) {
            newStr = null;
        }

        return newStr;
    }

    private final String getTextViewStr(int viewId)
    {
        TextView tv = (TextView)findViewById(viewId);
        return tv.getText().toString();
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
        case R.id.group:
            tv.addTextChangedListener(itsTextWatcher);
            break;
        }
    }

    private final class SaveTask extends AsyncTask<PasswdFileData, Void, Object>
    {
        @Override
        protected Object doInBackground(PasswdFileData... params)
        {
            try {
                for (PasswdFileData file : params) {
                    file.save();
                }
                // TODO Block file close timer while saving
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
