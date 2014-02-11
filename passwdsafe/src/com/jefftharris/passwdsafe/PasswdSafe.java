/*
 * Copyright (©) 2009-2014 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pwsafe.lib.exception.InvalidPassphraseException;
import org.pwsafe.lib.file.PwsRecord;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdFileUri;
import com.jefftharris.passwdsafe.file.PasswdRecordFilter;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.view.AbstractDialogClickListener;
import com.jefftharris.passwdsafe.view.DialogUtils;
import com.jefftharris.passwdsafe.view.DialogValidator;
import com.jefftharris.passwdsafe.view.GuiUtils;
import com.jefftharris.passwdsafe.view.PasswordVisibilityMenuHandler;

public class PasswdSafe extends AbstractPasswdSafeActivity
{
    private static final int DIALOG_GET_PASSWD =        MAX_DIALOG + 1;
    private static final int DIALOG_PROGRESS =          MAX_DIALOG + 2;
    private static final int DIALOG_DETAILS =           MAX_DIALOG + 3;
    private static final int DIALOG_CHANGE_PASSWD =     MAX_DIALOG + 4;
    private static final int DIALOG_FILE_NEW =          MAX_DIALOG + 5;
    private static final int DIALOG_DELETE =            MAX_DIALOG + 6;
    private static final int DIALOG_PASSWD_EXPIRYS =    MAX_DIALOG + 7;
    private static final int DIALOG_PASSWD_EXPIRYS_CUSTOM = MAX_DIALOG + 8;

    private static final int MENU_ADD_RECORD =      ABS_MENU_MAX + 1;
    private static final int MENU_DETAILS =         ABS_MENU_MAX + 2;
    private static final int MENU_CHANGE_PASSWD =   ABS_MENU_MAX + 3;
    private static final int MENU_DELETE =          ABS_MENU_MAX + 4;
    private static final int MENU_PROTECT=          ABS_MENU_MAX + 5;
    private static final int MENU_UNPROTECT=        ABS_MENU_MAX + 6;
    private static final int MENU_PASSWD_POLICIES = ABS_MENU_MAX + 7;
    private static final int MENU_PASSWD_EXPIRYS =  ABS_MENU_MAX + 8;
    private static final int MENU_PASSWD_EXPIRY_NOTIF = ABS_MENU_MAX + 9;

    private static final int CTXMENU_COPY_USER = 1;
    private static final int CTXMENU_COPY_PASSWD = 2;

    private static final int RECORD_VIEW_REQUEST = 0;
    private static final int RECORD_ADD_REQUEST = 1;
    private static final int POLICY_VIEW_REQUEST = 2;

    private AbstractTask itsLoadTask;
    private DialogValidator itsChangePasswdValidator;
    private DialogValidator itsFileNewValidator;
    private DialogValidator itsDeleteValidator;
    private String itsRecToOpen;
    private boolean itsIsNotifyExpirations = true;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        registerForContextMenu(getListView());

        Intent intent = getIntent();
        PasswdSafeUtil.dbginfo(TAG, "onCreate intent: %s", intent);

        View v = findViewById(R.id.expiry_clear_btn);
        v.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                View panel = findViewById(R.id.expiry_panel);
                panel.setVisibility(View.GONE);
            }
        });

        v = findViewById(R.id.expiry_panel);
        v.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                View panel = findViewById(R.id.expiry_panel);
                panel.setVisibility(View.GONE);
                PasswdRecordFilter.ExpiryFilter filter =
                    itsExpiryNotifPref.getFilter();
                if (filter != null) {
                    setRecordExpiryFilter(filter, null);
                }
            }
        });

        String action = intent.getAction();
        if (action.equals(PasswdSafeUtil.VIEW_INTENT) ||
            action.equals(Intent.ACTION_VIEW)) {
            onCreateView(intent);
        } else if (action.equals(PasswdSafeUtil.NEW_INTENT)) {
            onCreateNew(intent);
        } else {
            Log.e(TAG, "Unknown action for intent: " + intent);
            finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        if (intent != null) {
            String action = intent.getAction();
            if (action.equals(PasswdSafeUtil.VIEW_INTENT) ||
                action.equals(Intent.ACTION_VIEW)) {
                initNewViewIntent();
                showFileData(MOD_INIT);
                onCreateView(intent);
                return;
            }
        }
    }


    /** Initialize the activity for a new view intent */
    @Override
    protected void initNewViewIntent()
    {
        super.initNewViewIntent();
        itsIsNotifyExpirations = true;
        findViewById(R.id.expiry_panel).setVisibility(View.GONE);
        itsRecToOpen = null;
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @SuppressWarnings("deprecation")
    @Override
    protected void onPause()
    {
        removeDialog(DIALOG_GET_PASSWD);
        removeDialog(DIALOG_DETAILS);
        if (itsLoadTask != null) {
            itsLoadTask.cancel(true);
        }

        super.onPause();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @SuppressWarnings("deprecation")
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        removeDialog(DIALOG_GET_PASSWD);
        removeDialog(DIALOG_PROGRESS);
        removeProgressDialog();
        super.onSaveInstanceState(outState);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        PasswdFileData fileData = getPasswdFileData();
        boolean isV3 = (fileData != null) && fileData.isV3();

        MenuItem mi;

        addSearchMenuItem(menu);

        mi = menu.add(0, MENU_ADD_RECORD, 0, R.string.add_record);
        mi.setIcon(android.R.drawable.ic_menu_add);

        addCloseMenuItem(menu);

        if (isV3) {
            mi = menu.add(0, MENU_PASSWD_POLICIES, 0,
                          R.string.password_policies);

            mi = menu.add(0, MENU_PASSWD_EXPIRYS, 0, R
                          .string.expired_passwords);

            mi = menu.add(0, MENU_PASSWD_EXPIRY_NOTIF, 0,
                          R.string.expiration_notifications);
            mi.setCheckable(true);
        }

        // File operations submenu
        SubMenu submenu = menu.addSubMenu(R.string.file_operations);

        mi = submenu.add(0, MENU_CHANGE_PASSWD, 0, R.string.change_password);
        mi.setIcon(android.R.drawable.ic_menu_edit);

        mi = submenu.add(0, MENU_DELETE, 0, R.string.delete_file);
        mi.setIcon(android.R.drawable.ic_menu_delete);

        mi = submenu.add(0, MENU_PROTECT, 0, R.string.protect_all);
        mi = submenu.add(0, MENU_UNPROTECT, 0, R.string.unprotect_all);
        // End file operations submenu

        mi = menu.add(0, MENU_DETAILS, 0, R.string.details);
        mi.setIcon(android.R.drawable.ic_menu_info_details);

        addParentMenuItem(menu);

        return true;
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        boolean editEnabled = false;
        boolean deleteEnabled = false;
        boolean isRoot = isRootGroup();
        boolean detailsEnabled = false;
        PasswdFileData fileData = getPasswdFileData();
        if (fileData != null) {
            editEnabled = fileData.canEdit();
            deleteEnabled = fileData.canDelete();
            detailsEnabled = true;
        }

        MenuItem mi = menu.findItem(MENU_ADD_RECORD);
        if (mi != null) {
            mi.setEnabled(editEnabled);
        }

        mi = menu.findItem(MENU_CHANGE_PASSWD);
        if (mi != null) {
            mi.setEnabled(editEnabled);
        }

        mi = menu.findItem(MENU_DELETE);
        if (mi != null) {
            mi.setEnabled(deleteEnabled);
        }

        mi = menu.findItem(MENU_PROTECT);
        if (mi != null) {
            mi.setTitle(isRoot ? R.string.protect_all : R.string.protect_group);
            mi.setEnabled(editEnabled);
        }

        mi = menu.findItem(MENU_UNPROTECT);
        if (mi != null) {
            mi.setTitle(isRoot ?
                        R.string.unprotect_all : R.string.unprotect_group);
            mi.setEnabled(editEnabled);
        }

        mi = menu.findItem(MENU_PASSWD_EXPIRY_NOTIF);
        if (mi != null) {
            PasswdFileUri uri = getUri();
            boolean enabled = NotificationMgr.notifSupported(uri);
            boolean checked = false;
            if (enabled) {
                NotificationMgr notifyMgr = getPasswdSafeApp().getNotifyMgr();
                checked = notifyMgr.hasPasswdExpiryNotif(uri);
            }
            mi.setChecked(checked);
            mi.setEnabled(enabled);
        }

        mi = menu.findItem(MENU_DETAILS);
        if (mi != null) {
            mi.setEnabled(detailsEnabled);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @SuppressWarnings("deprecation")
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        boolean rc = true;
        switch (item.getItemId()) {
        case MENU_ADD_RECORD:
        {
            Intent addIntent =
                new Intent(Intent.ACTION_INSERT, getUri().getUri(),
                           this, RecordEditActivity.class);
            addIntent.putExtra(RecordEditActivity.INSERT_INTENT_EXTRA_GROUP,
                               getCurrGroupStr());
            startActivityForResult(addIntent, RECORD_ADD_REQUEST);
            break;
        }
        case MENU_DETAILS:
        {
            showDialog(DIALOG_DETAILS);
            break;
        }
        case MENU_CHANGE_PASSWD:
        {
            showDialog(DIALOG_CHANGE_PASSWD);
            break;
        }
        case MENU_DELETE:
        {
            showDialog(DIALOG_DELETE);
            break;
        }
        case MENU_PROTECT:
        {
            setProtectRecords(true);
            break;
        }
        case MENU_UNPROTECT:
        {
            setProtectRecords(false);
            break;
        }
        case MENU_PASSWD_POLICIES: {
            startActivityForResult(new Intent(Intent.ACTION_VIEW,
                                              getUri().getUri(),
                                              this, PasswdPolicyActivity.class),
                                   POLICY_VIEW_REQUEST);
            break;
        }
        case MENU_PASSWD_EXPIRYS: {
            showDialog(DIALOG_PASSWD_EXPIRYS);
            break;
        }
        case MENU_PASSWD_EXPIRY_NOTIF: {
            NotificationMgr notifyMgr = getPasswdSafeApp().getNotifyMgr();
            notifyMgr.togglePasswdExpiryNotif(getPasswdFileData(), this);
            GuiUtils.invalidateOptionsMenu(this);
            break;
        }
        default:
        {
            rc = super.onOptionsItemSelected(item);
            break;
        }
        }
        return rc;
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        PasswdSafeUtil.dbginfo(TAG, "onActivityResult req: %d, rc: %d",
                               requestCode, resultCode);
         if (((requestCode == RECORD_VIEW_REQUEST) ||
              (requestCode == RECORD_ADD_REQUEST) ||
              (requestCode == POLICY_VIEW_REQUEST)) &&
             (resultCode == PasswdSafeApp.RESULT_MODIFIED)) {
             showFileData(MOD_DATA);
         } else {
             super.onActivityResult(requestCode, resultCode, data);
         }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog = null;
        switch (id) {
        case DIALOG_GET_PASSWD:
        {
            LayoutInflater factory = LayoutInflater.from(this);
            View passwdView = factory.inflate(R.layout.passwd_entry, null);
            AbstractDialogClickListener dlgClick =
                new AbstractDialogClickListener()
            {
                @Override
                public void onOkClicked(DialogInterface dialog)
                {
                    Dialog d = (Dialog)dialog;
                    CheckBox cb = (CheckBox)d.findViewById(R.id.read_only);
                    boolean readonly;
                    if (cb.isEnabled()) {
                        readonly = cb.isChecked();
                        SharedPreferences prefs =
                            PreferenceManager.getDefaultSharedPreferences(PasswdSafe.this);
                        Preferences.setFileOpenReadOnlyPref(readonly, prefs);
                    } else {
                        readonly = true;
                    }

                    EditText passwdInput =
                        (EditText)d.findViewById(R.id.passwd_edit);
                    openFile(
                         new StringBuilder(passwdInput.getText().toString()),
                         readonly);
                }

                @Override
                public void onCancelClicked(DialogInterface dialog)
                {
                    cancelFileTask();
                }
            };

            TextView tv = (TextView)passwdView.findViewById(R.id.passwd_edit);
            PasswordVisibilityMenuHandler.set(tv);

            AlertDialog.Builder alert = new AlertDialog.Builder(this)
                .setTitle(R.string.open_file_title)
                .setView(passwdView)
                .setPositiveButton(R.string.ok, dlgClick)
                .setNegativeButton(R.string.cancel, dlgClick)
                .setOnCancelListener(dlgClick);
            AlertDialog alertDialog = alert.create();
            GuiUtils.setupDialogKeyboard(alertDialog, tv, tv, this);
            dialog = alertDialog;
            break;
        }
        case DIALOG_PROGRESS:
        {
            ProgressDialog dlg = new ProgressDialog(this);
            dlg.setTitle(PasswdSafeUtil.getAppTitle(this));
            dlg.setIndeterminate(true);
            dlg.setCancelable(true);

            if (itsLoadTask != null) {
                dlg.setMessage(itsLoadTask.getTitle(this));
            }

            dialog = dlg;
            break;
        }
        case DIALOG_DETAILS:
        {
            LayoutInflater factory = LayoutInflater.from(this);
            View detailsView = factory.inflate(R.layout.file_details, null);
            AlertDialog.Builder alert = new AlertDialog.Builder(this)
                .setTitle(getUriName(true))
                .setView(detailsView)
                .setNeutralButton(R.string.close, new OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });
            dialog = alert.create();
            break;
        }
        case DIALOG_CHANGE_PASSWD:
        {
            LayoutInflater factory = LayoutInflater.from(this);
            View passwdView = factory.inflate(R.layout.change_passwd, null);
            AbstractDialogClickListener dlgClick =
                new AbstractDialogClickListener()
                {
                    @Override
                    public void onOkClicked(DialogInterface dialog)
                    {
                        Dialog d = (Dialog)dialog;
                        EditText passwdEdit = (EditText)
                                        d.findViewById(R.id.password);
                        StringBuilder passwdText =
                            new StringBuilder(passwdEdit.getText().toString());
                        dialog.dismiss();
                        changePasswd(passwdText);
                    }
                };

            TextView tv1 = (TextView)passwdView.findViewById(R.id.password);
            TextView tv2 =
                (TextView)passwdView.findViewById(R.id.password_confirm);
            PasswordVisibilityMenuHandler.set(tv1, tv2);

            AlertDialog.Builder alert = new AlertDialog.Builder(this)
                .setTitle(getUriName(true))
                .setView(passwdView)
                .setPositiveButton(R.string.ok, dlgClick)
                .setNegativeButton(R.string.cancel, dlgClick)
                .setOnCancelListener(dlgClick);
            AlertDialog alertDialog = alert.create();
            itsChangePasswdValidator =
                new DialogValidator.AlertValidator(alertDialog,
                                                   passwdView, this);
            GuiUtils.setupDialogKeyboard(alertDialog, tv1, tv2, this);
            dialog = alertDialog;
            break;
        }
        case DIALOG_FILE_NEW:
        {
            LayoutInflater factory = LayoutInflater.from(this);
            final View fileNewView = factory.inflate(R.layout.file_new, null);
            final TextView fileNameView =
                (TextView)fileNewView.findViewById(R.id.file_name);
            AbstractDialogClickListener dlgClick =
                new AbstractDialogClickListener()
            {
                @Override
                public final void onOkClicked(DialogInterface dialog)
                {
                    TextView passwdView = (TextView)
                        fileNewView.findViewById(R.id.password);
                    StringBuilder passwd =
                        new StringBuilder(passwdView.getText());
                    String fileName = fileNameView.getText().toString();
                    dialog.dismiss();
                    createNewFile(fileName, passwd);
                }

                @Override
                public final void onCancelClicked(DialogInterface dialog)
                {
                    dialog.dismiss();
                    finish();
                }
            };

            TextView tv1 = (TextView)fileNewView.findViewById(R.id.password);
            TextView tv2 =
                (TextView)fileNewView.findViewById(R.id.password_confirm);
            PasswordVisibilityMenuHandler.set(tv1, tv2);

            int titleId = R.string.new_file;
            PasswdFileUri uri = getUri();
            switch (uri.getType()) {
            case FILE: {
                titleId = R.string.new_local_file;
                break;
            }
            case SYNC_PROVIDER: {
                switch (uri.getSyncType()) {
                case GDRIVE: {
                    titleId = R.string.new_drive_file;
                    break;
                }
                case DROPBOX: {
                    titleId = R.string.new_dropbox_file;
                    break;
                }
                case BOX: {
                    titleId = R.string.new_box_file;
                    break;
                }
                }
            }
            case EMAIL:
            case GENERIC_PROVIDER: {
                break;
            }
            }

            AlertDialog.Builder alert = new AlertDialog.Builder(this)
                .setTitle(titleId)
                .setView(fileNewView)
                .setPositiveButton(R.string.ok, dlgClick)
                .setNegativeButton(R.string.cancel, dlgClick)
                .setOnCancelListener(dlgClick);
            AlertDialog alertDialog = alert.create();
            itsFileNewValidator =
                new DialogValidator.AlertValidator(alertDialog,
                                                   fileNewView, this)
            {
                @Override
                protected final String doValidation()
                {
                    CharSequence fileName = fileNameView.getText();
                    if (fileName.length() == 0) {
                        return getString(R.string.empty_file_name);
                    }

                    for (int i = 0; i < fileName.length(); ++i) {
                        char c = fileName.charAt(i);
                        if ((c == '/') || (c == '\\')) {
                            return getString(R.string.invalid_file_name);
                        }
                    }

                    String error = getUri().validateNewChild(fileName.toString(),
                                                        PasswdSafe.this);
                    if (error != null) {
                        return error;
                    }

                    return super.doValidation();
                }
            };
            itsFileNewValidator.registerTextView(fileNameView);
            GuiUtils.setupDialogKeyboard(alertDialog, fileNameView, tv2, this);
            dialog = alertDialog;
            break;
        }
        case DIALOG_DELETE:
        {
            AbstractDialogClickListener dlgClick =
                new AbstractDialogClickListener()
            {
                @Override
                public final void onOkClicked(DialogInterface dialog)
                {
                    deleteFile();
                }
            };

            String prompt = getString(R.string.delete_file_msg,
                                      getUriName(false));
            String title = getString(R.string.delete_file_title);
            DialogUtils.DialogData data =
                DialogUtils.createConfirmPrompt(this, dlgClick, title, prompt);
            dialog = data.itsDialog;
            itsDeleteValidator = data.itsValidator;
            break;
        }
        case DIALOG_PASSWD_EXPIRYS: {
            AlertDialog.Builder alert = new AlertDialog.Builder(this)
                .setTitle(R.string.expired_passwords)
                .setItems(R.array.expire_filters,
                          new DialogInterface.OnClickListener()
                {
                    @SuppressWarnings("deprecation")
                    public void onClick(DialogInterface dialog, int which)
                    {
                        PasswdRecordFilter.ExpiryFilter filter =
                            PasswdRecordFilter.ExpiryFilter.fromIdx(which);
                        switch (filter) {
                        case EXPIRED:
                        case TODAY:
                        case IN_A_WEEK:
                        case IN_TWO_WEEKS:
                        case IN_A_MONTH:
                        case IN_A_YEAR:
                        case ANY: {
                            setRecordExpiryFilter(filter, null);
                            break;
                        }
                        case CUSTOM: {
                            showDialog(DIALOG_PASSWD_EXPIRYS_CUSTOM);
                            break;
                        }
                        }

                    }
                });
            dialog = alert.create();
            break;
        }
        case DIALOG_PASSWD_EXPIRYS_CUSTOM: {
            Calendar now = Calendar.getInstance();
            dialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener()
                {
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth)
                    {
                        Calendar date = Calendar.getInstance();
                        date.set(year, monthOfYear, dayOfMonth, 0, 0, 0);
                        date.set(Calendar.MILLISECOND, 0);
                        date.add(Calendar.DAY_OF_MONTH, 1);
                        setRecordExpiryFilter(
                            PasswdRecordFilter.ExpiryFilter.CUSTOM,
                            date.getTime());
                    }
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH));
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

    @SuppressWarnings("deprecation")
    @Override
    protected void onPrepareDialog(int id, Dialog dialog)
    {
        super.onPrepareDialog(id, dialog);
        switch (id)
        {
        case DIALOG_GET_PASSWD:
        {
            SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(this);
            TextView tv = (TextView)dialog.findViewById(R.id.file);
            tv.setText(getString(R.string.file_label_val, getUriName(false)));
            CheckBox cb = (CheckBox)dialog.findViewById(R.id.read_only);
            if (getUri().isWritable()) {
                cb.setEnabled(true);
                cb.setChecked(Preferences.getFileOpenReadOnlyPref(prefs));
            } else {
                cb.setEnabled(false);
                cb.setChecked(true);
            }
            break;
        }
        case DIALOG_DETAILS:
        {
            TextView tv;
            tv = (TextView)dialog.findViewById(R.id.file);
            tv.setText(getUri().toString());

            PasswdFileData fileData = getPasswdFileData();
            if (fileData == null) {
                break;
            }
            tv = (TextView)dialog.findViewById(R.id.permissions);
            tv.setText(fileData.canEdit() ?
                       R.string.read_write : R.string.read_only);

            tv = (TextView)dialog.findViewById(R.id.num_records);
            tv.setText(Integer.toString(fileData.getRecords().size()));

            tv = (TextView)dialog.findViewById(R.id.password_encoding);
            tv.setText(fileData.getOpenPasswordEncoding());

            if (fileData.isV3()) {
                StringBuilder build = new StringBuilder();
                String str = fileData.getHdrLastSaveUser();
                if (!TextUtils.isEmpty(str)) {
                    build.append(str);
                }
                str = fileData.getHdrLastSaveHost();
                if (!TextUtils.isEmpty(str)) {
                    if (build.length() > 0) {
                        build.append(" on ");
                    }
                    build.append(str);
                }
                tv = (TextView)dialog.findViewById(R.id.last_save_by);
                tv.setText(build);

                tv = (TextView)dialog.findViewById(R.id.database_version);
                tv.setText(fileData.getHdrVersion());
                tv = (TextView)dialog.findViewById(R.id.last_save_app);
                tv.setText(fileData.getHdrLastSaveApp());
                tv = (TextView)dialog.findViewById(R.id.last_save_time);
                tv.setText(fileData.getHdrLastSaveTime());
            } else {
                dialog.findViewById(R.id.database_version_row).
                    setVisibility(View.GONE);
                dialog.findViewById(R.id.last_save_by_row).
                    setVisibility(View.GONE);
                dialog.findViewById(R.id.last_save_app_row).
                    setVisibility(View.GONE);
                dialog.findViewById(R.id.last_save_time_row).
                    setVisibility(View.GONE);
            }
            break;
        }
        case DIALOG_CHANGE_PASSWD:
        {
            itsChangePasswdValidator.reset();
            break;
        }
        case DIALOG_FILE_NEW:
        {
            itsFileNewValidator.reset();
            break;
        }
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

    @Override
    protected void onRecordClick(PwsRecord rec)
    {
        PasswdFileData fileData = getPasswdFileData();
        String uuid = fileData.getUUID(rec);
        openRecord(uuid);
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

        AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
        HashMap<String, Object> listItem = itsListData.get(info.position);
        PwsRecord rec = (PwsRecord)listItem.get(RECORD);
        if (rec != null) {
            menu.setHeaderTitle((String)listItem.get(TITLE));
            menu.add(0, CTXMENU_COPY_USER, 0, R.string.copy_user);
            menu.add(0, CTXMENU_COPY_PASSWD, 0, R.string.copy_password);
        }
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterContextMenuInfo info =
            (AdapterContextMenuInfo)item.getMenuInfo();
        PasswdFileData fileData = getPasswdFileData();
        HashMap<String, Object> listItem = itsListData.get(info.position);
        PwsRecord rec = (PwsRecord)listItem.get(RECORD);

        switch(item.getItemId()) {
        case CTXMENU_COPY_USER:
        {
            if ((rec != null) && (fileData != null)) {
                String str = fileData.getUsername(rec);
                PasswdSafeUtil.copyToClipboard(str, this);
            }
            return true;
        }
        case CTXMENU_COPY_PASSWD:
        {
            if ((rec != null) && (fileData != null)) {
                String str = fileData.getPassword(rec);
                PasswdSafeUtil.copyToClipboard(str, this);
            }
            return true;
        }
        default:
        {
            return super.onContextItemSelected(item);
        }
        }
    }


    @Override
    protected void showFileData(int mod)
    {
        if ((mod & MOD_INIT) == 0) {
            if (itsRecToOpen != null) {
                openRecord(itsRecToOpen);
            }
        }
        itsRecToOpen = null;
        super.showFileData(mod);

        if (itsIsNotifyExpirations && ((mod & MOD_DATA) != 0)) {
            itsIsNotifyExpirations = false;
            PasswdRecordFilter.ExpiryFilter filter =
                itsExpiryNotifPref.getFilter();
            if (filter != null) {
                TextView tv = (TextView)findViewById(R.id.expiry);
                tv.setText(filter.getRecordsExpireStr(itsNumExpired,
                                                      getResources()));
            }
            View group = findViewById(R.id.expiry_panel);
            group.setVisibility(itsNumExpired > 0 ? View.VISIBLE : View.GONE);
        }

        if ((mod & MOD_OPEN_NEW) != 0) {
            NotificationMgr notifyMgr = getPasswdSafeApp().getNotifyMgr();
            notifyMgr.cancelNotification(getUri());
        }
    }


    private final void onCreateView(final Intent intent)
    {
        runTask(new AbstractTask()
        {
            @Override
            public String getTitle(Context ctx)
            {
                return "";
            }

            @Override
            protected Object handleDoInBackground(PasswdFileUri uri,
                                                  Context ctx)
                                                                                 throws Exception
            {
                return PasswdSafeApp.getFileUriFromIntent(intent, ctx);
            }

            @SuppressWarnings("deprecation")
            @Override
            protected void handleOnPostExecute(Object result)
            {
                PasswdFileUri uri = (PasswdFileUri)(result);
                if (!uri.exists()) {
                    PasswdSafeUtil.showFatalMsg("File does't exist: " + uri,
                                                PasswdSafe.this);
                    return;
                }
                openFile(uri);
                String title = PasswdSafeApp.getAppFileTitle(getUri(),
                                                             PasswdSafe.this);
                if (PasswdSafeApp.DEBUG_AUTO_FILE != null) {
                    title += " - AUTOOPEN!!!!!";
                }
                setTitle(title);

                itsRecToOpen = intent.getData().getQueryParameter("recToOpen");

                if (!getPasswdFile().isOpen()) {
                    if ((PasswdSafeApp.DEBUG_AUTO_FILE != null) &&
                        (getUri().getUri().getPath().equals(
                             PasswdSafeApp.DEBUG_AUTO_FILE))) {
                        openFile(new StringBuilder("test123"), false);
                    } else {
                        showDialog(DIALOG_GET_PASSWD);
                    }
                } else {
                    showFileData(MOD_DATA | MOD_OPEN_NEW);
                }
            }
        });
    }

    @SuppressWarnings("deprecation")
    private final void onCreateNew(Intent intent)
    {
        initUri(PasswdSafeApp.getFileUriFromIntent(intent, this));
        showDialog(DIALOG_FILE_NEW);
    }

    @SuppressWarnings("deprecation")
    private final void openFile(StringBuilder passwd, boolean readonly)
    {
        removeDialog(DIALOG_GET_PASSWD);
        runTask(new OpenTask(passwd, readonly));
    }

    private final void createNewFile(String fileName, StringBuilder passwd)
    {
        runTask(new NewTask(fileName, passwd));
    }

    private final void deleteFile()
    {
        runTask(new DeleteTask());
    }

    /** Run a background task */
    @SuppressWarnings("deprecation")
    private final void runTask(AbstractTask task)
    {
        itsLoadTask = task;
        itsLoadTask.execute();
        showDialog(DIALOG_PROGRESS);
    }

    private final void changePasswd(StringBuilder passwd)
    {
        PasswdFileData fileData = getPasswdFileData();
        if (fileData != null) {
            fileData.changePasswd(passwd);
            getPasswdFile().save();
        } else {
            PasswdSafeUtil.showFatalMsg(
                "Could not change password on closed file: " + getUri(), this);
        }
    }

    /**
     * Protect or unprotect entries
     */
    private final void setProtectRecords(boolean prot)
    {
        PasswdFileData fileData = getPasswdFileData();
        if (fileData == null) {
            return;
        }

        setProtectRecords(prot, fileData, getCurrGroupNode());
        getPasswdFile().save();
    }

    /** Protect or unprotect entries in the given group */
    private final void setProtectRecords(boolean prot,
                                         PasswdFileData fileData,
                                         GroupNode node)
    {
	Map<String, GroupNode> childGroups = node.getGroups();
	if (childGroups != null) {
	    for (GroupNode child : childGroups.values()) {
	        setProtectRecords(prot, fileData, child);
	    }
	}

	List<MatchPwsRecord> childRecords = node.getRecords();
	if (childRecords != null) {
	    for (MatchPwsRecord matchRec : childRecords) {
	        fileData.setProtected(prot, matchRec.itsRecord);
	    }
	}
    }

    @SuppressWarnings("deprecation")
    private final void cancelFileTask()
    {
        removeDialog(DIALOG_PROGRESS);
        removeDialog(DIALOG_GET_PASSWD);
        finish();
    }


    private void openRecord(String uuid)
    {
        RecordView.startActivityForResult(getUri(), uuid, RECORD_VIEW_REQUEST,
                                          this);
    }

    /** Background task for opening a file */
    private final class OpenTask extends AbstractTask
    {
        private final StringBuilder itsPasswd;
        private final boolean itsIsReadOnly;

        /** Constructor */
        public OpenTask(StringBuilder passwd, boolean readonly)
        {
            itsPasswd = passwd;
            itsIsReadOnly = readonly;
        }

        /* (non-Javadoc)
         * @see com.jefftharris.passwdsafe.PasswdSafe.AbstractTask#getTitle(android.content.Context)
         */
        @Override
        public String getTitle(Context ctx)
        {
            return ctx.getString(R.string.loading_file, getUriName(true));
        }

        /* (non-Javadoc)
         * @see com.jefftharris.passwdsafe.PasswdSafe.AbstractTask#handleDoInBackground(com.jefftharris.passwdsafe.file.PasswdFileUri, android.content.Context)
         */
        @Override
        protected Object handleDoInBackground(PasswdFileUri uri, Context ctx)
            throws Exception
        {
            PasswdFileData fileData = new PasswdFileData(uri);
            fileData.load(itsPasswd, itsIsReadOnly, ctx);
            return fileData;
        }

        /* (non-Javadoc)
         * @see com.jefftharris.passwdsafe.PasswdSafe.AbstractTask#handleOnPostExecute(java.lang.Object)
         */
        @Override
        protected void handleOnPostExecute(Object result)
        {
            PasswdFileData fileData = (PasswdFileData)result;
            getPasswdFile().setFileData(fileData);
            showFileData(MOD_DATA | MOD_OPEN_NEW);
        }
    }

    /** Background task for creating a new file */
    private final class NewTask extends AbstractTask
    {
        private final String itsFileName;
        private final StringBuilder itsPasswd;

        /** Constructor */
        public NewTask(String fileName, StringBuilder passwd)
        {
            itsFileName = fileName;
            itsPasswd = passwd;
        }

        /* (non-Javadoc)
         * @see com.jefftharris.passwdsafe.PasswdSafe.AbstractTask#getTitle(android.content.Context)
         */
        @Override
        public String getTitle(Context ctx)
        {
            return ctx.getString(R.string.new_file);
        }

        /* (non-Javadoc)
         * @see com.jefftharris.passwdsafe.PasswdSafe.AbstractTask#handleDoInBackground(com.jefftharris.passwdsafe.file.PasswdFileUri, android.content.Context)
         */
        @Override
        protected Object handleDoInBackground(PasswdFileUri uri, Context ctx)
                throws Exception
        {
            PasswdFileUri childUri =
                    uri.createNewChild(itsFileName + ".psafe3", ctx);
            PasswdFileData fileData = new PasswdFileData(childUri);
            fileData.createNewFile(itsPasswd, ctx);
            return fileData;
        }

        /* (non-Javadoc)
         * @see com.jefftharris.passwdsafe.PasswdSafe.AbstractTask#handleOnPostExecute(java.lang.Object)
         */
        @Override
        protected void handleOnPostExecute(Object result)
        {
            PasswdFileData fileData = (PasswdFileData)result;
            openFile(fileData.getUri());
            getPasswdFile().setFileData(fileData);
            setTitle(PasswdSafeApp.getAppFileTitle(getUri(), PasswdSafe.this));
            showFileData(MOD_DATA);
        }

        /* (non-Javadoc)
         * @see com.jefftharris.passwdsafe.PasswdSafe.AbstractTask#handleGetExceptionMsg(java.lang.Exception)
         */
        @Override
        protected String handleGetExceptionMsg(Exception e, Context ctx)
        {
            return ctx.getString(R.string.cannot_create_file, getUri());
        }
    }

    /** Background task for deleting the file */
    private final class DeleteTask extends AbstractTask
    {
        /** Constructor */
        public DeleteTask()
        {
        }

        /* (non-Javadoc)
         * @see com.jefftharris.passwdsafe.PasswdSafe.AbstractTask#getTitle(android.content.Context)
         */
        @Override
        public String getTitle(Context ctx)
        {
            return ctx.getString(R.string.delete_file);
        }

        /* (non-Javadoc)
         * @see com.jefftharris.passwdsafe.PasswdSafe.AbstractTask#handleDoInBackground(com.jefftharris.passwdsafe.file.PasswdFileUri, android.content.Context)
         */
        @Override
        protected Object handleDoInBackground(PasswdFileUri uri, Context ctx)
                throws Exception
        {
            uri.delete(ctx);
            return null;
        }

        /* (non-Javadoc)
         * @see com.jefftharris.passwdsafe.PasswdSafe.AbstractTask#handleOnPostExecute(java.lang.Object)
         */
        @Override
        protected void handleOnPostExecute(Object result)
        {
            PasswdSafe.this.finish();
        }
    }

    /** Background task for some file operations */
    private abstract class AbstractTask extends AsyncTask<Void, Void, Object>
    {
        /** Get a title for the task for the progress dialog */
        public abstract String getTitle(Context ctx);

        /** Implement the doInBackground operation */
        protected abstract Object handleDoInBackground(PasswdFileUri uri,
                                                       Context ctx)
            throws Exception;

        /** Implement the onPostExecute operation */
        protected abstract void handleOnPostExecute(Object result);

        /** Get a message for an exception during the task */
        protected String handleGetExceptionMsg(Exception e, Context ctx)
        {
            return null;
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected final Object doInBackground(Void... params)
        {
            PasswdFileUri uri = getUri();
            Context ctx = PasswdSafe.this;
            try {
                return handleDoInBackground(uri, ctx);
            } catch (Exception e) {
                return e;
            }
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onCancelled()
         */
        @Override
        protected void onCancelled()
        {
            PasswdSafeUtil.dbginfo(TAG, "LoadTask cancelled");
            itsLoadTask = null;
            cancelFileTask();
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @SuppressWarnings("deprecation")
        @Override
        protected void onPostExecute(Object result)
        {
            if (isCancelled()) {
                onCancelled();
                return;
            }
            PasswdSafeUtil.dbginfo(TAG, "LoadTask post execute");
            removeDialog(DIALOG_PROGRESS);
            itsLoadTask = null;
            if (!(result instanceof Exception)) {
                handleOnPostExecute(result);
            } else {
                Exception e = (Exception)result;
                if (((e instanceof IOException) &&
                     TextUtils.equals(e.getMessage(), "Invalid password")) ||
                    (e instanceof InvalidPassphraseException)) {
                    PasswdSafeUtil.showFatalMsg(
                        getString(R.string.invalid_password), PasswdSafe.this,
                        false);
                } else {
                    String msg = handleGetExceptionMsg(e, PasswdSafe.this);
                    if (msg == null) {
                        msg = e.toString();
                    }
                    PasswdSafeUtil.showFatalMsg(e, msg, PasswdSafe.this);
                }
            }
        }
    }
}
