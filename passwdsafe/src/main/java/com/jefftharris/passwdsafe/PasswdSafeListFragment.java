/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.jefftharris.passwdsafe.view.PasswdLocation;
import com.jefftharris.passwdsafe.view.PasswdRecordListData;

import java.util.ArrayList;
import java.util.List;

/**
 *  Fragment showing lists of items from a PasswdSafe file
 */
public class PasswdSafeListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<List<PasswdRecordListData>>,
                   View.OnClickListener
{
    /** Listener interface for owning activity */
    public interface Listener
    {
        /** Mode for which items are shown from the file */
        enum Mode
        {
            GROUPS,
            RECORDS,
            ALL
        }

        /** Get the current record items in a background thread */
        List<PasswdRecordListData> getBackgroundRecordItems(Mode mode);

        /** Change the location in the password file */
        void changeLocation(PasswdLocation location);

        /** Update the view for the location in the password file */
        void updateLocationView(PasswdLocation location);
    }


    private Listener.Mode itsMode = Listener.Mode.GROUPS;
    private PasswdLocation itsLocation;
    private boolean itsIsContents = false;
    private Listener itsListener;
    private View itsGroupPanel;
    private TextView itsGroupLabel;
    private ItemListAdapter itsAdapter;


    /** Create a new instance */
    public static PasswdSafeListFragment newInstance(Listener.Mode mode,
                                                     PasswdLocation location,
                                                     boolean isContents)
    {
        PasswdSafeListFragment frag = new PasswdSafeListFragment();
        Bundle args = new Bundle();
        args.putString("mode", mode.toString());
        args.putStringArrayList("groups", location.getGroups());
        args.putBoolean("isContents", isContents);
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

        String modestr = null;
        ArrayList<String> groups = null;
        boolean isContents = false;
        if (args != null) {
            modestr = args.getString("mode");
            groups = args.getStringArrayList("groups");
            isContents = args.getBoolean("isContents", false);
        }
        itsMode = (modestr == null) ?
                Listener.Mode.GROUPS : Listener.Mode.valueOf(modestr);
        itsLocation = new PasswdLocation(groups);
        itsIsContents = isContents;
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

        itsGroupPanel = root.findViewById(R.id.current_group_panel);
        itsGroupPanel.setOnClickListener(this);
        itsGroupLabel = (TextView)root.findViewById(R.id.current_group_label);

        return root;
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        itsAdapter = new ItemListAdapter(getActivity());
        setListAdapter(itsAdapter);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onResume()
     */
    @Override
    public void onResume()
    {
        super.onResume();

        if (itsIsContents) {
            itsListener.updateLocationView(itsLocation);
        }
        refreshList();
    }

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
        PasswdRecordListData item = itsAdapter.getItem(position);
        if (item.itsRecord != null) {
            itsListener.changeLocation(itsLocation.selectRecord(item.itsUuid));
        } else {
            itsListener.changeLocation(itsLocation.selectGroup(item.itsTitle));
        }
    }


    /* (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick(View v)
    {
        switch(v.getId()) {
        case R.id.current_group_panel: {
            itsListener.changeLocation(itsLocation.popGroup());
            break;
        }
        }
    }


    /** Update the location shown by the list */
    public void updateLocationView(PasswdLocation location, Listener.Mode mode)
    {
        itsLocation = location;
        itsMode = mode;
        refreshList();
    }


    /** Refresh the list due to file changes */
    private void refreshList()
    {
        if (!isAdded()) {
            return;
        }

        LoaderManager lm = getLoaderManager();
        lm.restartLoader(0, null, this);

        boolean groupVisible = false;
        switch (itsMode) {
        case GROUPS: {
            groupVisible = true;
            break;
        }
        case RECORDS: {
            groupVisible = false;
            break;
        }
        case ALL: {
            groupVisible = true;
            break;
        }
        }

        if (groupVisible) {
            String groupPath = itsLocation.getGroupPath();
            if (TextUtils.isEmpty(groupPath)) {
                groupVisible = false;
            } else {
                itsGroupLabel.setText(groupPath);
            }
        }

        itsGroupPanel.setVisibility(groupVisible ? View.VISIBLE : View.GONE);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onCreateLoader(int, android.os.Bundle)
     */
    @Override
    public Loader<List<PasswdRecordListData>> onCreateLoader(int id, Bundle args)
    {
        return new ItemLoader(itsMode, itsListener, getActivity());
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoadFinished(android.support.v4.content.Loader, java.lang.Object)
     */
    @Override
    public void onLoadFinished(Loader<List<PasswdRecordListData>> loader,
                               List<PasswdRecordListData> data)
    {
        itsAdapter.setData(data);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoaderReset(android.support.v4.content.Loader)
     */
    @Override
    public void onLoaderReset(Loader<List<PasswdRecordListData>> loader)
    {
        itsAdapter.setData(null);
    }


    /** List adapter for file items */
    private static class ItemListAdapter
            extends ArrayAdapter<PasswdRecordListData>
    {
        private final LayoutInflater itsInflater;

        /** Constructor */
        public ItemListAdapter(Context context)
        {
            super(context, R.layout.passwdsafe_list_item, android.R.id.text1);
            itsInflater = (LayoutInflater)context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);

            // TODO: section list
        }

        /** Set the list data */
        public void setData(List<PasswdRecordListData> data)
        {
            clear();
            if (data != null) {
                for (PasswdRecordListData item: data) {
                    add(item);
                }
            }
        }

        /* (non-Javadoc)
         * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View view;
            if (convertView == null) {
                view = itsInflater.inflate(R.layout.passwdsafe_list_item,
                                           parent, false);
            } else {
                view = convertView;
            }

            // TODO: view holder

            PasswdRecordListData item = getItem(position);
            setTextView(view, android.R.id.text1, item.itsTitle);
            setTextView(view, android.R.id.text2, item.itsUser);
            setTextView(view, R.id.match, item.itsMatch);
            ((ImageView)view.findViewById(R.id.icon)).setImageResource(item.itsIcon);
            return view;
        }

        /** Set a text view */
        private static void setTextView(View view, int textId, String str)
        {
            if (str == null) {
                str = "";
            }
            ((TextView)view.findViewById(textId)).setText(str);
        }
    }


    /** Loader for file items */
    private static class ItemLoader
            extends AsyncTaskLoader<List<PasswdRecordListData>>
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
        public List<PasswdRecordListData> loadInBackground()
        {
            return itsActListener.getBackgroundRecordItems(itsMode);
        }
    }
}

