/*
 * Copyright (©) 2009-2010 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.File;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

public abstract class AbstractRecordActivity extends Activity
{
    private static final String TAG = "AbstractRecordActivity";

    private static final int DIALOG_PROGRESS = 0;
    protected static final int MAX_DIALOG = DIALOG_PROGRESS;

    private File itsFile;
    private String itsUUID;
    private ActivityPasswdFile itsPasswdFile;
    private SaveTask itsSaveTask;


    protected final File getFile()
    {
        return itsFile;
    }

    protected final String getUUID()
    {
        return itsUUID;
    }

    protected final ActivityPasswdFile getPasswdFile()
    {
        return itsPasswdFile;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        PasswdSafeApp.dbginfo(TAG, "onCreate intent:" + getIntent());

        itsFile = new File(intent.getData().getPath());
        itsUUID = intent.getData().getQueryParameter("rec");

        PasswdSafeApp app = (PasswdSafeApp)getApplication();
        itsPasswdFile = app.accessPasswdFile(itsFile, this);
        PasswdFileData fileData = itsPasswdFile.getFileData();
        if (fileData == null) {
            PasswdSafeApp.showFatalMsg("File not open: " + itsFile, this);
            return;
        }

        setTitle(PasswdSafeApp.getAppFileTitle(itsPasswdFile, this));
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
            itsPasswdFile.release();
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        PasswdSafeApp.dbginfo(TAG, "onPause");
        if (itsSaveTask != null) {
            try {
                itsSaveTask.get();
            } catch (Exception e) {
                PasswdSafeApp.showFatalMsg(e.toString(), this);
            }
            itsSaveTask = null;
            removeDialog(DIALOG_PROGRESS);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog = null;
        switch (id)
        {
        case DIALOG_PROGRESS:
        {
            ProgressDialog dlg = new ProgressDialog(this);
            dlg.setTitle(PasswdSafeApp.getAppTitle(this));
            dlg.setMessage("Saving " + itsFile.getName() + "...");
            dlg.setIndeterminate(true);
            dlg.setCancelable(false);
            dialog = dlg;
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

    protected final void saveFile()
    {
        PasswdSafeApp.dbginfo(TAG, "saving");
        // TODO update header fields for last save info??
        showDialog(DIALOG_PROGRESS);
        itsSaveTask = new SaveTask();
        itsSaveTask.execute();
    }

    private final class SaveTask extends AsyncTask<Void, Void, Object>
    {
        @Override
        protected Object doInBackground(Void... params)
        {
            try {
                itsPasswdFile.save();
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Object result)
        {
            removeDialog(DIALOG_PROGRESS);
            if (result instanceof Exception) {
                PasswdSafeApp.showFatalMsg(((Exception)result).toString(),
                                           AbstractRecordActivity.this);
            } else {
                setResult(PasswdSafeApp.RESULT_MODIFIED);
            }
            itsSaveTask = null;
            finish();
        }
    }

}
