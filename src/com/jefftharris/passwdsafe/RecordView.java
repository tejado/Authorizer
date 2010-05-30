/*
 * Copyright (©) 2009-2010 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.File;

import org.pwsafe.lib.file.PwsRecord;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.ClipboardManager;
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

    private static final int MENU_EDIT = 1;
    private static final int MENU_TOGGLE_PASSWORD = 2;
    private static final int MENU_COPY_USER = 3;
    private static final int MENU_COPY_PASSWORD = 4;
    private static final int MENU_COPY_NOTES = 5;
    private static final int MENU_TOGGLE_WRAP_NOTES = 6;

    private static final int EDIT_RECORD_REQUEST = 0;

    private File itsFile;
    private String itsUUID;
    private ActivityPasswdFile itsPasswdFile;
    private TextView itsUserView;
    private boolean isPasswordShown = false;
    private String itsPassword;
    private TextView itsPasswordView;
    private boolean isWordWrap = true;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        PasswdSafeApp.dbginfo(TAG, "onCreate intent:" + getIntent());

        itsFile = new File(intent.getData().getPath());
        itsUUID = intent.getData().getQueryParameter("rec");
        if (itsUUID == null) {
            PasswdSafeApp.showFatalMsg("No record chosen for file: " + itsFile,
                                       this);
            return;
        }

        PasswdSafeApp app = (PasswdSafeApp)getApplication();
        itsPasswdFile = app.accessPasswdFile(itsFile, this);

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        isWordWrap = prefs.getBoolean(WORD_WRAP_PREF, true);

        setContentView(R.layout.record_view);
        setTitle(PasswdSafeApp.getAppFileTitle(itsPasswdFile, this));
        refresh();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy()
    {
        PasswdSafeApp.dbginfo(TAG, "onDestroy");
        super.onDestroy();
        if (itsPasswdFile != null) {
            itsPasswdFile.release();
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        if (itsPasswdFile != null) {
            itsPasswdFile.touch();
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case MENU_COPY_PASSWORD:
        {
            copyToClipboard(itsPassword);
            return true;
        }
        case MENU_COPY_USER:
        {
            TextView tv = (TextView)findViewById(R.id.user);
            copyToClipboard(tv.getText().toString());
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
        if (v == itsUserView) {
            menu.setHeaderTitle(R.string.username);
            menu.add(0, MENU_COPY_USER, 0, R.string.copy_clipboard);
        } else if (v == itsPasswordView) {
            menu.setHeaderTitle(R.string.password);
            menu.add(0, MENU_COPY_PASSWORD, 0, R.string.copy_clipboard);
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuItem mi = menu.add(0, MENU_EDIT, 0, R.string.edit);
        mi.setIcon(android.R.drawable.ic_menu_edit);

        menu.add(0, MENU_TOGGLE_PASSWORD, 0, R.string.show_password);
        menu.add(0, MENU_COPY_USER, 0, R.string.copy_user);
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

        item = menu.findItem(MENU_EDIT);
        if (item != null) {
            item.setEnabled(itsPasswdFile.getFileData().canEdit());
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
        case MENU_EDIT:
        {
            startActivityForResult(
                new Intent(Intent.ACTION_EDIT, getIntent().getData(),
                           this, RecordEditActivity.class),
                EDIT_RECORD_REQUEST);
            return true;
        }
        case MENU_TOGGLE_PASSWORD:
        {
            togglePasswordShown();
            return true;
        }
        case MENU_COPY_USER:
        {
            TextView tv = (TextView)findViewById(R.id.user);
            copyToClipboard(tv.getText().toString());
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

    /* (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        PasswdSafeApp.dbginfo(TAG,
                              "onActivityResult req: " + requestCode +
                              ", rc: " + resultCode);
        if ((requestCode == EDIT_RECORD_REQUEST) &&
            (resultCode == PasswdSafeApp.RESULT_MODIFIED)) {
            setResult(PasswdSafeApp.RESULT_MODIFIED);
            refresh();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private final void refresh()
    {
        PasswdFileData fileData = itsPasswdFile.getFileData();
        if (fileData == null) {
            PasswdSafeApp.showFatalMsg("File not open: " + itsFile, this);
            return;
        }
        PwsRecord rec = fileData.getRecord(itsUUID);
        if (rec == null) {
            PasswdSafeApp.showFatalMsg("Unknown record: " + itsUUID, this);
            return;
        }

        setText(R.id.title, View.NO_ID, fileData.getTitle(rec));
        setText(R.id.group, R.id.group_row, fileData.getGroup(rec));
        setText(R.id.url, R.id.url_row, fileData.getURL(rec));
        setText(R.id.email, R.id.email_row, fileData.getEmail(rec));
        itsUserView =
            setText(R.id.user, R.id.user_row, fileData.getUsername(rec));
        if (itsUserView != null) {
            registerForContextMenu(itsUserView);
        }
        setText(R.id.expiration, R.id.expiration_row,
                fileData.getPasswdExpiryTime(rec));
        setText(R.id.notes, R.id.notes_row, fileData.getNotes(rec));
        isPasswordShown = false;
        itsPassword = fileData.getPassword(rec);
        itsPasswordView =
            setText(R.id.password, R.id.password_row,
                    (itsPassword == null ? null : HIDDEN_PASSWORD));
        if (itsPasswordView != null) {
            itsPasswordView.setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View v)
                {
                    togglePasswordShown();
                }
            });
            registerForContextMenu(itsPasswordView);
        }
        setWordWrap();
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

    private final TextView setText(int id, int rowId, String text)
    {
        View row = findViewById(rowId);
        if (row != null) {
            row.setVisibility((text != null) ? View.VISIBLE : View.GONE);
        }

        TextView tv = null;
        if (text != null) {
            tv = (TextView)findViewById(id);
            tv.setText(text);
        }
        return tv;
    }
}
