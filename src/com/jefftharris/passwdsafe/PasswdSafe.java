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

import org.pwsafe.lib.file.PwsRecord;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;

public class PasswdSafe extends ExpandableListActivity {
    private static final String TAG = "PasswdSafe";

    private static final int DIALOG_GET_PASSWD = 0;
    private static final int DIALOG_PROGRESS = 1;

    private File itsFile;
    private ActivityPasswdFile itsPasswdFile;
    private LoadTask itsLoadTask;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        PasswdSafeApp.dbginfo(TAG, "onCreate intent:" + getIntent());
        itsFile = new File(getIntent().getData().getPath());

        PasswdSafeApp app = (PasswdSafeApp)getApplication();
        itsPasswdFile = app.accessPasswdFile(itsFile, this);
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

            // TODO: click Ok when enter pressed
            AlertDialog.Builder alert = new AlertDialog.Builder(this)
                .setTitle("Open " + itsFile.getName())
                .setMessage("Enter password:")
                .setView(passwdView)
                .setPositiveButton("Ok",
                                   new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        EditText passwdInput = (EditText) passwdView
                            .findViewById(R.id.passwd_edit);
                        openFile(
                            new StringBuilder(passwdInput.getText().toString()));
                    }
                })
                .setNegativeButton("Cancel",
                                   new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        cancelFileOpen();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener()
                {
                    public void onCancel(DialogInterface dialog)
                    {
                        cancelFileOpen();
                    }
                });
            dialog = alert.create();
            break;
        }
        case DIALOG_PROGRESS:
        {
            ProgressDialog dlg = new ProgressDialog(this);
            dlg.setMessage("Loading...");
            dlg.setIndeterminate(true);
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
        PwsRecord rec = fileData.getRecord(groupPosition, childPosition);

        Uri.Builder builder = getIntent().getData().buildUpon();
        builder.appendQueryParameter("rec", fileData.getUUID(rec).toString());
        Intent intent = new Intent(Intent.ACTION_VIEW, builder.build(),
                                   this, RecordView.class);
        startActivity(intent);
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
        PasswdFileData fileData = itsPasswdFile.getFileData();
        ExpandableListAdapter adapter =
            new SimpleExpandableListAdapter(PasswdSafe.this,
                                            fileData.itsGroupData,
                                            android.R.layout.simple_expandable_list_item_1,
                                            new String[] { PasswdFileData.GROUP },
                                            new int[] { android.R.id.text1 },
                                            fileData.itsChildData,
                                            android.R.layout.simple_expandable_list_item_1,
                                            new String[] { PasswdFileData.TITLE },
                                            new int[] { android.R.id.text1 });
        setListAdapter(adapter);
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
}