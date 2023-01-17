/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *  The DynamicPermissionMgr class manages dynamic permissions
 */
public class DynamicPermissionMgr implements View.OnClickListener
{
    public static final String PERM_POST_NOTIFICATIONS =
            "android.permission.POST_NOTIFICATIONS";

    private final HashMap<String, Permission> itsPerms = new HashMap<>();
    private final Activity itsActivity;
    private final int itsPermsRequestCode;
    private final int itsAppSettingsRequestCode;
    private final String itsPackageName;
    private final View itsReloadBtn;
    private final View itsAppSettingsBtn;

    /**
     * Constructor
     */
    public DynamicPermissionMgr(Activity act,
                                int permsRequestCode,
                                int appSettingsRequestCode,
                                String packageName,
                                int reloadId,
                                int appSettingsId)
    {
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
     * Add a dynamic permission
     */
    public void addPerm(String perm, boolean required)
    {
        itsPerms.put(perm, new Permission(perm, required, itsActivity));
    }

    /**
     * Get whether required permissions have been granted
     */
    public boolean hasRequiredPerms()
    {
        boolean hasPerms = true;
        for (var entry: itsPerms.entrySet()) {
            var perm = entry.getValue();
            if (perm.itsIsRequired && !perm.itsIsGranted) {
                hasPerms = false;
                break;
            }
        }
        return hasPerms;
    }

    /**
     * Check whether permissions are granted
     * @return Whether the required permissions were granted
     */
    public boolean checkPerms()
    {
        ArrayList<String> checkPerms = null;
        for (var perm: itsPerms.entrySet()) {
            if (!perm.getValue().itsIsChecked) {
                if (checkPerms == null) {
                    checkPerms = new ArrayList<>();
                }
                checkPerms.add(perm.getKey());
            }
        }

        if (checkPerms != null) {
            ActivityCompat.requestPermissions(
                    itsActivity, checkPerms.toArray(new String[0]),
                    itsPermsRequestCode);
        }

        return hasRequiredPerms();
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
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        if (requestCode == itsPermsRequestCode) {
            boolean restart = false;
            for (int i = 0; i < permissions.length; ++i) {
                var perm = itsPerms.get(permissions[i]);
                if (perm != null) {
                    boolean granted = grantResults[i] ==
                                      PackageManager.PERMISSION_GRANTED;
                    if (perm.updateGranted(granted)) {
                        restart = true;
                    }
                }
            }

            if (restart) {
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
        itsActivity.recreate();
    }

    /**
     * A dynamic permission
     */
    private static class Permission
    {
        private final boolean itsIsRequired;
        private boolean itsIsGranted;
        private boolean itsIsChecked;

        protected Permission(String permission, boolean required, Context ctx)
        {
            itsIsRequired = required;

            if (TextUtils.equals(permission,
                                 Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                !ApiCompat.supportsWriteExternalStoragePermission()) {
                itsIsGranted = true;
            } else if (TextUtils.equals(permission, PERM_POST_NOTIFICATIONS) &&
                       !ApiCompat.supportsPostNotificationsPermission()) {
                itsIsGranted = true;
            } else {
                itsIsGranted =
                        ContextCompat.checkSelfPermission(ctx, permission) ==
                        PackageManager.PERMISSION_GRANTED;
            }

            itsIsChecked = itsIsGranted;
        }

        /**
         * Set whether the permission is granted after being checked
         * @return Whether the granted state changed
         */
        protected boolean updateGranted(boolean granted) {
            itsIsChecked = true;
            if (granted != itsIsGranted) {
                itsIsGranted = granted;
                return true;
            } else {
                return false;
            }
        }
    }
}
