/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.lib.view.PasswdCursorLoader;
import com.jefftharris.passwdsafe.sync.ProviderFactory;
import com.jefftharris.passwdsafe.sync.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *  Activity for managing files synced from providers
 */
public abstract class AbstractSyncedFilesActivity extends AppCompatActivity
        implements SyncedFilesFragment.Listener
{
    public static final String INTENT_PROVIDER_URI = "provider_uri";

    private static final int LOADER_TITLE = 0;
    private static final int LOADER_FILES = 1;

    private static final String TAG = "AbstractSyncedFilesAct";

    private Uri itsProviderUri;
    private Uri itsFilesUri;
    private final String itsRootId;
    private final ProviderType itsProviderType;
    private final HashMap<String, Long> itsSyncedFiles = new HashMap<>();
    private LoaderCallbacks<Cursor> itsProviderLoaderCb;
    private LoaderCallbacks<Cursor> itsFilesLoaderCb;
    private final List<AbstractListFilesTask> itsListTasks = new ArrayList<>();
    private final List<FileSyncedUpdateTask> itsUpdateTasks = new ArrayList<>();

    /** Constructor */
    protected AbstractSyncedFilesActivity(ProviderType providerType)
    {
        this(providerType, ProviderRemoteFile.PATH_SEPARATOR);
    }


    /** Constructor */
    protected AbstractSyncedFilesActivity(ProviderType providerType,
                                          String rootId)
    {
        itsProviderType = providerType;
        itsRootId = rootId;
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle args)
    {
        super.onCreate(args);
        setContentView(R.layout.activity_synced_files);

        itsProviderUri = getIntent().getParcelableExtra(INTENT_PROVIDER_URI);
        if (itsProviderUri == null) {
            PasswdSafeUtil.showFatalMsg("Required args missing", this);
            return;
        }

        itsFilesUri = itsProviderUri.buildUpon().appendPath(
                PasswdSafeContract.RemoteFiles.TABLE).build();

        if (args == null) {
            changeDir(ProviderRemoteFile.PATH_SEPARATOR, itsRootId);
        }

        itsProviderLoaderCb = new ProviderLoaderCb();
        itsFilesLoaderCb = new FilesLoaderCb();
        LoaderManager lm = getSupportLoaderManager();
        lm.initLoader(LOADER_TITLE, null, itsProviderLoaderCb);
        lm.initLoader(LOADER_FILES, null, itsFilesLoaderCb);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onDestroy()
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        for (AbstractListFilesTask task: itsListTasks) {
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
        getMenuInflater().inflate(R.menu.activity_synced_files, menu);
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
     * @see com.jefftharris.passwdsafe.sync.lib.SyncedFilesFragment.Listener#listFiles(java.lang.String, com.jefftharris.passwdsafe.sync.lib.SyncedFilesFragment.Listener.ListFilesCb)
     */
    @Override
    public void listFiles(String path, final ListFilesCb cb)
    {
        PasswdSafeUtil.dbginfo(TAG, "listFiles client path: %s", path);
        for (AbstractListFilesTask task: itsListTasks) {
            task.cancel(true);
        }
        itsListTasks.clear();

        AbstractListFilesTask task =
                createListFilesTask(this, new AbstractListFilesTask.Callback()
                {
                    @Override
                    public void handleFiles(List<ProviderRemoteFile> files,
                                            AbstractListFilesTask task)
                    {
                        itsListTasks.remove(task);
                        cb.handleFiles(files);
                    }
                });
        itsListTasks.add(task);
        task.execute(path);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.SyncedFilesFragment.Listener#changeDir(java.lang.String)
     */
    @Override
    public void changeDir(String pathDisplay, String pathId)
    {
        PasswdSafeUtil.dbginfo(TAG, "changeDir: %s", pathDisplay);
        Fragment files = SyncedFilesFragment.newInstance(pathDisplay, pathId);
        FragmentManager fragmgr = getSupportFragmentManager();
        FragmentTransaction txn = fragmgr.beginTransaction();
        txn.replace(R.id.content, files);
        if (!TextUtils.equals(pathId, itsRootId)) {
            txn.addToBackStack(null);
        }
        txn.commit();
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.SyncedFilesFragment.Listener#changeParentDir()
     */
    public void changeParentDir()
    {
        PasswdSafeUtil.dbginfo(TAG, "changeParentDir");
        FragmentManager fragmgr = getSupportFragmentManager();
        fragmgr.popBackStack();
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.SyncedFilesFragment.Listener#isSelected(java.lang.String)
     */
    @Override
    public boolean isSelected(String filePath)
    {
        return itsSyncedFiles.containsKey(filePath);
    }


    @Override
    public void updateFileSynced(final ProviderRemoteFile file,
                                 final boolean synced)
    {
        PasswdSafeUtil.dbginfo(TAG, "updateFileSynced sync %b, file: %s",
                               synced, file.toDebugString());

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
                                    msg, AbstractSyncedFilesActivity.this);
                        }
                        getContentResolver().notifyChange(itsFilesUri, null);
                        Provider provider = ProviderFactory.getProvider(
                                itsProviderType,
                                AbstractSyncedFilesActivity.this);
                        provider.requestSync(false);
                    }
                });
        itsUpdateTasks.add(task);
        task.execute();
    }


    /** Create a list files task */
    protected abstract AbstractListFilesTask createListFilesTask(
            Context ctx,
            AbstractListFilesTask.Callback cb);


    /** Reload the files shown by the activity */
    protected void reloadFiles()
    {
        FragmentManager fragmgr = getSupportFragmentManager();
        Fragment filesfrag = fragmgr.findFragmentById(R.id.content);
        if (filesfrag instanceof SyncedFilesFragment) {
            ((SyncedFilesFragment)filesfrag).reload();
        }
    }


    /** Update the state of the synced files shown by the activity */
    private void updateSyncedFiles()
    {
        FragmentManager fragmgr = getSupportFragmentManager();
        Fragment filesfrag = fragmgr.findFragmentById(R.id.content);
        if (filesfrag instanceof SyncedFilesFragment) {
            ((SyncedFilesFragment)filesfrag).updateSyncedFiles();
        }
    }


   /** Loader callbacks for the provider */
    private class ProviderLoaderCb implements LoaderCallbacks<Cursor>
    {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args)
        {
            return new PasswdCursorLoader(
                    AbstractSyncedFilesActivity.this, itsProviderUri,
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

                String typeStr = cursor.getString(
                        PasswdSafeContract.Providers.PROJECTION_IDX_TYPE);
                ProviderType type = ProviderType.valueOf(typeStr);
                type.setIcon((ImageView)findViewById(R.id.icon));
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
                    AbstractSyncedFilesActivity.this, itsFilesUri,
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


    /** Background task for listing files from a provider */
    protected static abstract class AbstractListFilesTask
            extends AsyncTask<String, Void,
                              Pair<List<ProviderRemoteFile>, Exception>>
    {
        /** Callback for when the task is finished; null files if cancelled
         *  or an error occurred.
         */
        public interface Callback
        {
            void handleFiles(List<ProviderRemoteFile> files,
                             AbstractListFilesTask task);
        }

        protected final Context itsContext;
        private final Callback itsCb;


        /** Constructor */
        public AbstractListFilesTask(Context ctx, Callback cb)
        {
            itsCb = cb;
            itsContext = ctx;
        }


        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(
                Pair<List<ProviderRemoteFile>, Exception> result)
        {
            if (result.second != null) {
                Log.e(TAG, "Error listing files", result.second);
                Toast.makeText(
                        itsContext,
                        "Error listing files: " + result.second.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
            itsCb.handleFiles(result.first, this);
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onCancelled(java.lang.Object)
         */
        @Override
        protected void onCancelled(
                Pair<List<ProviderRemoteFile>, Exception> result)
        {
            itsCb.handleFiles(null, this);
        }
    }


    /** Background task for updating the synced state of a file from a
     * provider */
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
        private final ProviderRemoteFile itsFile;
        private final boolean itsIsSynced;
        private final Callback itsCb;


        /** Constructor */
        public FileSyncedUpdateTask(Uri providerUri, ProviderRemoteFile file,
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
