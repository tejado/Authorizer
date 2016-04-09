/*
 * Copyright (©) 2013-2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.lib;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.Html;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.jefftharris.passwdsafe.lib.view.GuiUtils;

/**
 *  The about dialog
 */
public class AboutDialog extends AppCompatDialogFragment
        implements DialogInterface.OnClickListener
{
    // TODO: Move passwdsafe about_fragment to lib once sync app able to use extra libs

    /** Create a new instance */
    public static AboutDialog newInstance(String extraLicenseInfo)
    {
        AboutDialog frag = new AboutDialog();
        Bundle args = new Bundle();
        args.putString("extraLicenses", extraLicenseInfo);
        frag.setArguments(args);
        return frag;
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
     */
    @SuppressLint("InflateParams")
    @Override
    public @NonNull
    Dialog onCreateDialog(Bundle savedInstanceState)
    {
        final String extraLicenseInfo =
                getArguments().getString("extraLicenses");

        Activity act = getActivity();
        LayoutInflater factory = LayoutInflater.from(act);
        View detailsView = factory.inflate(R.layout.fragment_about, null);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int pad = (int)(dm.density * 6);
        detailsView.setPadding(pad, pad, pad, pad);

        View fileDetails = detailsView.findViewById(R.id.file_details_group);
        GuiUtils.setVisible(fileDetails, false);
        String name = updateAboutFields(detailsView, extraLicenseInfo, act);

        AlertDialog.Builder builder = new AlertDialog.Builder(act)
            .setTitle(name)
            .setIcon(R.drawable.ic_action_info_dark)
            .setView(detailsView)
            .setPositiveButton(R.string.close, this);
        return builder.create();
    }

    /** Handle a click on the dialog button */
    public void onClick(DialogInterface dialog, int which)
    {
        dialog.dismiss();
    }

    /**
     * Update the fields of the about fragment
     */
    public static String updateAboutFields(View detailsView,
                                           final String extraLicenseInfo,
                                           Context ctx)
    {
        String name;
        StringBuilder version = new StringBuilder();
        PackageInfo pkgInfo = PasswdSafeUtil.getAppPackageInfo(ctx);
        if (pkgInfo != null) {
            name = ctx.getString(pkgInfo.applicationInfo.labelRes);
            version.append(pkgInfo.versionName);
        } else {
            name = null;
        }

        if (PasswdSafeUtil.DEBUG) {
            version.append(" (DEBUG)");
        }

        TextView tv = (TextView)detailsView.findViewById(R.id.version);
        tv.setText(version);
        tv = (TextView)detailsView.findViewById(R.id.build_id);
        tv.setText(BuildConfig.BUILD_ID);
        tv = (TextView)detailsView.findViewById(R.id.build_date);
        tv.setText(BuildConfig.BUILD_DATE);
        tv = (TextView)detailsView.findViewById(R.id.release_notes);
        tv.setText(
                Html.fromHtml(tv.getText().toString().replace("\n", "<br>")));

        ToggleButton btn =
                (ToggleButton)detailsView.findViewById(R.id.toggle_license);
        final TextView licenseView =
                (TextView)detailsView.findViewById(R.id.license);
        btn.setOnCheckedChangeListener(new OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked)
            {
                licenseView.setText(extraLicenseInfo);
                GuiUtils.setVisible(licenseView, isChecked);
            }
        });
        GuiUtils.setVisible(btn, !TextUtils.isEmpty(extraLicenseInfo));
        return name;
    }
}
