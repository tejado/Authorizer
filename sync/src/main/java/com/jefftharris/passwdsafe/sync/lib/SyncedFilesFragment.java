/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuItemCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.Utils;
import com.jefftharris.passwdsafe.sync.R;

/**
 *  Fragment to show synced password files
 */
public class SyncedFilesFragment extends ListFragment
{
    /** Listener interface for the owning activity */
    public interface Listener
    {
        /** Callback to handle the result of listing files */
        interface ListFilesCb
        {
            void handleFiles(List<ProviderRemoteFile> files);
        }

        /** List files for a given path */
        void listFiles(String path, ListFilesCb cb);

        /** Change directory to the given path */
        void changeDir(String pathName, String pathId);

        /** Change directory to the parent path */
        void changeParentDir();

        /** Is the given file selected to be synced */
        boolean isSelected(String filePath);

        /** Update whether a file is synced */
        void updateFileSynced(ProviderRemoteFile file, boolean synced);
    }

    private static final String TAG = "SyncedFilesFragment";

    private String itsPathName;
    private String itsPathId;
    private Listener itsListener;
    private ArrayAdapter<ListItem> itsFilesAdapter;
    private ProgressBar itsProgressBar;
    private int itsProgressBarRefCount = 0;

    /** Create a new instance of the fragment */
    public static SyncedFilesFragment newInstance(String pathName,
                                                  String pathId)
    {
        SyncedFilesFragment frag = new SyncedFilesFragment();
        Bundle args = new Bundle();
        args.putString("pathName", pathName);
        args.putString("pathId", pathId);
        frag.setArguments(args);
        return frag;
    }


    @Override
    public void onAttach(Context ctx)
    {
        super.onAttach(ctx);
        itsListener = (Listener)ctx;
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        itsPathName = getArguments().getString("pathName");
        itsPathId = getArguments().getString("pathId");
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        View rootView = inflater.inflate(R.layout.fragment_synced_files,
                                         container, false);

        TextView path = (TextView)rootView.findViewById(R.id.path);
        path.setText(getString(R.string.choose_sync_files_from_dir,
                               itsPathName));

        itsProgressBar = (ProgressBar)rootView.findViewById(R.id.progress);
        itsProgressBar.setVisibility(View.GONE);

        return rootView;
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        itsFilesAdapter = new FilesAdapter(getActivity());
        setListAdapter(itsFilesAdapter);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onResume()
     */
    @Override
    public void onResume()
    {
        super.onResume();
        reload();
    }

    /**
     * Initialize the contents of the Activity's standard options menu
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.fragment_synced_files, menu);

        boolean parentEnabled =
                !TextUtils.equals(ProviderRemoteFile.PATH_SEPARATOR, itsPathId);

        MenuItem item = menu.findItem(R.id.menu_parent_dir);
        MenuItemCompat.setShowAsAction(item,
                                       MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        item.setVisible(parentEnabled);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * This hook is called whenever an item in your options menu is selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_parent_dir: {
            itsListener.changeParentDir();
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
        ListItem item = itsFilesAdapter.getItem(position);
        if (item == null) {
            return;
        }

        ProviderRemoteFile file = item.itsFile;
        if (file.isFolder()) {
            itsListener.changeDir(file.getPath(), file.getRemoteId());
        } else {
            boolean newSelected = !item.itsIsSelected;
            PasswdSafeUtil.dbginfo(TAG, "item selected %b: %s",
                                   newSelected, file.toDebugString());
            item.itsIsSelected = newSelected;
            itsListener.updateFileSynced(file, newSelected);
        }
    }


    /** Reload the files shown by the fragment */
    public void reload()
    {
        PasswdSafeUtil.dbginfo(TAG, "reload");
        if (itsProgressBarRefCount++ <= 0) {
            itsProgressBar.setVisibility(View.VISIBLE);
            itsProgressBarRefCount = 1;
        }
        itsListener.listFiles(itsPathId, new Listener.ListFilesCb()
        {
            @Override
            public void handleFiles(List<ProviderRemoteFile> files)
            {
                if (files != null) {
                    itsFilesAdapter.clear();
                    for (ProviderRemoteFile file: files) {
                        PasswdSafeUtil.dbginfo(TAG, "list file: %s",
                                               file.toDebugString());
                        boolean selected =
                                itsListener.isSelected(file.getRemoteId());
                        itsFilesAdapter.add(new ListItem(file, selected));
                    }
                    itsFilesAdapter.sort(new ListItemComparator());
                    itsFilesAdapter.notifyDataSetChanged();
                }

                if (--itsProgressBarRefCount <= 0) {
                    itsProgressBar.setVisibility(View.GONE);
                    itsProgressBarRefCount = 0;
                }
            }
        });
    }


    /** Update the state of synced files */
    public void updateSyncedFiles()
    {
        PasswdSafeUtil.dbginfo(TAG, "updateSyncedFiles count %d",
                               itsFilesAdapter.getCount());
        for (int i = 0; i < itsFilesAdapter.getCount(); ++i) {
            ListItem item = itsFilesAdapter.getItem(i);
            item.itsIsSelected =
                    itsListener.isSelected(item.itsFile.getRemoteId());
        }
        itsFilesAdapter.notifyDataSetChanged();
    }


    /** Holder for each item in the list view */
    private static class ListItem
    {
        public final ProviderRemoteFile itsFile;
        public boolean itsIsSelected;

        /** Constructor */
        public ListItem(ProviderRemoteFile file, boolean selected)
        {
            itsFile = file;
            itsIsSelected = selected;
        }
    }


    /** Adapter for files shown in the list */
    private static class FilesAdapter extends ArrayAdapter<ListItem>
    {
        private final LayoutInflater itsInflater;

        /** Constructor */
        public FilesAdapter(Activity act)
        {
            super(act, R.layout.listview_sync_file_item);
            setNotifyOnChange(false);
            itsInflater = act.getLayoutInflater();
        }


        /* (non-Javadoc)
         * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            ViewHolder views;
            if (convertView == null) {
                convertView = itsInflater.inflate(
                        R.layout.listview_sync_file_item, parent, false);
                views = new ViewHolder(convertView);
                convertView.setTag(views);
            } else {
                views = (ViewHolder)convertView.getTag();
            }

            ListItem item = getItem(position);
            ProviderRemoteFile file = item.itsFile;
            views.itsText.setText(file.getTitle());
            views.itsText.requestLayout();

            if (file.isFolder()) {
                views.itsSelected.setVisibility(View.GONE);
                views.itsModDate.setVisibility(View.GONE);
                views.itsIcon.setImageResource(R.drawable.folder_rev);
            } else {
                views.itsSelected.setVisibility(View.VISIBLE);
                views.itsSelected.setChecked(item.itsIsSelected);
                views.itsModDate.setVisibility(View.VISIBLE);
                views.itsModDate.setText(Utils.formatDate(file.getModTime(),
                                                          getContext()));
                views.itsIcon.setImageResource(R.drawable.login_rev);
            }

            return convertView;
        }

        /** View holder for fields in a list item */
        private static class ViewHolder
        {
            public final TextView itsText;
            public final TextView itsModDate;
            public final ImageView itsIcon;
            public final CheckBox itsSelected;

            /** Constructor */
            public ViewHolder(View view)
            {
                itsText = (TextView)view.findViewById(R.id.text);
                itsModDate = (TextView)view.findViewById(R.id.mod_date);
                itsIcon = (ImageView)view.findViewById(R.id.icon);
                itsSelected = (CheckBox)view.findViewById(R.id.selected);
            }
        }
    }


    /** File comparator */
    private static final class ListItemComparator
            implements Comparator<ListItem>
    {
        @Override
        public int compare(ListItem lhs, ListItem rhs)
        {
            ProviderRemoteFile lhsFile = lhs.itsFile;
            ProviderRemoteFile rhsFile = rhs.itsFile;
            boolean lhsFolder = lhsFile.isFolder();
            boolean rhsFolder = rhsFile.isFolder();
            if (!lhsFolder && rhsFolder) {
                return -1;
            } else if (!rhsFolder && lhsFolder) {
                return 1;
            } else {
                return lhsFile.getTitle().compareToIgnoreCase(
                        rhsFile.getTitle());
            }
        }
    }
}
