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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.view.GuiUtils;
import net.tjado.passwdsafe.view.CopyField;
import net.tjado.passwdsafe.view.PasswdLocation;
import net.tjado.passwdsafe.view.PasswdRecordListData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


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
    }

    private static final String STATE_SELECTED_RECORD = "selectedRecord";
    private static final String STATE_SELECTED_POS = "selectedPos";

    private Mode itsMode = Mode.NONE;
    private PasswdLocation itsLocation;
    private boolean itsIsContents = false;
    private Listener itsListener;
    private View itsGroupPanel;
    private TextView itsGroupLabel;
    private TextView itsEmptyText;
    private ItemListAdapter itsAdapter;
    private String itsSelectedRecord;
    private int itsSelectedPos = -1;

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
    public void onAttach(@NonNull Context ctx)
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
        View root = inflater.inflate(R.layout.fragment_passwdsafe_list,
                                     container, false);

        itsGroupPanel = root.findViewById(R.id.current_group_panel);
        itsGroupPanel.setOnClickListener(this);
        itsGroupLabel = root.findViewById(R.id.current_group_label);
        itsEmptyText = root.findViewById(android.R.id.empty);

        return root;
    }


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
    public void onViewStateRestored(@Nullable Bundle savedInstanceState)
    {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            itsSelectedRecord =
                    savedInstanceState.getString(STATE_SELECTED_RECORD);
            itsSelectedPos =
                    savedInstanceState.getInt(STATE_SELECTED_POS, -1);
        }
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
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SELECTED_RECORD, itsSelectedRecord);
        outState.putInt(STATE_SELECTED_POS, itsSelectedPos);
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
        if ((listItem != null) && listItem.itsIsRecord) {
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

        int itemId = item.getItemId();
        if ((itemId == R.id.menu_copy_password) ||
            (itemId == R.id.menu_copy_user)) {
            AdapterView.AdapterContextMenuInfo info =
                    (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
            final PasswdRecordListData listItem = (info != null) ? itsAdapter.getItem(info.position) : null;
            if ((listItem != null) && listItem.itsIsRecord) {
                itsSelectedRecord = listItem.itsUuid;
                itsListener.copyField(
                        (item.getItemId() == R.id.menu_copy_password) ?
                                CopyField.PASSWORD : CopyField.USER_NAME,
                        listItem.itsUuid);
            }

            return true;
        }
        return super.onContextItemSelected(item);
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        if (!isResumed()) {
            return;
        }
        PasswdRecordListData item = itsAdapter.getItem(position);
        if (item == null) {
            return;
        }
        if (item.itsIsRecord) {
            itsSelectedRecord = item.itsUuid;
            itsSelectedPos = position;
            itsListener.changeLocation(itsLocation.selectRecord(item.itsUuid));
        } else {
            itsListener.changeLocation(itsLocation.selectGroup(item.itsTitle));
        }
    }


    @Override
    public void onClick(View v)
    {
        if (v.getId() == R.id.current_group_panel) {
            itsListener.changeLocation(itsLocation.popGroup());
        }
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
            itsSelectedPos = -1;
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

        boolean groupVisible = false;
        switch (itsMode) {
        case RECORDS:
        case NONE: {
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

        ListView list = getListView();
        int firstPos = list.getFirstVisiblePosition();
        int lastPos = list.getLastVisiblePosition();
        View topView = list.getChildAt(0);
        int top = (topView == null) ?
                0 : (topView.getTop() - list.getPaddingTop());

        int selPos = itsAdapter.setData(
                data,
                itsIsContents ? itsSelectedRecord : itsLocation.getRecord());
        if (selPos != -1) {
            if (itsIsContents) {
                // List typically takes care of position unless it changes
                if (selPos != itsSelectedPos) {
                    list.setSelection(selPos);
                    itsSelectedPos = selPos;
                }
            } else {
                // If item is outside previous visible range, update selection.
                // Otherwise don't scroll to reduce jumpy UI
                if ((selPos <= firstPos) ||
                    ((lastPos > firstPos) && (selPos > lastPos))) {
                    list.setSelection(selPos);
                } else {
                    list.setSelectionFromTop(firstPos, top);
                }

                list.setItemChecked(selPos, true);
                list.smoothScrollToPosition(selPos);
            }
        } else {
            itsSelectedPos = -1;
            list.clearChoices();
        }

        if (itsEmptyText.getText().length() == 0) {
            itsEmptyText.setText(itsIsContents ? R.string.no_records : R.string.no_groups);
        }
    }


    @Override
    public void onLoaderReset(@NonNull Loader<List<PasswdRecordListData>> loader)
    {
        onLoadFinished(loader, null);
    }


    /** List adapter for file items */
    private static final class ItemListAdapter
            extends ArrayAdapter<PasswdRecordListData>
            implements SectionIndexer
    {
        private final LayoutInflater itsInflater;
        private final boolean itsIsContents;
        private ItemSection[] itsSections = new ItemSection[0];

        /** Constructor */
        private ItemListAdapter(boolean isContents, Context context)
        {
            super(context, R.layout.passwdsafe_list_item, android.R.id.text1);
            itsInflater = (LayoutInflater)context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            itsIsContents = isContents;
        }

        /** Set the list data */
        private int setData(List<PasswdRecordListData> data,
                           String selectedRecord)
        {
            int selectedPos = -1;
            setNotifyOnChange(false);
            clear();

            ArrayList<ItemSection> sectionIdxs = new ArrayList<>();
            ItemSection currSection = null;

            if (data != null) {
                int idx = 0;
                for (PasswdRecordListData item: data) {
                    add(item);

                    String str = TextUtils.isEmpty(item.itsTitle) ?
                            " " : item.itsTitle.substring(0, 1).toUpperCase();
                    if ((idx == 0) ||
                        !TextUtils.equals(currSection.itsTitle, str)) {
                        currSection = new ItemSection(str, idx);
                        sectionIdxs.add(currSection);
                    }

                    if ((selectedRecord != null) &&
                        TextUtils.equals(item.itsUuid, selectedRecord)) {
                        selectedPos = idx;
                    }
                    ++idx;
                }
            }

            itsSections = sectionIdxs.toArray(new ItemSection[0]);
            setNotifyOnChange(true);
            notifyDataSetChanged();
            return selectedPos;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView,  @NonNull ViewGroup parent)
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
                PasswdRecordListData item =
                        Objects.requireNonNull(getItem(position));
                ListView list = (ListView)parent;
                boolean isSelected = list.isItemChecked(position);
                itemViews.update(item, isSelected,
                                 !itsIsContents && item.itsIsRecord);
            } else {
                itemViews.reset();
            }
            return convertView;
        }

        @Override
        public Object[] getSections()
        {
            return itsSections;
        }

        @Override
        public int getPositionForSection(int sectionIndex)
        {
            if (sectionIndex < 0) {
                return 0;
            } else if (sectionIndex >= itsSections.length) {
                return itsSections[itsSections.length - 1].itsPosition;
            }
            return itsSections[sectionIndex].itsPosition;
        }

        @Override
        public int getSectionForPosition(int position)
        {
            for (int section = 0; section < itsSections.length; ++section)
            {
                if (itsSections[section].itsPosition >= position) {
                    return section;
                }
            }
            return 0;
        }

        /**
         * An indexed section item
         */
        private static final class ItemSection
        {
            private final String itsTitle;
            private final int itsPosition;

            /**
             * Constructor
             */
            private ItemSection(String title, int position)
            {
                itsTitle = title;
                itsPosition = position;
            }

            @Override
            @NonNull
            public String toString()
            {
                return itsTitle;
            }
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
            private final View itsSelection;
            private int itsLastIconImage;

            /** Constructor */
            protected ViewHolder(View view)
            {
                itsTitle = view.findViewById(android.R.id.text1);
                itsUser = view.findViewById(android.R.id.text2);
                itsMatch = view.findViewById(R.id.match);
                itsIcon = view.findViewById(R.id.icon);
                itsSelection = view.findViewById(R.id.selection);
                itsLastIconImage = -1;
            }

            /** Update the fields for a list item */
            protected void update(PasswdRecordListData item,
                               boolean selected,
                               boolean isLeftListRecord)
            {
                setText(itsTitle, item.itsTitle);
                setText(itsUser, item.itsUser);
                setText(itsMatch, item.itsMatch);
                if (itsLastIconImage != item.itsAppIcon) {
                    itsIcon.setImageResource(item.itsAppIcon);
                    itsLastIconImage = item.itsAppIcon;
                }
                GuiUtils.setVisible(itsSelection, isLeftListRecord && selected);

                itsTitle.requestLayout();
            }

            /** Reset the fields */
            protected void reset()
            {
                setText(itsTitle, null);
                setText(itsUser, null);
                setText(itsMatch, null);
                itsIcon.setImageDrawable(null);
                itsLastIconImage = -1;
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
        protected ItemLoader(Mode mode, Listener actListener,
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

