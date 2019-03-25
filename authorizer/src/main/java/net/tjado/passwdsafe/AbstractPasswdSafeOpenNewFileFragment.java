/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.net.Uri;
import android.os.AsyncTask;
import androidx.fragment.app.Fragment;
import android.view.View;
import android.widget.ProgressBar;

import net.tjado.passwdsafe.file.PasswdFileUri;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.util.CountedBool;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for a fragment to open or create a new file
 */
public abstract class AbstractPasswdSafeOpenNewFileFragment extends Fragment
{
    private final List<ResolveTask> itsResolveTasks = new ArrayList<>();
    private Uri itsFileUri;
    private ProgressBar itsProgress;
    private PasswdFileUri itsPasswdFileUri;
    private final CountedBool itsProgressVisible = new CountedBool();
    private final CountedBool itsFieldsDisabled = new CountedBool();
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
    protected final void startResolve()
    {
        ResolveTask task = new ResolveTask();
        itsResolveTasks.add(task);
        task.execute();
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
     * Derived-class handler to enable/disable field controls during
     * background operations
     */
    protected abstract void doSetFieldsEnabled(boolean enabled);

    /**
     * Cancel the fragment
     */
    protected final void cancelFragment(boolean userCancel)
    {
        for (ResolveTask task: itsResolveTasks) {
            task.cancel(false);
        }
        itsResolveTasks.clear();
        doCancelFragment(userCancel);
    }

    /**
     * Set whether the progress bar is visible
     */
    protected final void setProgressVisible(boolean visible,
                                            boolean indeterminate)
    {
        switch (itsProgressVisible.update(visible)) {
        case TRUE: {
            itsProgress.setIndeterminate(indeterminate);
            itsProgress.setVisibility(View.VISIBLE);
            break;
        }
        case FALSE: {
            itsProgress.setVisibility(View.INVISIBLE);
            break;
        }
        case SAME: {
            break;
        }
        }
    }

    /**
     * Disable field controls during background operations
     */
    protected final void setFieldsDisabled(boolean disabled)
    {
        switch (itsFieldsDisabled.update(disabled)) {
        case TRUE: {
            doSetFieldsEnabled(false);
            break;
        }
        case FALSE: {
            doSetFieldsEnabled(true);
            break;
        }
        case SAME: {
            break;
        }
        }
    }

    /**
     * Handle when the resolve task is finished
     */
    private void resolveTaskFinished(PasswdFileUri uri, ResolveTask task)
    {
        if (!itsResolveTasks.remove(task)) {
            return;
        }
        if ((uri == null) || !isAdded()) {
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
    private class ResolveTask extends BackgroundTask<PasswdFileUri>
    {
        private final PasswdFileUri.Creator itsUriCreator =
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
                resolveTaskFinished(uri, this);
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
            setFieldsDisabled(true);
        }

        @Override
        protected void onPostExecute(ResultT data)
        {
            setProgressVisible(false, true);
            setFieldsDisabled(false);
        }
    }
}
