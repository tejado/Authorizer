/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.lib;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.Html;

/**
 * The release notes dialog
 */
public class ReleaseNotesDialog extends DialogFragment
{
    private static final String PREF_RELEASE_NOTES = "releaseNotes";
    private static String itsAppVersion;

    public static void checkNotes(FragmentActivity act)
    {
        if (itsAppVersion != null) {
            return;
        }
        itsAppVersion = PasswdSafeUtil.getAppVersion(act);
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(act);
        String prefVersion = prefs.getString(PREF_RELEASE_NOTES, "");
        if (!itsAppVersion.equals(prefVersion)) {
            ReleaseNotesDialog dlg = new ReleaseNotesDialog();
            dlg.show(act.getSupportFragmentManager(), "ReleaseNotesDialog");

            SharedPreferences.Editor prefEdit = prefs.edit();
            prefEdit.putString(PREF_RELEASE_NOTES, itsAppVersion);
            prefEdit.commit();
        }
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Activity act = getActivity();
        String notes =
                "<b>4.2.0</b> - An input method is available which can paste fields " +
                "from the last selected password record.  The input method " +
                "must be enabled in the system settings before it can be used. " +
                "<br><br>To use:<br>" +
                "- Select a record in PasswdSafe<br>" +
                "- Open the app which will use the record's fields<br>" +
                "- Switch to the PasswdSafe input method<br>" +
                "- Select the fields to paste values into the app<br>" +
                "- Select the PasswdSafe icon to choose a different record if needed<br>";

        AlertDialog.Builder builder = new AlertDialog.Builder(act)
            .setTitle("Release Notes")
            .setIcon(android.R.drawable.ic_menu_info_details)
            .setMessage(Html.fromHtml(notes))
            .setPositiveButton(R.string.close,
                               new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
        return builder.create();
    }
}
