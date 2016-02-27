/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.gdriveplay;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.ProviderSyncer;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;

import java.util.concurrent.TimeUnit;

/**
 * The GDrivePlaySyncer class encapsulates a Google Drive sync operation
 */
public class GDrivePlaySyncer
        extends ProviderSyncer<GoogleApiClient>
        implements GoogleApiClient.ConnectionCallbacks,
                   GoogleApiClient.OnConnectionFailedListener
{
    private static final String TAG = "GDrivePlaySyncer";

    /** Constructor */
    public GDrivePlaySyncer(String acctName,
                            DbProvider provider,
                            SQLiteDatabase db,
                            SyncLogRecord logrec,
                            Context ctx)
    {
        super(GDrivePlayProvider.createClient(ctx, acctName, null, null),
              provider, db, logrec, ctx, TAG);
        itsProviderClient.registerConnectionCallbacks(this);
        itsProviderClient.registerConnectionFailedListener(this);
    }

    /**
     * Sync the provider
     */
    public void sync()
    {
        ConnectionResult connResult =
                itsProviderClient.blockingConnect(30, TimeUnit.SECONDS);
        if (!connResult.isSuccess()) {
            PasswdSafeUtil.dbginfo(TAG, "Can't connect: %s", connResult);
            return;
        }

        DriveFolder root = Drive.DriveApi.getRootFolder(itsProviderClient);
        PendingResult<DriveApi.MetadataBufferResult> pendRes =
                root.listChildren(itsProviderClient);
        DriveApi.MetadataBufferResult res = pendRes.await(30, TimeUnit.SECONDS);
        try {
            MetadataBuffer buf = res.getMetadataBuffer();
            if (buf != null) {
                try {
                    PasswdSafeUtil.dbginfo(TAG, "root count: %d",
                                           buf.getCount());
                    for (Metadata meta: buf) {
                        PasswdSafeUtil.dbginfo(TAG, "root item: %s", meta);
                    }
                } finally {
                    buf.release();
                }
            }
        } finally {
            res.release();
        }
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        PasswdSafeUtil.dbginfo(TAG, "onConnected: %s", bundle);
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        PasswdSafeUtil.dbginfo(TAG, "onConnectionSuspended %d", i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        PasswdSafeUtil.dbginfo(TAG, "onConnectionFailed %s", connectionResult);
    }
}
