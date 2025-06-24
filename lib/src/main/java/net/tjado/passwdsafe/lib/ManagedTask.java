/*
 * Copyright (©) 2017 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib;

import android.content.Context;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.core.util.Pair;

/**
 * An async task that is manageable from fragments or activities
 */
public abstract class ManagedTask<ResultT, FragT>
{
    private final ManagedRef<FragT> itsFrag;
    private final Context itsContext;
    private final Task<ResultT, FragT> itsTask;

    /**
     * Constructor
     */
    protected ManagedTask(FragT frag, Context context)
    {
        itsFrag = new ManagedRef<>(frag);
        itsContext = context.getApplicationContext();
        itsTask = new Task<>(this);
    }

    /**
     * Start the task
     */
    public void start()
    {
        itsTask.execute();
    }

    /**
     * Cancel the task if still running
     */
    public void cancel()
    {
        if (itsTask.cancel(true)) {
            finishTask(null, null);
        }
        itsFrag.clear();
    }

    /**
     * Get a context
     */
    protected Context getContext()
    {
        return itsContext;
    }

    /**
     * Handle when a task starts
     */
    protected abstract void onTaskStarted(@NonNull FragT frag);

    /**
     * Handle when a task finishes.  Called immediately on cancellation or
     * after the background task finishes
     * @param result The result of the task; null if canceled or an error
     * @param error An error running the task; null if successful or canceled
     * @param frag The task's fragment
     */
    protected abstract void onTaskFinished(ResultT result,
                                           Throwable error,
                                           @NonNull FragT frag);

    /**
     * Run the task in the background
     * @return The result of the task
     * @throws Throwable If an error occurs
     */
    protected abstract ResultT doInBackground() throws Throwable;

    /**
     * Finish the task
     */
    private void finishTask(ResultT result, Throwable error)
    {
        FragT frag = itsFrag.get();
        if (frag != null) {
            onTaskFinished(result, error, frag);
        }
    }

    /**
     * Background async task
     */
    private static class Task<ResultT, FragT>
            extends AsyncTask<Void, Void, Pair<ResultT, Throwable>>
    {
        private final ManagedTask<ResultT, FragT> itsTask;

        /**
         * Constructor
         */
        protected Task(ManagedTask<ResultT, FragT> task)
        {
            itsTask = task;
        }

        @Override
        protected void onPreExecute()
        {
            FragT frag = itsTask.itsFrag.get();
            if (frag != null) {
                itsTask.onTaskStarted(frag);
            }
        }

        @Override
        protected void onPostExecute(Pair<ResultT, Throwable> result)
        {
            itsTask.finishTask(result.first, result.second);
        }

        @Override
        protected Pair<ResultT, Throwable> doInBackground(Void... voids)
        {
            try {
                return new Pair<>(itsTask.doInBackground(), null);
            } catch (Throwable t) {
                return new Pair<>(null, t);
            }
        }
    }
}
