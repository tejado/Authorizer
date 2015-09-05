/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.owncloud;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;

import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.sync.ProviderFactory;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncedFilesActivity;
import com.jefftharris.passwdsafe.sync.lib.ProviderRemoteFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;

import java.util.ArrayList;
import java.util.List;

/**
 *  Activity for managing files synced from ownCloud
 */
public class OwncloudFilesActivity extends AbstractSyncedFilesActivity
{
    private OwnCloudClient itsClient = null;
    private AsyncTask<Void, Void, OwnCloudClient> itsClientLoadTask;


    /** Constructor */
    public OwncloudFilesActivity()
    {
        super(ProviderType.OWNCLOUD);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle args)
    {
        super.onCreate(args);
        itsClientLoadTask = new AsyncTask<Void, Void, OwnCloudClient>()
        {
            @Override
            protected OwnCloudClient doInBackground(Void... params)
            {
                Context ctx = OwncloudFilesActivity.this;
                OwncloudProvider provider = (OwncloudProvider)
                        ProviderFactory.getProvider(ProviderType.OWNCLOUD, ctx);
                return provider.getClient(ctx);
            }

            @Override
            protected void onPostExecute(OwnCloudClient result)
            {
                itsClient = result;
                reloadFiles();
            }
        };
        itsClientLoadTask.execute();
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onDestroy()
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        itsClientLoadTask.cancel(true);
    }


    /** Create a list files task */
    @Override
    protected AbstractListFilesTask createListFilesTask(
            Context ctx,
            AbstractListFilesTask.Callback cb)
    {
        return new ListFilesTask(itsClient, ctx, cb);
    }


    /** Background task for listing files from ownCloud */
    private static class ListFilesTask extends AbstractListFilesTask
    {
        private final OwnCloudClient itsClient;


        /** Constructor */
        public ListFilesTask(OwnCloudClient client, Context ctx, Callback cb)
        {
            super(ctx, cb);
            itsClient = client;
        }


        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
         */
        @Override
        protected Pair<List<ProviderRemoteFile>, Exception>
        doInBackground(String... params)
        {
            List<ProviderRemoteFile> files = new ArrayList<>();
            Pair<List<ProviderRemoteFile>, Exception> result =
                    Pair.create(files, (Exception)null);
            if (itsClient == null) {
                return result;
            }

            try {
                ReadRemoteFolderOperation oper =
                        new ReadRemoteFolderOperation(params[0]);
                RemoteOperationResult statusRes = oper.execute(itsClient);
                OwncloudSyncer.checkOperationResult(statusRes, itsContext);

                for (Object obj: statusRes.getData()) {
                    if (!(obj instanceof RemoteFile)) {
                        continue;
                    }
                    RemoteFile remfile = (RemoteFile)obj;

                    // Filter out the directory being listed
                    if (TextUtils.equals(params[0], remfile.getRemotePath())) {
                        continue;
                    }

                    if (OwncloudProviderFile.isFolder(remfile) ||
                            OwncloudProviderFile.isPasswordFile(remfile)) {
                        files.add(new OwncloudProviderFile(remfile));
                    }
                }
            } catch (Exception e) {
                result = Pair.create(null, e);
            }
            return result;
        }
    }
}
