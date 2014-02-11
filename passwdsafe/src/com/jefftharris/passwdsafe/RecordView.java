/*
 * Copyright (©) 2009-2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.util.Date;
import java.util.List;

import org.pwsafe.lib.file.PwsRecord;

import com.jefftharris.passwdsafe.file.HeaderPasswdPolicies;
import com.jefftharris.passwdsafe.file.PasswdExpiration;
import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdFileUri;
import com.jefftharris.passwdsafe.file.PasswdHistory;
import com.jefftharris.passwdsafe.file.PasswdPolicy;
import com.jefftharris.passwdsafe.file.PasswdRecord;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.Utils;
import com.jefftharris.passwdsafe.lib.view.AbstractDialogClickListener;
import com.jefftharris.passwdsafe.view.DialogUtils;
import com.jefftharris.passwdsafe.view.DialogValidator;
import com.jefftharris.passwdsafe.view.GuiUtils;
import com.jefftharris.passwdsafe.view.PasswdPolicyView;

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
    //private static final int TAB_PASSWORD = 1;
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
    private boolean itsHasReferences = false;
    private boolean itsIsProtected = false;
    private Drawable itsNotesTabDrawable;
    private DialogValidator itsDeleteValidator;


    public static void startActivityForResult(PasswdFileUri uri, String uuid,
                                              int requestCode,
                                              Activity parentAct)
    {
        Uri.Builder builder = uri.getUri().buildUpon();
        if (uuid != null) {
            builder.appendQueryParameter("rec", uuid);
        }
        PasswdSafeUtil.dbginfo(TAG, "start activity: %s", builder);
        Intent intent = new Intent(Intent.ACTION_VIEW, builder.build(),
                                   parentAct, RecordView.class);
        parentAct.startActivityForResult(intent, requestCode);
    }


    /** Called when the activity is first created. */
    @SuppressWarnings("deprecation")
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
            .setIndicator(getString(R.string.basic),
                          res.getDrawable(R.drawable.ic_tab_contact))
            .setContent(R.id.basic_tab);
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("password")
            .setIndicator(getString(R.string.password),
                          res.getDrawable(R.drawable.ic_tab_account_list))
            .setContent(R.id.password_tab);
        tabHost.addTab(spec);

        itsNotesTabDrawable = new NotesTabDrawable(res);
        spec = tabHost.newTabSpec("notes")
            .setIndicator(getString(R.string.notes), itsNotesTabDrawable)
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
            PasswdSafeUtil.showFatalMsg("No record chosen for file: " + getUri(),
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
    @SuppressWarnings("deprecation")
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
            PasswdSafeUtil.copyToClipboard(getPassword(), this);
            return true;
        }
        case MENU_COPY_USER:
        {
            TextView tv = (TextView)findViewById(R.id.user);
            PasswdSafeUtil.copyToClipboard(tv.getText().toString(), this);
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
    @SuppressWarnings("deprecation")
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
                canDelete = canEdit && !itsHasReferences && !itsIsProtected;
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
    @SuppressWarnings("deprecation")
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
            PasswdSafeUtil.copyToClipboard(tv.getText().toString(), this);
            break;
        }
        case MENU_COPY_PASSWORD:
        {
            PasswdSafeUtil.copyToClipboard(getPassword(), this);
            break;
        }
        case MENU_COPY_NOTES:
        {
            TextView tv = (TextView)findViewById(R.id.notes);
            PasswdSafeUtil.copyToClipboard(tv.getText().toString(), this);
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
                DialogUtils.createConfirmPrompt(this, dlgClick, title, prompt);
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
    @SuppressWarnings("deprecation")
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
        PasswdSafeUtil.dbginfo(TAG, "onActivityResult req: %d, rc: %d",
                               requestCode, resultCode);
        if (((requestCode == RECORD_EDIT_REQUEST) ||
             (requestCode == RECORD_VIEW_REQUEST))&&
            (resultCode == PasswdSafeApp.RESULT_MODIFIED)) {
            setResult(PasswdSafeApp.RESULT_MODIFIED);
            refresh();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @SuppressWarnings("deprecation")
    private final void refresh()
    {
        PasswdFileData fileData = getPasswdFile().getFileData();
        if (fileData == null) {
            return;
        }

        String uuid = getUUID();
        PwsRecord rec = fileData.getRecord(uuid);
        if (rec == null) {
            PasswdSafeUtil.showFatalMsg("Unknown record: " + uuid, this);
            return;
        } else {
            getPasswdFile().setLastViewedRecord(uuid);
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
        setPasswordFields(passwdRec, fileData, tabs);
        setNotesFields(passwdRec, fileData, tabs);
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

    /// Set the contents of a text field with a date
    private final TextView setDateText(int id, int rowId, Date date)
    {
        String str = null;
        if (date != null) {
            str = Utils.formatDate(date, this);
        }
        return setText(id, rowId, str);
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
            baseLabel.setText(R.string.alias_base_record_header);
            base.setText(fileData.getId(ref));
            break;
        }
        case SHORTCUT: {
            baseRow.setVisibility(View.VISIBLE);
            baseLabel.setText(R.string.shortcut_base_record_header);
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
        Date creationTime = null;
        Date lastModTime = null;
        switch (passwdRec.getType()) {
        case NORMAL: {
            creationTime = fileData.getCreationTime(rec);
            lastModTime = fileData.getLastModTime(rec);
            break;
        }
        case ALIAS: {
            recForPassword = passwdRec.getRef();
            hiddenId = R.string.hidden_password_alias;
            creationTime = fileData.getCreationTime(recForPassword);
            lastModTime = fileData.getLastModTime(recForPassword);
            break;
        }
        case SHORTCUT: {
            recForPassword = passwdRec.getRef();
            hiddenId = R.string.hidden_password_shortcut;
            creationTime = fileData.getCreationTime(recForPassword);
            lastModTime = fileData.getLastModTime(recForPassword);
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

        setVisibility(R.id.times_row,
                      (creationTime != null) || (lastModTime != null));
        setDateText(R.id.creation_time, R.id.creation_time_row, creationTime);
        setDateText(R.id.last_mod_time, R.id.last_mod_time_row, lastModTime);

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
        itsHasReferences = (references != null) && !references.isEmpty();
        if (itsHasReferences) {
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

        itsIsProtected = fileData.isProtected(rec);
        setVisibility(R.id.protected_row, itsIsProtected);

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

    private final void setPasswordFields(PasswdRecord passwdRec,
                                         PasswdFileData fileData,
                                         TabWidget tabs)
    {
        PasswdPolicy policy = null;
        String policyLoc = null;
        PasswdExpiration passwdExpiry = null;
        Date lastModTime = null;
        PasswdHistory history = null;
        switch (passwdRec.getType()) {
        case NORMAL: {
            policy = passwdRec.getPasswdPolicy();
            if (policy == null) {
                policy = getPasswdSafeApp().getDefaultPasswdPolicy();
                policyLoc = getString(R.string.default_policy);
            } else if (policy.getLocation() ==
                       PasswdPolicy.Location.RECORD_NAME) {
                HeaderPasswdPolicies hdrPolicies =
                    fileData.getHdrPasswdPolicies();
                String policyName = policy.getName();
                if (hdrPolicies != null) {
                    policy = hdrPolicies.getPasswdPolicy(policyName);
                }
                if (policy != null) {
                    policyLoc = getString(R.string.database_policy, policyName);
                }
            } else {
                policyLoc = getString(R.string.record);
            }

            PwsRecord rec = passwdRec.getRecord();
            passwdExpiry = fileData.getPasswdExpiry(rec);
            lastModTime = fileData.getPasswdLastModTime(rec);
            history = fileData.getPasswdHistory(rec);
            break;
        }
        case ALIAS: {
            PwsRecord recForPassword = passwdRec.getRef();
            passwdExpiry = fileData.getPasswdExpiry(recForPassword);
            lastModTime = fileData.getPasswdLastModTime(recForPassword);
            history = fileData.getPasswdHistory(recForPassword);
            break;
        }
        case SHORTCUT: {
            break;
        }
        }

        String expiryIntStr = null;
        if ((passwdExpiry != null) && passwdExpiry.itsIsRecurring) {
            int val = passwdExpiry.itsInterval;
            if (val != 0) {
                expiryIntStr =
                    getResources().getQuantityString(R.plurals.interval_days,
                                                     val, val);
            }
        }
        setVisibility(R.id.password_times_row,
                      (passwdExpiry != null) || (lastModTime != null) ||
                      (expiryIntStr != null));
        setDateText(R.id.expiration_time, R.id.expiration_time_row,
                    (passwdExpiry != null) ? passwdExpiry.itsExpiration : null);
        setDateText(R.id.password_mod_time, R.id.password_mod_time_row,
                    lastModTime);
        setText(R.id.expiration_interval, R.id.expiration_interval_row,
                expiryIntStr);

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
        GuiUtils.setListViewHeightBasedOnChildren(histView);
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

        int visibility = (policy != null) ? View.VISIBLE : View.GONE;
        PasswdPolicyView policyView =
            (PasswdPolicyView)findViewById(R.id.policy);
        findViewById(R.id.policy_label).setVisibility(visibility);
        findViewById(R.id.policy_sep).setVisibility(visibility);
        policyView.setVisibility(visibility);
        if (policy != null) {
            policyView.setGenerateEnabled(false);
            policyView.showLocation(policyLoc);
            policyView.showPolicy(policy, -1);
        }
    }

    @SuppressWarnings("deprecation")
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
