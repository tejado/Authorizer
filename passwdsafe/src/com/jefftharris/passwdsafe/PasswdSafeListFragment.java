/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

/**
 *  Fragment showing lists of items from a PasswdSafe file
 */
public class PasswdSafeListFragment extends ListFragment
        implements LoaderCallbacks<List<Map<String, Object>>>
{
    /** Listener interface for owning activity */
    public interface Listener
    {
        /** Mode for which items are shown from the file */
        public enum Mode
        {
            GROUPS,
            RECORDS,
            ALL
        }

        /** Get the current record items in a background thread */
        List<Map<String, Object>> getBackgroundRecordItems(Mode mode);
    }


    private static final String TAG = PasswdSafeListFragment.class.getName();
    private Listener.Mode itsMode = Listener.Mode.GROUPS;
    private Listener itsListener;
    private ItemListAdapter itsAdapter;


    /** Create a new instance */
    public static PasswdSafeListFragment newInstance(Listener.Mode mode)
    {
        PasswdSafeListFragment frag = new PasswdSafeListFragment();
        Bundle args = new Bundle();
        args.putString("mode", mode.toString());
        frag.setArguments(args);
        return frag;
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        String modestr = (args != null) ? args.getString("mode") : null;
        if (modestr == null) {
            itsMode = Listener.Mode.GROUPS;
        } else {
            itsMode = Listener.Mode.valueOf(modestr);
        }
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
     * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        View root = inflater.inflate(R.layout.fragment_passwdsafe_list,
                                     container, false);
        return root;
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        boolean hasMenu = false;
        setHasOptionsMenu(hasMenu);

        itsAdapter = new ItemListAdapter(getActivity());
        LoaderManager lm = getLoaderManager();
        lm.initLoader(0, null, this);
        setListAdapter(itsAdapter);
    }


    /** Refresh the list due to file changes */
    public void refreshList()
    {
        if (isAdded()) {
            LoaderManager lm = getLoaderManager();
            lm.restartLoader(0, null, this);
        }
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onCreateLoader(int, android.os.Bundle)
     */
    @Override
    public Loader<List<Map<String, Object>>> onCreateLoader(int id, Bundle args)
    {
        return new ItemLoader(itsMode, itsListener, getActivity());
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoadFinished(android.support.v4.content.Loader, java.lang.Object)
     */
    @Override
    public void onLoadFinished(Loader<List<Map<String, Object>>> loader,
                               List<Map<String, Object>> data)
    {
        itsAdapter.setData(data);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoaderReset(android.support.v4.content.Loader)
     */
    @Override
    public void onLoaderReset(Loader<List<Map<String, Object>>> loader)
    {
        itsAdapter.setData(null);
    }


    /** List adapter for file items */
    private static class ItemListAdapter
            extends ArrayAdapter<Map<String, Object>>
    {
        /** Constructor */
        public ItemListAdapter(Context context)
        {
            super(context, R.layout.passwdsafe_list_item, android.R.id.text1);
        }

        /** Set the list data */
        public void setData(List<Map<String, Object>> data)
        {
            clear();
            if (data != null) {
                for (Map<String, Object> item: data) {
                    add(item);
                }
            }
        }
    }


    /** Loader for file items */
    private static class ItemLoader
            extends AsyncTaskLoader<List<Map<String, Object>>>
    {
        private final Listener.Mode itsMode;
        private final Listener itsActListener;

        /** Constructor */
        public ItemLoader(Listener.Mode mode, Listener actListener,
                          Context context)
        {
            super(context);
            itsMode = mode;
            itsActListener = actListener;
        }


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

        /* (non-Javadoc)
         * @see android.support.v4.content.AsyncTaskLoader#loadInBackground()
         */
        @Override
        public List<Map<String, Object>> loadInBackground()
        {
            PasswdSafeUtil.dbginfo(TAG, "loadInBackground %s", itsMode);
            return itsActListener.getBackgroundRecordItems(itsMode);
        }
    }
}

