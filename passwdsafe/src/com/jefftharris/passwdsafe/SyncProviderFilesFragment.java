/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.jefftharris.passwdsafe.lib.ApiCompat;
import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.util.Utils;

/**
 * @author jharris
 *
 */
public class SyncProviderFilesFragment extends ListFragment
{
    public static final int LOADER_TITLE = 0;
    public static final int LOADER_FILES = 1;

    private SimpleCursorAdapter itsProviderAdapter;


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
        // TODO: add/delete file
        // TODO: open/save file
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        itsProviderAdapter = new SimpleCursorAdapter(
               getActivity(), android.R.layout.simple_list_item_2, null,
               new String[] { PasswdSafeContract.Files.COL_TITLE,
                              PasswdSafeContract.Files.COL_MOD_DATE },
               new int[] { android.R.id.text1, android.R.id.text2 }, 0);

        itsProviderAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder()
        {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int colIdx)
            {
                if (colIdx ==
                        PasswdSafeContract.Files.PROJECTION_IDX_MOD_DATE) {
                    long modDate = cursor.getLong(colIdx);
                    TextView tv = (TextView)view;
                    tv.setText(Utils.formatDate(modDate, getActivity()));
                    return true;
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
                    return new CursorLoader(
                            getActivity(), getProviderUri(),
                            PasswdSafeContract.Providers.PROJECTION,
                            null, null, null);
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
                {
                    View view = getView();
                    String str;
                    ImageView icon = (ImageView)view.findViewById(R.id.icon);
                    if (cursor.moveToFirst()) {
                        str = cursor.getString(
                            PasswdSafeContract.Providers.PROJECTION_IDX_ACCT);
                        String typeStr = cursor.getString(
                            PasswdSafeContract.Providers.PROJECTION_IDX_TYPE);
                        try {
                            PasswdSafeContract.Providers.Type type =
                                PasswdSafeContract.Providers.Type.valueOf(typeStr);
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
                     Uri uri = getProviderUri().buildUpon().appendPath(
                             PasswdSafeContract.Files.TABLE).build();
                     return new CursorLoader(
                             getActivity(), uri,
                             PasswdSafeContract.Files.PROJECTION,
                             null, null,
                             PasswdSafeContract.Files.TITLE_SORT_ORDER);
                 }

                 @Override
                 public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
                 {
                     itsProviderAdapter.swapCursor(cursor);
                 }

                 @Override
                 public void onLoaderReset(Loader<Cursor> loader)
                 {
                     itsProviderAdapter.swapCursor(null);
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

        MenuItem mi = menu.findItem(R.id.menu_sync_files);
        MenuItemCompat.setShowAsAction(mi,
                                       MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        super.onCreateOptionsMenu(menu, inflater);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_sync_files: {
            ApiCompat.requestProviderSync(PasswdSafeContract.CONTENT_URI,
                                          getActivity());
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }


    /** Get the URI of the provider whose files are shown */
    private Uri getProviderUri()
    {
        return Uri.parse(getArguments().getString("providerUri"));
    }
}
