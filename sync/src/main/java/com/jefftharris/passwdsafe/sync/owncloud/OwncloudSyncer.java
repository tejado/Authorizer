/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.owncloud;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.sync.MainActivity;
import com.jefftharris.passwdsafe.sync.R;
import com.jefftharris.passwdsafe.sync.lib.AbstractLocalToRemoteSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer;
import com.jefftharris.passwdsafe.sync.lib.AbstractRemoteToLocalSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractRmSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncIOException;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.CertificateCombinedException;
import com.owncloud.android.lib.common.network.NetworkUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.resources.users.GetRemoteUserNameOperation;

/**
 * The OwncloudSyncer class encapsulates an ownCloud sync operation
 */
public class OwncloudSyncer extends AbstractProviderSyncer<OwnCloudClient>
{
    private static final String TAG = "OwncloudSyncer";

    private boolean itsIsAuthorized = false;

    /** Constructor */
    public OwncloudSyncer(OwnCloudClient client,
                          DbProvider provider,
                          SQLiteDatabase db,
                          SyncLogRecord logrec,
                          Context ctx)
    {
        super(client, provider, db, logrec, ctx, TAG);
        itsIsAuthorized = itsProviderClient.hasCredentials();
    }


    /** Check the result of an operation; An exception is thrown on an error */
    public static void checkOperationResult(RemoteOperationResult result,
                                            Context ctx)
            throws IOException
    {
        checkOperationResult(result, false, ctx);
    }


    /** Check the result of an operation; An exception is thrown on an error */
    public static void checkOperationResult(RemoteOperationResult result,
                                            boolean ignoreFileNotFound,
                                            Context ctx)
            throws IOException
    {
        boolean retry = false;
        if (result.isSuccess()) {
            return;
        } else if (ignoreFileNotFound &&
                (result.getCode() ==
                 RemoteOperationResult.ResultCode.FILE_NOT_FOUND)) {
            return;
        }

        if (result.getCode() ==
            RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED) {
            try {
                CertificateCombinedException certExc =
                        (CertificateCombinedException)result.getException();
                X509Certificate cert = certExc.getServerCertificate();
                String alias =
                        NetworkUtils.addCertToKnownServersStore(cert, ctx);
                OwncloudProvider.saveCertAlias(alias, ctx);
                retry = true;

                NotificationManager notifMgr =
                        (NotificationManager) ctx.getSystemService(
                                Context.NOTIFICATION_SERVICE);

                PendingIntent mainIntent = PendingIntent.getActivity(
                        ctx, 0,
                        new Intent(ctx, MainActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT);

                String title = ctx.getString(R.string.owncloud_cert_trusted);
                GuiUtils.showSimpleNotification(
                        notifMgr, ctx, R.drawable.ic_stat_app,
                        title, R.drawable.ic_launcher_sync,
                        cert.getSubjectDN().toString(), mainIntent, 0, true);

            } catch (Exception e) {
                Log.e(TAG, "Error saving certificate", e);
            }
        }
        String msg = String.format(Locale.US,
                                   "ownCloud ERROR result %s, HTTP code %d: %s",
                                   result.getCode(), result.getHttpCode(),
                                   result.getLogMessage());
        throw new SyncIOException(msg, result.getException(), retry);
    }


    /** Get whether the sync is authorized */
    public final boolean isAuthorized()
    {
        return itsIsAuthorized;
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer#performSync()
     */
    @Override
    protected List<AbstractSyncOper<OwnCloudClient>> performSync()
            throws Exception
    {
        syncDisplayName();
        HashMap<String, ProviderRemoteFile> owncloudFiles = getOwncloudFiles();
        updateDbFiles(owncloudFiles);
        return resolveSyncOpers();
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer#createLocalToRemoteOper(com.jefftharris.passwdsafe.sync.lib.DbFile)
     */
    @Override
    protected AbstractLocalToRemoteSyncOper<OwnCloudClient>
    createLocalToRemoteOper(DbFile dbfile)
    {
        return new OwncloudLocalToRemoteOper(dbfile);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer#createRemoteToLocalOper(com.jefftharris.passwdsafe.sync.lib.DbFile)
     */
    @Override
    protected AbstractRemoteToLocalSyncOper<OwnCloudClient>
    createRemoteToLocalOper(DbFile dbfile)
    {
        return new OwncloudRemoteToLocalOper(dbfile);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer#createRmFileOper(com.jefftharris.passwdsafe.sync.lib.DbFile)
     */
    @Override
    protected AbstractRmSyncOper<OwnCloudClient>
    createRmFileOper(DbFile dbfile)
    {
        return new OwncloudRmFileOper(dbfile);
    }


    /** Sync the display name of the user */
    private void syncDisplayName()
            throws IOException
    {
        GetRemoteUserNameOperation oper = new GetRemoteUserNameOperation();
        RemoteOperationResult res = oper.execute(itsProviderClient);
        checkOperationResult(res, itsContext);

        PasswdSafeUtil.dbginfo(TAG, "syncDisplayName %s", oper.getUserName());
        StringBuilder displayName = new StringBuilder(oper.getUserName());
        displayName.append(" (");
        displayName.append(itsProviderClient.getBaseUri().toString());
        displayName.append(")");
        SyncDb.updateProviderDisplayName(itsProvider.itsId,
                                         displayName.toString(), itsDb);
    }


    /** Get the remote ownCloud files to sync */
    private HashMap<String, ProviderRemoteFile> getOwncloudFiles()
            throws IOException
    {
        HashMap<String, ProviderRemoteFile> files =
                new HashMap<String, ProviderRemoteFile>();

        List<DbFile> dbfiles = SyncDb.getFiles(itsProvider.itsId, itsDb);
        for (DbFile dbfile: dbfiles) {
            if (dbfile.itsRemoteId == null) {
                continue;
            }

            switch (dbfile.itsRemoteChange) {
            case NO_CHANGE:
            case ADDED:
            case MODIFIED: {
                ReadRemoteFileOperation fileOper =
                        new ReadRemoteFileOperation(dbfile.itsRemoteId);
                RemoteOperationResult fileRes =
                        fileOper.execute(itsProviderClient);
                checkOperationResult(fileRes, true, itsContext);
                if (fileRes.getData() == null) {
                    continue;
                }
                for (Object fileObj: fileRes.getData()) {
                    RemoteFile remfile = (RemoteFile)fileObj;
                    if (!OwncloudProviderFile.isPasswordFile(remfile)) {
                        continue;
                    }
                    PasswdSafeUtil.dbginfo(
                            TAG, "owncloud file: %s",
                            OwncloudProviderFile.fileToString(remfile));
                    files.put(remfile.getRemotePath(),
                              new OwncloudProviderFile(remfile));
                }
                break;
            }
            case REMOVED: {
                break;
            }
            }
        }

        return files;
    }
}
