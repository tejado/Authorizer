/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.owncloud;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.lib.AbstractLocalToRemoteSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer;
import com.jefftharris.passwdsafe.sync.lib.AbstractRemoteToLocalSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractRmSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.accounts.AccountTypeUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.resources.users.GetRemoteUserNameOperation;

/**
 * The OwncloudSyncer class encapsulates an ownCloud sync operation
 */
public class OwncloudSyncer extends AbstractProviderSyncer<OwnCloudClient>
{
    private static final String TAG = "OwncloudSyncer";

    /** Constructor */
    public OwncloudSyncer(Account account,
                          String userName,
                          Uri serverUri,
                          DbProvider provider,
                          SQLiteDatabase db,
                          SyncLogRecord logrec,
                          Context ctx)
    {
        super(getClient(serverUri, ctx), provider, db, logrec, ctx, TAG);
        setCredentials(account, userName);
    }


    /** Get the ownCloud authentication for an account.  If the activity is
     *  given, a dialog is shown if needed.  Otherwise, a notification may be
     *  presented. Must be called from a background thread. */
    @SuppressWarnings("deprecation")
    public static String getAuthToken(Account account,
                                      Context ctx,
                                      Activity activity)
    {
        String authToken = null;
        AccountManager acctMgr = AccountManager.get(ctx);
        String authType = AccountTypeUtils.getAuthTokenTypePass(
                SyncDb.OWNCLOUD_ACCOUNT_TYPE);
        AccountManagerFuture<Bundle> fut;
        if (activity != null) {
            fut = acctMgr.getAuthToken(account, authType, null,
                                       activity, null, null);
        } else {
            fut = acctMgr.getAuthToken(account, authType, true, null, null);
        }
        try {
            Bundle b = fut.getResult();
            authToken = b.getString(AccountManager.KEY_AUTHTOKEN);
        }
        catch (Exception e) {
            PasswdSafeUtil.dbginfo(TAG, e, "getAuthToken");
        }

        PasswdSafeUtil.dbginfo(TAG, "getAuthToken: %b", (authToken != null));
        return authToken;
    }


    /** Check the result of an operation; An exception is thrown on an error */
    public static void checkOperationResult(RemoteOperationResult result)
            throws IOException
    {
        if (result.isSuccess()) {
            return;
        }

        String msg = String.format(Locale.US,
                                   "ownCloud ERROR result %s, HTTP code %d: %s",
                                   result.getCode(), result.getHttpCode(),
                                   result.getLogMessage());
        throw new IOException(msg, result.getException());
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
        checkOperationResult(res);

        PasswdSafeUtil.dbginfo(TAG, "syncDisplayName %s", oper.getUserName());
        StringBuilder displayName = new StringBuilder(oper.getUserName());
        displayName.append(" (");
        displayName.append(itsProviderClient.getBaseUri().toString());
        displayName.append(")");
        SyncDb.updateProviderDisplayName(itsProvider.itsId,
                                         displayName.toString(), itsDb);
    }


    private HashMap<String, ProviderRemoteFile> getOwncloudFiles()
            throws IOException
    {
        // TODO: check files in other folders?
        ReadRemoteFolderOperation oper = new ReadRemoteFolderOperation(
                FileUtils.PATH_SEPARATOR);
        RemoteOperationResult res = oper.execute(itsProviderClient);
        checkOperationResult(res);

        HashMap<String, ProviderRemoteFile> files =
                new HashMap<String, ProviderRemoteFile>();
        for (Object obj: res.getData()) {
            RemoteFile remfile = (RemoteFile)obj;
            if (!isPasswordFile(remfile)) {
                continue;
            }
            PasswdSafeUtil.dbginfo(TAG, "owncloud file: %s",
                                   fileToString(remfile));
            files.put(remfile.getRemotePath(),
                      new OwncloudProviderFile(remfile));
        }
        return files;
    }


    /** Is a file a password file */
    private static boolean isPasswordFile(RemoteFile file)
    {
        return !isFolder(file) && file.getRemotePath().endsWith(".psafe3");
    }

    /** Is a file a folder */
    private static boolean isFolder(RemoteFile file)
    {
        return TextUtils.equals(file.getMimeType(), "DIR");
    }


    /** Get a string form for a remote file */
    private static String fileToString(RemoteFile file)
    {
        if (file == null) {
            return "{null}";
        }
        return String.format(Locale.US,
                             "{path:%s, mime:%s, hash:%s}",
                             file.getRemotePath(), file.getMimeType(),
                             file.getEtag());
    }


    /** Set the credentials for the client */
    private void setCredentials(Account account, String userName)
    {
        itsProviderClient.clearCredentials();
        String authToken = getAuthToken(account, itsContext, null);
        if (authToken != null) {
            itsProviderClient.setCredentials(
                    OwnCloudCredentialsFactory.newBasicCredentials(
                            userName, authToken));
        }
        // TODO if null returned, need message for user to handle notif
    }


    /** Create a ownCloud client to a server */
    private static OwnCloudClient getClient(Uri serverUri, Context ctx)
    {
        return OwnCloudClientFactory.createOwnCloudClient(serverUri, ctx, true);
    }
}
