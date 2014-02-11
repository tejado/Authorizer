/*
 * Copyright (©) 2012-2013 Jeff Harris <jefftharris@gmail.com>
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
import android.app.ListActivity;
import android.os.Bundle;

public abstract class AbstractPasswdFileListActivity extends ListActivity
    implements PasswdFileActivity
{
    private static final String TAG = "AbstractPasswdFileListActivity";

    private static final int DIALOG_SAVE_PROGRESS = 0;

    private static final String SAVED_URI = "saved_uri";

    protected static final int MAX_DIALOG = DIALOG_SAVE_PROGRESS;

    private PasswdFileUri itsUri;
    private ActivityPasswdFile itsPasswdFile;


    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            itsUri = savedInstanceState.getParcelable(SAVED_URI);
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SAVED_URI, itsUri);
    }

    /** Get the PasswdSafeApp */
    protected final PasswdSafeApp getPasswdSafeApp()
    {
        return (PasswdSafeApp)getApplication();
    }

    /** Initialize the file URI */
    protected final void initUri(PasswdFileUri uri)
    {
        itsUri = uri;
    }


    /** Open a file from a URI */
    protected final void openFile(PasswdFileUri uri)
    {
        itsUri = uri;
        itsPasswdFile = getPasswdSafeApp().accessPasswdFile(itsUri, this);
    }


    /** Access the open file */
    protected final boolean accessOpenFile()
    {
        itsPasswdFile = getPasswdSafeApp().accessOpenFile(this);
        if (itsPasswdFile != null) {
            itsUri = itsPasswdFile.getFileData().getUri();
            return true;
        }
        return false;
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#getActivity()
     */
    public Activity getActivity()
    {
        return this;
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#showProgressDialog()
     */
    @SuppressWarnings("deprecation")
    public void showProgressDialog()
    {
        showDialog(DIALOG_SAVE_PROGRESS);
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#removeProgressDialog()
     */
    @SuppressWarnings("deprecation")
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
        PasswdSafeUtil.dbginfo(TAG, "onDestroy");
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
        PasswdSafeUtil.dbginfo(TAG, "onPause");
        super.onPause();

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
     * @see android.app.Activity#onCreateDialog(int)
     */
    @SuppressWarnings("deprecation")
    @Override
    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog = null;
        switch (id) {
        case DIALOG_SAVE_PROGRESS:
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


    /** Get the password file */
    protected final ActivityPasswdFile getPasswdFile()
    {
        return itsPasswdFile;
    }


    /** Get the password file data */
    protected final PasswdFileData getPasswdFileData()
    {
        return (itsPasswdFile != null) ? itsPasswdFile.getFileData() : null;
    }


    /** Get the file's URI */
    protected final PasswdFileUri getUri()
    {
        return itsUri;
    }


    /** Get a name for the URI */
    protected final String getUriName(boolean shortId)
    {
        return itsUri.getIdentifier(this, shortId);
    }
}
