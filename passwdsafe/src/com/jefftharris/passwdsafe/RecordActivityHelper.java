/*
 * Copyright (©) 2011-2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdFileUri;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

public class RecordActivityHelper implements PasswdFileActivity
{
    private static final String TAG = "RecordActivityHelper";

    private static final int DIALOG_PROGRESS = 0;
    public static final int MAX_DIALOG = DIALOG_PROGRESS;

    private PasswdFileUri itsUri;
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
            itsActivity.finish();
        }
    }

    public final PasswdFileUri getUri()
    {
        return itsUri;
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
        PasswdSafeUtil.dbginfo(TAG, "onCreate intent: %s", intent);

        itsUri = PasswdSafeApp.getFileUriFromIntent(intent, itsActivity);
        itsUUID = intent.getData().getQueryParameter("rec");

        PasswdSafeApp app = (PasswdSafeApp)itsActivity.getApplication();
        itsPasswdFile = app.accessPasswdFile(itsUri, this);
        PasswdFileData fileData = itsPasswdFile.getFileData();
        if (fileData == null) {
            PasswdSafeUtil.showFatalMsg("File not open: " + itsUri,
                                        itsActivity);
            return;
        }

        itsActivity.setTitle(PasswdSafeApp.getAppFileTitle(itsPasswdFile,
                                                           itsActivity));
    }

    public void onDestroy()
    {
        PasswdSafeUtil.dbginfo(TAG, "onDestroy");
        if (itsPasswdFile != null) {
            itsPasswdFile.onActivityDestroy();
        }
    }

    public void onPause()
    {
        PasswdSafeUtil.dbginfo(TAG, "onPause");
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
