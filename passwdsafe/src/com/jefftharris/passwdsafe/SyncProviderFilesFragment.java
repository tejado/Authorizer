/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.util.Utils;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
        return inflater.inflate(R.layout.fragment_sync_provider_files,
                                container, false);
        // TODO: header
        // TODO: sync menu item
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
                    String str;
                    if (cursor.moveToFirst()) {
                        str = cursor.getString(
                            PasswdSafeContract.Providers.PROJECTION_IDX_ACCT);
                    } else {
                        str = getString(R.string.none);
                    }
                    TextView tv = (TextView)getView().findViewById(R.id.title);
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


    /** Get the URI of the provider whose files are shown */
    private Uri getProviderUri()
    {
        return Uri.parse(getArguments().getString("providerUri"));
    }
}
