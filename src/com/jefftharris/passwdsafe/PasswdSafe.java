/*
 * Copyright (©) 2009-2010 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.pwsafe.lib.exception.InvalidPassphraseException;
import org.pwsafe.lib.file.PwsRecord;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class PasswdSafe extends ListActivity
    implements PasswdFileActivity
{
    private static final String TAG = "PasswdSafe";

    private static final int DIALOG_GET_PASSWD = 0;
    private static final int DIALOG_PROGRESS = 1;
    private static final int DIALOG_DETAILS = 2;
    private static final int DIALOG_CHANGE_PASSWD = 3;
    private static final int DIALOG_SAVE_PROGRESS = 4;
    private static final int DIALOG_FILE_NEW = 5;
    private static final int DIALOG_DELETE = 6;

    private static final int MENU_ADD_RECORD = 1;
    private static final int MENU_DETAILS = 2;
    private static final int MENU_CHANGE_PASSWD = 3;
    private static final int MENU_DELETE = 4;
    private static final int MENU_SEARCH = 5;

    private static final String RECORD = "record";
    private static final String TITLE = "title";
    private static final String MATCH = "match";
    private static final String USERNAME = "username";
    private static final String ICON = "icon";

    private static final int RECORD_VIEW_REQUEST = 0;
    private static final int RECORD_ADD_REQUEST = 1;

    private static final String BUNDLE_SEARCH_QUERY =
        "passwdsafe.searchQuery";
    private static final String BUNDLE_CURR_GROUPS =
        "passwdsafe.currGroups";

    private File itsFile;
    private ActivityPasswdFile itsPasswdFile;
    private LoadTask itsLoadTask;
    private boolean itsGroupRecords = true;
    private boolean itsIsSortCaseSensitive = true;
    private boolean itsIsSearchCaseSensitive = false;
    private boolean itsIsSearchRegex = false;
    private DialogValidator itsChangePasswdValidator;
    private DialogValidator itsFileNewValidator;

    private final ArrayList<HashMap<String, Object>> itsListData =
        new ArrayList<HashMap<String, Object>>();

    private Pattern itsSearchQuery = null;
    private static final String QUERY_MATCH = "";
    private String QUERY_MATCH_TITLE;
    private String QUERY_MATCH_USERNAME;
    private String QUERY_MATCH_URL;
    private String QUERY_MATCH_EMAIL;
    private String QUERY_MATCH_NOTES;

    private ArrayList<String> itsCurrGroups = new ArrayList<String>();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passwd_safe);
        Button button = (Button)findViewById(R.id.query_clear_btn);
        button.setOnClickListener(new View.OnClickListener()
        {
            public final void onClick(View v)
            {
                setSearchQuery(null);
            }
        });

        button = (Button)findViewById(R.id.sub_group_up_btn);
        button.setOnClickListener(new View.OnClickListener()
        {
            public final void onClick(View v)
            {
                doBackPressed();
            }
        });

        String query = null;
        if (savedInstanceState != null) {
            query = savedInstanceState.getString(BUNDLE_SEARCH_QUERY);
            ArrayList<String> currGroups =
                savedInstanceState.getStringArrayList(BUNDLE_CURR_GROUPS);
            if (currGroups != null) {
                itsCurrGroups = new ArrayList<String>(currGroups);
            }
        }
        setSearchQuery(query);

        Intent intent = getIntent();
        PasswdSafeApp.dbginfo(TAG, "onCreate intent:" + intent);

        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        itsGroupRecords = PasswdSafeApp.getGroupRecordsPref(prefs);
        itsIsSortCaseSensitive = PasswdSafeApp.getSortCaseSensitivePref(prefs);
        itsIsSearchCaseSensitive =
            PasswdSafeApp.getSearchCaseSensitivePref(prefs);
        itsIsSearchRegex = PasswdSafeApp.getSearchRegexPref(prefs);

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

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        String query = null;
        if ((intent != null) &&
            intent.getAction().equals(Intent.ACTION_SEARCH)) {
            query = intent.getStringExtra(SearchManager.QUERY);
        }
        setSearchQuery(query);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#getActivity()
     */
    public Activity getActivity()
    {
        return this;
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#saveFinished(boolean)
     */
    public void saveFinished(boolean success)
    {
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#showProgressDialog()
     */
    public void showProgressDialog()
    {
        showDialog(DIALOG_SAVE_PROGRESS);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#removeProgressDialog()
     */
    public void removeProgressDialog()
    {
        removeDialog(DIALOG_SAVE_PROGRESS);
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
            itsPasswdFile.onActivityDestroy();
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause()
    {
        PasswdSafeApp.dbginfo(TAG, "onPause");
        super.onPause();

        removeDialog(DIALOG_GET_PASSWD);
        if (itsLoadTask != null) {
            itsLoadTask.cancel(true);
        }

        if (itsPasswdFile != null) {
            itsPasswdFile.onActivityPause();
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
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        removeDialog(DIALOG_GET_PASSWD);
        removeDialog(DIALOG_PROGRESS);
        removeDialog(DIALOG_SAVE_PROGRESS);
        super.onSaveInstanceState(outState);
        String query = null;
        if (itsSearchQuery != null) {
            query = itsSearchQuery.pattern();
        }
        outState.putString(BUNDLE_SEARCH_QUERY, query);
        outState.putStringArrayList(BUNDLE_CURR_GROUPS, itsCurrGroups);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuItem mi = menu.add(0, MENU_ADD_RECORD, 0, R.string.add_record);
        mi.setIcon(android.R.drawable.ic_menu_add);

        mi = menu.add(0, MENU_DETAILS, 0, R.string.details);
        mi.setIcon(android.R.drawable.ic_menu_info_details);

        mi = menu.add(0, MENU_CHANGE_PASSWD, 0, R.string.change_password);
        mi.setIcon(android.R.drawable.ic_menu_edit);

        mi = menu.add(0, MENU_DELETE, 0, R.string.delete_file);
        mi.setIcon(android.R.drawable.ic_menu_delete);

        mi = menu.add(0, MENU_SEARCH, 0, R.string.search);
        mi.setIcon(android.R.drawable.ic_menu_search);
        return true;
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        boolean addEnabled = false;
        boolean deleteEnabled = false;
        if (itsPasswdFile != null) {
            PasswdFileData fileData = itsPasswdFile.getFileData();
            if (fileData != null) {
                addEnabled = fileData.canEdit();
                deleteEnabled = fileData.getFile().canWrite();
            }
        }

        MenuItem mi = menu.findItem(MENU_ADD_RECORD);
        if (mi != null) {
            mi.setEnabled(addEnabled);
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
                new Intent(Intent.ACTION_INSERT, Uri.fromFile(itsFile),
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
        case MENU_SEARCH:
        {
            onSearchRequested();
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
            final View passwdView =
                factory.inflate(R.layout.passwd_entry, null);
            AbstractDialogClickListener dlgClick =
                new AbstractDialogClickListener()
            {
                @Override
                public void onOkClicked(DialogInterface dialog)
                {
                    EditText passwdInput = (EditText) passwdView
                        .findViewById(R.id.passwd_edit);
                    openFile(
                         new StringBuilder(passwdInput.getText().toString()));
                }

                @Override
                public void onCancelClicked(DialogInterface dialog)
                {
                    cancelFileOpen();
                }
            };

            AlertDialog.Builder alert = new AlertDialog.Builder(this)
                .setTitle("Open " + itsFile.getPath())
                .setMessage("Enter password:")
                .setView(passwdView)
                .setPositiveButton("Ok", dlgClick)
                .setNegativeButton("Cancel", dlgClick)
                .setOnCancelListener(dlgClick);
            dialog = alert.create();
            break;
        }
        case DIALOG_PROGRESS:
        {
            ProgressDialog dlg = new ProgressDialog(this);
            dlg.setTitle(PasswdSafeApp.getAppTitle(this));
            dlg.setMessage("Loading " + itsFile.getName() + "...");
            dlg.setIndeterminate(true);
            dlg.setCancelable(true);
            dialog = dlg;
            break;
        }
        case DIALOG_DETAILS:
        {
            LayoutInflater factory = LayoutInflater.from(this);
            final View detailsView =
                factory.inflate(R.layout.file_details, null);
            AlertDialog.Builder alert = new AlertDialog.Builder(this)
                .setTitle(itsFile.getName())
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
            final View passwdView =
                factory.inflate(R.layout.change_passwd, null);
            AbstractDialogClickListener dlgClick =
                new AbstractDialogClickListener()
                {
                    @Override
                    public void onOkClicked(DialogInterface dialog)
                    {
                        EditText passwdEdit = (EditText)
                            passwdView.findViewById(R.id.password);
                        StringBuilder passwdText =
                            new StringBuilder(passwdEdit.getText().toString());
                        dialog.dismiss();
                        changePasswd(passwdText);
                    }
                };
            AlertDialog.Builder alert = new AlertDialog.Builder(this)
                .setTitle(itsFile.getName())
                .setView(passwdView)
                .setPositiveButton(R.string.ok, dlgClick)
                .setNegativeButton(R.string.cancel, dlgClick)
                .setOnCancelListener(dlgClick);
            final AlertDialog alertDialog = alert.create();
            itsChangePasswdValidator = new DialogValidator(passwdView)
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
                        return "Empty password";
                    }
                    return super.doValidation();
                }
            };
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
            AlertDialog.Builder alert = new AlertDialog.Builder(this)
                .setTitle(R.string.new_file)
                .setView(fileNewView)
                .setPositiveButton(R.string.ok, dlgClick)
                .setNegativeButton(R.string.cancel, dlgClick)
                .setOnCancelListener(dlgClick);
            final AlertDialog alertDialog = alert.create();
            itsFileNewValidator = new DialogValidator(fileNewView)
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
                        return "Empty file name";
                    }

                    for (int i = 0; i < fileName.length(); ++i) {
                        char c = fileName.charAt(i);
                        if ((c == '/') || (c == '\\')) {
                            return "Invalid file name";
                        }
                    }

                    File f = new File(itsFile, fileName + ".psafe3");
                    if (f.exists()) {
                        return "File exists";
                    }

                    if (getPassword().getText().length() == 0) {
                        return "Empty password";
                    }
                    return super.doValidation();
                }
            };
            itsFileNewValidator.registerTextView(fileNameView);
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
            AlertDialog.Builder alert = new AlertDialog.Builder(this)
                .setTitle(R.string.delete_file_title)
                .setMessage(getString(R.string.delete_file_msg,
                                      itsFile.toString()))
                .setPositiveButton(R.string.ok, dlgClick)
                .setNegativeButton(R.string.cancel, dlgClick)
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

    @Override
    protected void onPrepareDialog(int id, Dialog dialog)
    {
        super.onPrepareDialog(id, dialog);
        switch (id)
        {
        case DIALOG_DETAILS:
        {
            TextView tv;
            tv = (TextView)dialog.findViewById(R.id.file);
            tv.setText(itsFile.getPath());

            PasswdFileData fileData = itsPasswdFile.getFileData();
            tv = (TextView)dialog.findViewById(R.id.permissions);
            tv.setText(fileData.canEdit() ?
                       R.string.read_write : R.string.read_only);

            tv = (TextView)dialog.findViewById(R.id.num_groups);
            // TODO: FIX
            //tv.setText(Integer.toString(itsGroupData.size()));

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
        default:
        {
            break;
        }
        }
    }

    /* (non-Javadoc)
     * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        PasswdFileData fileData = itsPasswdFile.getFileData();

        HashMap<String, Object> item = itsListData.get(position);
        PwsRecord rec = (PwsRecord)item.get(RECORD);
        if (rec != null) {
            Uri.Builder builder = Uri.fromFile(itsFile).buildUpon();
            String uuid = fileData.getUUID(rec);
            if (uuid != null) {
                builder.appendQueryParameter("rec", uuid.toString());
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, builder.build(),
                                       this, RecordView.class);
            startActivityForResult(intent, RECORD_VIEW_REQUEST);
        } else {
            String childTitle = (String)item.get(TITLE);
            itsCurrGroups.add(childTitle);
            showFileData();
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if ((android.os.Build.VERSION.SDK_INT <
                        android.os.Build.VERSION_CODES.ECLAIR)
            && keyCode == KeyEvent.KEYCODE_BACK
            && event.getRepeatCount() == 0) {
            // Take care of calling this method on earlier versions of
            // the platform where it doesn't exist.
            if (doBackPressed()) {
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed()
    {
        if (!doBackPressed()) {
            finish();
        }
    }


    private final void onCreateView(Intent intent)
    {
        itsFile = new File(intent.getData().getPath());
        PasswdSafeApp app = (PasswdSafeApp)getApplication();
        itsPasswdFile = app.accessPasswdFile(itsFile, this);
        setTitle(PasswdSafeApp.getAppFileTitle(itsFile, this));

        if (!itsPasswdFile.isOpen()) {
            showDialog(DIALOG_GET_PASSWD);
        } else {
            showFileData();
        }
    }

    private final void onCreateNew(Intent intent)
    {
        itsFile = new File(intent.getData().getPath());
        showDialog(DIALOG_FILE_NEW);
    }

    private final void openFile(StringBuilder passwd)
    {
        removeDialog(DIALOG_GET_PASSWD);
        showDialog(DIALOG_PROGRESS);
        itsLoadTask = new LoadTask(passwd);
        itsLoadTask.execute();
    }

    private final void createNewFile(String fileName, StringBuilder passwd)
    {
        try
        {
            File dir = itsFile;
            itsFile = new File(dir, fileName + ".psafe3");

            PasswdFileData fileData = new PasswdFileData(itsFile);
            fileData.createNewFile(passwd, this);

            PasswdSafeApp app = (PasswdSafeApp)getApplication();
            itsPasswdFile = app.accessPasswdFile(itsFile, this);
            itsPasswdFile.setFileData(fileData);
            setTitle(PasswdSafeApp.getAppFileTitle(itsFile, this));
            showFileData();
        } catch (Exception e) {
            PasswdSafeApp.showFatalMsg(e, "Can't create file: " + itsFile,
                                       this);
            finish();
        }
    }

    private final void deleteFile()
    {
        if (!itsFile.delete()) {
            PasswdSafeApp.showFatalMsg("Could not delete file: " + itsFile,
                                       this);
        } else {
            finish();
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
                "Could not change password on closed file: " + itsFile, this);
        }
    }

    private final void showFileData()
    {
        populateFileData();

        View panel = findViewById(R.id.sub_group_panel);
        if (itsCurrGroups.isEmpty()) {
            panel.setVisibility(View.GONE);
        } else {
            panel.setVisibility(View.VISIBLE);
            TextView tv = (TextView)findViewById(R.id.current_group);
            tv.setText(getString(R.string.current_group_label,
                                 TextUtils.join(" / ", itsCurrGroups)));
        }

        int layout = R.layout.passwdsafe_list_item;
        String[] from;
        int[] to;
        if (itsSearchQuery == null) {
            from = new String[] { TITLE, USERNAME, ICON };
            to = new int[] { android.R.id.text1, android.R.id.text2, R.id.icon };
        } else {
            from = new String[] { TITLE, USERNAME, ICON, MATCH };
            to = new int[] { android.R.id.text1, android.R.id.text2,
                             R.id.icon, R.id.match };
        }

        ListAdapter adapter = new SectionListAdapter(PasswdSafe.this,
                                                     itsListData, layout,
                                                     from, to,
                                                     itsIsSortCaseSensitive);
        setListAdapter(adapter);
    }

    private final void populateFileData()
    {
        itsListData.clear();
        if (itsPasswdFile == null) {
            return;
        }

        PasswdFileData fileData = itsPasswdFile.getFileData();
        if (fileData == null) {
            return;
        }
        ArrayList<PwsRecord> records = fileData.getRecords();
        RecordMapComparator comp =
            new RecordMapComparator(itsIsSortCaseSensitive);

        if (itsGroupRecords) {
            Comparator<String> groupComp;
            if (itsIsSortCaseSensitive) {
                groupComp = new StringComparator();
            } else {
                groupComp = String.CASE_INSENSITIVE_ORDER;
            }

            GroupNode root = new GroupNode();
            for (PwsRecord rec : records) {
                String match = filterRecord(rec, fileData);
                if (match == null) {
                    continue;
                }
                String group = fileData.getGroup(rec);
                if (group == null) {
                    group = "";
                }
                String[] groups = TextUtils.split(group, "\\.");
                GroupNode node = root;
                for (String g : groups) {
                    GroupNode groupNode = node.getGroup(g);
                    if (groupNode == null) {
                        groupNode = new GroupNode();
                        node.putGroup(g, groupNode, groupComp);
                    }
                    node = groupNode;
                }
                node.addRecord(new MatchPwsRecord(rec, match));
            }

            // find right group
            GroupNode node = root;
            for (String group : itsCurrGroups) {
                GroupNode childNode = node.getGroup(group);
                if (childNode == null) {
                    break;
                }
                node = childNode;
            }

            Map<String, GroupNode> entryGroups = node.getGroups();
            if (entryGroups != null) {
                for(Map.Entry<String, GroupNode> entry :
                        entryGroups.entrySet()) {
                    HashMap<String, Object> recInfo =
                        new HashMap<String, Object>();
                    recInfo.put(TITLE, entry.getKey());
                    recInfo.put(ICON,R.drawable.ic_menu_archive_rev);

                    int items = entry.getValue().getNumRecords();
                    String str = (items == 1) ? "item" : "items";
                    recInfo.put(USERNAME, "[" + items + " " + str + "]");
                    itsListData.add(recInfo);
                }
            }

            List<MatchPwsRecord> entryRecs = node.getRecords();
            if (entryRecs != null) {
                for (MatchPwsRecord rec : entryRecs) {
                    itsListData.add(createRecInfo(rec, fileData));
                }
            }
        } else {
            for (PwsRecord rec : records) {
                String match = filterRecord(rec, fileData);
                if (match == null) {
                    continue;
                }
                itsListData.add(createRecInfo(new MatchPwsRecord(rec, match),
                                              fileData));
            }
        }
        Collections.sort(itsListData, comp);
    }

    private static final HashMap<String, Object>
    createRecInfo(MatchPwsRecord rec, PasswdFileData fileData)
    {
        HashMap<String, Object> recInfo = new HashMap<String, Object>();
        String title = fileData.getTitle(rec.itsRecord);
        if (title == null) {
            title = "Untitled";
        }
        String user = fileData.getUsername(rec.itsRecord);
        if (!TextUtils.isEmpty(user)) {
            user = "[" + user + "]";
        }
        recInfo.put(TITLE, title);
        recInfo.put(RECORD, rec.itsRecord);
        recInfo.put(MATCH, rec.itsMatch);
        recInfo.put(USERNAME, user);
        // TODO: update current group label (add icon??)
        recInfo.put(ICON, R.drawable.ic_menu_contact_rev);
        return recInfo;
    }

    private final void setSearchQuery(String query)
    {
        itsSearchQuery = null;
        if ((query != null) && (query.length() != 0)) {
            if (QUERY_MATCH_TITLE == null) {
                QUERY_MATCH_TITLE = getString(R.string.title);
                QUERY_MATCH_USERNAME = getString(R.string.username);
                QUERY_MATCH_URL = getString(R.string.url);
                QUERY_MATCH_EMAIL = getString(R.string.email);
                QUERY_MATCH_NOTES = getString(R.string.notes);
            }

            try {
                int flags = 0;

                if (!itsIsSearchCaseSensitive) {
                    flags |= Pattern.CASE_INSENSITIVE;
                }
                if (!itsIsSearchRegex) {
                    flags |= Pattern.LITERAL;
                }
                itsSearchQuery = Pattern.compile(query, flags);
            } catch(PatternSyntaxException e) {
            }
        }

        View panel = findViewById(R.id.query_panel);
        if (itsSearchQuery != null) {
            panel.setVisibility(View.VISIBLE);
            TextView tv = (TextView)findViewById(R.id.query);
            tv.setText(getString(R.string.query_label,
                                 itsSearchQuery.pattern()));
        } else {
            panel.setVisibility(View.GONE);
        }

        showFileData();
    }

    private final String filterRecord(PwsRecord rec, PasswdFileData fileData)
    {
        if (itsSearchQuery == null) {
            return QUERY_MATCH;
        }

        if (filterField(fileData.getTitle(rec))) {
            return QUERY_MATCH_TITLE;
        }

        if (filterField(fileData.getUsername(rec))) {
            return QUERY_MATCH_USERNAME;
        }

        if (filterField(fileData.getURL(rec))) {
            return QUERY_MATCH_URL;
        }

        if (filterField(fileData.getEmail(rec))) {
            return QUERY_MATCH_EMAIL;
        }

        if (filterField(fileData.getNotes(rec))) {
            return QUERY_MATCH_NOTES;
        }

        return null;
    }

    private final boolean filterField(String field)
    {
        if (field != null) {
            Matcher m = itsSearchQuery.matcher(field);
            return m.find();
        } else {
            return false;
        }
    }

    private final void cancelFileOpen()
    {
        removeDialog(DIALOG_PROGRESS);
        removeDialog(DIALOG_GET_PASSWD);
        finish();
    }


    /**
     * @return true if a group was popped, false to use default behavior
     */
    private final boolean doBackPressed()
    {
        PasswdSafeApp.dbginfo(TAG, "doBackPressed");
        int size = itsCurrGroups.size();
        if (size != 0) {
            itsCurrGroups.remove(size - 1);
            showFileData();
            return true;
        } else {
            return false;
        }
    }


    private final class LoadTask extends AsyncTask<Void, Void, Object>
    {
        private final StringBuilder itsPasswd;

        private LoadTask(StringBuilder passwd)
        {
            itsPasswd = passwd;
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Object doInBackground(Void... params)
        {
            try {
                PasswdFileData fileData = new PasswdFileData(itsFile);
                fileData.load(itsPasswd);
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

    private static final class RecordMapComparator implements
                    Comparator<HashMap<String, Object>>
    {
        private boolean itsIsSortCaseSensitive;

        public RecordMapComparator(boolean sortCaseSensitive)
        {
            itsIsSortCaseSensitive = sortCaseSensitive;
        }

        public int compare(HashMap<String, Object> arg0,
                           HashMap<String, Object> arg1)
        {
            // Sort groups first
            Object rec0 = arg0.get(RECORD);
            Object rec1 = arg1.get(RECORD);
            if ((rec0 == null) && (rec1 != null)) {
                return -1;
            } else if ((rec0 != null) && (rec1 == null)) {
                return 1;
            }

            int rc = compareField(arg0, arg1, TITLE);
            if (rc == 0) {
                rc = compareField(arg0, arg1, USERNAME);
            }
            return rc;
        }

        private final int compareField(HashMap<String, Object> arg0,
                                       HashMap<String, Object> arg1,
                                       String field)
        {
            Object obj0 = arg0.get(field);
            Object obj1 = arg1.get(field);

            if ((obj0 == null) && (obj1 == null)) {
                return 0;
            } else if (obj0 == null) {
                return -1;
            } else if (obj1 == null) {
                return 1;
            } else {
                String str0 = obj0.toString();
                String str1 = obj1.toString();

                if (itsIsSortCaseSensitive) {
                    return str0.compareTo(str1);
                } else {
                    return str0.compareToIgnoreCase(str1);
                }
            }
        }
    }

    private static final class StringComparator implements Comparator<String>
    {
        public int compare(String arg0, String arg1)
        {
            return arg0.compareTo(arg1);
        }
    }

    private static final class MatchPwsRecord
    {
        public final PwsRecord itsRecord;
        public final String itsMatch;

        public MatchPwsRecord(PwsRecord rec, String match)
        {
            itsRecord = rec;
            itsMatch = match;
        }
    }

    private static final class GroupNode
    {
        private List<MatchPwsRecord> itsRecords = null;
        private TreeMap<String, GroupNode> itsGroups = null;

        public GroupNode()
        {
        }

        public final void addRecord(MatchPwsRecord rec)
        {
            if (itsRecords == null) {
                itsRecords = new ArrayList<MatchPwsRecord>();
            }
            itsRecords.add(rec);
        }

        public final List<MatchPwsRecord> getRecords()
        {
            return itsRecords;
        }

        public final void putGroup(String name, GroupNode node,
                                   Comparator<String> groupComp)
        {
            if (itsGroups == null) {
                itsGroups = new TreeMap<String, GroupNode>(groupComp);
            }
            itsGroups.put(name, node);
        }

        public final GroupNode getGroup(String name)
        {
            if (itsGroups == null) {
                return null;
            } else {
                return itsGroups.get(name);
            }
        }

        public final Map<String, GroupNode> getGroups()
        {
            return itsGroups;
        }

        public final int getNumRecords()
        {
            int num = 0;
            if (itsRecords != null) {
                num += itsRecords.size();
            }
            if (itsGroups != null) {
                for (GroupNode child: itsGroups.values()) {
                    num += child.getNumRecords();
                }
            }
            return num;
        }
    }


    private static final class SectionListAdapter
        extends SimpleAdapter implements SectionIndexer
    {
        private static final class Section
        {
            public final String itsName;
            public final int itsPos;
            public Section(String name, int pos)
            {
                itsName = name;
                itsPos = pos;
            }

            @Override
            public final String toString()
            {
                return itsName;
            }
        }

        private Section[] itsSections;

        public SectionListAdapter(Context context,
                                  List<? extends Map<String, ?>> data,
                                  int resource, String[] from, int[] to,
                                  boolean caseSensitive)
        {
            super(context, data, resource, from, to);
            ArrayList<Section> sections = new ArrayList<Section>();
            char compChar = '\0';
            char first;
            char compFirst;
            for (int i = 0; i < data.size(); ++i) {
                String title = (String) data.get(i).get(TITLE);
                if (TextUtils.isEmpty(title)) {
                    first = ' ';
                } else {
                    first = title.charAt(0);
                }

                if (!caseSensitive) {
                    compFirst = Character.toLowerCase(first);
                } else {
                    compFirst = first;
                }
                if (compChar != compFirst) {
                    Section s = new Section(Character.toString(first), i);
                    sections.add(s);
                    compChar = compFirst;
                }
            }

            itsSections = sections.toArray(new Section[sections.size()]);
        }

        public int getPositionForSection(int section)
        {
            if (section < itsSections.length) {
                return itsSections[section].itsPos;
            } else {
                return 0;
            }
        }

        public int getSectionForPosition(int position)
        {
            // Section positions in increasing order
            for (int i = 0; i < itsSections.length; ++i) {
                Section s = itsSections[i];
                if (position <= s.itsPos) {
                    return i;
                }
            }
            return 0;
        }

        public Object[] getSections()
        {
            return itsSections;
        }
    }
}