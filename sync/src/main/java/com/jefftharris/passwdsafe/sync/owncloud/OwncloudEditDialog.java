/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.owncloud;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.sync.ProviderFactory;
import com.jefftharris.passwdsafe.sync.R;
import com.jefftharris.passwdsafe.sync.lib.DialogValidator;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Dialog to edit an ownCloud account
 */
public class OwncloudEditDialog extends DialogFragment
        implements DialogInterface.OnClickListener
{
    private TextView itsUrlEdit;
    private DialogValidator itsValidator;

    /** Create an instance of the dialog */
    public static OwncloudEditDialog newInstance()
    {
        return new OwncloudEditDialog();
    }

    @Override
    @SuppressLint("InflateParams")
    public @NonNull
    Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Activity act = getActivity();
        LayoutInflater factory = LayoutInflater.from(act);
        View view = factory.inflate(R.layout.fragment_owncloud_edit_dialog,
                                    null);
        itsUrlEdit = (TextView)view.findViewById(R.id.url);

        AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.owncloud)
               .setView(view)
               .setPositiveButton(android.R.string.ok, this)
               .setNegativeButton(android.R.string.cancel, this);
        AlertDialog dialog = builder.create();
        itsValidator = new DialogValidator.AlertValidator(dialog, view, act)
        {
            @Override
            protected String doValidation()
            {
                try {
                    new URI(itsUrlEdit.getText().toString());
                } catch (URISyntaxException e) {
                    return e.getMessage();
                }
                return null;
            }
        };

        // Must set text before registering view so validation isn't
        // triggered right away
        OwncloudProvider provider = getOwncloudProvider();
        itsUrlEdit.setText(provider.getUrl().toString());
        itsValidator.registerTextView(itsUrlEdit);

        return dialog;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        itsValidator.reset();
    }

    /** Handle a click on the dialog button */
    public void onClick(DialogInterface dialog, int which)
    {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            OwncloudProvider provider = getOwncloudProvider();
            provider.setSettings(itsUrlEdit.getText().toString());
        }
        dialog.dismiss();
    }

    /** Get the ownCloud provider */
    private OwncloudProvider getOwncloudProvider()
    {
        return (OwncloudProvider)ProviderFactory.getProvider(
                ProviderType.OWNCLOUD, getActivity());
    }
}
