/*
 * Copyright (©) 2009-2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */

package com.jefftharris.passwdsafe;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ConcurrentModificationException;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.lib.ApiCompat;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

/**
 * The ActivityPasswdFile interface provides access to the password file data
 * for an application.
 *
 * @author Jeff Harris
 */
public abstract class ActivityPasswdFile
{
    private static final String TAG = "ActivityPasswdFile";

    private final class SaveTask extends AsyncTask<Void, Void, Object>
    {
        @Override
        protected Object doInBackground(Void... params)
        {
            try {
                doSave();
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
            itsActivity.removeProgressDialog();
            boolean success = !(result instanceof Exception);
            if (!success) {
                Exception e = (Exception)result;
                String msg = e.toString();
                if ((e instanceof IOException) &&
                        (ApiCompat.SDK_VERSION >= ApiCompat.SDK_KITKAT)) {
                    msg = getActivity().getString(
                            R.string.kitkat_sdcard_warning, msg);
                }
                PasswdSafeUtil.showFatalMsg(e, msg, getActivity(), true);
            }
            itsSaveTask = null;
            itsActivity.saveFinished(success);
        }
    }

    private final PasswdFileActivity itsActivity;
    private SaveTask itsSaveTask;

    protected ActivityPasswdFile(PasswdFileActivity activity)
    {
        itsActivity = activity;
    }

    public final void save()
    {
        PasswdSafeUtil.dbginfo(TAG, "saving");
        itsActivity.showProgressDialog();
        itsSaveTask = new SaveTask();
        itsSaveTask.execute();
    }

    public final Dialog createProgressDialog()
    {
        Activity activity = getActivity();
        ProgressDialog dlg = new ProgressDialog(activity);
        dlg.setTitle(PasswdSafeUtil.getAppTitle(activity));
        String uri = getFileData().getUri().getIdentifier(activity, true);
        dlg.setMessage(activity.getString(R.string.saving_file, uri));
        dlg.setIndeterminate(true);
        dlg.setCancelable(false);
        return dlg;
    }

    public final void onActivityPause()
    {
        if (itsSaveTask != null) {
            itsActivity.removeProgressDialog();
            try {
                itsSaveTask.get();
            } catch (Exception e) {
                PasswdSafeUtil.showFatalMsg(e, getActivity());
            }
            itsSaveTask = null;
        }

    }

    public final void onActivityDestroy()
    {
        release();
    }

    /**
     * @return the fileData
     */
    public abstract PasswdFileData getFileData();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public abstract boolean isOpen();

    public abstract void setFileData(PasswdFileData fileData);
    public abstract void setLastViewedRecord(String uuid);

    public abstract void touch();
    public abstract void release();
    public abstract void close();

    public abstract void pauseFileTimer();
    public abstract void resumeFileTimer();

    /**
     * Save the file.  Will likely be called in a background thread.
     * @throws IOException
     * @throws ConcurrentModificationException
     * @throws NoSuchAlgorithmException
     */
    protected abstract void doSave()
        throws NoSuchAlgorithmException, ConcurrentModificationException,
               IOException;

    /**
     * @return the activity
     */
    protected final Activity getActivity()
    {
        return itsActivity.getActivity();
    }

}
