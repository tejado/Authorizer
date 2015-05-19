/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.owncloud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import android.support.v4.view.MenuItemCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.lib.view.PasswdCursorLoader;
import com.jefftharris.passwdsafe.sync.ProviderFactory;
import com.jefftharris.passwdsafe.sync.R;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
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

    private OwnCloudClient itsClient = null;
    private final HashMap<String, Long> itsSyncedFiles = new HashMap<>();
    private AsyncTask<Void, Void, OwnCloudClient> itsClientLoadTask;
    private LoaderCallbacks<Cursor> itsProviderLoaderCb;
    private LoaderCallbacks<Cursor> itsFilesLoaderCb;
    private final List<ListFilesTask> itsListTasks = new ArrayList<>();
    private final List<FileSyncedUpdateTask> itsUpdateTasks = new ArrayList<>();


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
                PasswdSafeContract.RemoteFiles.TABLE).build();

        if (args == null) {
            changeDir(FileUtils.PATH_SEPARATOR);
        }

        itsProviderLoaderCb = new ProviderLoaderCb();
        itsFilesLoaderCb = new FilesLoaderCb();
        LoaderManager lm = getSupportLoaderManager();
        lm.initLoader(LOADER_TITLE, null, itsProviderLoaderCb);
        lm.initLoader(LOADER_FILES, null, itsFilesLoaderCb);

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
        for (ListFilesTask task: itsListTasks) {
            task.cancel(true);
        }
        for (FileSyncedUpdateTask task: itsUpdateTasks) {
            task.cancel(true);
        }
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_owncloud_files, menu);
        MenuItem item = menu.findItem(R.id.menu_reload);
        MenuItemCompat.setShowAsAction(item,
                                       MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        return super.onCreateOptionsMenu(menu);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_reload: {
            reloadFiles();
            LoaderManager lm = getSupportLoaderManager();
            lm.restartLoader(LOADER_TITLE, null, itsProviderLoaderCb);
            lm.restartLoader(LOADER_FILES, null, itsFilesLoaderCb);
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.owncloud.OwncloudFilesFragment.Listener#listFiles(java.lang.String, com.jefftharris.passwdsafe.sync.owncloud.OwncloudFilesFragment.Listener.ListFilesCb)
     */
    @Override
    public void listFiles(String path, final ListFilesCb cb)
    {
        PasswdSafeUtil.dbginfo(TAG, "listFiles client %b, path: %s",
                               (itsClient != null), path);
        for (ListFilesTask task: itsListTasks) {
            task.cancel(true);
        }
        itsListTasks.clear();

        ListFilesTask task =
                new ListFilesTask(itsClient, this, new ListFilesTask.Callback()
                {
                    @Override
                    public void handleFiles(List<OwncloudProviderFile> files,
                                            ListFilesTask task)
                    {
                        itsListTasks.remove(task);
                        cb.handleFiles(files);
                    }
                });
        itsListTasks.add(task);
        task.execute(path);
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


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.owncloud.OwncloudFilesFragment.Listener#isSelected(java.lang.String)
     */
    @Override
    public boolean isSelected(String filePath)
    {
        return itsSyncedFiles.containsKey(filePath);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.owncloud.OwncloudFilesFragment.Listener#updateFileSynced(com.jefftharris.passwdsafe.sync.owncloud.OwncloudProviderFile, boolean)
     */
    @Override
    public void updateFileSynced(final OwncloudProviderFile file,
                                 final boolean synced)
    {
        PasswdSafeUtil.dbginfo(
                TAG, "updateFileSynced sync %b, file: %s", synced,
                OwncloudProviderFile.fileToString(file.getRemoteFile()));

        FileSyncedUpdateTask task = new FileSyncedUpdateTask(
                itsProviderUri, file, synced,
                new FileSyncedUpdateTask.Callback()
                {
                    @Override
                    public void updateComplete(Exception error,
                                               long remFileId,
                                               FileSyncedUpdateTask task)
                    {
                        itsUpdateTasks.remove(task);
                        if (error == null) {
                            if (synced) {
                                itsSyncedFiles.put(file.getRemoteId(),
                                                   remFileId);
                            } else {
                                itsSyncedFiles.remove(file.getRemoteId());
                            }
                        } else {
                            String msg = "Error updating sync for " +
                                    file.getRemoteId();
                            Log.e(TAG, msg, error);
                            PasswdSafeUtil.showErrorMsg(
                                    msg, OwncloudFilesActivity.this);
                        }
                        getContentResolver().notifyChange(itsFilesUri, null);
                        OwncloudProvider provider = (OwncloudProvider)
                                ProviderFactory.getProvider(
                                        ProviderType.OWNCLOUD,
                                        OwncloudFilesActivity.this);
                        provider.requestSync(false);
                    }
                });
        itsUpdateTasks.add(task);
        task.execute();
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


    /** Update the state of the synced files shown by the activity */
    private void updateSyncedFiles()
    {
        FragmentManager fragmgr = getSupportFragmentManager();
        Fragment filesfrag = fragmgr.findFragmentById(R.id.content);
        if (filesfrag instanceof OwncloudFilesFragment) {
            ((OwncloudFilesFragment)filesfrag).updateSyncedFiles();
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
                name = PasswdSafeContract.Providers.getDisplayName(cursor);
            } else {
                name = getString(R.string.no_account);
            }
            PasswdSafeUtil.dbginfo(TAG, "provider: %s", name);
            TextView title = (TextView)findViewById(R.id.title);
            title.setText(name);
        }
    }


    /** Loader callbacks for the synced remote files for a provider */
    private class FilesLoaderCb implements LoaderCallbacks<Cursor>
    {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args)
        {
            return new PasswdCursorLoader(
                    OwncloudFilesActivity.this, itsFilesUri,
                    PasswdSafeContract.RemoteFiles.PROJECTION,
                    PasswdSafeContract.RemoteFiles.NOT_DELETED_SELECTION,
                    null, null);
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
            if (cursor != null) {
                for (boolean more = cursor.moveToFirst(); more;
                        more = cursor.moveToNext()) {
                    long id = cursor.getLong(
                        PasswdSafeContract.RemoteFiles.PROJECTION_IDX_ID);
                    String remoteId = cursor.getString(
                        PasswdSafeContract.RemoteFiles.PROJECTION_IDX_REMOTE_ID);

                    PasswdSafeUtil.dbginfo(TAG, "sync file: %s", remoteId);
                    itsSyncedFiles.put(remoteId, id);
                }
            }
            updateSyncedFiles();
        }
    }


    /** Background task for listing files from ownCloud */
    private static class ListFilesTask
            extends AsyncTask<String, Void,
                              Pair<List<OwncloudProviderFile>, Exception>>
    {
        /** Callback for when the task is finished; null files if cancelled
         *  or an error occurred.
         */
        public interface Callback
        {
            void handleFiles(List<OwncloudProviderFile> files,
                             ListFilesTask task);
        }

        private final OwnCloudClient itsClient;
        private final Callback itsCb;
        private final Context itsContext;


        /** Constructor */
        public ListFilesTask(OwnCloudClient client, Context ctx, Callback cb)
        {
            itsClient = client;
            itsCb = cb;
            itsContext = ctx;
        }


        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
         */
        @Override
        protected Pair<List<OwncloudProviderFile>, Exception>
        doInBackground(String... params)
        {
            List<OwncloudProviderFile> files = new ArrayList<>();
            Pair<List<OwncloudProviderFile>, Exception> result =
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

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(
                Pair<List<OwncloudProviderFile>, Exception> result)
        {
            if (result.second != null) {
                Log.e(TAG, "Error listing files", result.second);
                Toast.makeText(
                        itsContext,
                        "Error listing files: " + result.second.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
            itsCb.handleFiles(result.first, this);
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onCancelled(java.lang.Object)
         */
        @Override
        protected void onCancelled(
                Pair<List<OwncloudProviderFile>, Exception> result)
        {
            itsCb.handleFiles(null, this);
        }
    }


    /** Background task for updating the synced state of a file from ownCloud */
    private static class FileSyncedUpdateTask
            extends AsyncTask<Void, Void, Pair<Exception, Long>>
    {
        /** Callback for when the update is complete */
        public interface Callback
        {
            void updateComplete(Exception error,
                                long remFileId,
                                FileSyncedUpdateTask task);
        }


        private final Uri itsProviderUri;
        private final OwncloudProviderFile itsFile;
        private final boolean itsIsSynced;
        private final Callback itsCb;


        /** Constructor */
        public FileSyncedUpdateTask(Uri providerUri, OwncloudProviderFile file,
                                    boolean synced, Callback cb)
        {
            itsProviderUri = providerUri;
            itsFile = file;
            itsIsSynced = synced;
            itsCb = cb;
        }


        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
         */
        @Override
        protected Pair<Exception, Long> doInBackground(Void... params)
        {
            Exception error = null;
            long remFileId = -1;
            SyncDb syncDb = SyncDb.acquire();
            try {
                SQLiteDatabase db = syncDb.beginTransaction();

                long providerId =
                        PasswdSafeContract.Providers.getId(itsProviderUri);

                DbFile remfile = SyncDb.getFileByRemoteId(
                        providerId, itsFile.getRemoteId(), db);
                if (itsIsSynced) {
                    if (remfile != null) {
                        SyncDb.updateRemoteFileChange(
                                remfile.itsId, DbFile.FileChange.ADDED, db);
                        remFileId = remfile.itsId;
                    } else {
                        remFileId = SyncDb.addRemoteFile(
                                providerId, itsFile.getRemoteId(),
                                itsFile.getTitle(), itsFile.getFolder(),
                                itsFile.getModTime(), itsFile.getHash(), db);
                    }
                } else {
                    if (remfile != null) {
                        SyncDb.updateRemoteFileDeleted(remfile.itsId, db);
                    }
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                error = e;
            } finally {
                syncDb.endTransactionAndRelease();
            }
            return Pair.create(error, remFileId);
        }


        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Pair<Exception, Long> result)
        {
            itsCb.updateComplete(result.first, result.second, this);
        }
    }
}
