/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.owncloud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.accounts.AccountTypeUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
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


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer#performSync()
     */
    @Override
    protected List<AbstractSyncOper<OwnCloudClient>> performSync()
            throws Exception
    {
        syncDisplayName();
        syncFiles();

        List<AbstractSyncOper<OwnCloudClient>> opers =
                new ArrayList<AbstractSyncOper<OwnCloudClient>>();
        return opers;
    }


    /** Sync the files from the server */
    private void syncFiles()
    {
        //HashMap<String, RemoteFile> owncloudFiles = getOwncloudFiles();
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


//    private HashMap<String, RemoteFile> getOwncloudFiles()
//    {
//        HashMap<String, RemoteFile> files = new HashMap<String, RemoteFile>();
//
//
//
//        return files;
//    }


    /** Check the result of an operation; An exception is thrown on an error */
    private void checkOperationResult(RemoteOperationResult result)
            throws IOException
    {
        if (result.isSuccess()) {
            return;
        }

        // TODO i18n msg
        String msg = String.format(
                "ownCloud error result %s, HTTP code %d: %s",
                result.getCode(), result.getHttpCode(),
                result.getLogMessage());
        throw new IOException(msg, result.getException());
    }


    /** Set the credentials for the client */
    private void setCredentials(Account account, String userName)
    {
        itsProviderClient.clearCredentials();
        AccountManager acctMgr = AccountManager.get(itsContext);
        try {
            String authToken = acctMgr.blockingGetAuthToken(
                    account,
                    AccountTypeUtils.getAuthTokenTypePass(
                            SyncDb.OWNCLOUD_ACCOUNT_TYPE),
                    true);
            PasswdSafeUtil.dbginfo(TAG, "setCredentials %b",
                                   (authToken != null));
            if (authToken != null) {
                itsProviderClient.setCredentials(
                        OwnCloudCredentialsFactory.newBasicCredentials(
                                userName, authToken));
            }
            // TODO if null returned, need message for user to handle notif
        } catch (Exception e) {
            PasswdSafeUtil.dbginfo(TAG, e, "setCredentials");
        }
    }


    /** Create a ownCloud client to a server */
    private static OwnCloudClient getClient(Uri serverUri, Context ctx)
    {
        return OwnCloudClientFactory.createOwnCloudClient(serverUri, ctx, true);
    }
}
