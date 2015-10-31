/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdFileUri;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.view.AbstractTextWatcher;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.view.PasswordVisibilityMenuHandler;


/**
 * Fragment for creating a new file
 */
public class PasswdSafeNewFileFragment
        extends AbstractPasswdSafeOpenNewFileFragment
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

        /** Update the view for creating a new file */
        void updateViewFileNew();
    }

    private Listener itsListener;
    private String itsPsafe3Sfx;
    private TextView itsTitle;
    private TextInputLayout itsFileNameInput;
    private EditText itsFileName;
    private TextInputLayout itsPasswordInput;
    private TextView itsPassword;
    private TextInputLayout itsPasswordConfirmInput;
    private TextView itsPasswordConfirm;
    private Button itsOkBtn;
    private Validator itsValidator = new Validator();
    private NewTask itsNewTask;

    private static final String ARG_URI = "uri";

    /**
     * Create a new instance
     */
    public static PasswdSafeNewFileFragment newInstance(Uri newFileUri)
    {
        PasswdSafeNewFileFragment fragment = new PasswdSafeNewFileFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, newFileUri);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            setFileUri((Uri)args.getParcelable(ARG_URI));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_passwdsafe_new_file,
                                         container, false);
        setupView(rootView);

        itsPsafe3Sfx = getString(R.string.psafe3_ext);
        itsTitle = (TextView)rootView.findViewById(R.id.title);
        itsFileNameInput = (TextInputLayout)
                rootView.findViewById(R.id.file_name_input);
        itsFileName = (EditText)rootView.findViewById(R.id.file_name);
        itsFileName.setSelection(0);
        itsFileName.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after)
            {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count)
            {
            }

            @Override
            public void afterTextChanged(Editable s)
            {
                if (!s.toString().endsWith(itsPsafe3Sfx)) {
                    s.replace(0, s.length(), itsPsafe3Sfx);
                    itsFileName.setSelection(0);
                }
            }
        });
        itsValidator.registerTextView(itsFileName);

        itsPasswordInput = (TextInputLayout)
                rootView.findViewById(R.id.password_input);
        itsPassword = (TextView)rootView.findViewById(R.id.password);
        itsValidator.registerTextView(itsPassword);
        itsPasswordInput.setTypeface(Typeface.DEFAULT);

        itsPasswordConfirmInput = (TextInputLayout)
                rootView.findViewById(R.id.password_confirm_input);
        itsPasswordConfirm = (TextView)
                rootView.findViewById(R.id.password_confirm);
        itsValidator.registerTextView(itsPasswordConfirm);
        itsPasswordConfirmInput.setTypeface(Typeface.DEFAULT);
        PasswordVisibilityMenuHandler.set(itsPassword, itsPasswordConfirm);

        Button cancelBtn = (Button)rootView.findViewById(R.id.cancel);
        cancelBtn.setOnClickListener(this);
        itsOkBtn = (Button)rootView.findViewById(R.id.ok);
        itsOkBtn.setOnClickListener(this);
        itsOkBtn.setEnabled(false);

        GuiUtils.setupFormKeyboard(itsFileName, itsPasswordConfirm, itsOkBtn,
                                   getActivity());

        return rootView;
    }

    @Override
    public void onAttach(Context ctx)
    {
        super.onAttach(ctx);
        itsListener = (Listener)ctx;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        itsListener.updateViewFileNew();
        itsValidator.validate();
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
        case R.id.ok: {
            itsNewTask = new NewTask(
                    itsFileName.getText().toString(),
                    new StringBuilder(itsPassword.getText()));
            itsNewTask.execute();
            break;
        }
        }
    }

    /**
     * Derived-class handler for when the resolve task is finished
     */
    @Override
    protected final void doResolveTaskFinished()
    {
        int titleId = R.string.new_file;
        PasswdFileUri uri = getPasswdFileUri();
        PasswdFileUri.Type type =
                (uri != null) ? uri.getType() : PasswdFileUri.Type.FILE;
        switch (type) {
        case FILE: {
            titleId = R.string.new_local_file;
            break;
        }
        case SYNC_PROVIDER: {
            if (uri.getSyncType() == null) {
                PasswdSafeUtil.showFatalMsg("Unknown sync type", getActivity());
                break;
            }
            switch (uri.getSyncType()) {
            case GDRIVE:
            case GDRIVE_PLAY: {
                titleId = R.string.new_drive_file;
                break;
            }
            case DROPBOX: {
                titleId = R.string.new_dropbox_file;
                break;
            }
            case BOX: {
                titleId = R.string.new_box_file;
                break;
            }
            case ONEDRIVE: {
                titleId = R.string.new_onedrive_file;
                break;
            }
            case OWNCLOUD: {
                titleId = R.string.new_owncloud_file;
                break;
            }
            }
        }
        case EMAIL:
        case GENERIC_PROVIDER: {
            break;
        }
        }
        itsTitle.setText(titleId);
    }

    /**
     *  Derived-class handler when the fragment is canceled
     */
    @Override
    protected final void doCancelFragment(boolean userCancel)
    {
        if (itsNewTask != null) {
            NewTask task = itsNewTask;
            itsNewTask = null;
            task.cancel(false);
        }
        GuiUtils.setKeyboardVisible(itsPasswordInput, getActivity(), false);
        if (userCancel && itsListener != null) {
            itsListener.handleFileNewCanceled();
        }
    }

    /** Enable/disable field controls during background operations */
    @Override
    protected final void setFieldsEnabled(boolean enabled)
    {
        itsFileNameInput.setEnabled(enabled);
        itsPasswordInput.setEnabled(enabled);
        itsPasswordConfirmInput.setEnabled(enabled);
        if (enabled) {
            itsValidator.validate();
        } else {
            itsOkBtn.setEnabled(false);
        }
    }

    /**
     * Handle when the new task is finished
     */
    private void newTaskFinished(Object result, PasswdFileUri fileUri)
    {
        if (itsNewTask == null) {
            return;
        }
        itsNewTask = null;

        if (result == null) {
            cancelFragment(false);
            return;
        }

        if (result instanceof PasswdFileData) {
            itsListener.handleFileNew((PasswdFileData)result);
        } else {
            Exception e = (Exception) result;
            PasswdSafeUtil.showFatalMsg(
                    e, getString(R.string.cannot_create_file, fileUri),
                    getActivity());
        }
    }

    /**
     * Class to validate fields in the fragment
     */
    private class Validator
    {
        private final TextWatcher itsTextWatcher = new AbstractTextWatcher()
        {
            @Override
            public void afterTextChanged(Editable s)
            {
                validate();
            }
        };

        /**
         * Register a text view with the validator to revalidate on text change
         */
        public void registerTextView(TextView tv)
        {
            tv.addTextChangedListener(itsTextWatcher);
        }

        /**
         * Validate the fragment
         */
        public final void validate()
        {
            boolean isError;

            CharSequence fileName = itsFileName.getText();
            isError = GuiUtils.setTextInputError(
                    validateFileName(fileName.toString()), itsFileNameInput);

            CharSequence passwd = itsPassword.getText();
            isError |= GuiUtils.setTextInputError(
                    (passwd.length() == 0) ?
                            getString(R.string.empty_password) : null,
                    itsPasswordInput);

            CharSequence confirm = itsPasswordConfirm.getText();
            isError |= GuiUtils.setTextInputError(
                    !TextUtils.equals(passwd, confirm) ?
                            getString(R.string.passwords_do_not_match) : null,
                    itsPasswordConfirmInput);

            itsOkBtn.setEnabled(!isError);
        }

        /**
         * Validate the file name
         * @return error message if invalid; null if valid
         */
        private String validateFileName(String fileName)
        {
            if (!fileName.endsWith(itsPsafe3Sfx)) {
                return getString(R.string.invalid_file_name);
            }
            
            String fileNameBase = fileName.substring(
                    0, fileName.length() - itsPsafe3Sfx.length());

            if (fileNameBase.length() == 0) {
                return getString(R.string.empty_file_name);
            } else {
                for (int i = 0; i < fileNameBase.length(); ++i) {
                    char c = fileNameBase.charAt(i);
                    if (!Character.isLetterOrDigit(c)) {
                        return getString(R.string.invalid_file_name);
                    }
                }
            }

            PasswdFileUri uri = getPasswdFileUri();
            if (uri != null) {
                String error = uri.validateNewChild(fileNameBase,
                                                    getActivity());
                if (error != null) {
                    return error;
                }
            }

            return null;
        }
    }

    /**
     * Background task for creating a new file
     */
    private class NewTask extends BackgroundTask<Object>
    {
        private final String itsFileName;
        private final StringBuilder itsPassword;
        private PasswdFileUri itsFileUri;

        /**
         * Constructor
         */
        public NewTask(String fileName, StringBuilder password)
        {
            itsFileName = fileName;
            itsPassword = password;
        }

        @Override
        protected Object doInBackground(Void... voids)
        {
            try {
                Context ctx = getContext();
                itsFileUri = getPasswdFileUri().createNewChild(itsFileName, ctx);
                PasswdFileData fileData = new PasswdFileData(itsFileUri);
                fileData.createNewFile(itsPassword, ctx);
                return fileData;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Object data)
        {
            super.onPostExecute(data);
            newTaskFinished(data, itsFileUri);
        }
    }
}
