/*
 * Copyright (©) 2009-2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.pwsafe.lib.exception.InvalidPassphraseException;
import org.pwsafe.lib.file.PwsRecord;

import com.jefftharris.passwdsafe.view.DialogUtils;
import com.jefftharris.passwdsafe.view.PasswordVisibilityMenuHandler;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
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
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;

public class PasswdSafe extends AbstractPasswdSafeActivity
{
    private static final int DIALOG_GET_PASSWD = 0;
    private static final int DIALOG_PROGRESS = 1;
    private static final int DIALOG_DETAILS = 2;
    private static final int DIALOG_CHANGE_PASSWD = 3;
    private static final int DIALOG_SAVE_PROGRESS = 4;
    private static final int DIALOG_FILE_NEW = 5;
    private static final int DIALOG_DELETE = 6;

    private static final int MENU_ADD_RECORD =      ABS_MENU_MAX + 1;
    private static final int MENU_DETAILS =         ABS_MENU_MAX + 2;
    private static final int MENU_CHANGE_PASSWD =   ABS_MENU_MAX + 3;
    private static final int MENU_DELETE =          ABS_MENU_MAX + 4;

    private static final int CTXMENU_COPY_USER = 1;
    private static final int CTXMENU_COPY_PASSWD = 2;

    private static final int RECORD_VIEW_REQUEST = 0;
    private static final int RECORD_ADD_REQUEST = 1;

    private LoadTask itsLoadTask;
    private DialogValidator itsChangePasswdValidator;
    private DialogValidator itsFileNewValidator;
    private DialogValidator itsDeleteValidator;
    private String itsRecToOpen;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        registerForContextMenu(getListView());

        Intent intent = getIntent();
        PasswdSafeApp.dbginfo(TAG, "onCreate intent:" + intent);

        String action = intent.getAction();
        if (action.equals(PasswdSafeApp.VIEW_INTENT) ||
            action.equals(Intent.ACTION_VIEW)) {
            onCreateView(intent);
        } else if (action.equals(PasswdSafeApp.NEW_INTENT)) {
            onCreateNew(intent);
        } else {
            Log.e(TAG, "Unknown action for intent: " + intent);
            finish();
        }
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#showProgressDialog()
     */
    @Override
    public void showProgressDialog()
    {
        showDialog(DIALOG_SAVE_PROGRESS);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#removeProgressDialog()
     */
    @Override
    public void removeProgressDialog()
    {
        removeDialog(DIALOG_SAVE_PROGRESS);
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        if (intent != null) {
            String action = intent.getAction();
            if (action.equals(PasswdSafeApp.VIEW_INTENT) ||
                action.equals(Intent.ACTION_VIEW)) {
                onCreateView(intent);
                return;
            }
        }

        super.onNewIntent(intent);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause()
    {
        removeDialog(DIALOG_GET_PASSWD);
        if (itsLoadTask != null) {
            itsLoadTask.cancel(true);
        }

        super.onPause();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        removeDialog(DIALOG_GET_PASSWD);
        removeDialog(DIALOG_PROGRESS);
        removeDialog(DIALOG_SAVE_PROGRESS);
        super.onSaveInstanceState(outState);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuItem mi;

        addSearchMenuItem(menu);

        mi = menu.add(0, MENU_DETAILS, 0, R.string.details);
        mi.setIcon(android.R.drawable.ic_menu_info_details);

        mi = menu.add(0, MENU_ADD_RECORD, 0, R.string.add_record);
        mi.setIcon(android.R.drawable.ic_menu_add);

        addCloseMenuItem(menu);

        mi = menu.add(0, MENU_CHANGE_PASSWD, 0, R.string.change_password);
        mi.setIcon(android.R.drawable.ic_menu_edit);

        mi = menu.add(0, MENU_DELETE, 0, R.string.delete_file);
        mi.setIcon(android.R.drawable.ic_menu_delete);

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
        if (itsPasswdFile != null) {
            PasswdFileData fileData = itsPasswdFile.getFileData();
            if (fileData != null) {
                editEnabled = fileData.canEdit();
                deleteEnabled = fileData.canDelete();
            }
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

        return super.onPrepareOptionsMenu(menu);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        boolean rc = true;
        switch (item.getItemId()) {
        case MENU_ADD_RECORD:
        {
            startActivityForResult(
                new Intent(Intent.ACTION_INSERT, itsUri,
                           this, RecordEditActivity.class),
                RECORD_ADD_REQUEST);
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
        PasswdSafeApp.dbginfo(TAG,
                              "onActivityResult req: " + requestCode +
                              ", rc: " + resultCode);
         if (((requestCode == RECORD_VIEW_REQUEST) ||
              (requestCode == RECORD_ADD_REQUEST)) &&
             (resultCode == PasswdSafeApp.RESULT_MODIFIED)) {
             showFileData();
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
                    cancelFileOpen();
                }
            };

            TextView tv = (TextView)passwdView.findViewById(R.id.passwd_edit);
            PasswordVisibilityMenuHandler.set(tv);

            AlertDialog.Builder alert = new AlertDialog.Builder(this)
                .setTitle(R.string.open_file_title)
                .setView(passwdView)
                .setPositiveButton("Ok", dlgClick)
                .setNegativeButton("Cancel", dlgClick)
                .setOnCancelListener(dlgClick);
            AlertDialog alertDialog = alert.create();
            GuiUtils.setupDialogKeyboard(alertDialog, tv, tv, this);
            dialog = alertDialog;
            break;
        }
        case DIALOG_PROGRESS:
        {
            ProgressDialog dlg = new ProgressDialog(this);
            dlg.setTitle(PasswdSafeApp.getAppTitle(this));
            dlg.setMessage("Loading " + getUriName() + "...");
            dlg.setIndeterminate(true);
            dlg.setCancelable(true);
            dialog = dlg;
            break;
        }
        case DIALOG_DETAILS:
        {
            LayoutInflater factory = LayoutInflater.from(this);
            View detailsView = factory.inflate(R.layout.file_details, null);
            AlertDialog.Builder alert = new AlertDialog.Builder(this)
                .setTitle(getUriName())
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
                .setTitle(getUriName())
                .setView(passwdView)
                .setPositiveButton(R.string.ok, dlgClick)
                .setNegativeButton(R.string.cancel, dlgClick)
                .setOnCancelListener(dlgClick);
            final AlertDialog alertDialog = alert.create();
            itsChangePasswdValidator = new DialogValidator(passwdView, this)
            {
                @Override
                protected final View getDoneButton()
                {
                    return alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                }

                @Override
                protected final String doValidation()
                {
                    if (getPassword().getText().length() == 0) {
                        return getString(R.string.empty_password);
                    }
                    return super.doValidation();
                }
            };
            GuiUtils.setupDialogKeyboard(alertDialog, tv1, tv2, this);
            dialog = alertDialog;
            break;
        }
        case DIALOG_SAVE_PROGRESS:
        {
            dialog = itsPasswdFile.createProgressDialog();
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

            AlertDialog.Builder alert = new AlertDialog.Builder(this)
                .setTitle(R.string.new_file)
                .setView(fileNewView)
                .setPositiveButton(R.string.ok, dlgClick)
                .setNegativeButton(R.string.cancel, dlgClick)
                .setOnCancelListener(dlgClick);
            final AlertDialog alertDialog = alert.create();
            itsFileNewValidator = new DialogValidator(fileNewView, this)
            {
                @Override
                protected final View getDoneButton()
                {
                    return alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                }

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

                    File dir = getUriAsFile();
                    if (dir == null) {
                        return getString(R.string.new_file_not_supp_uri,
                                         itsUri);
                    }
                    File f = new File(dir, fileName + ".psafe3");
                    if (f.exists()) {
                        return getString(R.string.file_exists);
                    }

                    if (getPassword().getText().length() == 0) {
                        return getString(R.string.empty_password);
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

            String prompt =
                getString(R.string.delete_file_msg,
                          PasswdFileData.getUriIdentifier(itsUri, this, false));
            String title = getString(R.string.delete_file_title);
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
            tv.setText(getString(R.string.file_label_val,
                                 PasswdFileData.getUriIdentifier(itsUri, this,
                                                                 false)));
            CheckBox cb = (CheckBox)dialog.findViewById(R.id.read_only);
            if (PasswdFileData.isFileUri(itsUri)) {
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
            tv.setText(itsUri.toString());

            PasswdFileData fileData = itsPasswdFile.getFileData();
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
        PasswdFileData fileData = itsPasswdFile.getFileData();
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
        PasswdFileData fileData = itsPasswdFile.getFileData();
        HashMap<String, Object> listItem = itsListData.get(info.position);
        PwsRecord rec = (PwsRecord)listItem.get(RECORD);

        switch(item.getItemId()) {
        case CTXMENU_COPY_USER:
        {
            if ((rec != null) && (fileData != null)) {
                String str = fileData.getUsername(rec);
                PasswdSafeApp.copyToClipboard(str, this);
            }
            return true;
        }
        case CTXMENU_COPY_PASSWD:
        {
            if ((rec != null) && (fileData != null)) {
                String str = fileData.getPassword(rec);
                PasswdSafeApp.copyToClipboard(str, this);
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
    protected void showFileData()
    {
        if (itsRecToOpen != null) {
            openRecord(itsRecToOpen);
        }
        super.showFileData();
    }


    private final void onCreateView(Intent intent)
    {
        itsUri = PasswdSafeApp.getFileUriFromIntent(intent);
        PasswdSafeApp app = (PasswdSafeApp)getApplication();
        itsPasswdFile = app.accessPasswdFile(itsUri, this);
        String title = PasswdSafeApp.getAppFileTitle(itsUri, this);
        if (PasswdSafeApp.DEBUG_AUTO_FILE != null) {
            title += " - AUTOOPEN!!!!!";
        }
        setTitle(title);

        itsRecToOpen = intent.getData().getQueryParameter("recToOpen");

        if (!itsPasswdFile.isOpen()) {
            if ((PasswdSafeApp.DEBUG_AUTO_FILE != null) &&
                (itsUri.getPath().equals(PasswdSafeApp.DEBUG_AUTO_FILE))) {
                openFile(new StringBuilder("test123"), false);
            } else {
                showDialog(DIALOG_GET_PASSWD);
            }
        } else {
            showFileData();
        }
    }

    private final void onCreateNew(Intent intent)
    {
        itsUri = PasswdSafeApp.getFileUriFromIntent(intent);
        showDialog(DIALOG_FILE_NEW);
    }

    private final void openFile(StringBuilder passwd, boolean readonly)
    {
        removeDialog(DIALOG_GET_PASSWD);
        showDialog(DIALOG_PROGRESS);
        itsLoadTask = new LoadTask(passwd, readonly);
        itsLoadTask.execute();
    }

    private final void createNewFile(String fileName, StringBuilder passwd)
    {
        try
        {
            File dir = getUriAsFile();
            if (dir == null) {
                throw new Exception("File creation not supported for URI " +
                                    itsUri);
            }
            File file = new File(dir, fileName + ".psafe3");
            itsUri = Uri.fromFile(file);

            PasswdFileData fileData = new PasswdFileData(itsUri);
            fileData.createNewFile(passwd, this);

            PasswdSafeApp app = (PasswdSafeApp)getApplication();
            itsPasswdFile = app.accessPasswdFile(itsUri, this);
            itsPasswdFile.setFileData(fileData);
            setTitle(PasswdSafeApp.getAppFileTitle(itsUri, this));
            showFileData();
        } catch (Exception e) {
            PasswdSafeApp.showFatalMsg(e, "Can't create file: " + itsUri,
                                       this);
            finish();
        }
    }

    private final void deleteFile()
    {
        File file = getUriAsFile();
        if (file != null) {
            if (file.delete()) {
                finish();
            } else {
                PasswdSafeApp.showFatalMsg("Could not delete file: " + itsUri,
                                           this);
            }
        } else {
            PasswdSafeApp.showFatalMsg("Delete not supported for " + itsUri,
                                       this);
        }
    }

    private final void changePasswd(StringBuilder passwd)
    {
        PasswdFileData fileData = itsPasswdFile.getFileData();
        if (fileData != null) {
            fileData.changePasswd(passwd);
            itsPasswdFile.save();
        } else {
            PasswdSafeApp.showFatalMsg(
                "Could not change password on closed file: " + itsUri, this);
        }
    }

    private final void cancelFileOpen()
    {
        removeDialog(DIALOG_PROGRESS);
        removeDialog(DIALOG_GET_PASSWD);
        finish();
    }


    private void openRecord(String uuid)
    {
        RecordView.startActivityForResult(itsUri, uuid, RECORD_VIEW_REQUEST,
                                          this);
    }


    private final class LoadTask extends AsyncTask<Void, Void, Object>
    {
        private final StringBuilder itsPasswd;
        private final boolean itsIsReadOnly;

        private LoadTask(StringBuilder passwd, boolean readonly)
        {
            itsPasswd = passwd;
            itsIsReadOnly = readonly;
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Object doInBackground(Void... params)
        {
            try {
                PasswdFileData fileData = new PasswdFileData(itsUri);
                fileData.load(itsPasswd, itsIsReadOnly, PasswdSafe.this);
                return fileData;
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
            PasswdSafeApp.dbginfo(TAG, "LoadTask cancelled");
            itsLoadTask = null;
            cancelFileOpen();
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Object result)
        {
            if (isCancelled()) {
                onCancelled();
                return;
            }
            PasswdSafeApp.dbginfo(TAG, "LoadTask post execute");
            dismissDialog(DIALOG_PROGRESS);
            itsLoadTask = null;
            if (result instanceof PasswdFileData) {
                itsPasswdFile.setFileData((PasswdFileData)result);
                showFileData();
            } else if (result instanceof Exception) {
                Exception e = (Exception)result;
                if (((e instanceof IOException) &&
                     e.getMessage().equals("Invalid password")) ||
                    (e instanceof InvalidPassphraseException))
                    PasswdSafeApp.showFatalMsg("Invalid password",
                                               PasswdSafe.this);
                else
                    PasswdSafeApp.showFatalMsg(e, PasswdSafe.this);
            }
        }
    }

}