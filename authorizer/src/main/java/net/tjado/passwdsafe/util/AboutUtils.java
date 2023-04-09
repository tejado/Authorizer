/*
 * Copyright (©) 2023 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import androidx.preference.PreferenceManager;

import net.tjado.passwdsafe.lib.PasswdSafeUtil;

/**
 * Utilities for about dialogs
 */
public class AboutUtils
{
    private static final String PREF_RELEASE_NOTES = "releaseNotes";
    private static final String PREF_BLUETOOTH_HELP = "bluetoothHelp";
    private static String itsAppVersion;

    /**
     * Check whether the app should show release notes on startup
     */
    public static boolean checkShowNotes(Context ctx)
    {
        if (itsAppVersion != null) {
            return false;
        }
        itsAppVersion = PasswdSafeUtil.getAppVersion(ctx);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String prefVersion = prefs.getString(PREF_RELEASE_NOTES, "");
        if (!itsAppVersion.equals(prefVersion)) {
            SharedPreferences.Editor prefEdit = prefs.edit();
            prefEdit.putString(PREF_RELEASE_NOTES, itsAppVersion);
            prefEdit.apply();
            return true;
        }
        return false;
    }

    public static boolean checkShowBluetoothHelp(Context ctx)
    {
        itsAppVersion = PasswdSafeUtil.getAppVersion(ctx);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String prefVersion = prefs.getString(PREF_BLUETOOTH_HELP, "");
        if (!itsAppVersion.equals(prefVersion)) {
            SharedPreferences.Editor prefEdit = prefs.edit();
            prefEdit.putString(PREF_BLUETOOTH_HELP, itsAppVersion);
            prefEdit.apply();
            return true;
        }
        return false;
    }

    public static String getVersion(final Activity act) {
        StringBuilder version = new StringBuilder();
        final PackageInfo pkgInfo = PasswdSafeUtil.getAppPackageInfo(act);
        if (pkgInfo != null) {
            version.append(pkgInfo.versionName);
        }

        if (PasswdSafeUtil.DEBUG) {
            version.append(" (DEBUG)");
        }

        return String.valueOf(version);
    }
}
