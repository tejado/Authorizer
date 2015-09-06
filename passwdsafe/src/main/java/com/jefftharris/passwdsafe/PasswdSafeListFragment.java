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
import android.widget.SectionIndexer;
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
    // TODO: sort case pref
    private boolean itsIsSortCaseSensitive = false;


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
        itsAdapter = new ItemListAdapter(itsIsSortCaseSensitive, getActivity());
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
            extends ArrayAdapter<PasswdRecordListData> implements SectionIndexer
    {
        private final LayoutInflater itsInflater;
        private final boolean itsIsCaseSensitive;
        private Section[] itsSections;
        // TODO: keep fast scrolling??

        /** Constructor */
        public ItemListAdapter(boolean caseSensitive, Context context)
        {
            super(context, R.layout.passwdsafe_list_item, android.R.id.text1);
            itsInflater = (LayoutInflater)context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            itsIsCaseSensitive = caseSensitive;
        }

        /** Set the list data */
        public void setData(List<PasswdRecordListData> data)
        {
            setNotifyOnChange(false);
            clear();
            ArrayList<Section> sections = new ArrayList<>();
            if (data != null) {
                char compChar = '\0';
                int idx = 0;
                for (PasswdRecordListData item: data) {
                    add(item);

                    char first = TextUtils.isEmpty(item.itsTitle) ?
                            ' ' : item.itsTitle.charAt(0);
                    char compFirst = itsIsCaseSensitive ?
                            first : Character.toUpperCase(first);
                    if (compChar != compFirst) {
                        Section s = new Section(Character.toString(compFirst), idx);
                        sections.add(s);
                        compChar = compFirst;
                    }
                    ++idx;
                }
            }

            itsSections = sections.toArray(new Section[sections.size()]);
            setNotifyOnChange(true);
            notifyDataSetChanged();
        }

        /* (non-Javadoc)
         * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            ViewHolder itemViews;
            if (convertView == null) {
                convertView = itsInflater.inflate(R.layout.passwdsafe_list_item,
                                                  parent, false);
                itemViews = new ViewHolder(convertView);
                convertView.setTag(itemViews);
            } else {
                itemViews = (ViewHolder)convertView.getTag();
            }

            itemViews.update(getItem(position));
            return convertView;
        }

        @Override
        public int getPositionForSection(int section)
        {
            if (section < itsSections.length) {
                return itsSections[section].itsPos;
            } else {
                return 0;
            }
        }

        @Override
        public Object[] getSections()
        {
            return itsSections;
        }

        @Override
        public int getSectionForPosition(int position)
        {
            // Section positions in increasing order
            for (int i = 0; i < itsSections.length; ++i) {
                Section s = itsSections[i];
                if (position <= s.itsPos) {
                    return i;
                }
            }
            return 0;
        }

        /**
         * Holder for the views for an item in the list
         */
        private static class ViewHolder
        {
            private final TextView itsTitle;
            private final TextView itsUser;
            private final TextView itsMatch;
            private final ImageView itsIcon;
            private int itsLastIconImage;

            /** Constructor */
            public ViewHolder(View view)
            {
                itsTitle = (TextView)view.findViewById(android.R.id.text1);
                itsUser = (TextView)view.findViewById(android.R.id.text2);
                itsMatch = (TextView)view.findViewById(R.id.match);
                itsIcon = (ImageView)view.findViewById(R.id.icon);
                itsLastIconImage = -1;
            }

            /** Update the fields for a list item */
            public void update(PasswdRecordListData item)
            {
                setText(itsTitle, item.itsTitle);
                setText(itsUser, item.itsUser);
                setText(itsMatch, item.itsMatch);
                if (itsLastIconImage != item.itsIcon) {
                    itsIcon.setImageResource(item.itsIcon);
                    itsLastIconImage = item.itsIcon;
                }

                itsTitle.requestLayout();
            }

            /** Set text in a field */
            private static void setText(TextView tv, String text)
            {
                tv.setText((text == null) ? "" : text);
            }
        }

        /**
         * A section object
         */
        private static final class Section
        {
            public final String itsName;
            public final int itsPos;

            /** Constructor */
            public Section(String name, int pos)
            {
                itsName = name;
                itsPos = pos;
            }

            @Override
            public final String toString()
            {
                return itsName;
            }
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

