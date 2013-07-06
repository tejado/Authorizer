/*
 * Copyright (©) 2009-2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;

import com.jefftharris.passwdsafe.file.PasswdFileUri;
import com.jefftharris.passwdsafe.lib.ApiCompat;

public abstract class AbstractRecordActivity extends Activity
    implements PasswdFileActivity
{
    protected static final int MAX_DIALOG = RecordActivityHelper.MAX_DIALOG;

    private final RecordActivityHelper itsHelper;

    public AbstractRecordActivity()
    {
        itsHelper = new RecordActivityHelper(this);
    }

    public final Activity getActivity()
    {
        return itsHelper.getActivity();
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#showProgressDialog()
     */
    public final void showProgressDialog()
    {
        itsHelper.showProgressDialog();
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#removeProgressDialog()
     */
    public void removeProgressDialog()
    {
        itsHelper.removeProgressDialog();
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#saveFinished(boolean)
     */
    public void saveFinished(boolean success)
    {
        itsHelper.saveFinished(success);
    }


    protected final PasswdFileUri getUri()
    {
        return itsHelper.getUri();
    }

    protected final String getUUID()
    {
        return itsHelper.getUUID();
    }

    protected final ActivityPasswdFile getPasswdFile()
    {
        return itsHelper.getPasswdFile();
    }

    /** Get the PasswdSafeApp */
    protected final PasswdSafeApp getPasswdSafeApp()
    {
        return (PasswdSafeApp)getApplication();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        itsHelper.onCreate(savedInstanceState);
        ApiCompat.setRecentAppsVisible(getWindow(), false);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        itsHelper.onDestroy();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        itsHelper.onPause();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        itsHelper.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog = itsHelper.onCreateDialog(id);
        if (dialog == null) {
            dialog = super.onCreateDialog(id);
        }
        return dialog;
    }

    protected final void saveFile()
    {
        itsHelper.saveFile();
    }

}
