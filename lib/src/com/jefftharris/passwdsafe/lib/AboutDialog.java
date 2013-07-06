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

/**
 *  The about dialog
 */
public class AboutDialog extends DialogFragment
{
    /* (non-Javadoc)
     * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Activity act = getActivity();
        String name;
        String version;
        PackageInfo pkgInfo = PasswdSafeUtil.getAppPackageInfo(act);
        if (pkgInfo != null) {
            name = getString(pkgInfo.applicationInfo.labelRes);
            version = pkgInfo.versionName;
        } else {
            name = null;
            version = null;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(act)
            .setTitle(name)
            .setIcon(android.R.drawable.ic_menu_info_details)
            .setMessage(getString(R.string.about_details,
                                  version, Rev.BUILD_ID, Rev.BUILD_DATE))
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
