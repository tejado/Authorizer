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
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdFileUri;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.view.GuiUtils;
import com.jefftharris.passwdsafe.view.PasswordVisibilityMenuHandler;

/**
 *  Fragment for opening a file
 */
public class PasswdSafeOpenFileFragment extends Fragment
        implements OnClickListener, DialogInterface.OnCancelListener
{
    /** Listener interface for owning activity */
    public interface Listener
    {
        /** Set the open file */
        void setOpenFile(PasswdFileData passwdFile);
    }

    // TODO: common code between open and new fragments?

    private Listener itsListener;
    private Uri itsFileUri;
    private PasswdFileUri itsPasswdUri;
    private View itsRoot;
    private AsyncTask<Void, Void, Object> itsLoadTask = null;
    private ProgressDialog itsProgress = null;


    /** Create a new instance */
    public static PasswdSafeOpenFileFragment newInstance(Uri fileUri)
    {
        PasswdSafeOpenFileFragment frag = new PasswdSafeOpenFileFragment();
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
        itsFileUri = getArguments().getParcelable("uri");

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
        final Context ctx = getActivity();

        LayoutInflater factory = getActivity().getLayoutInflater();
        itsRoot = factory.inflate(R.layout.fragment_passwdsafe_open_file,
                                  container, false);

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(ctx);
        TextView tv = (TextView)itsRoot.findViewById(R.id.file);
        tv.setText(itsPasswdUri.getIdentifier(ctx, false));

        TextView passwdView = (TextView)itsRoot.findViewById(R.id.passwd_edit);
        PasswordVisibilityMenuHandler.set(passwdView);

        CheckBox cb = (CheckBox)itsRoot.findViewById(R.id.read_only);
        boolean writable = itsPasswdUri.isWritable();
        cb.setEnabled(writable);
        if (writable) {
            cb.setChecked(Preferences.getFileOpenReadOnlyPref(prefs));
        } else {
            cb.setChecked(true);
        }

        setErrorMsg(null);
        Button cancelBtn = (Button)itsRoot.findViewById(R.id.cancel);
        cancelBtn.setOnClickListener(this);
        Button okBtn = (Button)itsRoot.findViewById(R.id.ok);
        okBtn.setOnClickListener(this);

        GuiUtils.setupFragmentKeyboard(passwdView, passwdView, okBtn, ctx);

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
                (TextView)itsRoot.findViewById(R.id.passwd_edit);
        switch (v.getId()) {
        case R.id.cancel: {
            if (itsLoadTask == null) {
                GuiUtils.setKeyboardVisible(passwdView, act, false);
                act.onBackPressed();
            } else {
                cancelLoad();
            }
            break;
        }
        case R.id.ok: {
            GuiUtils.setKeyboardVisible(passwdView, act, false);

            CheckBox roCb = (CheckBox)itsRoot.findViewById(R.id.read_only);
            boolean ro;
            if (roCb.isEnabled()) {
                ro = roCb.isChecked();
                SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences(act);
                Preferences.setFileOpenReadOnlyPref(ro, prefs);
            } else {
                ro = true;
            }

            setErrorMsg(null);

            final StringBuilder passwd =
                    new StringBuilder(passwdView.getText().toString());
            final boolean readonly = ro;

            itsProgress = ProgressDialog.show(
                    act, PasswdSafeUtil.getAppTitle(act),
                    getString(R.string.loading_file,
                              itsPasswdUri.getIdentifier(act, false)),
                    true, true, this);

            itsLoadTask = new AsyncTask<Void, Void, Object>()
            {
                @Override
                protected Object doInBackground(Void... params)
                {
                    try {
                        PasswdFileData fileData =
                                new PasswdFileData(itsPasswdUri);
                        fileData.load(passwd, readonly, getActivity());
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
                            msg = e.toString();
                        }
                        setErrorMsg(msg);
                    }

                    cancelLoad();
                }
            };
            itsLoadTask.execute();
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
        cancelLoad();
    }


    /* (non-Javadoc)
     * @see android.content.DialogInterface.OnCancelListener#onCancel(android.content.DialogInterface)
     */
    @Override
    public void onCancel(DialogInterface dialog)
    {
        cancelLoad();
    }


    /** Cancel a load operation */
    private void cancelLoad()
    {
        if (itsLoadTask != null) {
            itsLoadTask.cancel(false);
            itsLoadTask = null;
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
