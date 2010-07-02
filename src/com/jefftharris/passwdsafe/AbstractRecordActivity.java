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
import android.content.Intent;
import android.os.Bundle;

public abstract class AbstractRecordActivity extends Activity
    implements PasswdFileActivity
{
    private static final String TAG = "AbstractRecordActivity";

    private static final int DIALOG_PROGRESS = 0;
    protected static final int MAX_DIALOG = DIALOG_PROGRESS;

    private File itsFile;
    private String itsUUID;
    private ActivityPasswdFile itsPasswdFile;


    public final Activity getActivity()
    {
        return this;
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#showProgressDialog()
     */
    public final void showProgressDialog()
    {
        showDialog(DIALOG_PROGRESS);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#removeProgressDialog()
     */
    public void removeProgressDialog()
    {
        removeDialog(DIALOG_PROGRESS);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#saveFinished(boolean)
     */
    public void saveFinished(boolean success)
    {
        if (success) {
            setResult(PasswdSafeApp.RESULT_MODIFIED);
        }
        finish();
    }


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
            itsPasswdFile.onActivityDestroy();
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
        if (itsPasswdFile != null) {
            itsPasswdFile.onActivityPause();
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        removeDialog(DIALOG_PROGRESS);
        super.onSaveInstanceState(outState);
    }


    @Override
    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog = null;
        switch (id)
        {
        case DIALOG_PROGRESS:
        {
            dialog = itsPasswdFile.createProgressDialog();
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
        itsPasswdFile.save();
    }

}
