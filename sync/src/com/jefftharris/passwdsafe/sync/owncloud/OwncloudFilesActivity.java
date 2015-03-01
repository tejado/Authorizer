/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.owncloud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.TextUtils;

import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.lib.view.PasswdCursorLoader;
import com.jefftharris.passwdsafe.sync.ProviderFactory;
import com.jefftharris.passwdsafe.sync.R;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;

/**
 *  Activity for managing files synced from ownCloud
 */
public class OwncloudFilesActivity extends FragmentActivity
        implements OwncloudFilesFragment.Listener
{
    public static final String INTENT_PROVIDER_URI = "provider_uri";

    private static final int LOADER_TITLE = 0;
    private static final int LOADER_FILES = 1;

    private static final String TAG = "OwncloudFilesActivity";

    private Uri itsProviderUri;
    private Uri itsFilesUri;

    // TODO: need to close client??
    private OwnCloudClient itsClient = null;
    private HashMap<String, DbFile> itsSyncedFiles =
            new HashMap<String, DbFile>();
    private AsyncTask<Void, Void, OwnCloudClient> itsClientLoadTask;


    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle args)
    {
        super.onCreate(args);
        setContentView(R.layout.activity_owncloud_files);

        itsProviderUri = getIntent().getParcelableExtra(INTENT_PROVIDER_URI);
        if (itsProviderUri == null) {
            PasswdSafeUtil.showFatalMsg("Required args missing", this);
            return;
        }

        itsFilesUri = itsProviderUri.buildUpon().appendPath(
                PasswdSafeContract.Files.TABLE).build();

        if (args == null) {
            changeDir(FileUtils.PATH_SEPARATOR);
        }

        LoaderManager lm = getSupportLoaderManager();
        lm.initLoader(LOADER_TITLE, null, new ProviderLoaderCb());
        lm.initLoader(LOADER_FILES, null, new FilesLoaderCb());

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
                synchronized (OwncloudFilesActivity.this) {
                    itsClient = result;
                }
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


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.owncloud.OwncloudFilesFragment.Listener#listFiles(java.lang.String, com.jefftharris.passwdsafe.sync.owncloud.OwncloudFilesFragment.Listener.ListFilesCb)
     */
    @Override
    public void listFiles(String path, final ListFilesCb cb)
    {
        PasswdSafeUtil.dbginfo(TAG, "listFiles client %b, path: %s",
                               (itsClient != null), path);
        new AsyncTask<String, Void, List<OwncloudProviderFile>>()
        {
            @Override
            protected List<OwncloudProviderFile>
            doInBackground(String... params)
            {
                List<OwncloudProviderFile> files =
                        new ArrayList<OwncloudProviderFile>();
                synchronized (OwncloudFilesActivity.this) {
                    if (itsClient == null) {
                        return files;
                    }

                    try {
                        ReadRemoteFolderOperation oper =
                                new ReadRemoteFolderOperation(params[0]);
                        RemoteOperationResult statusRes =
                                oper.execute(itsClient);
                        OwncloudSyncer.checkOperationResult(
                                statusRes, OwncloudFilesActivity.this);

                        for (Object obj: statusRes.getData()) {
                            if (!(obj instanceof RemoteFile)) {
                                continue;
                            }
                            RemoteFile remfile = (RemoteFile)obj;

                            // Filter out the directory being listed
                            if (TextUtils.equals(params[0],
                                                 remfile.getRemotePath())) {
                                continue;
                            }

                            if (OwncloudProviderFile.isFolder(remfile) ||
                                OwncloudProviderFile.isPasswordFile(remfile)) {
                                files.add(new OwncloudProviderFile(remfile));
                            }
                        }
                    } catch (IOException e) {
                        //  TODO: handle
                        PasswdSafeUtil.dbginfo(TAG, e, "Error listing files");
                    }
                }
                return files;
            }

            /* (non-Javadoc)
             * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
             */
            @Override
            protected void onPostExecute(List<OwncloudProviderFile> result)
            {
                cb.handleFiles(result);
            }

        }.execute(path);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.owncloud.OwncloudFilesFragment.Listener#changeDir(java.lang.String)
     */
    @Override
    public void changeDir(String path)
    {
        PasswdSafeUtil.dbginfo(TAG, "changeDir: %s", path);
        Fragment files = OwncloudFilesFragment.newInstance(path);
        FragmentManager fragmgr = getSupportFragmentManager();
        FragmentTransaction txn = fragmgr.beginTransaction();
        txn.replace(R.id.content, files);
        if (!TextUtils.equals(path, FileUtils.PATH_SEPARATOR)) {
            txn.addToBackStack(null);
        }
        txn.commit();
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.owncloud.OwncloudFilesFragment.Listener#changeParentDir()
     */
    public void changeParentDir()
    {
        PasswdSafeUtil.dbginfo(TAG, "changeParentDir");
        FragmentManager fragmgr = getSupportFragmentManager();
        fragmgr.popBackStack();
    }


    /** Reload the files shown by the activity */
    private void reloadFiles()
    {
        FragmentManager fragmgr = getSupportFragmentManager();
        Fragment filesfrag = fragmgr.findFragmentById(R.id.content);
        if (filesfrag instanceof OwncloudFilesFragment) {
            ((OwncloudFilesFragment)filesfrag).reload();
        }
    }


    /** Loader callbacks for the provider */
    private class ProviderLoaderCb implements LoaderCallbacks<Cursor>
    {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args)
        {
            return new PasswdCursorLoader(
                    OwncloudFilesActivity.this, itsProviderUri,
                    PasswdSafeContract.Providers.PROJECTION,
                    null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader,
                                   Cursor cursor)
        {
            if (PasswdCursorLoader.checkResult(loader)) {
                updateProvider(cursor);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader)
        {
            if (PasswdCursorLoader.checkResult(loader)) {
                updateProvider(null);
            }
        }

        /** Update the information for a provider */
        private void updateProvider(Cursor cursor)
        {
            String name;
            if ((cursor != null) && cursor.moveToFirst()) {
                name = PasswdSafeContract.Providers.getDisplayName(
                        cursor);
                // TODO: show account info
            } else {
                name = getString(R.string.no_account);
            }
            PasswdSafeUtil.dbginfo(TAG, "provider: %s", name);
        }
    }


    /** Loader callbacks for the synced files for a provider */
    private class FilesLoaderCb implements LoaderCallbacks<Cursor>
    {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args)
        {
            return new PasswdCursorLoader(
                    OwncloudFilesActivity.this, itsFilesUri,
                    PasswdSafeContract.Files.PROJECTION,
                    PasswdSafeContract.Files.NOT_DELETED_SELECTION,
                    null, PasswdSafeContract.Files.TITLE_SORT_ORDER);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader,
                                   Cursor cursor)
        {
            if (PasswdCursorLoader.checkResult(loader)) {
                updateFiles(cursor);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader)
        {
            if (PasswdCursorLoader.checkResult(loader)) {
                updateFiles(null);
            }
        }

        /** Update the files for a provider */
        private void updateFiles(Cursor cursor)
        {
            itsSyncedFiles.clear();
            if (cursor == null) {
                return;
            }
            for (boolean more = cursor.moveToFirst(); more;
                    more = cursor.moveToNext()) {
                DbFile file = new DbFile(cursor);
                PasswdSafeUtil.dbginfo(TAG, "sync file: %s", file);
                itsSyncedFiles.put(file.itsRemoteId, file);
            }
        }
    }
}
