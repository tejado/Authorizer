/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.jefftharris.passwdsafe.lib.ApiCompat;
import com.jefftharris.passwdsafe.lib.DocumentsContractCompat;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

import java.util.List;

/**
 *  The StorageFileListFragment fragment allows the user to open files using
 *  the storage access framework on Kitkat and higher
 */
@TargetApi(19)
public final class StorageFileListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor>
{
    // TODO: recent sync files

    /** Listener interface for the owning activity */
    public interface Listener
    {
        /** Open a file */
        void openFile(Uri uri, String fileName);

        /** Does the activity have a menu */
        boolean activityHasMenu();
    }

    private static final String TAG = "StorageFileListFragment";

    private static final int OPEN_RC = 1;

    private static final int LOADER_FILES = 0;

    private Listener itsListener;
    private RecentFilesDb itsRecentFilesDb;
    private SimpleCursorAdapter itsFilesAdapter;


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onAttach(android.app.Activity)
     */
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        itsListener = (Listener)activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        itsRecentFilesDb = new RecentFilesDb(getActivity());
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        if (itsListener.activityHasMenu()) {
            setHasOptionsMenu(true);
        }

        return inflater.inflate(R.layout.fragment_storage_file_list,
                                container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        itsFilesAdapter = new SimpleCursorAdapter(
                getActivity(), android.R.layout.simple_list_item_1, null,
                new String[] { RecentFilesDb.DB_COL_FILES_TITLE },
                new int[] { android.R.id.text1 }, 0);

        setListAdapter(itsFilesAdapter);

        LoaderManager lm = getLoaderManager();
        lm.initLoader(LOADER_FILES, null, this);
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onResume()
     */
    @Override
    public void onResume()
    {
        super.onResume();
        PasswdSafeApp app = (PasswdSafeApp)getActivity().getApplication();
        app.closeOpenFile();
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (itsRecentFilesDb != null) {
            itsRecentFilesDb.close();
        }
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode) {
        case OPEN_RC: {
            PasswdSafeUtil.dbginfo(TAG, "onActivityResult open %d: %s",
                                   resultCode, data);
            if ((resultCode == Activity.RESULT_OK) && (data != null)) {
                openUri(data);
            }
            break;
        }
        default: {
            super.onActivityResult(requestCode, resultCode, data);
            break;
        }
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        Cursor item = (Cursor)l.getItemAtPosition(position);
        String uristr = item.getString(RecentFilesDb.QUERY_COL_URI);
        String title = item.getString(RecentFilesDb.QUERY_COL_TITLE);
        Uri uri = Uri.parse(uristr);
        openUri(uri, title);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.fragment_storage_file_list, menu);
        super.onCreateOptionsMenu(menu, inflater);

        MenuItem mi = menu.findItem(R.id.menu_file_open);
        MenuItemCompat.setShowAsAction(mi,
                                       MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        mi = menu.findItem(R.id.menu_file_new);
        MenuItemCompat.setShowAsAction(mi,
                                       MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_file_open: {
            startOpenFile();
            return true;
        }
        case R.id.menu_file_new: {
            startActivity(new Intent(PasswdSafeUtil.NEW_INTENT));
            return true;
        }
        case R.id.menu_clear_recent: {
            try {
                itsRecentFilesDb.clear();
                getLoaderManager().restartLoader(LOADER_FILES, null, this);
            } catch (SQLException e) {
                Log.e(TAG, "Clear recent error", e);
            }
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle)
    {
        return new AsyncTaskLoader<Cursor>(getActivity())
        {
            /** Handle when the loader is reset */
            @Override
            protected void onReset()
            {
                super.onReset();
                onStopLoading();
            }

            /** Handle when the loader is started */
            @Override
            protected void onStartLoading()
            {
                forceLoad();
            }

            /** Handle when the loader is stopped */
            @Override
            protected void onStopLoading()
            {
                cancelLoad();
            }

            /** Load the files in the background */
            @Override
            public Cursor loadInBackground()
            {
                PasswdSafeUtil.dbginfo(TAG, "loadInBackground");
                int flags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                ContentResolver cr = getContext().getContentResolver();
                List<Uri> permUris = ApiCompat.getPersistedUriPermissions(cr);
                for (Uri permUri: permUris) {
                    PasswdSafeUtil.dbginfo(TAG, "Checking persist perm %s",
                                           permUri);
                    Cursor cursor = cr.query(permUri, null, null, null, null);
                    try {
                        if ((cursor != null) && (cursor.moveToFirst())) {
                            ApiCompat.takePersistableUriPermission(
                                    cr, permUri, flags);
                        } else {
                            ApiCompat.releasePersistableUriPermission(
                                    cr, permUri, flags);
                            itsRecentFilesDb.removeUri(permUri);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "File remove error", e);
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }

                try {
                    return itsRecentFilesDb.queryFiles();
                } catch (Exception e) {
                    Log.e(TAG, "Files load error", e);
                }
                return null;
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor)
    {
        itsFilesAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader)
    {
        itsFilesAdapter.swapCursor(null);
    }

    /** Start the intent to open a file */
    private void startOpenFile()
    {
        Intent intent = new Intent(
                DocumentsContractCompat.INTENT_ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setType("application/octet-stream");

        startActivityForResult(intent, OPEN_RC);
    }


    /** Open a password file URI from an intent */
    private void openUri(Intent openIntent)
    {
        Uri uri = openIntent.getData();
        int flags = openIntent.getFlags() &
                    (Intent.FLAG_GRANT_READ_URI_PERMISSION |
                     Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        String title = RecentFilesDb.updateOpenedSafFile(uri, flags,
                                                         getActivity());
        if (title != null) {
            openUri(uri, title);
        }
    }


    /** Open a password file URI */
    private void openUri(Uri uri, String title)
    {
        PasswdSafeUtil.dbginfo(TAG, "openUri %s: %s", uri, title);

        try {
            itsRecentFilesDb.insertOrUpdateFile(uri, title);
        } catch (Exception e) {
            Log.e(TAG, "Error inserting recent file", e);
        }

        itsListener.openFile(uri, title);
    }
}
