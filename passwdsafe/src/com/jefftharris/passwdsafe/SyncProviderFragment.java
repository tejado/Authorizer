/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import com.jefftharris.passwdsafe.lib.PasswdSafeContract;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * The SyncProviderFragment allows the user to choose a sync provider
 */
public class SyncProviderFragment extends ListFragment
        implements LoaderCallbacks<Cursor>
{
    /** Listener interface for the owning activity */
    public interface Listener
    {
        /** Show the files for a provider's URI */
        public void showSyncProviderFiles(Uri uri);
    }

    private SimpleCursorAdapter itsProviderAdapter;
    private boolean itsHasProvider = true;
    private Listener itsListener;

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
     * @see android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_sync_provider,
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
               getActivity(), android.R.layout.simple_list_item_2, null,
               new String[] { PasswdSafeContract.Providers.COL_ACCT,
                              PasswdSafeContract.Providers.COL_TYPE },
               new int[] { android.R.id.text1, android.R.id.text2 }, 0);
        setListAdapter(itsProviderAdapter);

        itsHasProvider = checkProvider();
        getLoaderManager().initLoader(0, null, this);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onDetach()
     */
    @Override
    public void onDetach()
    {
        super.onDetach();
        itsListener = null;
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

        Uri uri = ContentUris.withAppendedId(
                PasswdSafeContract.Providers.CONTENT_URI, id);
        itsListener.showSyncProviderFiles(uri);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onCreateLoader(int, android.os.Bundle)
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args)
    {
        if (!itsHasProvider) {
            return null;
        }
        Uri uri = PasswdSafeContract.Providers.CONTENT_URI;
        return new CursorLoader(getActivity(), uri,
                                PasswdSafeContract.Providers.PROJECTION,
                                null, null, null);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoadFinished(android.support.v4.content.Loader, java.lang.Object)
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
    {
        itsProviderAdapter.swapCursor(cursor);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoaderReset(android.support.v4.content.Loader)
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader)
    {
        itsProviderAdapter.swapCursor(null);
    }


    /** Check whether the sync provider is present */
    private final boolean checkProvider()
    {
        ContentResolver res = getActivity().getContentResolver();
        String type = res.getType(PasswdSafeContract.Providers.CONTENT_URI);
        return (type != null);
    }
}
