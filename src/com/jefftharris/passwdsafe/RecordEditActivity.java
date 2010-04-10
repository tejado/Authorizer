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
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class RecordEditActivity extends Activity
{
    private static final String TAG = "RecordEditActivity";

    private ActivityPasswdFile itsFile;
    private String itsUUID;

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

        // TODO hide fields that aren't valid for version

        setText(R.id.rec_title, "Edit " + fileData.getTitle(record));
        setText(R.id.title, fileData.getTitle(record));
        // TODO editable combo-box for groups??
        setText(R.id.group, fileData.getGroup(record));
        setText(R.id.url, fileData.getURL(record));
        setText(R.id.email, fileData.getEmail(record));
        setText(R.id.user, fileData.getUsername(record));
        setText(R.id.notes, fileData.getNotes(record));

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

        boolean recordChanged = false;
        String currStr = fileData.getNotes(record);
        TextView tv = (TextView)findViewById(R.id.notes);
        String newStr = tv.getText().toString();
        if (!newStr.equals(currStr)) {
            recordChanged = true;
            fileData.setNotes(newStr, record);
        }

        Log.e("RecordEditActivity", record.toString());
        if (recordChanged) {
            try {
                // TODO Save in background
                // TODO Need to reload prev record view
                fileData.save();
                finish();
            } catch (Exception e) {
                PasswdSafeApp.showFatalMsg(e.toString(), this);
            }
        }
    }

    private final void setText(int id, String text)
    {
        if (text != null) {
            TextView tv = (TextView)findViewById(id);
            tv.setText(text);
        }
    }
}
