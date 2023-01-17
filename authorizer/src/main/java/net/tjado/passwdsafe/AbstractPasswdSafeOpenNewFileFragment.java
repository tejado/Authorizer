/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.net.Uri;
import android.view.View;
import android.widget.ProgressBar;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import net.tjado.passwdsafe.file.PasswdFileUri;
import net.tjado.passwdsafe.lib.ManagedTask;
import net.tjado.passwdsafe.lib.ManagedTasks;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.util.CountedBool;

/**
 * Base class for a fragment to open or create a new file
 */
public abstract class AbstractPasswdSafeOpenNewFileFragment extends Fragment
{
    private final ManagedTasks itsTasks = new ManagedTasks();
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
     * Set the password file URI
     */
    protected void setPasswdFileUri(PasswdFileUri uri)
    {
        itsPasswdFileUri = uri;
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
        itsProgress = rootView.findViewById(R.id.progress);
        itsProgress.setVisibility(View.INVISIBLE);
    }

    /**
     * Start the resolve task
     */
    protected final void startResolve()
    {
        startTask(new ResolveTask(itsFileUri, this));
    }

    /**
     * Start a managed task
     */
    protected final void startTask(ManagedTask<?,?> task)
    {
        itsTasks.startTask(task);
    }

    /**
     * Update the tasks when one is finished.  Called from BackgroundTask.
     */
    protected final void taskFinished(ManagedTask<?,?> task)
    {
        itsTasks.taskFinished(task);
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
        itsTasks.cancelTasks();
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
    private void resolveTaskFinished(PasswdFileUri uri, Throwable error)
    {
        if ((uri != null) && isAdded()) {
            if (uri.exists()) {
                itsPasswdFileUri = uri;
                doResolveTaskFinished();
            } else {
                PasswdSafeUtil.showFatalMsg("File doesn't exist: " + uri,
                                            getActivity());
            }
        } else if (error != null) {
            PasswdSafeUtil.showFatalMsg(
                    error, getString(R.string.file_not_found_perm_denied),
                    getActivity(), false);
        } else {
            cancelFragment(isAdded());
        }
    }

    /**
     * Background task for resolving the file URI
     */
    private static final class ResolveTask
            extends BackgroundTask<PasswdFileUri,
            AbstractPasswdSafeOpenNewFileFragment>
    {
        private final PasswdFileUri.Creator itsUriCreator;

        /**
         * Constructor
         */
        private ResolveTask(Uri uri,
                            AbstractPasswdSafeOpenNewFileFragment frag)
        {
            super(frag);
            itsUriCreator = new PasswdFileUri.Creator(uri, getContext());
        }

        @Override
        protected void onTaskStarted(
                @NonNull AbstractPasswdSafeOpenNewFileFragment frag)
        {
            super.onTaskStarted(frag);
            itsUriCreator.onPreExecute();
        }

        @Override
        protected PasswdFileUri doInBackground() throws Throwable
        {
            return itsUriCreator.finishCreate();
        }

        @Override
        protected void onTaskFinished(
                PasswdFileUri result,
                Throwable error,
                @NonNull AbstractPasswdSafeOpenNewFileFragment frag)
        {
            super.onTaskFinished(result, error, frag);
            frag.resolveTaskFinished(result, error);
        }
    }

    /**
     * Background task
     */
    protected static abstract class BackgroundTask<
            ResultT, FragT extends AbstractPasswdSafeOpenNewFileFragment>
            extends ManagedTask<ResultT, FragT>
    {
        /**
         * Constructor
         */
        protected BackgroundTask(FragT frag)
        {
            super(frag, frag.requireContext());
        }

        @Override @CallSuper
        protected void onTaskStarted(@NonNull FragT frag)
        {
            setRunning(true, frag);
        }

        @Override @CallSuper
        protected void onTaskFinished(ResultT result,
                                      Throwable error,
                                      @NonNull FragT frag)
        {
            frag.taskFinished(this);
            setRunning(false, frag);
        }

        /**
         * Set whether the task is running
         */
        private void setRunning(boolean running, FragT frag)
        {
            frag.setProgressVisible(running, true);
            frag.setFieldsDisabled(running);
        }
    }
}
