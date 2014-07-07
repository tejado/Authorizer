/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.IOException;

import org.pwsafe.lib.exception.InvalidPassphraseException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdFileUri;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.view.DialogValidator;
import com.jefftharris.passwdsafe.view.GuiUtils;
import com.jefftharris.passwdsafe.view.PasswordVisibilityMenuHandler;

/**
 *  Fragment for creating a new file
 */
public class PasswdSafeNewFileFragment extends Fragment implements
        OnClickListener, DialogInterface.OnCancelListener
{
    /** Listener interface for owning activity */
    public interface Listener
    {
        /** Set the open file */
        void setOpenFile(PasswdFileData passwdFile);
    }


    private Listener itsListener;
    private Uri itsFileUri;
    private PasswdFileUri itsPasswdUri;
    private View itsRoot;
    private DialogValidator itsValidator;
    private AsyncTask<Void, Void, Object> itsNewTask = null;
    private ProgressDialog itsProgress = null;


    /** Create a new instance */
    public static PasswdSafeNewFileFragment newInstance(Uri fileUri)
    {
        PasswdSafeNewFileFragment frag = new PasswdSafeNewFileFragment();
        Bundle args = new Bundle();
        args.putParcelable("uri", fileUri);
        frag.setArguments(args);
        return frag;
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        itsFileUri = args.getParcelable("uri");

        Uri.Builder builder = itsFileUri.buildUpon();
        builder.fragment("");
        builder.query("");
        itsPasswdUri = new PasswdFileUri(builder.build(), getActivity());
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onAttach(android.app.Activity)
     */
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        itsListener = (Listener)activity;
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        Context ctx = getActivity();

        LayoutInflater factory = getActivity().getLayoutInflater();
        itsRoot = factory.inflate(R.layout.fragment_passwdsafe_new_file,
                                  container, false);

        TextView titleView = (TextView)itsRoot.findViewById(R.id.title);
        TextView locationView =
                (TextView)itsRoot.findViewById(R.id.location);

        int titleId = R.string.new_file;
        String locationStr = null;
        switch (itsPasswdUri.getType()) {
        case FILE: {
            titleId = R.string.new_local_file;
            locationStr = itsPasswdUri.getIdentifier(ctx, false);
            break;
        }
        case SYNC_PROVIDER: {
            switch (itsPasswdUri.getSyncType()) {
            case GDRIVE: {
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
            }
        }
        case EMAIL:
        case GENERIC_PROVIDER: {
            break;
        }
        }

        titleView.setText(titleId);
        locationView.setText(locationStr);
        locationView.setVisibility(
                (locationStr != null) ? View.VISIBLE : View.GONE);

        final TextView filenameView =
                (TextView)itsRoot.findViewById(R.id.file_name);
        final TextView passwdView =
                (TextView)itsRoot.findViewById(R.id.password);
        TextView confirmView =
                (TextView)itsRoot.findViewById(R.id.password_confirm);
        PasswordVisibilityMenuHandler.set(passwdView, confirmView);

        setErrorMsg(null);
        Button cancelBtn = (Button)itsRoot.findViewById(R.id.cancel);
        cancelBtn.setOnClickListener(this);
        Button okBtn = (Button)itsRoot.findViewById(R.id.ok);
        okBtn.setOnClickListener(this);

        GuiUtils.setupFragmentKeyboard(filenameView, confirmView,
                                       okBtn, ctx);

        itsValidator = new DialogValidator.FragmentValidator(itsRoot, okBtn,
                                                             true, ctx)
        {
            @Override
            protected final String doValidation()
            {
                CharSequence fileName = filenameView.getText();
                if (fileName.length() == 0) {
                    return getString(R.string.empty_file_name);
                }

                for (int i = 0; i < fileName.length(); ++i) {
                    char c = fileName.charAt(i);
                    if ((c == '/') || (c == '\\')) {
                        return getString(R.string.invalid_file_name);
                    }
                }

                String error = itsPasswdUri.validateNewChild(
                        fileName.toString(), getContext());
                if (error != null) {
                    return error;
                }

                return super.doValidation();
            }
        };
        itsValidator.registerTextView(filenameView);
        itsValidator.reset();

        return itsRoot;
    }


    /* (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick(View v)
    {
        final Activity act = getActivity();
        TextView passwdView =
                (TextView)itsRoot.findViewById(R.id.password);
        switch (v.getId()) {
        case R.id.cancel: {
            if (itsNewTask == null) {
                GuiUtils.setKeyboardVisible(passwdView, act, false);
                act.onBackPressed();
            } else {
                cancelNew();
            }
            break;
        }
        case R.id.ok: {
            GuiUtils.setKeyboardVisible(passwdView, act, false);
            setErrorMsg(null);
            TextView fileNameView =
                    (TextView)itsRoot.findViewById(R.id.file_name);
            final String fileName = fileNameView.getText().toString();
            final StringBuilder passwd =
                    new StringBuilder(passwdView.getText().toString());
            itsProgress = ProgressDialog.show(
                    act, PasswdSafeUtil.getAppTitle(act),
                    getString(R.string.new_file), true, true, this);
            itsNewTask = new AsyncTask<Void, Void, Object>()
            {
                @Override
                protected Object doInBackground(Void... params)
                {
                    try {
                        PasswdFileUri childUri = itsPasswdUri.createNewChild(
                                fileName + ".psafe3", act);
                        PasswdFileData fileData = new PasswdFileData(childUri);
                        fileData.createNewFile(passwd, act);
                        return fileData;
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
                    if (!(result instanceof Exception)) {
                        setErrorMsg(null);
                        itsListener.setOpenFile((PasswdFileData)result);
                    } else {
                        String msg;
                        Exception e = (Exception)result;
                        if (((e instanceof IOException) &&
                             TextUtils.equals(e.getMessage(),
                                              "Invalid password")) ||
                            (e instanceof InvalidPassphraseException)) {
                            msg = getString(R.string.invalid_password);
                        } else {
                            msg = getString(R.string.cannot_create_file,
                                            itsPasswdUri);
                        }
                        setErrorMsg(msg);
                    }

                    cancelNew();
                }
            };
            itsNewTask.execute();
            break;
        }
        }
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onPause()
     */
    @Override
    public void onPause()
    {
        super.onPause();
        cancelNew();
    }


    /* (non-Javadoc)
     * @see android.content.DialogInterface.OnCancelListener#onCancel(android.content.DialogInterface)
     */
    @Override
    public void onCancel(DialogInterface dialog)
    {
        cancelNew();
    }


    /** Cancel a new operation */
    private void cancelNew()
    {
        if (itsNewTask != null) {
            itsNewTask.cancel(false);
            itsNewTask = null;
        }

        if (itsProgress != null) {
            itsProgress.dismiss();
            itsProgress = null;
        }
    }


    /** Set the error message */
    private void setErrorMsg(String msg)
    {
        TextView errorMsgView = (TextView)itsRoot.findViewById(R.id.error_msg);
        if (msg != null) {
            errorMsgView.setVisibility(View.VISIBLE);
            errorMsgView.setText(
                    Html.fromHtml(getString(R.string.error_msg, msg)));
        } else {
            errorMsgView.setVisibility(View.GONE);
        }
    }
}
