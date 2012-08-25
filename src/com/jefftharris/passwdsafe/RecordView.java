/*
 * Copyright (©) 2009-2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.util.List;

import org.pwsafe.lib.file.PwsRecord;

import com.jefftharris.passwdsafe.view.DialogUtils;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabWidget;
import android.widget.TextView;


public class RecordView extends AbstractRecordTabActivity
{
    private static final String TAG = "RecordView";

    private static final String WORD_WRAP_PREF = "wordwrap";
    private static final String MONOSPACE_PREF = "monospace";

    private static final int DIALOG_DELETE = MAX_DIALOG + 1;

    private static final int MENU_EDIT = 1;
    private static final int MENU_DELETE = 2;
    private static final int MENU_TOGGLE_PASSWORD = 3;
    private static final int MENU_COPY_USER = 4;
    private static final int MENU_COPY_PASSWORD = 5;
    private static final int MENU_COPY_NOTES = 6;
    private static final int MENU_TOGGLE_WRAP_NOTES = 7;
    private static final int MENU_CLOSE = 8;
    private static final int MENU_TOGGLE_MONOSPACE = 9;

    private static final int RECORD_EDIT_REQUEST = 0;
    private static final int RECORD_VIEW_REQUEST = 1;

    private static final int TAB_BASIC = 0;
    private static final int TAB_HISTORY = 1;
    private static final int TAB_NOTES = 2;

    private static final int NOTES_ICON_LEVEL_BASE = 0;
    private static final int NOTES_ICON_LEVEL_NOTES = 1;

    private class NotesTabDrawable extends StateListDrawable
    {
        public NotesTabDrawable(Resources res)
        {
            addState(new int[] { android.R.attr.state_selected },
                     res.getDrawable(R.drawable.ic_tab_attachment_selected));
            addState(new int[] { },
                     res.getDrawable(R.drawable.ic_tab_attachment_normal));
        }

        @Override
        protected boolean onStateChange(int[] stateSet)
        {
            boolean rc = super.onStateChange(stateSet);

            Drawable draw = getCurrent();
            if (draw != null) {
                draw.setLevel(itsHasNotes ? NOTES_ICON_LEVEL_NOTES :
                                            NOTES_ICON_LEVEL_BASE);
                rc = true;
            }

            return rc;
        }

        @Override
        public boolean isStateful()
        {
            return true;
        }
    }

    private TextView itsUserView;
    private boolean isPasswordShown = false;
    private TextView itsPasswordView;
    private String itsHiddenPasswordStr;
    private boolean isWordWrap = true;
    private boolean itsIsMonospace = false;
    private boolean itsHasNotes = false;
    private Drawable itsNotesTabDrawable;
    private DialogValidator itsDeleteValidator;


    public static void startActivityForResult(Uri fileUri, String uuid,
                                              int requestCode,
                                              Activity parentAct)
    {
        Uri.Builder builder = fileUri.buildUpon();
        if (uuid != null) {
            builder.appendQueryParameter("rec", uuid);
        }
        PasswdSafeApp.dbginfo(TAG, "start activity: " + builder);
        Intent intent = new Intent(Intent.ACTION_VIEW, builder.build(),
                                   parentAct, RecordView.class);
        parentAct.startActivityForResult(intent, requestCode);
    }


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

        itsNotesTabDrawable = new NotesTabDrawable(res);
        spec = tabHost.newTabSpec("notes")
            .setIndicator("Notes", itsNotesTabDrawable)
            .setContent(R.id.notes_tab);
        tabHost.addTab(spec);

        tabHost.setCurrentTab(0);
        tabHost.setOnTabChangedListener(new OnTabChangeListener()
        {
            public void onTabChanged(String tabId)
            {
                scrollTabToTop();
                GuiUtils.invalidateOptionsMenu(RecordView.this);
            }
        });

        if (getUUID() == null) {
            PasswdSafeApp.showFatalMsg("No record chosen for file: " + getUri(),
                                       this);
            return;
        }

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        isWordWrap = prefs.getBoolean(WORD_WRAP_PREF, true);
        itsIsMonospace = prefs.getBoolean(MONOSPACE_PREF, false);
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
        switch (v.getId()) {
        case R.id.username_label:
        case R.id.username_sep:
        case R.id.user: {
            menu.setHeaderTitle(R.string.username);
            menu.add(0, MENU_COPY_USER, 0, R.string.copy_clipboard);
            break;
        }
        case R.id.password_label:
        case R.id.password_sep:
        case R.id.password: {
            menu.setHeaderTitle(R.string.password);
            menu.add(0, MENU_COPY_PASSWORD, 0, R.string.copy_clipboard);
            break;
        }
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

        mi = menu.add(0, MENU_CLOSE, 0, R.string.close);
        mi.setIcon(android.R.drawable.ic_menu_close_clear_cancel);

        menu.add(0, MENU_TOGGLE_PASSWORD, 0, R.string.show_password);
        menu.add(0, MENU_COPY_USER, 0, R.string.copy_user);
        menu.add(0, MENU_COPY_PASSWORD, 0, R.string.copy_password);
        menu.add(0, MENU_COPY_NOTES, 0, R.string.copy_notes);
        menu.add(0, MENU_TOGGLE_MONOSPACE, 0, R.string.toggle_monospace);
        menu.add(0, MENU_TOGGLE_WRAP_NOTES, 0, R.string.toggle_word_wrap);
        return true;
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        int tab = getTabHost().getCurrentTab();

        boolean hasPassword = (itsPasswordView != null);
        MenuItem item = menu.findItem(MENU_TOGGLE_PASSWORD);
        if (item != null) {
            item.setTitle(isPasswordShown ?
                R.string.hide_password : R.string.show_password);
            item.setEnabled(hasPassword);
            item.setVisible(tab == TAB_BASIC);
        }

        item = menu.findItem(MENU_COPY_USER);
        if (item != null) {
            item.setVisible(tab == TAB_BASIC);
        }

        item = menu.findItem(MENU_COPY_PASSWORD);
        if (item != null) {
            item.setEnabled(hasPassword);
            item.setVisible(tab == TAB_BASIC);
        }

        item = menu.findItem(MENU_COPY_NOTES);
        if (item != null) {
            item.setVisible(tab == TAB_NOTES);
        }

        item = menu.findItem(MENU_TOGGLE_MONOSPACE);
        if (item != null) {
            item.setVisible(tab == TAB_NOTES);
        }

        item = menu.findItem(MENU_TOGGLE_WRAP_NOTES);
        if (item != null) {
            item.setVisible(tab == TAB_NOTES);
        }

        ActivityPasswdFile passwdFile = getPasswdFile();
        boolean canEdit = false;
        boolean canDelete = false;
        if (passwdFile != null) {
            PasswdFileData fileData = passwdFile.getFileData();
            if (fileData != null) {
                canEdit = fileData.canEdit();
                canDelete = canEdit;
                if (canDelete) {
                    PwsRecord rec = fileData.getRecord(getUUID());
                    if ((rec != null) && fileData.isProtected(rec)) {
                        canDelete = false;
                    }
                }
            } else {
                finish();
                return false;
            }
        }

        item = menu.findItem(MENU_EDIT);
        if (item != null) {
            item.setEnabled(canEdit);
        }

        item = menu.findItem(MENU_DELETE);
        if (item != null) {
            item.setEnabled(canDelete);
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
                RECORD_EDIT_REQUEST);
            break;
        }
        case MENU_DELETE:
        {
            showDialog(DIALOG_DELETE);
            break;
        }
        case MENU_TOGGLE_PASSWORD:
        {
            togglePasswordShown();
            break;
        }
        case MENU_COPY_USER:
        {
            TextView tv = (TextView)findViewById(R.id.user);
            PasswdSafeApp.copyToClipboard(tv.getText().toString(), this);
            break;
        }
        case MENU_COPY_PASSWORD:
        {
            PasswdSafeApp.copyToClipboard(getPassword(), this);
            break;
        }
        case MENU_COPY_NOTES:
        {
            TextView tv = (TextView)findViewById(R.id.notes);
            PasswdSafeApp.copyToClipboard(tv.getText().toString(), this);
            break;
        }
        case MENU_TOGGLE_WRAP_NOTES:
        {
            isWordWrap = !isWordWrap;
            saveNotesOptionsPrefs();
            setNotesOptions();
            break;
        }
        case MENU_CLOSE:
        {
            ActivityPasswdFile passwdFile = getPasswdFile();
            if (passwdFile != null) {
                passwdFile.close();
            }
            break;
        }
        case MENU_TOGGLE_MONOSPACE: {
            itsIsMonospace = !itsIsMonospace;
            saveNotesOptionsPrefs();
            setNotesOptions();
            break;
        }
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
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
            String prompt = getString(R.string.delete_record_msg, tv.getText());
            String title = getString(R.string.delete_record_title);
            DialogUtils.DialogData data =
                DialogUtils.createDeletePrompt(this, dlgClick, title, prompt);
            dialog = data.itsDialog;
            itsDeleteValidator = data.itsValidator;
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
     * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
     */
    @Override
    protected void onPrepareDialog(int id, Dialog dialog)
    {
        super.onPrepareDialog(id, dialog);
        switch (id)
        {
        case DIALOG_DELETE:
        {
            itsDeleteValidator.reset();
            break;
        }
        default:
        {
            break;
        }
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
        if (((requestCode == RECORD_EDIT_REQUEST) ||
             (requestCode == RECORD_VIEW_REQUEST))&&
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
        PasswdRecord passwdRec = fileData.getPasswdRecord(rec);

        TabWidget tabs = getTabHost().getTabWidget();

        setBaseRecord(passwdRec, fileData);
        setText(R.id.title, View.NO_ID, fileData.getTitle(rec));
        setText(R.id.group, R.id.group_row, fileData.getGroup(rec));
        itsUserView =
            setText(R.id.user, R.id.user_row, fileData.getUsername(rec));
        if (itsUserView != null) {
            registerForContextMenu(itsUserView);
            View v = findViewById(R.id.username_label);
            registerForContextMenu(v);
            v = findViewById(R.id.username_sep);
            registerForContextMenu(v);
        }

        setBasicFields(passwdRec, fileData);
        setNotesFields(passwdRec, fileData, tabs);
        setHistoryFields(passwdRec, fileData, tabs);
        GuiUtils.invalidateOptionsMenu(this);
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

            removed = fileData.removeRecord(rec, this);
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
            isPasswordShown ? getPassword() : itsHiddenPasswordStr);
        passwordField.setTypeface(
            isPasswordShown ? Typeface.MONOSPACE : Typeface.DEFAULT);

    }

    private final String getPassword()
    {
        String password = null;

        PasswdFileData fileData = getPasswdFile().getFileData();
        if (fileData != null) {
            PwsRecord rec = fileData.getRecord(getUUID());
            if (rec != null) {
                PasswdRecord passwdRec = fileData.getPasswdRecord(rec);
                switch (passwdRec.getType()) {
                case NORMAL: {
                    password = fileData.getPassword(rec);
                    break;
                }
                case ALIAS:
                case SHORTCUT: {
                    password = fileData.getPassword(passwdRec.getRef());
                    break;
                }
                }
            }
        }

        return password;
    }

    private final void saveNotesOptionsPrefs()
    {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(WORD_WRAP_PREF, isWordWrap);
        editor.putBoolean(MONOSPACE_PREF, itsIsMonospace);
        editor.commit();
    }

    private final void setNotesOptions()
    {
        TextView tv = (TextView)findViewById(R.id.notes);
        tv.setHorizontallyScrolling(!isWordWrap);
        tv.setTypeface(
                       itsIsMonospace ? Typeface.MONOSPACE : Typeface.DEFAULT);
   }

    private final TextView setText(int id, int rowId, String text)
    {
        if (rowId != View.NO_ID) {
            setVisibility(rowId, (text != null));
        }

        TextView tv = (TextView)findViewById(id);
        tv.setText(text);
        return (text == null) ? null : tv;
    }

    private final void setVisibility(int id, boolean visible)
    {
        View v = findViewById(id);
        if (v != null) {
            v.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private final void setBaseRecord(PasswdRecord passwdRec,
                                     PasswdFileData fileData)
    {
        View baseRow = findViewById(R.id.base_row);
        TextView baseLabel = (TextView)findViewById(R.id.base_label);
        TextView base = (TextView)findViewById(R.id.base);
        PwsRecord ref = passwdRec.getRef();

        switch (passwdRec.getType()) {
        case NORMAL: {
            baseRow.setVisibility(View.GONE);
            break;
        }
        case ALIAS: {
            baseRow.setVisibility(View.VISIBLE);
            baseLabel.setText(R.string.alias_base_record_label);
            base.setText(fileData.getId(ref));
            break;
        }
        case SHORTCUT: {
            baseRow.setVisibility(View.VISIBLE);
            baseLabel.setText(R.string.shortcut_base_record_label);
            base.setText(fileData.getId(ref));
            break;
        }
        }

        View.OnClickListener onclick = new View.OnClickListener()
        {
            public void onClick(View v)
            {
                showRefRec(true, 0);
            }
        };
        View baseGroup = findViewById(R.id.base_group);
        View baseBtn = findViewById(R.id.base_btn);
        base.setOnClickListener(onclick);
        baseGroup.setOnClickListener(onclick);
        baseBtn.setOnClickListener(onclick);
    }

    private final void showRefRec(boolean baseRef, int referencingPos)
    {
        PasswdFileData fileData = getPasswdFile().getFileData();
        if (fileData == null) {
            return;
        }
        PwsRecord rec = fileData.getRecord(getUUID());
        if (rec == null) {
            return;
        }
        PasswdRecord passwdRec = fileData.getPasswdRecord(rec);
        PwsRecord refRec = null;
        if (baseRef) {
            refRec = passwdRec.getRef();
        } else {
            List<PwsRecord> references = passwdRec.getRefsToRecord();
            if ((referencingPos >= 0) && (referencingPos < references.size())) {
                refRec = references.get(referencingPos);
            }
        }
        if (refRec == null) {
            return;
        }
        String uuid = fileData.getUUID(refRec);
        if (uuid == null) {
            return;
        }

        startActivityForResult(getUri(), uuid, RECORD_VIEW_REQUEST, this);
    }

    private final void setBasicFields(PasswdRecord passwdRec,
                                      PasswdFileData fileData)
    {
        PwsRecord rec = passwdRec.getRecord();

        switch (passwdRec.getType()) {
        case NORMAL:
        case ALIAS: {
            setText(R.id.url, R.id.url_row, fileData.getURL(rec));
            setText(R.id.email, R.id.email_row, fileData.getEmail(rec));
            break;
        }
        case SHORTCUT: {
            setText(R.id.url, R.id.url_row, null);
            setText(R.id.email, R.id.email_row, null);
            break;
        }
        }

        PwsRecord recForPassword = rec;
        int hiddenId = R.string.hidden_password_normal;
        String passwdExpiry = null;
        switch (passwdRec.getType()) {
        case NORMAL: {
            passwdExpiry = fileData.getPasswdExpiryTime(rec);
            break;
        }
        case ALIAS: {
            recForPassword = passwdRec.getRef();
            hiddenId = R.string.hidden_password_alias;
            passwdExpiry = fileData.getPasswdExpiryTime(recForPassword);
            break;
        }
        case SHORTCUT: {
            recForPassword = passwdRec.getRef();
            hiddenId = R.string.hidden_password_shortcut;
            break;
        }
        }
        isPasswordShown = false;
        itsHiddenPasswordStr = getString(hiddenId);
        itsPasswordView = setText(R.id.password, R.id.password_row,
                                  (fileData.hasPassword(recForPassword)
                                      ? itsHiddenPasswordStr : null));
        if (itsPasswordView != null) {
            View.OnClickListener listener = new View.OnClickListener()
            {
                public void onClick(View v)
                {
                    togglePasswordShown();
                }
            };
            View v = findViewById(R.id.password_label);
            v.setOnClickListener(listener);
            registerForContextMenu(v);
            v = findViewById(R.id.password_sep);
            v.setOnClickListener(listener);
            registerForContextMenu(v);
            itsPasswordView.setOnClickListener(listener);
            registerForContextMenu(itsPasswordView);

        }
        setText(R.id.expiration, R.id.expiration_row, passwdExpiry);

        ListView referencesView = (ListView)findViewById(R.id.references);
        referencesView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent,
                                    View view,
                                    int position,
                                    long id)
            {
                ListView parentList = (ListView)parent;
                showRefRec(false, position - parentList.getHeaderViewsCount());
            }
        });

        if (referencesView.getHeaderViewsCount() == 0) {
            View header = getLayoutInflater().inflate(R.layout.listview_header,
                                                      null);
            TextView tv = (TextView)header.findViewById(R.id.text);
            tv.setText(R.string.references_label);
            referencesView.addHeaderView(header);
        }

        List<PwsRecord> references = passwdRec.getRefsToRecord();
        if ((references != null) && !references.isEmpty()) {
            ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this,
                                         R.layout.normal_list_item1);
            for (PwsRecord refRec: references) {
                adapter.add(fileData.getId(refRec));
            }
            referencesView.setAdapter(adapter);
            referencesView.setVisibility(View.VISIBLE);
        } else {
            referencesView.setAdapter(null);
            referencesView.setVisibility(View.GONE);
        }
        GuiUtils.setListViewHeightBasedOnChildren(referencesView);

        setVisibility(R.id.protected_row, fileData.isProtected(rec));

        scrollTabToTop();
    }

    private final void setNotesFields(PasswdRecord passwdRec,
                                      PasswdFileData fileData,
                                      TabWidget tabs)
    {
        PwsRecord rec = passwdRec.getRecord();

        switch (passwdRec.getType()) {
        case NORMAL:
        case ALIAS: {
            String notes = fileData.getNotes(rec);
            itsHasNotes = !TextUtils.isEmpty(notes);
            setText(R.id.notes, View.NO_ID, notes);
            break;
        }
        case SHORTCUT: {
            itsHasNotes = false;
            break;
        }
        }

        int[] currState = itsNotesTabDrawable.getState();
        itsNotesTabDrawable.setState(new int[currState.length + 1]);
        itsNotesTabDrawable.setState(currState);
        View notesTab = tabs.getChildAt(TAB_NOTES);
        View notesTitle = notesTab.findViewById(android.R.id.title);
        notesTab.setEnabled(itsHasNotes);
        notesTitle.setEnabled(itsHasNotes);
        setNotesOptions();
    }

    private final void setHistoryFields(PasswdRecord passwdRec,
                                        PasswdFileData fileData,
                                        TabWidget tabs)
    {
        View historyTab = tabs.getChildAt(TAB_HISTORY);
        View historyTitle = historyTab.findViewById(android.R.id.title);
        PasswdHistory history = null;
        boolean tabEnabled = true;
        switch (passwdRec.getType()) {
        case NORMAL: {
            history = fileData.getPasswdHistory(passwdRec.getRecord());
            tabEnabled = true;
            break;
        }
        case ALIAS: {
            history = fileData.getPasswdHistory(passwdRec.getRef());
            tabEnabled = true;
            break;
        }
        case SHORTCUT: {
            tabEnabled = false;
            break;
        }
        }
        historyTab.setEnabled(tabEnabled);
        historyTitle.setEnabled(tabEnabled);

        boolean historyExists = (history != null);
        boolean historyEnabled = false;
        String historyMaxSize;
        ListView histView = (ListView)findViewById(R.id.history);
        if (historyExists) {
            historyEnabled = history.isEnabled();
            historyMaxSize = Integer.toString(history.getMaxSize());
            histView.setAdapter(
                GuiUtils.createPasswdHistoryAdapter(history, this, true));
        } else {
            historyMaxSize = getString(R.string.n_a);
            histView.setAdapter(null);
        }
        CheckBox enabledCb = (CheckBox)findViewById(R.id.history_enabled);
        enabledCb.setClickable(false);
        enabledCb.setChecked(historyEnabled);
        enabledCb.setEnabled(historyExists);
        TextView historyMaxSizeView =
            (TextView)findViewById(R.id.history_max_size);
        historyMaxSizeView.setText(historyMaxSize);
        historyMaxSizeView.setEnabled(historyExists);
        histView.setEnabled(historyEnabled);
        findViewById(R.id.history_max_size_label).setEnabled(historyExists);
        findViewById(R.id.history_sep).setEnabled(historyExists);
    }

    private void scrollTabToTop()
    {
        TabHost host = getTabHost();
        int id = host.getCurrentTab();
        final View v = host.getTabContentView().getChildAt(id);
        v.post(new Runnable() {
            public void run()
            {
                v.scrollTo(0, 0);
            }
        });
    }
}
