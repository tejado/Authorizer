/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import java.util.List;

import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.Pair;

import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;

/**
 * The GDriveLaunchActivity is used to open files from within the Google Drive
 * app
 */
public class GDriveLaunchActivity extends FragmentActivity
{
    private static final String TAG = "GDriveLaunchActivity";

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle args)
    {
        super.onCreate(args);

        Intent intent = getIntent();
        String action = intent.getAction();
        boolean doFinish = true;
        if (action.equals("com.google.android.apps.drive.DRIVE_OPEN")) {
            String fileId = intent.getStringExtra("resourceId");
            PasswdSafeUtil.dbginfo(TAG, "Open GDrive file %s", fileId);

            Pair<DbProvider, DbFile> rc = getFile(fileId);
            if (rc != null) {
                Uri uri = PasswdSafeContract.Providers.CONTENT_URI;
                Uri.Builder builder = uri.buildUpon();
                ContentUris.appendId(builder, rc.first.itsId);
                builder.appendPath(PasswdSafeContract.Files.TABLE);
                ContentUris.appendId(builder, rc.second.itsId);
                uri = builder.build();
                PasswdSafeUtil.dbginfo(TAG, "uri %s", uri);
                Intent viewIntent = PasswdSafeUtil.createOpenIntent(uri, null);
                try {
                    startActivity(viewIntent);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "Can not open file", e);
                }
            } else {
                PasswdSafeUtil.showFatalMsg(
                        getString(R.string.cant_launch_drive_file), this);
                doFinish = false;
            }
        }
        if (doFinish) {
            finish();
        }
    }


    /** Get the database info for the drive file */
    private final Pair<DbProvider, DbFile> getFile(String fileId)
    {
        Pair<DbProvider, DbFile> rc = null;
        SyncDb syncDb = SyncApp.acquireSyncDb(this);
        try {
            SQLiteDatabase db = syncDb.beginTransaction();
            List<DbProvider> providers = SyncDb.getProviders(db);
            for (DbProvider provider: providers) {
                if (provider.itsType == ProviderType.GDRIVE) {
                    DbFile file = SyncDb.getFileByRemoteId(provider.itsId,
                                                           fileId, db);
                    if (file != null) {
                        rc = new Pair<DbProvider, DbFile>(provider, file);
                    }
                    break;
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error opening Google Drive file: " + fileId, e);
        } finally {
            syncDb.endTransactionAndRelease();
        }
        return rc;
    }
}
