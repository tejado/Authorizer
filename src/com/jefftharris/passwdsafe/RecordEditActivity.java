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
import android.widget.TextView;

public class RecordEditActivity extends Activity
{
    private static final String TAG = "RecordEditActivity";

    private ActivityPasswdFile itsFile;
    private PwsRecord itsRecord;

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
        String uuid = intent.getData().getQueryParameter("rec");
        if (uuid == null) {
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

        itsRecord = fileData.getRecord(uuid);
        if (itsRecord == null) {
            PasswdSafeApp.showFatalMsg("Unknown record: " + uuid, this);
            return;
        }

        setContentView(R.layout.record_edit);

        setText(R.id.rec_title, "Edit " + fileData.getTitle(itsRecord));
        setText(R.id.title, fileData.getTitle(itsRecord));
        // TODO editable combo-box for groups??
        setText(R.id.group, fileData.getGroup(itsRecord));
        setText(R.id.url, fileData.getURL(itsRecord));
        setText(R.id.email, fileData.getEmail(itsRecord));
        setText(R.id.user, fileData.getUsername(itsRecord));
        setText(R.id.notes, fileData.getNotes(itsRecord));
    }

    private final void setText(int id, String text)
    {
        if (text != null) {
            TextView tv = (TextView)findViewById(id);
            tv.setText(text);
        }
    }
}
