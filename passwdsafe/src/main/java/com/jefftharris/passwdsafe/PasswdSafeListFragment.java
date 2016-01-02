/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

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
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.view.PasswdLocation;
import com.jefftharris.passwdsafe.view.PasswdRecordListData;

import java.util.List;

/**
 *  Fragment showing lists of items from a PasswdSafe file
 */
public class PasswdSafeListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<List<PasswdRecordListData>>,
                   View.OnClickListener
{
    /** Mode for which items are shown from the file */
    public enum Mode
    {
        NONE,
        GROUPS,
        RECORDS,
        ALL
    }

    /** Listener interface for owning activity */
    public interface Listener
    {
        /** Get the current record items in a background thread */
        List<PasswdRecordListData> getBackgroundRecordItems(boolean incRecords,
                                                            boolean incGroups);

        /** Change the location in the password file */
        void changeLocation(PasswdLocation location);

        /** Update the view for a list of records */
        void updateViewList(PasswdLocation location);
    }


    private Mode itsMode = Mode.NONE;
    private PasswdLocation itsLocation;
    private boolean itsIsContents = false;
    private Listener itsListener;
    private View itsGroupPanel;
    private TextView itsGroupLabel;
    private TextView itsEmptyText;
    private ItemListAdapter itsAdapter;

    /** Create a new instance */
    public static PasswdSafeListFragment newInstance(
            PasswdLocation location,
            @SuppressWarnings("SameParameterValue") boolean isContents)
    {
        PasswdSafeListFragment frag = new PasswdSafeListFragment();
        Bundle args = new Bundle();
        args.putParcelable("location", location);
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

        PasswdLocation location;
        boolean isContents = false;
        if (args != null) {
            location = args.getParcelable("location");
            isContents = args.getBoolean("isContents", false);
        } else {
            location = new PasswdLocation();
        }
        itsLocation = location;
        itsIsContents = isContents;
    }


    @Override
    public void onAttach(Context ctx)
    {
        super.onAttach(ctx);
        itsListener = (Listener)ctx;
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
        itsEmptyText = (TextView)root.findViewById(android.R.id.empty);

        return root;
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        itsAdapter = new ItemListAdapter(itsIsContents, getActivity());
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
            itsListener.updateViewList(itsLocation);
        }
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
    public void updateLocationView(PasswdLocation location, Mode mode)
    {
        itsLocation = location;
        itsMode = mode;
        refreshList();
    }


    /** Refresh the list due to file changes */
    private void refreshList()
    {
        if (!isResumed()) {
            return;
        }

        LoaderManager lm = getLoaderManager();
        lm.destroyLoader(0);
        lm.restartLoader(0, null, this);

        boolean groupVisible = false;
        switch (itsMode) {
        case RECORDS:
        case NONE: {
            groupVisible = false;
            break;
        }
        case GROUPS:
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
        int selPos = itsAdapter.setData(
                data, itsIsContents ? null : itsLocation.getRecord());
        if (isResumed()) {
            ListView list = getListView();
            if (selPos != -1) {
                list.setItemChecked(selPos, true);
                list.smoothScrollToPosition(selPos);
            } else {
                list.clearChoices();
            }

            if (itsEmptyText.getText().length() == 0) {
                itsEmptyText.setText(itsIsContents ? R.string.no_records :
                                             R.string.no_groups);
            }
        }
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoaderReset(android.support.v4.content.Loader)
     */
    @Override
    public void onLoaderReset(Loader<List<PasswdRecordListData>> loader)
    {
        onLoadFinished(loader, null);
    }


    /** List adapter for file items */
    private static class ItemListAdapter
            extends ArrayAdapter<PasswdRecordListData>
    {
        private final LayoutInflater itsInflater;
        private final boolean itsIsContents;

        /** Constructor */
        public ItemListAdapter(boolean isContents, Context context)
        {
            super(context, R.layout.passwdsafe_list_item, android.R.id.text1);
            itsInflater = (LayoutInflater)context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            itsIsContents = isContents;
        }

        /** Set the list data */
        public int setData(List<PasswdRecordListData> data,
                           String selectedRecord)
        {
            int selectedPos = -1;
            setNotifyOnChange(false);
            clear();
            if (data != null) {
                int idx = 0;
                for (PasswdRecordListData item: data) {
                    add(item);
                    if ((selectedRecord != null) &&
                        TextUtils.equals(item.itsUuid, selectedRecord)) {
                        selectedPos = idx;
                    }
                    ++idx;
                }
            }

            setNotifyOnChange(true);
            notifyDataSetChanged();
            return selectedPos;
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

            PasswdRecordListData item = getItem(position);
            ListView list = (ListView)parent;
            boolean isSelected = list.isItemChecked(position);
            itemViews.update(item, isSelected,
                             !itsIsContents && (item.itsRecord != null));
            return convertView;
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
            private final CheckBox itsSelection;
            private int itsLastIconImage;

            /** Constructor */
            public ViewHolder(View view)
            {
                itsTitle = (TextView)view.findViewById(android.R.id.text1);
                itsUser = (TextView)view.findViewById(android.R.id.text2);
                itsMatch = (TextView)view.findViewById(R.id.match);
                itsIcon = (ImageView)view.findViewById(R.id.icon);
                itsSelection = (CheckBox)view.findViewById(R.id.selection);
                itsLastIconImage = -1;
            }

            /** Update the fields for a list item */
            public void update(PasswdRecordListData item,
                               boolean selected,
                               boolean isLeftListRecord)
            {
                setText(itsTitle, item.itsTitle);
                setText(itsUser, item.itsUser);
                setText(itsMatch, item.itsMatch);
                if (itsLastIconImage != item.itsIcon) {
                    itsIcon.setImageResource(item.itsIcon);
                    itsLastIconImage = item.itsIcon;
                }
                itsSelection.setChecked(selected);
                GuiUtils.setVisible(itsSelection, isLeftListRecord);

                itsTitle.requestLayout();
            }

            /** Set text in a field */
            private static void setText(TextView tv, String text)
            {
                tv.setText((text == null) ? "" : text);
            }
        }
    }


    /** Loader for file items */
    private static class ItemLoader
            extends AsyncTaskLoader<List<PasswdRecordListData>>
    {
        private final Mode itsMode;
        private final Listener itsActListener;

        /** Constructor */
        public ItemLoader(Mode mode, Listener actListener,
                          Context context)
        {
            super(context);
            itsMode = mode;
            itsActListener = actListener;
        }

        /** Handle when the loader is started */
        @Override
        protected void onStartLoading()
        {
            forceLoad();
        }

        /* (non-Javadoc)
         * @see android.support.v4.content.AsyncTaskLoader#loadInBackground()
         */
        @Override
        public List<PasswdRecordListData> loadInBackground()
        {
            boolean incRecords = false;
            boolean incGroups = false;
            switch (itsMode) {
            case NONE: {
                break;
            }
            case RECORDS: {
                incRecords = true;
                break;
            }
            case GROUPS: {
                incGroups = true;
                break;
            }
            case ALL: {
                incRecords = true;
                incGroups = true;
                break;
            }
            }
            return itsActListener.getBackgroundRecordItems(incRecords,
                                                           incGroups);
        }
    }
}

