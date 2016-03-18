/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ProgressBar;

import com.jefftharris.passwdsafe.file.PasswdFileUri;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

/**
 * Base class for a fragment to open or create a new file
 */
public abstract class AbstractPasswdSafeOpenNewFileFragment extends Fragment
{
    private Uri itsFileUri;
    private ProgressBar itsProgress;
    private PasswdFileUri itsPasswdFileUri;
    private ResolveTask itsResolveTask;
    private int itsNumProgressUsers = 0;
    private boolean itsDoResolveOnStart = true;

    @Override
    public void onStart()
    {
        super.onStart();
        if (itsDoResolveOnStart) {
            startResolve();
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        cancelFragment(false);
    }

    /**
     * Get the file URI
     */
    protected final Uri getFileUri()
    {
        return itsFileUri;
    }

    /**
     * Set the file URI
     */
    protected final void setFileUri(Uri fileUri)
    {
        itsFileUri = fileUri;
    }

    /**
     * Set whether to do a resolve on start
     */
    protected final void setDoResolveOnStart(boolean doResolveOnStart)
    {
        itsDoResolveOnStart = doResolveOnStart;
    }

    /**
     * Get the password file URI
     */
    protected final PasswdFileUri getPasswdFileUri()
    {
        return itsPasswdFileUri;
    }

    /**
     * Get the progress bar
     */
    protected final ProgressBar getProgress()
    {
        return itsProgress;
    }

    /**
     * Setup the view
     */
    protected final void setupView(View rootView)
    {
        itsProgress = (ProgressBar)rootView.findViewById(R.id.progress);
        itsProgress.setVisibility(View.INVISIBLE);
    }

    /**
     * Start the resolve task
     */
    protected void startResolve()
    {
        itsResolveTask = new ResolveTask();
        itsResolveTask.execute();
    }

    /**
     * Derived-class handler for when the resolve task is finished
     */
    protected abstract void doResolveTaskFinished();

    /**
     * Derived-class handler when the fragment is canceled
     */
    protected abstract void doCancelFragment(boolean userCancel);

    /**
     * Enable/disable field controls during background operations
     */
    protected abstract void setFieldsEnabled(boolean enabled);

    /**
     *  Cancel the fragment
     */
    protected final void cancelFragment(boolean userCancel)
    {
        if (itsResolveTask != null) {
            ResolveTask task = itsResolveTask;
            itsResolveTask = null;
            task.cancel(false);
        }
        doCancelFragment(userCancel);
    }

    /**
     * Set whether the progress bar is visible
     */
    protected void setProgressVisible(boolean visible,
                                      boolean indeterminate)
    {
        if (visible) {
            if (++itsNumProgressUsers == 1) {
                itsProgress.setIndeterminate(indeterminate);
                itsProgress.setVisibility(View.VISIBLE);
            }
        } else {
            if (--itsNumProgressUsers <= 0) {
                itsNumProgressUsers = 0;
                itsProgress.setVisibility(View.INVISIBLE);
            }
        }
    }

    /**
     * Handle when the resolve task is finished
     */
    private void resolveTaskFinished(PasswdFileUri uri)
    {
        if (itsResolveTask == null) {
            return;
        }
        itsResolveTask = null;
        if (uri == null) {
            cancelFragment(false);
            return;
        }

        if (!uri.exists()) {
            PasswdSafeUtil.showFatalMsg("File doesn't exist: " + uri,
                                        getActivity());
            return;
        }

        itsPasswdFileUri = uri;
        doResolveTaskFinished();
    }

    /**
     * Background task for resolving the file URI
     */
    protected class ResolveTask extends BackgroundTask<PasswdFileUri>
    {
        private PasswdFileUri.Creator itsUriCreator =
                new PasswdFileUri.Creator(itsFileUri, getActivity());

        @Override
        protected final void onPreExecute()
        {
            super.onPreExecute();
            itsUriCreator.onPreExecute();
        }

        @Override
        protected final PasswdFileUri doInBackground(Void... voids)
        {
            return itsUriCreator.finishCreate();
        }

        @Override
        protected final void onPostExecute(PasswdFileUri uri)
        {
            super.onPostExecute(uri);
            Throwable resolveEx = itsUriCreator.getResolveEx();
            if (resolveEx != null) {
                PasswdSafeUtil.showFatalMsg(
                        getString(R.string.file_not_found_perm_denied),
                        getActivity());
            } else {
                resolveTaskFinished(uri);
            }
        }
    }

    /**
     * Background task
     */
    protected abstract class BackgroundTask<ResultT>
            extends AsyncTask<Void, Void, ResultT>
    {
        @Override
        protected final void onCancelled()
        {
            onPostExecute(null);
        }

        @Override
        protected void onPreExecute()
        {
            setProgressVisible(true, true);
            setFieldsEnabled(false);
        }

        @Override
        protected void onPostExecute(ResultT data)
        {
            setProgressVisible(false, true);
            setFieldsEnabled(true);
        }
    }
}
