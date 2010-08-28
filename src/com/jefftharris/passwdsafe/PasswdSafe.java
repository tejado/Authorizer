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
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

public class PasswdSafe extends ExpandableListActivity
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
    private static final String GROUP = "group";
    private static final String MATCH = "match";

    private static final String NO_GROUP_GROUP = "Records";

    private static final int RECORD_VIEW_REQUEST = 0;
    private static final int RECORD_ADD_REQUEST = 1;

    private File itsFile;
    private ActivityPasswdFile itsPasswdFile;
    private LoadTask itsLoadTask;
    private boolean itsGroupRecords = true;
    private boolean itsIsSortCaseSensitive = true;
    private DialogValidator itsChangePasswdValidator;
    private DialogValidator itsFileNewValidator;

    private final ArrayList<Map<String, String>> itsGroupData =
        new ArrayList<Map<String, String>>();
    private final ArrayList<ArrayList<HashMap<String, Object>>> itsChildData =
        new ArrayList<ArrayList<HashMap<String, Object>>>();

    private Pattern itsSearchQuery = null;
    private static final String QUERY_MATCH = "";
    private String QUERY_MATCH_TITLE;
    private String QUERY_MATCH_USERNAME;
    private String QUERY_MATCH_URL;
    private String QUERY_MATCH_EMAIL;
    private String QUERY_MATCH_NOTES;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passwd_safe);
        updateQueryPanelVisibility();
        Button button = (Button)findViewById(R.id.query_clear_btn);
        button.setOnClickListener(new View.OnClickListener()
        {
            public final void onClick(View v)
            {
                onNewIntent(null);
            }
        });

        Intent intent = getIntent();
        PasswdSafeApp.dbginfo(TAG, "onCreate intent:" + intent);

        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        itsGroupRecords = PasswdSafeApp.getGroupRecordsPref(prefs);
        itsIsSortCaseSensitive = PasswdSafeApp.getSortCaseSensitivePref(prefs);

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
        // TODO: Save query across screen orientation changes
        super.onNewIntent(intent);
        itsSearchQuery = null;
        if (intent != null) {
            if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
                if (QUERY_MATCH_TITLE == null) {
                    QUERY_MATCH_TITLE = getString(R.string.title);
                    QUERY_MATCH_USERNAME = getString(R.string.username);
                    QUERY_MATCH_URL = getString(R.string.url);
                    QUERY_MATCH_EMAIL = getString(R.string.email);
                    QUERY_MATCH_NOTES = getString(R.string.notes);
                }

                String query = intent.getStringExtra(SearchManager.QUERY);
                if (query.length() > 0) {
                    try {
                        itsSearchQuery = Pattern.compile(
                            query, Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
                    } catch(PatternSyntaxException e) {
                        itsSearchQuery = null;
                    }
                }
            }
        }
        updateQueryPanelVisibility();
        showFileData();
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
            tv.setText(Integer.toString(itsGroupData.size()));

            tv = (TextView)dialog.findViewById(R.id.num_records);
            tv.setText(Integer.toString(fileData.getRecords().size()));
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
     * @see android.app.ExpandableListActivity#onChildClick(android.widget.ExpandableListView, android.view.View, int, int, long)
     */
    @Override
    public boolean onChildClick(ExpandableListView parent,
                                View v,
                                int groupPosition,
                                int childPosition,
                                long id)
    {
        PasswdFileData fileData = itsPasswdFile.getFileData();

        PwsRecord rec = (PwsRecord)
            itsChildData.get(groupPosition).
            get(childPosition).
            get(RECORD);

        Uri.Builder builder = Uri.fromFile(itsFile).buildUpon();
        String uuid = fileData.getUUID(rec);
        if (uuid != null) {
            builder.appendQueryParameter("rec", uuid.toString());
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, builder.build(),
                                   this, RecordView.class);
        startActivityForResult(intent, RECORD_VIEW_REQUEST);
        return true;
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
            fileData.createNewFile(passwd);

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
        fileData.changePasswd(passwd);
        itsPasswdFile.save();
    }

    private final void showFileData()
    {
        populateFileData();

        int childLayout;
        String[] childFrom;
        int[] childTo;
        if (itsSearchQuery == null) {
            childLayout = android.R.layout.simple_expandable_list_item_1;
            childFrom = new String[] { TITLE };
            childTo = new int[] { android.R.id.text1 };
        } else {
            childLayout = android.R.layout.simple_expandable_list_item_2;
            childFrom = new String[] { TITLE, MATCH };
            childTo = new int[] { android.R.id.text1, android.R.id.text2 };
        }

        ExpandableListAdapter adapter =
            new SimpleExpandableListAdapter(PasswdSafe.this,
                                            itsGroupData,
                                            android.R.layout.simple_expandable_list_item_1,
                                            new String[] { GROUP },
                                            new int[] { android.R.id.text1 },
                                            itsChildData,
                                            childLayout,
                                            childFrom,
                                            childTo);
        setListAdapter(adapter);

        if (itsGroupData.size() == 1) {
            getExpandableListView().expandGroup(0);
        } else if (itsSearchQuery != null) {
            int size = itsGroupData.size();
            ExpandableListView view = getExpandableListView();
            for (int i = 0; i < size; ++i) {
                view.expandGroup(i);
            }
        }
    }

    private final void populateFileData()
    {
        itsGroupData.clear();
        itsChildData.clear();

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
            TreeMap<String, ArrayList<MatchPwsRecord>> recsByGroup;
            if (itsIsSortCaseSensitive) {
                recsByGroup = new TreeMap<String, ArrayList<MatchPwsRecord>>();
            } else {
                recsByGroup = new TreeMap<String, ArrayList<MatchPwsRecord>>(
                                String.CASE_INSENSITIVE_ORDER);
            }

            for (PwsRecord rec : records) {
                String match = filterRecord(rec, fileData);
                if (match == null) {
                    continue;
                }
                String group = fileData.getGroup(rec);
                if ((group == null) || (group.length() == 0)) {
                    group = NO_GROUP_GROUP;
                }
                ArrayList<MatchPwsRecord> groupList = recsByGroup.get(group);
                if (groupList == null) {
                    groupList = new ArrayList<MatchPwsRecord>();
                    recsByGroup.put(group, groupList);
                }
                groupList.add(new MatchPwsRecord(rec, match));
            }

            for (Map.Entry<String, ArrayList<MatchPwsRecord>> entry :
                recsByGroup.entrySet()) {
                Map<String, String> groupInfo =
                    Collections.singletonMap(GROUP, entry.getKey());
                itsGroupData.add(groupInfo);

                ArrayList<HashMap<String, Object>> children =
                    new ArrayList<HashMap<String, Object>>();
                for (MatchPwsRecord rec : entry.getValue()) {
                    HashMap<String, Object> recInfo =
                        new HashMap<String, Object>();
                    String title = fileData.getTitle(rec.itsRecord);
                    if (title == null) {
                        title = "Untitled";
                    }
                    recInfo.put(TITLE, title);
                    recInfo.put(RECORD, rec.itsRecord);
                    recInfo.put(MATCH, rec.itsMatch);
                    children.add(recInfo);
                }
                Collections.sort(children, comp);
                itsChildData.add(children);
            }
        } else {
            Map<String, String> groupInfo =
                Collections.singletonMap(GROUP, NO_GROUP_GROUP);
            itsGroupData.add(groupInfo);

            ArrayList<HashMap<String, Object>> children =
                new ArrayList<HashMap<String, Object>>();
            for (PwsRecord rec : records) {
                String match = filterRecord(rec, fileData);
                if (match == null) {
                    continue;
                }
                HashMap<String, Object> recInfo = new HashMap<String, Object>();
                String title = fileData.getTitle(rec);
                if (title == null) {
                    title = "Untitled";
                }
                recInfo.put(TITLE, title);
                recInfo.put(RECORD, rec);
                recInfo.put(MATCH, match);
                children.add(recInfo);
            }
            Collections.sort(children, comp);
            itsChildData.add(children);
        }
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

    private final void updateQueryPanelVisibility()
    {
        View panel = findViewById(R.id.query_panel);
        if (itsSearchQuery != null) {
            panel.setVisibility(View.VISIBLE);
            TextView tv = (TextView)findViewById(R.id.query);
            tv.setText(getString(R.string.query_label,
                                 itsSearchQuery.pattern()));
        } else {
            panel.setVisibility(View.GONE);
        }
    }

    private final void cancelFileOpen()
    {
        removeDialog(DIALOG_PROGRESS);
        removeDialog(DIALOG_GET_PASSWD);
        finish();
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
            String title0 = arg0.get(TITLE).toString();
            String title1 = arg1.get(TITLE).toString();

            if (itsIsSortCaseSensitive) {
                return title0.compareTo(title1);
            } else {
                return title0.compareToIgnoreCase(title1);
            }
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
}