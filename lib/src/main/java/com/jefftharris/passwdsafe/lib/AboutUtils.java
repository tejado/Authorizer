/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.lib;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.jefftharris.passwdsafe.lib.view.GuiUtils;

/**
 * Utilities for about dialogs
 */
public class AboutUtils
{
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
        btn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
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
