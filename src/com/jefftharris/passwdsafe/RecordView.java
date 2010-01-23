/*
 * Copyright (©) 2009-2010 Jeff Harris <jefftharris@gmail.com>
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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.TextView;

public class RecordView extends Activity
{
    private static final String TAG = "RecordView";
    private static final String HIDDEN_PASSWORD = "***** (click to show)";

    private static final String WORD_WRAP_PREF = "wordwrap";

    private static final int ID_COPY_CLIPBOARD = 1;

    private static final int MENU_TOGGLE_PASSWORD = 1;
    private static final int MENU_COPY_PASSWORD = 2;
    private static final int MENU_COPY_NOTES = 3;
    private static final int MENU_TOGGLE_WRAP_NOTES = 4;

    private ActivityPasswdFile itsFile;
    private boolean isPasswordShown = false;
    private String itsPassword = null;
    private boolean isWordWrap = true;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Log.i(TAG, "onCreate intent:" + getIntent());

        String fileName = intent.getData().getPath();
        String uuid = intent.getData().getQueryParameter("rec");

        PasswdSafeApp app = (PasswdSafeApp)getApplication();
        itsFile = app.accessPasswdFile(fileName, this);
        PasswdFileData fileData = itsFile.getFileData();
        if (fileData == null) {
            PasswdSafeApp.showFatalMsg("File not open: " + fileName, this);
            return;
        }

        PwsRecord rec = fileData.getRecord(uuid);
        if (rec == null) {
            PasswdSafeApp.showFatalMsg("Unknown record: " + uuid, this);
            return;
        }

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        isWordWrap = prefs.getBoolean(WORD_WRAP_PREF, true);

        setContentView(R.layout.record_view);

        setText(R.id.title, fileData.getTitle(rec));
        setText(R.id.group, fileData.getGroup(rec));
        setText(R.id.url, fileData.getURL(rec));
        setText(R.id.email, fileData.getEmail(rec));
        setText(R.id.user, fileData.getUsername(rec));
        setText(R.id.notes,
                fileData.getNotes(rec).replace("\r\n", "\n"));

        isPasswordShown = false;
        itsPassword = fileData.getPassword(rec);
        TextView passwordField = (TextView)findViewById(R.id.password);
        passwordField.setText(HIDDEN_PASSWORD);
        passwordField.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                togglePasswordShown();
            }
        });
        registerForContextMenu(passwordField);

        setWordWrap();
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
     * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case ID_COPY_CLIPBOARD:
        {
            copyToClipboard(itsPassword);
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

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, MENU_TOGGLE_PASSWORD, 0, R.string.show_password);
        menu.add(0, MENU_COPY_PASSWORD, 0, R.string.copy_password);
        menu.add(0, MENU_COPY_NOTES, 0, R.string.copy_notes);
        menu.add(0, MENU_TOGGLE_WRAP_NOTES, 0, R.string.toggle_word_wrap);
        return true;
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        MenuItem item = menu.findItem(MENU_TOGGLE_PASSWORD);
        if (item != null) {
            item.setTitle(isPasswordShown ?
                R.string.hide_password : R.string.show_password);
        }
        return true;
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case MENU_TOGGLE_PASSWORD:
        {
            togglePasswordShown();
            return true;
        }
        case MENU_COPY_PASSWORD:
        {
            copyToClipboard(itsPassword);
            return true;
        }
        case MENU_COPY_NOTES:
        {
            TextView tv = (TextView)findViewById(R.id.notes);
            copyToClipboard(tv.getText().toString());
            return true;
        }
        case MENU_TOGGLE_WRAP_NOTES:
        {
            SharedPreferences prefs = getPreferences(MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            isWordWrap = !isWordWrap;
            editor.putBoolean(WORD_WRAP_PREF, isWordWrap);
            editor.commit();

            setWordWrap();
            return true;
        }
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private final void togglePasswordShown()
    {
        TextView passwordField = (TextView)findViewById(R.id.password);
        isPasswordShown = !isPasswordShown;
        passwordField.setText(isPasswordShown ? itsPassword : HIDDEN_PASSWORD);
    }

    private final void setWordWrap()
    {
        TextView tv = (TextView)findViewById(R.id.notes);
        tv.setHorizontallyScrolling(!isWordWrap);
    }

    private final void copyToClipboard(String str)
    {
        ClipboardManager clipMgr = (ClipboardManager)
            getSystemService(Context.CLIPBOARD_SERVICE);
        clipMgr.setText(str);
    }

    private final void setText(int id, String text)
    {
        TextView tv = (TextView)findViewById(id);
        tv.setText(text);
    }
}
