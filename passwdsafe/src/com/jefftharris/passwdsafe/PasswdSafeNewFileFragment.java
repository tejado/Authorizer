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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileUri;
import com.jefftharris.passwdsafe.view.DialogValidator;
import com.jefftharris.passwdsafe.view.GuiUtils;
import com.jefftharris.passwdsafe.view.PasswordVisibilityMenuHandler;

/**
 *  Fragment for creating a new file
 */
public class PasswdSafeNewFileFragment extends Fragment implements
        OnClickListener
{
    /** Listener interface for owning activity */
    public interface Listener
    {
    }


    private Listener itsListener;
    private Uri itsFileUri;
    private PasswdFileUri itsPasswdUri;
    private View itsRoot;
    private DialogValidator itsValidator;


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
        Activity act = getActivity();
        TextView passwdView =
                (TextView)itsRoot.findViewById(R.id.password);
        switch (v.getId()) {
        case R.id.cancel: {
            GuiUtils.setKeyboardVisible(passwdView, act, false);
            act.onBackPressed();
            break;
        }
        case R.id.ok: {
            GuiUtils.setKeyboardVisible(passwdView, act, false);

            // TODO: keyboard 'go' support
            // TODO: finish new
            break;
        }
        }
    }
}
