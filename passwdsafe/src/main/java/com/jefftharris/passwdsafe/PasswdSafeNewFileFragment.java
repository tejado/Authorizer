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
import android.os.Bundle;
import android.text.Editable;
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
    private TextView itsTitle;
    private EditText itsFileName;
    private TextView itsPasswordEdit;
    private TextView itsPasswordConfirm;
    private Button itsOkBtn;

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
            setFileUri((Uri)args.getParcelable("uri"));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_passwdsafe_new_file,
                                         container, false);
        setupView(rootView);

        itsTitle = (TextView)rootView.findViewById(R.id.title);
        itsFileName = (EditText)rootView.findViewById(R.id.file_name);
        itsFileName.setSelection(0);
        itsFileName.addTextChangedListener(new TextWatcher()
        {
            private final String itsSuffix;

            {
                itsSuffix = getString(R.string.psafe3_ext);
            }

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
                if (!s.toString().endsWith(itsSuffix)) {
                    s.replace(0, s.length(), itsSuffix);
                    itsFileName.setSelection(0);
                }
            }
        });

        itsPasswordEdit = (TextView)rootView.findViewById(R.id.password);
        itsPasswordConfirm =
                (TextView)rootView.findViewById(R.id.password_confirm);
        PasswordVisibilityMenuHandler.set(itsPasswordEdit, itsPasswordConfirm);
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
    public void onResume()
    {
        super.onResume();
        itsListener.updateViewFileNew();
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
        GuiUtils.setKeyboardVisible(itsPasswordEdit, getActivity(), false);
        if (userCancel && itsListener != null) {
            itsListener.handleFileNewCanceled();
        }
    }

    /** Enable/disable field controls during background operations */
    @Override
    protected final void setFieldsEnabled(boolean enabled)
    {
        itsFileName.setEnabled(enabled);
        itsPasswordEdit.setEnabled(enabled);
        itsPasswordConfirm.setEnabled(enabled);
        itsOkBtn.setEnabled(enabled);
    }
}
