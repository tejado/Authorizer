/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.view.GuiUtils;
import net.tjado.passwdsafe.view.CopyField;
import net.tjado.passwdsafe.view.PasswdLocation;
import net.tjado.passwdsafe.view.PasswdRecordListData;
import net.tjado.passwdsafe.view.PasswdRecordListHeaderHolder;
import net.tjado.passwdsafe.view.PasswdRecordListItemHolder;
import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;

import java.util.List;


/**
 *  Fragment showing lists of items from a PasswdSafe file
 */
public class PasswdSafeListFragmentTree extends ListFragment
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

        /** Is copying supported */
        boolean isCopySupported();

        /** Copy a field */
        void copyField(CopyField field, String recUuid);

        /** Change the location in the password file */
        void changeLocation(PasswdLocation location);

        /** Update the view for a list of records */
        void updateViewList(PasswdLocation location);
    }

    private static final String STATE_SELECTED_RECORD = "selectedRecord";

    private Mode itsMode = Mode.NONE;
    private PasswdLocation itsRootLocation;
    private PasswdLocation itsLocation;
    private boolean itsIsContents = false;
    private Listener itsListener;
    private TextView itsEmptyText;
    private ItemListAdapter itsAdapter;
    private String itsSelectedRecord;
    private AndroidTreeView itsAndroidTreeView;
    private TreeNode itsTreeNodeRoot;
    private View itsRoot;

    private static PasswdSafeListFragmentTree instance;
    public static synchronized PasswdSafeListFragmentTree newInstance(
            PasswdLocation location,
            @SuppressWarnings("SameParameterValue") boolean isContents)
    {
        if (PasswdSafeListFragmentTree.instance == null) {
            PasswdSafeListFragmentTree.instance = PasswdSafeListFragmentTree.newInstance_internal(location, isContents);
        }
        return PasswdSafeListFragmentTree.instance;
    }

    /** Create a new instance */
    public static PasswdSafeListFragmentTree newInstance_internal(
            PasswdLocation location,
            @SuppressWarnings("SameParameterValue") boolean isContents)
    {
        PasswdSafeListFragmentTree frag = new PasswdSafeListFragmentTree();
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
        itsRootLocation = location;
        itsLocation = location;
        itsIsContents = isContents;

        if (savedInstanceState != null) {
            itsSelectedRecord =
                    savedInstanceState.getString(STATE_SELECTED_RECORD);
        }
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
        View root = inflater.inflate(R.layout.fragment_passwdsafe_list_tree,
                                     container, false);

        itsEmptyText = (TextView)root.findViewById(android.R.id.empty);


        itsRoot = root;
        /*
        List<String> x = itsLocation.getGroups();
        String y = itsLocation.getRecord();

        PasswdSafeUtil.dbginfo("test", String.valueOf(x.size()));
        for(String n : x) {
            PasswdSafeUtil.dbginfo("test", n);
            TreeNode node = new TreeNode(new PasswdRecordListItemHolder.IconTreeItem(R.string.ic_folder, n)).setViewHolder(new PasswdRecordListHeaderHolder(getActivity()));
            itsTreeNodeRoot.addChild(node);
        }

        TreeNode parent = new TreeNode(new PasswdRecordListItemHolder.IconTreeItem(R.string.ic_folder, "Folder with very long name 1")).setViewHolder(new PasswdRecordListHeaderHolder(getActivity()));
        TreeNode child0 = new TreeNode(new PasswdRecordListItemHolder.IconTreeItem(R.string.ic_folder, "Folder with very long name 2")).setViewHolder(new PasswdRecordListHeaderHolder(getActivity()));
        TreeNode child1 = new TreeNode(new PasswdRecordListItemHolder.IconTreeItem(R.string.ic_folder, "Folder with very long name 3")).setViewHolder(new PasswdRecordListHeaderHolder(getActivity()));
        parent.addChildren(child0, child1);
        itsTreeNodeRoot.addChild(parent);

        TreeNode parent1 = new TreeNode(new PasswdRecordListItemHolder.IconTreeItem(R.string.ic_folder, "Folder with very long name 4")).setViewHolder(new PasswdRecordListHeaderHolder(getActivity()));
        TreeNode child01 = new TreeNode(new PasswdRecordListItemHolder.IconTreeItem(R.string.ic_folder, "Folder with very long name 5")).setViewHolder(new PasswdRecordListHeaderHolder(getActivity()));
        TreeNode child11 = new TreeNode(new PasswdRecordListItemHolder.IconTreeItem(R.string.ic_folder, "Folder with very long name 6")).setViewHolder(new PasswdRecordListHeaderHolder(getActivity()));
        parent1.addChildren(child01, child11);
        itsTreeNodeRoot.addChild(parent1);*/


        itsTreeNodeRoot = TreeNode.root();

        addGroup(itsRootLocation, itsTreeNodeRoot);

        //itsListener.updateViewList(itsRootLocation);
        //List<PasswdRecordListData> data3 = itsListener.getBackgroundRecordItems(true,true);

        /*
        if (data3 != null) {
            for (PasswdRecordListData item : data3) {

                int icon = R.string.ic_drive_file;
                String title = item.itsTitle;
                if( ! item.itsIsRecord ) {
                    icon = R.string.ic_folder;

                    TreeNode node = new TreeNode(
                            new PasswdRecordListItemHolder.IconTreeItem(
                                    icon, title))
                            .setViewHolder(new PasswdRecordListHeaderHolder(
                                    getActivity()));
                    itsTreeNodeRoot.addChild(node);

                    PasswdLocation newLoc = itsRootLocation.selectGroup(item.itsTitle);
                    itsListener.updateViewList(newLoc);
                    List<PasswdRecordListData> data2 = itsListener.getBackgroundRecordItems(true,true);

                    for (PasswdRecordListData item2 : data2) {
                        String title2 = item2.itsTitle;
                        int icon2 = R.string.ic_drive_file;
                        TreeNode node1 = new TreeNode(
                                new PasswdRecordListItemHolder.IconTreeItem(
                                        icon2, title2))
                                .setViewHolder(new PasswdRecordListHeaderHolder(
                                        getActivity()));
                        node.addChild(node1);
                    }
                } else {

                    TreeNode node = new TreeNode(
                            new PasswdRecordListItemHolder.IconTreeItem(
                                    icon, title))
                            .setViewHolder(new PasswdRecordListHeaderHolder(
                                    getActivity()));
                    itsTreeNodeRoot.addChild(node);
                }
            }
        }*/


        itsAndroidTreeView = new AndroidTreeView(getActivity(), itsTreeNodeRoot);
        itsAndroidTreeView.setDefaultAnimation(true);
        itsAndroidTreeView.setUse2dScroll(true);
        itsAndroidTreeView.setDefaultContainerStyle(R.style.TreeNodeStyleCustom);
        itsAndroidTreeView.setDefaultNodeClickListener(nodeClickListener);

        ViewGroup containerView = (ViewGroup) itsRoot.findViewById(R.id.container);
        containerView.removeAllViews();
        containerView.addView(itsAndroidTreeView.getView());

        return root;
    }

    public void addGroup(PasswdLocation location, TreeNode node) {

        itsListener.updateViewList(location);
        List<PasswdRecordListData> data = itsListener.getBackgroundRecordItems(true,true);

        for (PasswdRecordListData item : data) {
            PasswdLocation sublocation;
            int icon;
            String title = item.itsTitle;

            if( item.itsIsRecord ) {
                sublocation = location.selectRecord(item.itsTitle);
                icon = R.string.ic_drive_file;
            } else {
                sublocation = location.selectGroup(item.itsTitle);
                icon = R.string.ic_folder;
            }

            TreeNode subnode = new TreeNode(
                    new PasswdRecordListItemHolder.IconTreeItem(icon, title, item.itsUuid, location))
                    .setViewHolder(new PasswdRecordListHeaderHolder(getActivity()));
            node.addChild(subnode);

            if( ! item.itsIsRecord ) {
                addGroup(sublocation, subnode);
            }
        }
    }

    private TreeNode.TreeNodeClickListener nodeClickListener = new TreeNode.TreeNodeClickListener() {
        @Override
        public void onClick(TreeNode node, Object value) {
            final PasswdRecordListItemHolder.IconTreeItem item = (PasswdRecordListItemHolder.IconTreeItem) value;

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    itsListener.changeLocation(item.location.selectRecord(item.uuid));
                }
            }, 100);
        }
    };


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        if (itsListener.isCopySupported()) {
            registerForContextMenu(getListView());
        }
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
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SELECTED_RECORD, itsSelectedRecord);
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        itsListener = null;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo)menuInfo;
        PasswdRecordListData listItem = itsAdapter.getItem(info.position);
        if (listItem.itsIsRecord) {
            menu.setHeaderTitle(listItem.itsTitle);

            int group = itsIsContents ? PasswdSafe.CONTEXT_GROUP_LIST_CONTENTS :
                        PasswdSafe.CONTEXT_GROUP_LIST;
            menu.add(group, R.id.menu_copy_user, 0, R.string.copy_user);
            menu.add(group, R.id.menu_copy_password, 0, R.string.copy_password);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        int group = itsIsContents ? PasswdSafe.CONTEXT_GROUP_LIST_CONTENTS :
                    PasswdSafe.CONTEXT_GROUP_LIST;
        if (item.getGroupId() != group) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
        case R.id.menu_copy_password:
        case R.id.menu_copy_user: {
                AdapterView.AdapterContextMenuInfo info =
                    (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
            final PasswdRecordListData listItem =
                    itsAdapter.getItem(info.position);
            if (listItem.itsIsRecord) {
                itsSelectedRecord = listItem.itsUuid;
                itsListener.copyField(
                        (item.getItemId() == R.id.menu_copy_password) ?
                        CopyField.PASSWORD : CopyField.USER_NAME,
                        listItem.itsUuid);
            }

            return true;
        }
        }
        return super.onContextItemSelected(item);
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onListItemClick(android.widget.ListView, android.view.View, int, long)
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        PasswdRecordListData item = itsAdapter.getItem(position);
        if (item.itsIsRecord) {
            itsSelectedRecord = item.itsUuid;
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
        /*
        switch(v.getId()) {
            case R.id.current_group_panel: {
                itsListener.changeLocation(itsLocation.popGroup());
                break;
            }
        }
        */
    }


    /** Update the location shown by the list */
    public void updateLocationView(PasswdLocation location, Mode mode)
    {
        /* dummy */

        //itsLocation = location;
        //itsMode = mode;
        //refreshList();
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

        /*
        if (groupVisible) {
            String groupPath = itsLocation.getGroupPath();
            if (TextUtils.isEmpty(groupPath)) {
                groupVisible = false;
            } else {
                itsGroupLabel.setText(groupPath);
            }
        }

        itsGroupPanel.setVisibility(groupVisible ? View.VISIBLE : View.GONE);
        */
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
                data,
                itsIsContents ? itsSelectedRecord : itsLocation.getRecord());


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
                    PasswdSafeUtil.dbginfo("test_tm", item.itsTitle);
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

            if (position < getCount()) {
                PasswdRecordListData item = getItem(position);
                ListView list = (ListView)parent;
                boolean isSelected = list.isItemChecked(position);
                itemViews.update(item, isSelected,
                                 !itsIsContents && item.itsIsRecord);
            } else {
                itemViews.reset();
            }
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

            /** Reset the fields */
            public void reset()
            {
                setText(itsTitle, null);
                setText(itsUser, null);
                setText(itsMatch, null);
                itsIcon.setImageDrawable(null);
                itsLastIconImage = -1;
                itsSelection.setChecked(false);
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

