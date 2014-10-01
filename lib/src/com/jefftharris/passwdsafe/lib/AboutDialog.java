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
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;

/**
 *  The about dialog
 */
public class AboutDialog extends DialogFragment
        implements DialogInterface.OnClickListener
{
    /* (non-Javadoc)
     * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Activity act = getActivity();
        String name;
        StringBuilder version = new StringBuilder();
        PackageInfo pkgInfo = PasswdSafeUtil.getAppPackageInfo(act);
        if (pkgInfo != null) {
            name = getString(pkgInfo.applicationInfo.labelRes);
            version.append(pkgInfo.versionName);
        } else {
            name = null;
        }

        if (PasswdSafeUtil.DEBUG) {
            version.append(" (DEBUG)");
        }

        String msg = getString(R.string.about_details,
                               version, Rev.BUILD_ID, Rev.BUILD_DATE,
                               getString(R.string.release_notes_title),
                               getString(R.string.release_notes));
        msg = msg.replace("\n", "<br>");

        AlertDialog.Builder builder = new AlertDialog.Builder(act)
            .setTitle(name)
            .setIcon(android.R.drawable.ic_menu_info_details)
            .setMessage(Html.fromHtml(msg))
            .setPositiveButton(R.string.close, this);
        return builder.create();
    }

    /** Handle a click on the dialog button */
    public void onClick(DialogInterface dialog, int which)
    {
        dialog.dismiss();
    }
}
