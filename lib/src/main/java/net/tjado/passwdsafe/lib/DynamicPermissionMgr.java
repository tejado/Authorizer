/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.view.View;

/**
 *  The DynamicPermissionMgr class manages dynamic permissions
 */
public class DynamicPermissionMgr implements View.OnClickListener
{
    private final String itsPerm;
    private final Activity itsActivity;
    private final int itsPermsRequestCode;
    private final int itsAppSettingsRequestCode;
    private final String itsPackageName;
    private final View itsReloadBtn;
    private final View itsAppSettingsBtn;

    /**
     * Constructor
     */
    public DynamicPermissionMgr(String perm,
                                Activity act,
                                int permsRequestCode,
                                int appSettingsRequestCode,
                                String packageName,
                                int reloadId,
                                int appSettingsId)
    {
        itsPerm = perm;
        itsActivity = act;
        itsPermsRequestCode = permsRequestCode;
        itsAppSettingsRequestCode = appSettingsRequestCode;
        itsPackageName = packageName;

        itsReloadBtn = act.findViewById(reloadId);
        itsReloadBtn.setOnClickListener(this);
        itsAppSettingsBtn = act.findViewById(appSettingsId);
        itsAppSettingsBtn.setOnClickListener(this);
    }

    /**
     * Get whether permissions have been granted
     */
    public boolean hasPerms()
    {
        return ContextCompat.checkSelfPermission(itsActivity, itsPerm) ==
               PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check whether permissions are granted
     */
    public boolean checkPerms()
    {
        boolean perms = hasPerms();
        if (!perms) {
            ActivityCompat.requestPermissions(
                    itsActivity, new String[] { itsPerm }, itsPermsRequestCode);
        }
        return perms;
    }

    /**
     * Handle an activity result
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean handleActivityResult(int requestCode)
    {
        if (requestCode == itsAppSettingsRequestCode) {
            restart();
            return true;
        }
        return false;
    }

    /**
     * Handle a permissions result
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean handlePermissionsResult(int requestCode,
                                           @NonNull int[] grantResults)
    {
        if (requestCode == itsPermsRequestCode) {
            if ((grantResults.length > 0) &&
                (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                restart();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v)
    {
        int id = v.getId();
        if (id == itsReloadBtn.getId()) {
            checkPerms();
        } else if (id == itsAppSettingsBtn.getId()) {
            Intent intent = new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + itsPackageName));
            if (intent.resolveActivity(itsActivity.getPackageManager()) !=
                null) {
                itsActivity.startActivityForResult(intent,
                                                   itsAppSettingsRequestCode);
            }
        }
    }

    /**
     * Restart the app
     */
    private void restart()
    {
        ApiCompat.recreateActivity(itsActivity);
        //System.exit(0);
    }
}
