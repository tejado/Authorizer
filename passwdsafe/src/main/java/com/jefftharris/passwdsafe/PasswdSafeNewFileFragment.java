/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdFileUri;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;


/**
 * Fragment for creating a new file
 */
public class PasswdSafeNewFileFragment extends Fragment
    implements View.OnClickListener
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
    {
        /** Handle when the file new is canceled */
        void handleFileNewCanceled();

        /** Handle when the file was successfully created */
        void handleFileNew(PasswdFileData fileData);
    }

    private Listener itsListener;
    private Uri itsNewFileUri;
    private TextView itsFileName;
    private TextView itsPasswordEdit;
    private TextView itsPasswordConfirm;
    private ProgressBar itsProgress;
    private Button itsOkBtn;
    private PasswdFileUri itsPasswdFileUri;
    private ResolveTask itsResolveTask;

    /**
     * Create a new instance
     */
    public static PasswdSafeNewFileFragment newInstance(Uri newFileUri)
    {
        PasswdSafeNewFileFragment fragment = new PasswdSafeNewFileFragment();
        Bundle args = new Bundle();
        args.putParcelable("uri", newFileUri);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            itsNewFileUri = args.getParcelable("uri");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_passwdsafe_new_file,
                                         container, false);

        itsFileName = (TextView)rootView.findViewById(R.id.file_name);
        itsPasswordEdit = (TextView)rootView.findViewById(R.id.password);
        itsPasswordConfirm =
                (TextView)rootView.findViewById(R.id.password_confirm);
        itsProgress = (ProgressBar)rootView.findViewById(R.id.progress);
        itsProgress.setVisibility(View.INVISIBLE);
        Button cancelBtn = (Button)rootView.findViewById(R.id.cancel);
        cancelBtn.setOnClickListener(this);
        itsOkBtn = (Button)rootView.findViewById(R.id.ok);
        itsOkBtn.setOnClickListener(this);
        itsOkBtn.setEnabled(false);

        GuiUtils.setupFormKeyboard(itsFileName, itsPasswordConfirm,
                                   itsOkBtn, getActivity());

        return rootView;
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        itsListener = (Listener)activity;
    }

    @Override
    public void onStart()
    {
        super.onStart();
        itsResolveTask = new ResolveTask();
        itsResolveTask.execute();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        cancelOpen(false);
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        itsListener = null;
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId()) {
        case R.id.cancel: {
            if (itsListener != null) {
                itsListener.handleFileNewCanceled();
            }
            break;
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
            cancelOpen(true);
            return;
        }

        if (!uri.exists()) {
            PasswdSafeUtil.showFatalMsg("File does't exist: " + uri,
                                        getActivity());
            return;
        }

        itsPasswdFileUri = uri;
    }

    /**
     * Set whether a background task is running
     */
    private void setBgTaskRunning(boolean running)
    {
        itsFileName.setEnabled(!running);
        itsPasswordEdit.setEnabled(!running);
        itsPasswordConfirm.setEnabled(!running);
        itsProgress.setVisibility(running ? View.VISIBLE : View.INVISIBLE);
        itsOkBtn.setEnabled(!running);
    }

    /**
     *  Cancel the file open
     */
    private void cancelOpen(boolean userCancel)
    {
        if (itsResolveTask != null) {
            ResolveTask task = itsResolveTask;
            itsResolveTask = null;
            task.cancel(false);
        }
        GuiUtils.setKeyboardVisible(itsFileName, getActivity(), false);
        if (userCancel && itsListener != null) {
            itsListener.handleFileNewCanceled();
        }
    }

    /**
     * Background task for resolving the file URI
     */
    private class ResolveTask extends BackgroundTask<PasswdFileUri>
    {
        @Override
        protected PasswdFileUri doInBackground(Void... voids)
        {
            return new PasswdFileUri(itsNewFileUri, getActivity());
        }

        @Override
        protected void onPostExecute(PasswdFileUri uri)
        {
            super.onPostExecute(uri);
            resolveTaskFinished(uri);
        }
    }

    /**
     * Background task
     */
    private abstract class BackgroundTask<ResultT>
            extends AsyncTask<Void, Void, ResultT>
    {
        @Override
        protected void onCancelled()
        {
            onPostExecute(null);
        }

        @Override
        protected void onPreExecute()
        {
            setBgTaskRunning(true);
        }

        @Override
        protected void onPostExecute(ResultT data)
        {
            setBgTaskRunning(false);
        }
    }
}
