package com.jefftharris.passwdsafe;

import org.pwsafe.lib.file.PwsRecord;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

    public static final String INTENT = "com.jefftharris.passwdsafe.action.VIEW";

    private String itsFileName;
    private PasswdFileData itsFileData;

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
            // TODO: what if user hits back button when password dialog shown
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
        // TODO: what if interrupted while loading data??
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
                        PasswdSafe.this.removeDialog(DIALOG_GET_PASSWD);
                        openFile(passwdInput.getText().toString());
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

    private void openFile(String passwd)
    {
        final ProgressDialog progress =
            ProgressDialog.show(this, "", "Loading...", true);

        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                progress.dismiss();
                if (msg.what == LoadFileThread.RESULT_DATA) {
                    itsFileData = (PasswdFileData)msg.obj;
                    PasswdSafeApp app = (PasswdSafeApp)getApplication();
                    app.setFileData(itsFileData);
                    showFileData();
                } else {
                    Exception e = (Exception)msg.obj;
                    PasswdSafeApp.showFatalMsg(e.toString(), PasswdSafe.this);
                }
            }
        };

        LoadFileThread thr = new LoadFileThread(itsFileName,
                                                new StringBuilder(passwd),
                                                handler);
        thr.start();
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
        removeDialog(DIALOG_GET_PASSWD);
        finish();
    }

    private static final class LoadFileThread extends Thread
    {
        private final String itsFile;
        private StringBuilder itsPasswd;
        private final Handler itsMsgHandler;

        public static final int RESULT_DATA = 0;
        public static final int RESULT_EXCEPTION = 1;

        public LoadFileThread(String file,
                              StringBuilder passwd,
                              Handler msgHandler) {
            itsFile = file;
            itsPasswd = passwd;
            itsMsgHandler = msgHandler;
        }

        @Override
        public void run() {
            PasswdFileData data = null;
            Exception resultException = null;
            try {
                // TODO: on pause, close file, clear password, etc.
                data = new PasswdFileData(itsFile, itsPasswd);
            } catch (Exception e) {
                Log.e(TAG, "Exception", e);
                resultException = e;
            }

            Message msg;
            if (data != null) {
                msg = Message.obtain(itsMsgHandler, RESULT_DATA, data);
            } else {
                msg = Message.obtain(itsMsgHandler, RESULT_EXCEPTION,
                                     resultException);
            }
            itsMsgHandler.sendMessage(msg);
        }
    }
}