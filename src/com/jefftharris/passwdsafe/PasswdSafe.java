package com.jefftharris.passwdsafe;

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
import android.util.Log;
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

    public static final String INTENT = "com.jefftharris.passwdsafe.action.VIEW";

    private String itsFileName;
    private PasswdFileData itsFileData;
    private LoadTask itsLoadTask;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate bundle:" + savedInstanceState + ", intent:" +
              getIntent());
        itsFileName = getIntent().getData().getPath();

        PasswdSafeApp app = (PasswdSafeApp)getApplication();
        itsFileData = app.getFileData(itsFileName);
        if (itsFileData == null) {
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
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause()
    {
        Log.i(TAG, "onPause");
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
        Log.i(TAG, "onResume");
        super.onResume();
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
        Log.i(TAG, "onSaveInstanceState state:" + outState);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id)
    {
        Log.i(TAG, "onCreateDialog id:" + id);
        Dialog dialog = null;
        switch (id) {
        case DIALOG_GET_PASSWD:
        {
            LayoutInflater factory = LayoutInflater.from(this);
            final View passwdView =
                factory.inflate(R.layout.passwd_entry, null);

            // TODO: click Ok when enter pressed
            AlertDialog.Builder alert = new AlertDialog.Builder(this)
                .setTitle("Enter Password")
                .setMessage("Password:")
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
        PwsRecord rec = itsFileData.getRecord(groupPosition, childPosition);
        Log.i(TAG, "selected child:" + itsFileData.getTitle(rec));

        Uri.Builder builder = getIntent().getData().buildUpon();
        builder.appendQueryParameter("rec",
                                     itsFileData.getUUID(rec).toString());
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
        ExpandableListAdapter adapter =
            new SimpleExpandableListAdapter(PasswdSafe.this,
                                            itsFileData.itsGroupData,
                                            android.R.layout.simple_expandable_list_item_1,
                                            new String[] { PasswdFileData.GROUP },
                                            new int[] { android.R.id.text1 },
                                            itsFileData.itsChildData,
                                            android.R.layout.simple_expandable_list_item_1,
                                            new String[] { PasswdFileData.TITLE },
                                            new int[] { android.R.id.text1 });
        setListAdapter(adapter);
        Log.i(TAG, "adapter set");
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
                return new PasswdFileData(itsFileName, itsPasswd);
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
            Log.i(TAG, "LoadTask cancelled");
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
            Log.i(TAG, "LoadTask post execute");
            dismissDialog(DIALOG_PROGRESS);
            itsLoadTask = null;
            if (result instanceof PasswdFileData) {
                itsFileData = (PasswdFileData)result;
                PasswdSafeApp app = (PasswdSafeApp)getApplication();
                app.setFileData(itsFileData);
                showFileData();
            } else if (result instanceof Exception) {
                Exception e = (Exception)result;
                PasswdSafeApp.showFatalMsg(e.toString(), PasswdSafe.this);
            }
        }
    }
}