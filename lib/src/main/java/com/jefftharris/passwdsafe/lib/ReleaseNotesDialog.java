/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.lib;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.text.Html;

/**
 * The release notes dialog
 */
public class ReleaseNotesDialog extends DialogFragment
        implements DialogInterface.OnClickListener
{
    private static final String PREF_RELEASE_NOTES = "releaseNotes";
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

    // TODO: remove when sync app uses about fragment
    public static void checkNotes(FragmentActivity act)
    {
        if (checkShowNotes(act)) {
            ReleaseNotesDialog dlg = new ReleaseNotesDialog();
            dlg.show(act.getSupportFragmentManager(), "ReleaseNotesDialog");
        }
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
     */
    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Activity act = getActivity();
        String notes = getString(R.string.release_notes);
        notes = notes.replace("\n", "<br>");

        AlertDialog.Builder builder = new AlertDialog.Builder(act)
            .setTitle(R.string.release_notes_title)
            .setIcon(R.drawable.ic_action_info_dark)
            .setMessage(Html.fromHtml(notes))
            .setPositiveButton(R.string.close, this);
        return builder.create();
    }

    /** Handle a click on the dialog button */
    public void onClick(DialogInterface dialog, int which)
    {
        dialog.dismiss();
    }
}
