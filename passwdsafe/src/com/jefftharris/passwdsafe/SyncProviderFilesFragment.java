/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.lib.Utils;
import com.jefftharris.passwdsafe.lib.view.PasswdCursorLoader;

/**
 * The SyncProviderFilesFragment shows the list of files for a provider
 */
public class SyncProviderFilesFragment extends ListFragment
{
    /** Listener interface for the owning activity */
    public interface Listener
    {
        /** Open a file */
        public void openFile(Uri uri, String fileName);

        /** Create a new file */
        public void createNewFile(Uri locationUri);
    }

    private static final String TAG = "SyncProviderFilesFragment";
    private static final int LOADER_TITLE = 0;
    private static final int LOADER_FILES = 1;

    private Uri itsProviderUri;
    private Uri itsFilesUri;
    private SimpleCursorAdapter itsProviderAdapter;
    private Listener itsListener;


    /** Create a new instance of the fragment */
    public static SyncProviderFilesFragment newInstance(Uri providerUri)
    {
        SyncProviderFilesFragment frag = new SyncProviderFilesFragment();
        Bundle args = new Bundle();
        args.putString("providerUri", providerUri.toString());
        frag.setArguments(args);
        return frag;
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onAttach(android.app.Activity)
     */
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        itsListener = (Listener)activity;
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        itsProviderUri = Uri.parse(getArguments().getString("providerUri"));
        itsFilesUri = itsProviderUri.buildUpon().appendPath(
                PasswdSafeContract.Files.TABLE).build();
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_sync_provider_files,
                                container, false);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        itsProviderAdapter = new SimpleCursorAdapter(
               getActivity(), R.layout.sync_provider_file_list_item, null,
               new String[] { PasswdSafeContract.Files.COL_TITLE,
                              PasswdSafeContract.Files.COL_MOD_DATE,
                              PasswdSafeContract.Files.COL_FOLDER },
               new int[] { R.id.title, R.id.mod_date, R.id.folder },
               0);

        itsProviderAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder()
        {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int colIdx)
            {
                switch (colIdx) {
                case PasswdSafeContract.Files.PROJECTION_IDX_MOD_DATE: {
                    long modDate = cursor.getLong(colIdx);
                    TextView tv = (TextView)view;
                    tv.setText(Utils.formatDate(modDate, getActivity()));
                    return true;
                }
                case PasswdSafeContract.Files.PROJECTION_IDX_FOLDER: {
                    String folder = cursor.getString(colIdx);
                    if (TextUtils.isEmpty(folder)) {
                        view.setVisibility(View.GONE);
                    } else {
                        view.setVisibility(View.VISIBLE);
                        ((TextView)view).setText(folder);
                    }
                    return true;
                }
                }
                return false;
            }
        });

        setListAdapter(itsProviderAdapter);

        LoaderManager lm = getLoaderManager();
        lm.initLoader(LOADER_TITLE, null, new LoaderCallbacks<Cursor>()
            {
                @Override
                public Loader<Cursor> onCreateLoader(int id, Bundle args)
                {
                    return new PasswdCursorLoader(
                            getActivity(), itsProviderUri,
                            PasswdSafeContract.Providers.PROJECTION,
                            null, null, null);
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
                {
                    if (!PasswdCursorLoader.checkResult(loader)) {
                        return;
                    }
                    View view = getView();
                    String str;
                    ImageView icon = (ImageView)view.findViewById(R.id.icon);
                    if (cursor.moveToFirst()) {
                        str = PasswdSafeContract.Providers.getDisplayName(cursor);
                        String typeStr = cursor.getString(
                            PasswdSafeContract.Providers.PROJECTION_IDX_TYPE);
                        try {
                            ProviderType type = ProviderType.valueOf(typeStr);
                            type.setIcon(icon);
                        } catch (IllegalArgumentException e) {
                        }
                    } else {
                        str = getString(R.string.none);
                        icon.setImageDrawable(null);
                    }
                    TextView tv = (TextView)view.findViewById(R.id.title);
                    tv.setText(str);
                }

                @Override
                public void onLoaderReset(Loader<Cursor> loader)
                {
                }
            });

        lm.initLoader(LOADER_FILES, null, new LoaderCallbacks<Cursor>()
            {
                 @Override
                 public Loader<Cursor> onCreateLoader(int id, Bundle args)
                 {
                     return new PasswdCursorLoader(
                             getActivity(), itsFilesUri,
                             PasswdSafeContract.Files.PROJECTION,
                             PasswdSafeContract.Files.NOT_DELETED_SELECTION,
                             null, PasswdSafeContract.Files.TITLE_SORT_ORDER);
                 }

                 @Override
                 public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
                 {
                     if (PasswdCursorLoader.checkResult(loader)) {
                         itsProviderAdapter.swapCursor(cursor);
                     }
                 }

                 @Override
                 public void onLoaderReset(Loader<Cursor> loader)
                 {
                     if (PasswdCursorLoader.checkResult(loader)) {
                         itsProviderAdapter.swapCursor(null);
                     }
                 }
            });
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onCreateOptionsMenu(android.view.Menu, android.view.MenuInflater)
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.fragment_sync_provider_files, menu);
        super.onCreateOptionsMenu(menu, inflater);

        // TODO: no add file menu item when used with launcher shortcuts
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_sync_files: {
            try {
                ContentResolver cr = getActivity().getContentResolver();
                cr.query(PasswdSafeContract.Methods.CONTENT_URI,
                         null, null,
                         new String[] { PasswdSafeContract.Methods.METHOD_SYNC,
                                        itsProviderUri.toString() },
                         null);
            } catch (Exception e) {
                Log.e(TAG, "Error syncing", e);
            }
            return true;
        }
        case R.id.menu_file_new: {
            itsListener.createNewFile(itsFilesUri);
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onListItemClick(android.widget.ListView, android.view.View, int, long)
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        Cursor cursor = (Cursor)getListAdapter().getItem(position);
        if ((cursor == null) || (itsListener == null)) {
            return;
        }

        Uri uri = ContentUris.withAppendedId(itsFilesUri, id);
        PasswdSafeUtil.dbginfo(TAG, "Open provider uri %s", uri);
        itsListener.openFile(
                uri,
                cursor.getString(PasswdSafeContract.Files.PROJECTION_IDX_TITLE));
    }
}
