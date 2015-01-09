/*
 * Copyright (©) 2013-2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

/**
 * Async task to update an account
 */
public abstract class AccountUpdateTask extends AsyncTask<Void, Void, Void>
{
    private static final String TAG = "AccountUpdateTask";

    protected final Uri itsAccountUri;

    private final String itsProgressMsg;
    private FragmentActivity itsActivity;
    private ProgressFragment itsProgressFrag;

    /** Start the update task */
    public void startTask(FragmentActivity activity)
    {
        itsActivity = activity;
        execute();
    }

    /** Constructor */
    protected AccountUpdateTask(Uri accountUri, String progressMsg)
    {
        itsAccountUri = accountUri;
        itsProgressMsg = progressMsg;
    }

    /* (non-Javadoc)
     * @see android.os.AsyncTask#onPreExecute()
     */
    @Override
    protected void onPreExecute()
    {
        super.onPreExecute();
        itsProgressFrag = ProgressFragment.newInstance(itsProgressMsg);
        itsProgressFrag.show(itsActivity.getSupportFragmentManager(), null);
    }

    /* (non-Javadoc)
     * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
     */
    @Override
    protected final Void doInBackground(Void... params)
    {
        try {
            ContentResolver cr = itsActivity.getContentResolver();
            doAccountUpdate(cr);
        } catch (Exception e) {
            Log.e(TAG, "Account update error", e);
        }

        return null;
    }

    /** Do the account update in the background */
    protected abstract void doAccountUpdate(ContentResolver cr);

    /* (non-Javadoc)
     * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
     */
    @Override
    protected void onPostExecute(Void arg)
    {
        super.onPostExecute(arg);
        itsProgressFrag.dismissAllowingStateLoss();
    }
}
