/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.owncloud;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.lib.AbstractLocalToRemoteSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer;
import com.jefftharris.passwdsafe.sync.lib.AbstractRemoteToLocalSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractRmSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.NotifUtils;
import com.jefftharris.passwdsafe.sync.lib.SyncConnectivityResult;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncIOException;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;
import com.jefftharris.passwdsafe.sync.lib.SyncRemoteFiles;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.CertificateCombinedException;
import com.owncloud.android.lib.common.network.NetworkUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.resources.users.GetRemoteUserNameOperation;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;

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
                          SyncConnectivityResult connResult,
                          SQLiteDatabase db,
                          SyncLogRecord logrec,
                          Context ctx)
    {
        super(client, provider, connResult, db, logrec, ctx, TAG);
        itsIsAuthorized = itsProviderClient.hasCredentials();
    }

    /**
     * Get the account display name
     */
    public static String getDisplayName(OwnCloudClient client, Context ctx)
            throws IOException
    {
        GetRemoteUserNameOperation oper = new GetRemoteUserNameOperation();
        RemoteOperationResult res = oper.execute(client);
        checkOperationResult(res, ctx);

        return oper.getUserName() +
               " (" + client.getBaseUri().toString() + ")";
    }

    /** Check the result of an operation; An exception is thrown on an error */
    public static void checkOperationResult(RemoteOperationResult result,
                                            Context ctx)
            throws IOException
    {
        checkOperationResult(result, false, ctx);
    }


    /** Check the result of an operation; An exception is thrown on an error */
    private static void checkOperationResult(RemoteOperationResult result,
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
                @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
                CertificateCombinedException certExc =
                        (CertificateCombinedException)result.getException();
                X509Certificate cert = certExc.getServerCertificate();
                String alias =
                        NetworkUtils.addCertToKnownServersStore(cert, ctx);
                OwncloudProvider.saveCertAlias(alias, ctx);
                retry = true;

                NotifUtils.showNotif(NotifUtils.Type.OWNCLOUD_CERT_TRUSTED,
                                     cert.getSubjectDN().toString(), ctx);
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

    /**
     * Create a remote identifier from the local name of a file
     */
    public static String createRemoteIdFromLocal(DbFile dbfile)
    {
        return FileUtils.PATH_SEPARATOR + dbfile.itsLocalTitle;
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
        updateDbFiles(getOwncloudFiles());
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
        String displayName = itsConnResult.getDisplayName();
        PasswdSafeUtil.dbginfo(TAG, "syncDisplayName %s", displayName);
        if (!TextUtils.equals(itsProvider.itsDisplayName, displayName)) {
            SyncDb.updateProviderDisplayName(itsProvider.itsId, displayName,
                                             itsDb);
        }
    }


    /** Get the remote ownCloud files to sync */
    private SyncRemoteFiles getOwncloudFiles()
            throws IOException
    {
        SyncRemoteFiles files = new SyncRemoteFiles();

        List<DbFile> dbfiles = SyncDb.getFiles(itsProvider.itsId, itsDb);
        for (DbFile dbfile: dbfiles) {
            if (dbfile.itsRemoteId == null) {
                RemoteFile remfile =
                        getRemoteFile(createRemoteIdFromLocal(dbfile));
                if (remfile != null) {
                    PasswdSafeUtil.dbginfo(
                            TAG, "owncloud file for local: %s",
                            OwncloudProviderFile.fileToString(remfile));
                    files.addRemoteFileForNew(
                            dbfile.itsId, new OwncloudProviderFile(remfile));
                }
            } else {
                switch (dbfile.itsRemoteChange) {
                case NO_CHANGE:
                case ADDED:
                case MODIFIED: {
                    RemoteFile remfile = getRemoteFile(dbfile.itsRemoteId);
                    if (remfile != null) {
                        PasswdSafeUtil.dbginfo(
                                TAG, "owncloud file: %s",
                                OwncloudProviderFile.fileToString(remfile));
                        files.addRemoteFile(new OwncloudProviderFile(remfile));
                    }
                    break;
                }
                case REMOVED: {
                    break;
                }
                }
            }
        }

        return files;
    }

    /**
     * Get a remote file ownCloud
     */
    private RemoteFile getRemoteFile(String remoteId)
            throws IOException
    {
        ReadRemoteFileOperation fileOper =
                new ReadRemoteFileOperation(remoteId);
        RemoteOperationResult fileRes = fileOper.execute(itsProviderClient);
        checkOperationResult(fileRes, true, itsContext);
        if (fileRes.getData() != null) {
            for (Object fileObj : fileRes.getData()) {
                RemoteFile remfile = (RemoteFile)fileObj;
                if (!OwncloudProviderFile.isPasswordFile(remfile)) {
                    continue;
                }
                return remfile;
            }
        }
        return null;
    }
}
