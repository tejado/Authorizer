/*
 * Copyright (©) 2009-2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import com.jefftharris.passwdsafe.file.PasswdFileUri;
import com.jefftharris.passwdsafe.lib.ApiCompat;

import android.app.Activity;
import android.app.Dialog;
import android.app.TabActivity;
import android.os.Bundle;
import android.support.annotation.NonNull;

@SuppressWarnings("deprecation")
public abstract class AbstractRecordTabActivity extends TabActivity implements
                PasswdFileActivity
{
    protected static final int MAX_DIALOG = RecordActivityHelper.MAX_DIALOG;

    private final RecordActivityHelper itsHelper;

    protected AbstractRecordTabActivity()
    {
        itsHelper = new RecordActivityHelper(this);
    }

    public Activity getActivity()
    {
        return itsHelper.getActivity();
    }

    public void showProgressDialog()
    {
        itsHelper.showProgressDialog();
    }

    public void removeProgressDialog()
    {
        itsHelper.removeProgressDialog();
    }

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
        itsHelper.onCreate();
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
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        itsHelper.onSaveInstanceState();
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
