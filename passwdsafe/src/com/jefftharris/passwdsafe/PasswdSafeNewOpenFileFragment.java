/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileUri;
import com.jefftharris.passwdsafe.view.GuiUtils;
import com.jefftharris.passwdsafe.view.PasswordVisibilityMenuHandler;

/**
 *  Fragment for opening or specifying a new file
 */
public class PasswdSafeNewOpenFileFragment extends Fragment
        implements OnClickListener
{
    /** Listener interface for owning activity */
    public interface Listener
    {
    }


    private Listener itsListener;
    private Uri itsFileUri;
    private PasswdFileUri itsPasswdUri;
    private View itsRoot;


    /** Create a new instance */
    public static PasswdSafeNewOpenFileFragment newInstance(Uri fileUri)
    {
        PasswdSafeNewOpenFileFragment frag =
                new PasswdSafeNewOpenFileFragment();
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
        itsRoot = factory.inflate(
                R.layout.fragment_passwdsafe_newopen_file, container,
                false);

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(ctx);
        TextView tv = (TextView)itsRoot.findViewById(R.id.file);
        tv.setText(itsPasswdUri.getIdentifier(ctx, false));

        final TextView passwdView =
                (TextView)itsRoot.findViewById(R.id.passwd_edit);
        PasswordVisibilityMenuHandler.set(passwdView);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                GuiUtils.setKeyboardVisible(passwdView, ctx, true);
            } }, 250);

        CheckBox cb = (CheckBox)itsRoot.findViewById(R.id.read_only);
        if (itsPasswdUri.isWritable()) {
            cb.setEnabled(true);
            cb.setChecked(Preferences.getFileOpenReadOnlyPref(prefs));
        } else {
            cb.setEnabled(false);
            cb.setChecked(true);
        }

        Button cancelBtn = (Button)itsRoot.findViewById(R.id.cancel);
        cancelBtn.setOnClickListener(this);
        Button okBtn = (Button)itsRoot.findViewById(R.id.ok);
        okBtn.setOnClickListener(this);

        return itsRoot;
    }


    /* (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick(View v)
    {
        Activity act = getActivity();
        TextView passwdView =
                (TextView)itsRoot.findViewById(R.id.passwd_edit);
        switch (v.getId()) {
        case R.id.cancel: {
            GuiUtils.setKeyboardVisible(passwdView, act, false);
            act.onBackPressed();
            break;
        }
        case R.id.ok: {
            GuiUtils.setKeyboardVisible(passwdView, act, false);

            // TODO: finish open
            break;
        }
        }
    }
}
