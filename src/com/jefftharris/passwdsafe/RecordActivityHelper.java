/*
 * Copyright (©) 2011 Jeff Harris <jefftharris@gmail.com>
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

public class RecordActivityHelper implements PasswdFileActivity
{
    private static final String TAG = "RecordActivityHelper";

    private static final int DIALOG_PROGRESS = 0;
    public static final int MAX_DIALOG = DIALOG_PROGRESS;

    private File itsFile;
    private String itsUUID;
    private ActivityPasswdFile itsPasswdFile;
    private final Activity itsActivity;

    public RecordActivityHelper(Activity activity)
    {
        itsActivity = activity;
    }

    public Activity getActivity()
    {
        return itsActivity;
    }

    public void showProgressDialog()
    {
        itsActivity.showDialog(DIALOG_PROGRESS);
    }

    public void removeProgressDialog()
    {
        itsActivity.removeDialog(DIALOG_PROGRESS);
    }

    public void saveFinished(boolean success)
    {
        if (success) {
            itsActivity.setResult(PasswdSafeApp.RESULT_MODIFIED);
        }
        itsActivity.finish();
    }

    public final File getFile()
    {
        return itsFile;
    }

    public final String getUUID()
    {
        return itsUUID;
    }

    public final ActivityPasswdFile getPasswdFile()
    {
        return itsPasswdFile;
    }

    public void onCreate(Bundle savedInstanceState)
    {
        Intent intent = itsActivity.getIntent();
        PasswdSafeApp.dbginfo(TAG, "onCreate intent:" + intent);

        itsFile = new File(intent.getData().getPath());
        itsUUID = intent.getData().getQueryParameter("rec");

        PasswdSafeApp app = (PasswdSafeApp)itsActivity.getApplication();
        itsPasswdFile = app.accessPasswdFile(itsFile, this);
        PasswdFileData fileData = itsPasswdFile.getFileData();
        if (fileData == null) {
            PasswdSafeApp.showFatalMsg("File not open: " + itsFile,
                                       itsActivity);
            return;
        }

        itsActivity.setTitle(PasswdSafeApp.getAppFileTitle(itsPasswdFile,
                                                           itsActivity));
    }

    public void onDestroy()
    {
        PasswdSafeApp.dbginfo(TAG, "onDestroy");
        if (itsPasswdFile != null) {
            itsPasswdFile.onActivityDestroy();
        }
    }

    public void onPause()
    {
        PasswdSafeApp.dbginfo(TAG, "onPause");
        if (itsPasswdFile != null) {
            itsPasswdFile.onActivityPause();
        }
    }

    public void onSaveInstanceState(Bundle outState)
    {
        itsActivity.removeDialog(DIALOG_PROGRESS);
    }

    public Dialog onCreateDialog(int id)
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
            break;
        }
        }
        return dialog;
    }

    public final void saveFile()
    {
        itsPasswdFile.save();
    }
}
