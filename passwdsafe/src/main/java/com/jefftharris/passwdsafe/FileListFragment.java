/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.Utils;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.util.FileComparator;

/**
 * The FileListFragment allows the user to choose which file to open
 */
public final class FileListFragment extends ListFragment
        implements LoaderCallbacks<List<Map<String, Object>>>
{
    /** Listener interface for the owning activity */
    public interface Listener
    {
        /** Open a file */
        void openFile(Uri uri, String fileName);

        /** Does the activity have a menu */
        boolean activityHasMenu();

        /** Does the activity have a 'none' item */
        boolean activityHasNoneItem();
    }

    /** File data information for the list */
    public static final class FileData
    {
        public final File itsFile;
        private final String itsName;

        public FileData(File f)
        {
            itsFile = f;
            itsName = itsFile.getName();
        }

        /** Constructor for a null file */
        public FileData(Context ctx)
        {
            itsFile = null;
            itsName = ctx.getString(R.string.none_paren);
        }

        @Override
        public final String toString()
        {
            return itsName;
        }

        /** Does the data indicate a directory */
        public boolean isDirectory()
        {
            return (itsFile != null) && (itsFile.isDirectory());
        }
    }

    private static final String TAG = "FileListFragment";

    private static final String TITLE = "title";
    private static final String ICON = "icon";
    private static final String MOD_DATE = "mod_date";

    private File itsDir;
    private final LinkedList<File> itsDirHistory = new LinkedList<>();
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
        View view = inflater.inflate(R.layout.fragment_file_list,
                                     container, false);

        View.OnClickListener parentListener = new View.OnClickListener()
        {
            public final void onClick(View v)
            {
                doParentPressed();
            }
        };
        View v = view.findViewById(R.id.up_icon);
        v.setOnClickListener(parentListener);
        v = view.findViewById(R.id.current_group_label);
        v.setOnClickListener(parentListener);

        v = view.findViewById(R.id.home);
        v.setOnClickListener(new View.OnClickListener()
        {
            public final void onClick(View v)
            {
                doHomePressed();
            }
        });

        return view;
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        //noinspection ConstantConditions
        if (PasswdSafeApp.DEBUG_AUTO_FILE != null) {
            openFile(new File(PasswdSafeApp.DEBUG_AUTO_FILE));
        }

        LoaderManager lm = getLoaderManager();
        lm.initLoader(0, null, this);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onResume()
     */
    @Override
    public void onResume()
    {
        super.onResume();
        PasswdSafeApp app = (PasswdSafeApp)getActivity().getApplication();
        app.closeOpenFile();
        itsDirHistory.clear();
        showFiles();
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onCreateOptionsMenu(android.view.Menu, android.view.MenuInflater)
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.fragment_file_list, menu);
        super.onCreateOptionsMenu(menu, inflater);

        MenuItem mi = menu.findItem(R.id.menu_file_new);
        MenuItemCompat.setShowAsAction(mi,
                                       MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onPrepareOptionsMenu(android.view.Menu)
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        MenuItem mi = menu.findItem(R.id.menu_parent);
        if (mi != null) {
            mi.setEnabled((itsDir != null) && (itsDir.getParentFile() != null));
        }
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_file_new: {
            if (itsDir != null) {
                startActivity(new Intent(PasswdSafeUtil.NEW_INTENT,
                                         Uri.fromFile(itsDir)));
            }
            return true;
        }
        case R.id.menu_home: {
            doHomePressed();
            return true;
        }
        case R.id.menu_parent: {
            doParentPressed();
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
        @SuppressWarnings("unchecked")
        Map<String, Object> item =
                (HashMap<String, Object>)l.getItemAtPosition(position);
        if (item == null) {
            return;
        }

        FileData file = (FileData) item.get(TITLE);
        if (file == null) {
            return;
        }

        if (file.isDirectory()) {
            changeDir(file.itsFile, true);
        } else {
            PasswdSafeUtil.dbginfo(TAG, "Open file: %s", file.itsFile);
            openFile(file.itsFile);
        }
    }

    /** Create a loader for files */
    @Override
    public Loader<List<Map<String, Object>>> onCreateLoader(int id, Bundle args)
    {
        return new FileLoader(itsDir, itsListener.activityHasNoneItem(),
                              getActivity());
    }

    /** Callback when a loader is finished */
    @Override
    public void onLoadFinished(Loader<List<Map<String, Object>>> loader,
                               List<Map<String, Object>> data)
    {
        updateFiles(data);
    }

    /** Callback when a loader is reset */
    @Override
    public void onLoaderReset(Loader<List<Map<String, Object>>> loader)
    {
        updateFiles(null);
    }

    /**
     * @return true if a directory was popped, false to use default behavior
     */
    public final boolean doBackPressed()
    {
        PasswdSafeUtil.dbginfo(TAG, "doBackPressed");
        if (itsDirHistory.size() == 0) {
            return false;
        }
        changeDir(itsDirHistory.removeFirst(), false);
        return true;
    }


    /** Get the files in a directory */
    public static FileData[] getFiles(File dir,
                                      final boolean showHiddenFiles,
                                      final boolean showDirs)
    {
        File[] files = dir.listFiles(new FileFilter() {
            public final boolean accept(File pathname) {
                String filename = pathname.getName();
                if (pathname.isDirectory()) {
                    return showDirs &&
                            (showHiddenFiles ||
                             !(filename.startsWith(".") ||
                               filename.equalsIgnoreCase("LOST.DIR")));
                }
                return filename.endsWith(".psafe3") ||
                        filename.endsWith(".dat") ||
                        (showHiddenFiles &&
                                (filename.endsWith(".psafe3~") ||
                                 filename.endsWith(".dat~") ||
                                 filename.endsWith(".ibak")));
            }
        });

        FileData[] data;
        if (files != null) {
            Arrays.sort(files, new FileComparator());
            data = new FileData[files.length];
            for (int i = 0; i < files.length; ++i) {
                data[i] = new FileData(files[i]);
            }
        } else {
            data = new FileData[0];
        }

        return data;
    }

    /** Show the files in the current directory */
    private void showFiles()
    {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state) &&
            !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            itsDir = null;
        } else {
            itsDir = getFileDir();
        }

        LoaderManager lm = getLoaderManager();
        lm.restartLoader(0, null, this);
    }

    /** Update files after the loader is complete */
    private void updateFiles(List<Map<String, Object>> fileData)
    {
        SimpleAdapter adapter = null;
        if (fileData != null) {
            adapter = new SimpleAdapter(getActivity(), fileData,
                                        R.layout.file_list_item,
                                        new String[] { TITLE, ICON, MOD_DATE },
                                        new int[] { R.id.text, R.id.icon,
                                                    R.id.mod_date });
            adapter.setViewBinder(new SimpleAdapter.ViewBinder()
            {
                @Override
                public boolean setViewValue(View view,
                                            Object data,
                                            String textRepresentation)
                {
                    if (view.getId() == R.id.mod_date) {
                        if (data == null) {
                            view.setVisibility(View.GONE);
                            return true;
                        } else {
                            view.setVisibility(View.VISIBLE);
                        }
                    }
                    return false;
                }
            });
        }

        View rootView = getView();
        if (rootView == null) {
            // Fragment destroyed
            return;
        }
        View groupPanel = rootView.findViewById(R.id.current_group_panel);
        TextView groupLabel =
                (TextView)rootView.findViewById(R.id.current_group_label);
        TextView emptyLabel =
                (TextView)rootView.findViewById(android.R.id.empty);
        if (itsDir == null) {
            groupPanel.setVisibility(View.GONE);
            groupLabel.setText("");
            emptyLabel.setText(R.string.ext_storage_not_mounted);
        } else {
            groupPanel.setVisibility(View.VISIBLE);
            groupLabel.setText(itsDir.toString());
            emptyLabel.setText(R.string.no_files);
        }

        View selectFileLabel = rootView.findViewById(R.id.select_file_label);
        if ((adapter != null) && !adapter.isEmpty()) {
            selectFileLabel.setVisibility(View.VISIBLE);
        } else {
            selectFileLabel.setVisibility(View.GONE);
        }

        setListAdapter(adapter);

        // Open the default file
        if (getListAdapter() != null) {
            Activity act = getActivity();
            PasswdSafeApp app = (PasswdSafeApp)act.getApplication();
            if (app.checkOpenDefault()) {
                SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(act);
                Uri defFile = Preferences.getDefFilePref(prefs);
                if (defFile != null) {
                    itsListener.openFile(defFile, null);
                }
            }
        }
    }


    /** Open the given file */
    private void openFile(File file)
    {
        if (file == null) {
            itsListener.openFile(null, null);
        } else {
            itsListener.openFile(Uri.fromFile(file), file.getName());
        }
    }


    /** Handle the action to navigate to the parent directory */
    private void doParentPressed()
    {
        PasswdSafeUtil.dbginfo(TAG, "doParentPressed");
        if (itsDir != null) {
            File newdir = itsDir.getParentFile();
            if (newdir != null) {
                changeDir(newdir, true);
            }
        }
    }


    /** Handle the action to navigate to the home directory */
    private void doHomePressed()
    {
        changeDir(Environment.getExternalStorageDirectory(), true);
    }


    /** Get the files in the given directory */
    private static FileData[] getFiles(File dir, Context ctx)
    {
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean showHiddenFiles =
            Preferences.getShowHiddenFilesPref(prefs);
        return getFiles(dir, showHiddenFiles, true);
    }


    /** Change to the given directory */
    private void changeDir(File newDir, boolean saveHistory)
    {
        if (saveHistory && (itsDir != null)) {
            itsDirHistory.addFirst(itsDir);
        }
        setFileDir(newDir);
        showFiles();
        GuiUtils.invalidateOptionsMenu(getActivity());
    }


    /** Get the directory for listing files */
    private File getFileDir()
    {
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(getActivity());
        return Preferences.getFileDirPref(prefs);
    }


    /** Set the directory for listing files */
    private void setFileDir(File dir)
    {
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(getActivity());
        Preferences.setFileDirPref(dir, prefs);
    }

    /** Async class to load files in a directory */
    private static class FileLoader
            extends AsyncTaskLoader<List<Map<String, Object>>>
    {
        private final File itsDir;
        private final boolean itsIncludeNone;

        /** Constructor */
        public FileLoader(File dir, boolean includeNone, Context context)
        {
            super(context);
            itsDir = dir;
            itsIncludeNone = includeNone;
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

        /** Load the files in the background */
        @Override
        public List<Map<String, Object>> loadInBackground()
        {
            if (itsDir == null) {
                return null;
            }

            FileData[] data = getFiles(itsDir, getContext());
            List<Map<String, Object>> fileData = new ArrayList<>(data.length);

            if (itsIncludeNone) {
                fileData.add(createItem(new FileData(getContext())));
            }

            for (FileData file: data) {
                fileData.add(createItem(file));
            }
            return fileData;
        }

        /** Create an adapter map for the file */
        private Map<String, Object> createItem(FileData file)
        {
            HashMap<String, Object> item = new HashMap<>(3);
            item.put(TITLE, file);

            int icon;
            if (file.itsFile == null) {
                icon = 0;
            } else if (file.itsFile.isDirectory()) {
                icon = R.drawable.folder_rev;
            } else {
                icon = R.drawable.login_rev;
                item.put(MOD_DATE, Utils.formatDate(file.itsFile.lastModified(),
                                                    getContext()));
            }
            item.put(ICON, icon);
            return item;
        }
    }
}
