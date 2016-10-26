/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import net.tjado.passwdsafe.lib.view.GuiUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Utilities for about dialogs
 */
public class AboutUtils
{
    private static final String TAG = "AboutUtils";
    private static final String PREF_RELEASE_NOTES = "releaseNotes";

    private static String itsAppVersion;

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

    /**
     * Get the licenses
     */
    public static String getLicenses(Context ctx, String... assets)
    {
        StringBuilder licenses = new StringBuilder();
        AssetManager assetMgr = ctx.getResources().getAssets();
        for (String asset: assets) {
            licenses.append(asset).append(":\n");
            try {
                InputStream is = null;
                try {
                    is = assetMgr.open(asset);
                    BufferedReader r =
                            new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = r.readLine()) != null) {
                        licenses.append(line).append("\n");
                    }
                } finally {
                    Utils.closeStreams(is, null);
                }
            } catch (Exception e) {
                Log.e(TAG, "Can't load asset: " + asset, e);
            }
            licenses.append("\n\n\n");
        }
        return licenses.toString();
    }

    /**
     * Check whether the app should show release notes on startup
     */
    public static boolean checkShowNotes(Context ctx)
    {
        if (itsAppVersion != null) {
            return false;
        }
        itsAppVersion = PasswdSafeUtil.getAppVersion(ctx);
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(ctx);
        String prefVersion = prefs.getString(PREF_RELEASE_NOTES, "");
        if (!itsAppVersion.equals(prefVersion)) {
            SharedPreferences.Editor prefEdit = prefs.edit();
            prefEdit.putString(PREF_RELEASE_NOTES, itsAppVersion);
            prefEdit.apply();
            return true;
        }
        return false;
    }
}
