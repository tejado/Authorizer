package com.jefftharris.passwdsafe;

import org.pwsafe.lib.file.PwsRecord;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class RecordView extends Activity
{
    private static final String TAG = "RecordView";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Log.i(TAG, "onCreate intent:" + getIntent());

        String fileName = intent.getData().getPath();
        String uuid = intent.getData().getQueryParameter("rec");

        PasswdSafeApp app = (PasswdSafeApp)getApplication();
        PasswdFileData fileData = app.getFileData(fileName);
        if (fileData == null) {
            PasswdSafeApp.showFatalMsg("File not open: " + fileName, this);
            return;
        }

        PwsRecord rec = fileData.getRecord(uuid);
        if (rec == null) {
            PasswdSafeApp.showFatalMsg("Unknown record: " + uuid, this);
            return;
        }

        setContentView(R.layout.record_view);

        setText(R.id.title, fileData.getTitle(rec));
        setText(R.id.group, fileData.getGroup(rec));
        setText(R.id.url, fileData.getURL(rec));
        setText(R.id.email, fileData.getEmail(rec));
        setText(R.id.user, fileData.getUsername(rec));

    }

    private final void setText(int id, String text)
    {
        TextView tv = (TextView)findViewById(id);
        tv.setText(text);
    }
}
