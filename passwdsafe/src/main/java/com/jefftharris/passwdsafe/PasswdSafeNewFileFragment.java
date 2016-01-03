/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
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
import com.jefftharris.passwdsafe.lib.ApiCompat;
import com.jefftharris.passwdsafe.lib.DocumentsContractCompat;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.view.AbstractTextWatcher;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.view.PasswordVisibilityMenuHandler;
import com.jefftharris.passwdsafe.view.TextInputUtils;


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
    private final Validator itsValidator = new Validator();
    private NewTask itsNewTask;
    private boolean itsUseStorage = false;

    private static final String ARG_URI = "uri";

    private static final int CREATE_DOCUMENT_REQUEST = 0;

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
            Uri uri = args.getParcelable(ARG_URI);
            itsUseStorage = (uri == null);
            setFileUri(uri);
            setDoResolveOnStart(!itsUseStorage);
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
        if (itsUseStorage) {
            itsTitle.setText(R.string.new_file);
        }
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
            String fileName = itsFileName.getText().toString();
            if (itsUseStorage) {
                Intent createIntent = new Intent(
                        DocumentsContractCompat.INTENT_ACTION_CREATE_DOCUMENT);

                // Filter to only show results that can be "opened", such as
                // a file (as opposed to a list of contacts or timezones).
                createIntent.addCategory(Intent.CATEGORY_OPENABLE);

                // Create a file with the requested MIME type and name.
                createIntent.setType("application/psafe3");
                createIntent.putExtra(Intent.EXTRA_TITLE, fileName);

                startActivityForResult(createIntent, CREATE_DOCUMENT_REQUEST);
             } else {
                itsNewTask = new NewTask(
                        fileName, new StringBuilder(itsPassword.getText()));
                itsNewTask.execute();
            }
            break;
        }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode) {
        case CREATE_DOCUMENT_REQUEST: {
            if (resultCode != Activity.RESULT_OK) {
                cancelFragment(true);
                break;
            }

            Context ctx = getContext();
            Uri newUri = data.getData();
            String title = RecentFilesDb.getSafDisplayName(newUri, ctx);
            String error = validateFileName(title);
            if (error != null) {
                ContentResolver cr = ctx.getContentResolver();
                ApiCompat.documentsContractDeleteDocument(cr, newUri);
                String fileError = getString(R.string.cannot_create_file,
                                             title);
                PasswdSafeUtil.showFatalMsg(
                        String.format("%s - %s", fileError, error),
                        getActivity());
                break;
            }

            RecentFilesDb.updateOpenedSafFile(
                    newUri, (Intent.FLAG_GRANT_READ_URI_PERMISSION |
                             Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
                    ctx);
            if (title != null) {
                RecentFilesDb recentFilesDb = new RecentFilesDb(ctx);
                try {
                    recentFilesDb.insertOrUpdateFile(newUri, title);
                } finally {
                    recentFilesDb.close();
                }
            }
            setFileUri(newUri);
            itsFileName.setText(title);
            startResolve();
            break;
        }
        default: {
            super.onActivityResult(requestCode, resultCode, data);
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
        if (itsUseStorage) {
            String fileName = itsFileName.getText().toString();
            itsNewTask = new NewTask(
                    fileName, new StringBuilder(itsPassword.getText()));
            itsNewTask.execute();
        } else {
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
                    PasswdSafeUtil.showFatalMsg("Unknown sync type",
                                                getActivity());
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
            String error = uri.validateNewChild(fileNameBase, getContext());
            if (error != null) {
                return error;
            }
        }

        return null;
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
            isError = TextInputUtils.setTextInputError(
                    validateFileName(fileName.toString()), itsFileNameInput);

            CharSequence passwd = itsPassword.getText();
            isError |= TextInputUtils.setTextInputError(
                    (passwd.length() == 0) ?
                            getString(R.string.empty_password) : null,
                    itsPasswordInput);

            CharSequence confirm = itsPasswordConfirm.getText();
            isError |= TextInputUtils.setTextInputError(
                    !TextUtils.equals(passwd, confirm) ?
                            getString(R.string.passwords_do_not_match) : null,
                    itsPasswordConfirmInput);

            itsOkBtn.setEnabled(!isError);
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
                if (itsUseStorage) {
                    itsFileUri = getPasswdFileUri();
                } else {
                    itsFileUri = getPasswdFileUri().createNewChild(itsFileName,
                                                                   ctx);
                }
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
