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
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.jefftharris.passwdsafe.lib.ApiCompat;
import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.view.PasswdCursorLoader;

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

        /** Does the activity have a menu */
        public boolean activityHasMenu();
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
        if (itsListener.activityHasMenu()) {
            setHasOptionsMenu(true);
        }
        View rootView = inflater.inflate(R.layout.fragment_sync_provider,
                                         container, false);

        View launchBtn = rootView.findViewById(android.R.id.empty);
        launchBtn.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                PasswdSafeUtil.startMainActivity(
                        "com.jefftharris.passwdsafe.sync", getActivity());
            }
        });

        return rootView;
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        itsProviderAdapter = new SimpleCursorAdapter(
               getActivity(), R.layout.passwdsafe_list_item, null,
               new String[] { PasswdSafeContract.Providers.COL_ACCT,
                              PasswdSafeContract.Providers.COL_TYPE,
                              PasswdSafeContract.Providers.COL_TYPE},
               new int[] { android.R.id.text1, android.R.id.text2, R.id.icon },
               0);
        itsProviderAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder()
        {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int colIdx)
            {
                int id = view.getId();
                if ((id != android.R.id.text2) && (id != R.id.icon)) {
                    return false;
                }

                try {
                    String typeStr = cursor.getString(colIdx);
                    PasswdSafeContract.Providers.Type type =
                            PasswdSafeContract.Providers.Type.valueOf(typeStr);
                    switch (id) {
                    case android.R.id.text2: {
                        type.setText((TextView)view);
                        break;
                    }
                    case R.id.icon: {
                        type.setIcon((ImageView)view);
                        break;
                    }
                    }
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
        });
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
     * @see android.support.v4.app.Fragment#onCreateOptionsMenu(android.view.Menu, android.view.MenuInflater)
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.fragment_sync_provider, menu);
        super.onCreateOptionsMenu(menu, inflater);

        MenuItem mi = menu.findItem(R.id.menu_sync);
        MenuItemCompat.setShowAsAction(mi,
                                       MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_sync: {
            ApiCompat.requestManualSync(
                    null, PasswdSafeContract.Providers.CONTENT_URI,
                    getActivity());
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
        View v = getView();
        if (!itsHasProvider) {
            v.setVisibility(View.GONE);
            return null;
        }
        v.setVisibility(View.VISIBLE);
        Uri uri = PasswdSafeContract.Providers.CONTENT_URI;
        return new PasswdCursorLoader(getActivity(), uri,
                                      PasswdSafeContract.Providers.PROJECTION,
                                      null, null, null);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoadFinished(android.support.v4.content.Loader, java.lang.Object)
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
    {
        if (PasswdCursorLoader.checkResult(loader)) {
            itsProviderAdapter.swapCursor(cursor);
        }
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoaderReset(android.support.v4.content.Loader)
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader)
    {
        if (PasswdCursorLoader.checkResult(loader)) {
            itsProviderAdapter.swapCursor(null);
        }
    }


    /** Check whether the sync provider is present */
    private final boolean checkProvider()
    {
        ContentResolver res = getActivity().getContentResolver();
        String type = res.getType(PasswdSafeContract.Providers.CONTENT_URI);
        return (type != null);
    }
}
