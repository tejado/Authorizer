/*
 * Copyright (©) 2009-2010 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.pwsafe.lib.file.PwsRecord;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TabHost;
import android.widget.TextView;

public class RecordView extends AbstractRecordTabActivity
{
    private static final String TAG = "RecordView";
    private static final String HIDDEN_PASSWORD = "***** (tap to show)";

    private static final String WORD_WRAP_PREF = "wordwrap";

    private static final int DIALOG_DELETE = MAX_DIALOG + 1;

    private static final int MENU_EDIT = 1;
    private static final int MENU_DELETE = 2;
    private static final int MENU_TOGGLE_PASSWORD = 3;
    private static final int MENU_COPY_USER = 4;
    private static final int MENU_COPY_PASSWORD = 5;
    private static final int MENU_COPY_NOTES = 6;
    private static final int MENU_TOGGLE_WRAP_NOTES = 7;

    private static final int EDIT_RECORD_REQUEST = 0;

    private static final String PASSWD = "passwd";
    private static final String DATE = "date";

    private TextView itsUserView;
    private boolean isPasswordShown = false;
    private TextView itsPasswordView;
    private boolean isWordWrap = true;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.record_view);

        Resources res = getResources();
        TabHost tabHost = getTabHost();
        TabHost.TabSpec spec;

        spec = tabHost.newTabSpec("basic")
            .setIndicator("Basic", res.getDrawable(R.drawable.ic_tab_contact))
            .setContent(R.id.basic_tab);
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("history")
            .setIndicator("History",
                          res.getDrawable(R.drawable.ic_tab_account_list))
            .setContent(R.id.history_tab);
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("notes")
            .setIndicator("Notes",
                          res.getDrawable(R.drawable.ic_tab_attachment))
            .setContent(R.id.notes_tab);
        tabHost.addTab(spec);

        tabHost.setCurrentTab(0);

        if (getUUID() == null) {
            PasswdSafeApp.showFatalMsg("No record chosen for file: " + getFile(),
                                       this);
            return;
        }

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        isWordWrap = prefs.getBoolean(WORD_WRAP_PREF, true);

        refresh();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        ActivityPasswdFile passwdFile = getPasswdFile();
        if (passwdFile != null) {
            passwdFile.touch();
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
            PasswdSafeApp.copyToClipboard(getPassword(), this);
            return true;
        }
        case MENU_COPY_USER:
        {
            TextView tv = (TextView)findViewById(R.id.user);
            PasswdSafeApp.copyToClipboard(tv.getText().toString(), this);
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

        mi = menu.add(0, MENU_DELETE, 0, R.string.delete);
        mi.setIcon(android.R.drawable.ic_menu_delete);

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
        boolean hasPassword = (itsPasswordView != null);
        MenuItem item = menu.findItem(MENU_TOGGLE_PASSWORD);
        if (item != null) {
            item.setTitle(isPasswordShown ?
                R.string.hide_password : R.string.show_password);
            item.setEnabled(hasPassword);
        }

        item = menu.findItem(MENU_COPY_PASSWORD);
        if (item != null) {
            item.setEnabled(hasPassword);
        }

        ActivityPasswdFile passwdFile = getPasswdFile();
        boolean canEdit = (passwdFile != null) &&
            passwdFile.getFileData().canEdit();

        item = menu.findItem(MENU_EDIT);
        if (item != null) {
            item.setEnabled(canEdit);
        }

        item = menu.findItem(MENU_DELETE);
        if (item != null) {
            item.setEnabled(canEdit);
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
        case MENU_DELETE:
        {
            showDialog(DIALOG_DELETE);
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
            PasswdSafeApp.copyToClipboard(tv.getText().toString(), this);
            return true;
        }
        case MENU_COPY_PASSWORD:
        {
            PasswdSafeApp.copyToClipboard(getPassword(), this);
            return true;
        }
        case MENU_COPY_NOTES:
        {
            TextView tv = (TextView)findViewById(R.id.notes);
            PasswdSafeApp.copyToClipboard(tv.getText().toString(), this);
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

    @Override
    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog = null;
        switch (id) {
        case DIALOG_DELETE:
        {
            AbstractDialogClickListener dlgClick =
                new AbstractDialogClickListener()
                {
                    @Override
                    public void onOkClicked(DialogInterface dialog)
                    {
                        deleteRecord();
                    }
                };

            TextView tv = (TextView)findViewById(R.id.title);
            AlertDialog.Builder alert = new AlertDialog.Builder(this)
                .setTitle("Delete Record?")
                .setMessage("Delete record \"" + tv.getText() + "\"?")
                .setPositiveButton("Ok", dlgClick)
                .setNegativeButton("Cancel", dlgClick)
                .setOnCancelListener(dlgClick);
            dialog = alert.create();
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
        PasswdFileData fileData = getPasswdFile().getFileData();
        if (fileData == null) {
            return;
        }

        String uuid = getUUID();
        PwsRecord rec = fileData.getRecord(uuid);
        if (rec == null) {
            PasswdSafeApp.showFatalMsg("Unknown record: " + uuid, this);
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
        setText(R.id.notes, View.NO_ID, fileData.getNotes(rec));

        PasswdHistory history = fileData.getPasswdHistory(rec);
        CheckBox enabledCb = (CheckBox)findViewById(R.id.history_enabled);
        enabledCb.setClickable(false);
        if (history != null) {
            enabledCb.setChecked(history.isEnabled());
            setText(R.id.history_max_size, View.NO_ID,
                    Integer.toString(history.getMaxSize()));

            ArrayList<HashMap<String, Object>> histData =
                new ArrayList<HashMap<String, Object>>();
            DateFormat dateFormatter = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM, DateFormat.MEDIUM);
            for (PasswdHistory.Entry entry : history.getPasswds()) {
                HashMap<String, Object> entryData =
                    new HashMap<String, Object>();
                entryData.put(PASSWD, entry.getPasswd());
                entryData.put(DATE, dateFormatter.format(entry.getDate()));
                histData.add(entryData);
            }

            ListView histView = (ListView)findViewById(R.id.history);
            ListAdapter adapter =
                new SimpleAdapter(this, histData,
                                  android.R.layout.simple_list_item_2,
                                  new String[] { PASSWD, DATE },
                                  new int[] { android.R.id.text1,
                                              android.R.id.text2 });
            histView.setAdapter(adapter);
        } else {
            enabledCb.setChecked(false);
        }

        isPasswordShown = false;
        itsPasswordView =
            setText(R.id.password, R.id.password_row,
                    (fileData.hasPassword(rec) ? HIDDEN_PASSWORD : null));
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

    private final void deleteRecord()
    {
        boolean removed = false;
        do {
            PasswdFileData fileData = getPasswdFile().getFileData();
            if (fileData == null) {
                break;
            }

            PwsRecord rec = fileData.getRecord(getUUID());
            if (rec == null) {
                break;
            }

            removed = fileData.removeRecord(rec);
        } while(false);

        if (removed) {
            saveFile();
        }
    }

    private final void togglePasswordShown()
    {
        TextView passwordField = (TextView)findViewById(R.id.password);
        isPasswordShown = !isPasswordShown;
        passwordField.setText(
            isPasswordShown ? getPassword() : HIDDEN_PASSWORD);
    }

    private final String getPassword()
    {
        String password = null;

        PasswdFileData fileData = getPasswdFile().getFileData();
        if (fileData != null) {
            PwsRecord rec = fileData.getRecord(getUUID());
            if (rec != null) {
                password = fileData.getPassword(rec);
            }
        }

        return password;
    }

    private final void setWordWrap()
    {
        TextView tv = (TextView)findViewById(R.id.notes);
        tv.setHorizontallyScrolling(!isWordWrap);
    }

    private final TextView setText(int id, int rowId, String text)
    {
        if (rowId != View.NO_ID) {
            View row = findViewById(rowId);
            if (row != null) {
                row.setVisibility((text != null) ? View.VISIBLE : View.GONE);
            }
        }

        TextView tv = null;
        if (text != null) {
            tv = (TextView)findViewById(id);
            tv.setText(text);
        }
        return tv;
    }
}
