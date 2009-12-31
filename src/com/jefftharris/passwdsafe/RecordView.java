/*
 * Copyright (©) 2009-2010 Jeff Harris <jeffharris@users.sourceforge.net>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import org.pwsafe.lib.file.PwsRecord;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.TextView;

public class RecordView extends Activity
{
    private static final String TAG = "RecordView";
    private static final String HIDDEN_PASSWORD = "***** (click to show)";

    private static final int ID_COPY_CLIPBOARD = 1;

    private boolean isPasswordShown = false;
    private String itsPassword = null;

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

        isPasswordShown = false;
        itsPassword = fileData.getPassword(rec);
        TextView passwordField = (TextView)findViewById(R.id.password);
        passwordField.setText(HIDDEN_PASSWORD);
        passwordField.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                TextView passwordField = (TextView)v;
                isPasswordShown = !isPasswordShown;
                passwordField.setText(isPasswordShown ?
                    itsPassword : HIDDEN_PASSWORD);
            }
        });
        registerForContextMenu(passwordField);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case ID_COPY_CLIPBOARD:
        {
            ClipboardManager clipMgr = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
            clipMgr.setText(itsPassword);
            return true;
        }
        default:
            return super.onContextItemSelected(item);
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu,
                                    View v,
                                    ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.password);
        menu.add(0, ID_COPY_CLIPBOARD, 0, R.string.copy_clipboard);
    }

    private final void setText(int id, String text)
    {
        TextView tv = (TextView)findViewById(id);
        tv.setText(text);
    }
}
