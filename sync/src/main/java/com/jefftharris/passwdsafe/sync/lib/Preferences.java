/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

/**
 * The Preferences class manages preferences for the application
 */
public class Preferences
{
    private static final String PREF_NOTIF_SHOW_SYNC = "notifShowSyncPref";
    private static final boolean PREF_NOTIF_SHOW_SYNC_DEF = true;

    /**
     * Get the default shared preferences
     */
    public static SharedPreferences getSharedPrefs(Context ctx)
    {
        return PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    /**
     * Get the preference to show sync notifications
     */
    public static boolean getNotifShowSyncPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_NOTIF_SHOW_SYNC, PREF_NOTIF_SHOW_SYNC_DEF);
    }
}
