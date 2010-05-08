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

import org.pwsafe.lib.file.PwsRecord;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;

public class PasswdSafe extends ExpandableListActivity {
    private static final String TAG = "PasswdSafe";

    private static final int DIALOG_GET_PASSWD = 0;
    private static final int DIALOG_PROGRESS = 1;

    private static final int MENU_ADD_RECORD = 1;

    private static final String RECORD = "record";
    private static final String TITLE = "title";
    private static final String GROUP = "group";

    private static final String NO_GROUP_GROUP = "Records";

    private static final int RECORD_VIEW_REQUEST = 0;
    private static final int RECORD_ADD_REQUEST = 1;

    private File itsFile;
    private ActivityPasswdFile itsPasswdFile;
    private LoadTask itsLoadTask;
    private boolean itsGroupRecords = true;
    private boolean itsIsSortCaseSensitive = true;

    private final ArrayList<Map<String, String>> itsGroupData =
        new ArrayList<Map<String, String>>();
    private final ArrayList<ArrayList<HashMap<String, Object>>> itsChildData =
        new ArrayList<ArrayList<HashMap<String, Object>>>();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        PasswdSafeApp.dbginfo(TAG, "onCreate intent:" + getIntent());
        itsFile = new File(getIntent().getData().getPath());

        PasswdSafeApp app = (PasswdSafeApp)getApplication();

        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        itsGroupRecords = PasswdSafeApp.getGroupRecordsPref(prefs);
        itsIsSortCaseSensitive = PasswdSafeApp.getSortCaseSensitivePref(prefs);

        itsPasswdFile = app.accessPasswdFile(itsFile, this);
        setTitle(PasswdSafeApp.getAppFileTitle(itsFile, this));

        if (!itsPasswdFile.isOpen()) {
            showDialog(DIALOG_GET_PASSWD);
        } else {
            showFileData();
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy()
    {
        PasswdSafeApp.dbginfo(TAG, "onDestroy");
        super.onDestroy();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause()
    {
        super.onPause();

        removeDialog(DIALOG_GET_PASSWD);
        if (itsLoadTask != null)
            itsLoadTask.cancel(true);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        itsPasswdFile.touch();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        removeDialog(DIALOG_GET_PASSWD);
        removeDialog(DIALOG_PROGRESS);
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
        return true;
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case MENU_ADD_RECORD:
        {
            startActivityForResult(
                new Intent(Intent.ACTION_INSERT, getIntent().getData(),
                           this, RecordEditActivity.class),
                RECORD_ADD_REQUEST);
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
                .setTitle("Open " + itsFile.getName())
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
        }
        return dialog;
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

        Uri.Builder builder = getIntent().getData().buildUpon();
        String uuid = fileData.getUUID(rec);
        if (uuid != null) {
            builder.appendQueryParameter("rec", uuid.toString());
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, builder.build(),
                                   this, RecordView.class);
        startActivityForResult(intent, RECORD_VIEW_REQUEST);
        return true;
    }

    private void openFile(StringBuilder passwd)
    {
        removeDialog(DIALOG_GET_PASSWD);
        showDialog(DIALOG_PROGRESS);
        itsLoadTask = new LoadTask(passwd);
        itsLoadTask.execute();
    }

    private void showFileData()
    {
        itsGroupData.clear();
        itsChildData.clear();

        PasswdFileData fileData = itsPasswdFile.getFileData();
        ArrayList<PwsRecord> records = fileData.getRecords();
        RecordMapComparator comp =
            new RecordMapComparator(itsIsSortCaseSensitive);

        if (itsGroupRecords) {
            TreeMap<String, ArrayList<PwsRecord>> recsByGroup;
            if (itsIsSortCaseSensitive) {
                recsByGroup = new TreeMap<String, ArrayList<PwsRecord>>();
            } else {
                recsByGroup = new TreeMap<String, ArrayList<PwsRecord>>(
                                String.CASE_INSENSITIVE_ORDER);
            }

            for (PwsRecord rec : records) {
                String group = fileData.getGroup(rec);
                if ((group == null) || (group.length() == 0)) {
                    group = NO_GROUP_GROUP;
                }
                ArrayList<PwsRecord> groupList = recsByGroup.get(group);
                if (groupList == null) {
                    groupList = new ArrayList<PwsRecord>();
                    recsByGroup.put(group, groupList);
                }
                groupList.add(rec);
            }

            for (Map.Entry<String, ArrayList<PwsRecord>> entry :
                recsByGroup.entrySet()) {
                Map<String, String> groupInfo =
                    Collections.singletonMap(GROUP, entry.getKey());
                itsGroupData.add(groupInfo);

                ArrayList<HashMap<String, Object>> children =
                    new ArrayList<HashMap<String, Object>>();
                for (PwsRecord rec : entry.getValue()) {
                    HashMap<String, Object> recInfo =
                        new HashMap<String, Object>();
                    String title = fileData.getTitle(rec);
                    if (title == null) {
                        title = "Untitled";
                    }
                    recInfo.put(TITLE, title);
                    recInfo.put(RECORD, rec);
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
                HashMap<String, Object> recInfo = new HashMap<String, Object>();
                String title = fileData.getTitle(rec);
                if (title == null) {
                    title = "Untitled";
                }
                recInfo.put(TITLE, title);
                recInfo.put(RECORD, rec);
                children.add(recInfo);
            }
            Collections.sort(children, comp);
            itsChildData.add(children);
        }

        ExpandableListAdapter adapter =
            new SimpleExpandableListAdapter(PasswdSafe.this,
                                            itsGroupData,
                                            android.R.layout.simple_expandable_list_item_1,
                                            new String[] { GROUP },
                                            new int[] { android.R.id.text1 },
                                            itsChildData,
                                            android.R.layout.simple_expandable_list_item_1,
                                            new String[] { TITLE },
                                            new int[] { android.R.id.text1 });
        setListAdapter(adapter);

        if (itsGroupData.size() == 1) {
            getExpandableListView().expandGroup(0);
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
                return new PasswdFileData(itsFile, itsPasswd);
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
                String str;
                if ((e instanceof IOException) &&
                                (e.getMessage().equals("Invalid password")))
                    str = e.getMessage();
                else
                    str = e.toString();
                PasswdSafeApp.showFatalMsg(str, PasswdSafe.this);
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
}