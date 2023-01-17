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
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;

import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.view.CopyField;
import net.tjado.passwdsafe.view.PasswdLocation;
import net.tjado.passwdsafe.view.PasswdRecordListData;
import net.tjado.passwdsafe.view.PasswdRecordListItemHolder;

import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;

import java.util.HashSet;
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

        /** Show the record preferences */
        void showRecordPreferences();

        /** Change the location in the password file */
        void changeLocation(PasswdLocation location);

        /** Update the view for a list of records */
        void updateViewList(PasswdLocation location);

        /** Does the activity have a menu */
        boolean activityHasMenu();

        /** Is the navigation drawer closed */
        boolean isNavDrawerClosed();

        void sendCredentialOverUsbByRecordLocation(final String recUuid);
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
    private View tvView;

    /* with this, we have our own TreeView state which
       is faster than the TV internal stateRestore */
    private HashSet<String> treeViewState;

    private static int[] scrollState;

    private static final String TAG = "AuthorizerFragmentTree";

    private static PasswdSafeListFragmentTree instance;
    public static synchronized PasswdSafeListFragmentTree newInstance(
            PasswdLocation location,
            @SuppressWarnings("SameParameterValue") boolean isContents,
            boolean search)
    {
        if( search ) {
            PasswdSafeUtil.dbginfo(TAG, "New search list fragment");
            return PasswdSafeListFragmentTree.newInstance_internal(location, isContents);
        }

        PasswdSafeUtil.dbginfo(TAG, "Standard list fragment (no search)");
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

        treeViewState = new HashSet<>();
    }

    @Override
    public void onAttach(Context ctx)
    {
        super.onAttach(ctx);
        itsListener = (Listener)ctx;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        View root = inflater.inflate(R.layout.fragment_passwdsafe_list_tree,
                                     container, false);

        itsEmptyText = (TextView) root.findViewById(R.id.empty);

        final TreeNode itsTreeNodeRoot = TreeNode.root();
        addGroup(itsTreeNodeRoot, itsRootLocation, 0);

        final AndroidTreeView itsAndroidTreeView = new AndroidTreeView(getActivity(), itsTreeNodeRoot);
        itsAndroidTreeView.setDefaultContainerStyle(R.style.TreeNodeStyleCustom, true);
        itsAndroidTreeView.setDefaultNodeClickListener(nodeClickListener);

        tvView = itsAndroidTreeView.getView();

        final ViewGroup containerView = (ViewGroup)root.findViewById(R.id.container);
        //containerView.removeAllViews();
        containerView.addView( tvView );

        if(scrollState != null)
            tvView.post(new Runnable() {
                public void run() {
                    tvView.scrollTo(scrollState[0], scrollState[1]);
                }
            });

        return root;
    }

    public int addGroup(TreeNode node, PasswdLocation location, int level) {

        int countEntries = 0;

        itsListener.updateViewList(location);
        List<PasswdRecordListData> data = itsListener.getBackgroundRecordItems(true,true);

        for (PasswdRecordListData item : data) {
            int countEntriesSub = 0;

            PasswdLocation sublocation;
            String icon = item.itsRecordIcon;
            String title = item.itsTitle;

            if( item.itsIsRecord ) {
                sublocation = location.selectRecord(item.itsTitle);
            } else {
                sublocation = location.selectGroup(item.itsTitle);
            }

            final String uuid = item.itsUuid;
            PasswdRecordListItemHolder itemHolder = new PasswdRecordListItemHolder(getActivity());
            itemHolder.setIcon2ViewOnClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    itsListener.sendCredentialOverUsbByRecordLocation(uuid);
                    return true;
                }
            });

            PasswdRecordListItemHolder.IconTreeItem nodeInfo = new PasswdRecordListItemHolder.IconTreeItem(level, icon, title, item.itsUuid, location);
            TreeNode subnode = new TreeNode(nodeInfo).setViewHolder(itemHolder);
            node.addChild(subnode);

            if(treeViewState.contains(subnode.getPath())) {
                PasswdSafeUtil.dbginfo(TAG, "TreeView State: expand " + subnode.getPath());
                subnode.setExpanded(true);
            }

            if( ! item.itsIsRecord ) {
                countEntriesSub += addGroup(subnode, sublocation, level + 1);
            } else {
                countEntriesSub++;
            }

            nodeInfo.setGroupCount(countEntriesSub);
            countEntries += countEntriesSub;
        }

        return countEntries;
    }

    private TreeNode.TreeNodeClickListener nodeClickListener = new TreeNode.TreeNodeClickListener() {
        @Override
        public void onClick(TreeNode node, Object value) {
            final PasswdRecordListItemHolder.IconTreeItem item = (PasswdRecordListItemHolder.IconTreeItem) value;

            if( node.isLeaf() ) {

                scrollState = new int[]{
                        tvView.getScrollX(),
                        tvView.getScrollY()
                };

                Handler handler = new Handler();
                handler.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        itsListener.changeLocation(
                                item.location.selectRecord(item.uuid));
                    }
                }, 50);
            } else {
                if (treeViewState.contains(node.getPath())) {
                    PasswdSafeUtil.dbginfo(TAG, "TreeView State: remove expand " + node.getPath());
                    treeViewState.remove(node.getPath());
                } else {
                    PasswdSafeUtil.dbginfo(TAG, "TreeView State: add expand " + node.getPath());
                    treeViewState.add(node.getPath());
                }
            }
        }
    };

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        if (itsListener.isCopySupported()) {
            registerForContextMenu(getListView());
        }
        itsAdapter = new ItemListAdapter(itsIsContents, getActivity());
        setListAdapter(itsAdapter);
    }

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

        if( tvView != null ) {
            scrollState = new int[]{
                    tvView.getScrollX(),
                    tvView.getScrollY()
            };
        }
    }


    @Override
    public void onDetach()
    {
        super.onDetach();
        itsListener = null;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater)
    {
        if (itsIsContents && (itsListener != null) &&
            itsListener.activityHasMenu() && itsListener.isNavDrawerClosed()) {
            inflater.inflate(R.menu.fragment_passwdsafe_list, menu);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.menu_sort) {
            if (itsListener != null) {
                itsListener.showRecordPreferences();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        itsLocation = location;
        itsMode = mode;
        refreshList();
    }

    /**
     * Update the record which is selected by the list
     */
    public void updateSelection(PasswdLocation location)
    {
        if (location.isRecord()) {
            itsSelectedRecord = location.getRecord();
            refreshList();
        }
    }


    /** Refresh the list due to file changes */
    private void refreshList()
    {
        if (!isResumed()) {
            return;
        }

        LoaderManager lm = LoaderManager.getInstance(this);
        if (lm.hasRunningLoaders()) {
            // Trash loader if running.  See
            // https://code.google.com/p/android/issues/detail?id=56464
            lm.destroyLoader(0);
        }
        lm.restartLoader(0, null, this);
    }


    @NonNull
    @Override
    public Loader<List<PasswdRecordListData>> onCreateLoader(int id, Bundle args)
    {
        return new ItemLoader(itsMode, itsListener, getActivity());
    }


    @Override
    public void onLoadFinished(@NonNull Loader<List<PasswdRecordListData>> loader,
                               List<PasswdRecordListData> data)
    {
        if (!isResumed()) {
            return;
        }


        int selPos = itsAdapter.setData(
                data,
                itsIsContents ? itsSelectedRecord : itsLocation.getRecord());


        ListView list = getListView();
        if (selPos != -1) {
            list.setItemChecked(selPos, true);
            list.smoothScrollToPosition(selPos);
        } else {
            list.clearChoices();
        }

        if (itsEmptyText.getText().length() == 0 && data.size() == 0) {
            itsEmptyText.setText(itsIsContents ? R.string.no_records : R.string.no_groups);
        }

    }


    @Override
    public void onLoaderReset(@NonNull Loader<List<PasswdRecordListData>> loader)
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
                    //PasswdSafeUtil.dbginfo("test_tm", item.itsTitle);
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

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            return convertView;
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

