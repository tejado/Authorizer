/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

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
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.Utils;
import net.tjado.passwdsafe.lib.view.GuiUtils;
import net.tjado.passwdsafe.util.FileComparator;

/**
 * The FileListFragment allows the user to choose which file to open
 */
public final class FileListFragment extends ListFragment
        implements LoaderCallbacks<List<Map<String, Object>>>,
                   View.OnClickListener,
                   View.OnLongClickListener
{
    /** Listener interface for the owning activity */
    public interface Listener
    {
        /** Open a file */
        void openFile(Uri uri, String fileName);

        /** Create a new file */
        void createNewFile(Uri dirUri);

        /** Does the activity have a menu */
        boolean activityHasMenu();

        /** Does the activity have a 'none' item */
        boolean activityHasNoneItem();

        /** Does the app have file permission */
        boolean appHasFilePermission();

        /** Update the view for a list of files */
        void updateViewFiles();
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

    @Override
    public void onAttach(Context ctx)
    {
        super.onAttach(ctx);
        itsListener = (Listener)ctx;
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

        View v = view.findViewById(R.id.current_group_panel);
        v.setOnClickListener(this);
        v.setOnLongClickListener(this);
        v = view.findViewById(R.id.home);
        v.setOnClickListener(this);
        v.setOnLongClickListener(this);

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
        itsDirHistory.clear();
        showFiles();
        itsListener.updateViewFiles();
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onCreateOptionsMenu(android.view.Menu, android.view.MenuInflater)
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.fragment_file_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        MenuItem item = menu.findItem(R.id.menu_file_new);
        item.setEnabled(itsListener.appHasFilePermission());
        super.onPrepareOptionsMenu(menu);
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
                itsListener.createNewFile(Uri.fromFile(itsDir));
            }
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

    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
        case R.id.current_group_panel: {
            doParentPressed();
            break;
        }
        case R.id.home: {
            doHomePressed();
            break;
        }
        }
    }

    @Override
    public boolean onLongClick(View v)
    {
        switch (v.getId()) {
        case R.id.current_group_panel: {
            Toast.makeText(getContext(), R.string.parent_directory,
                           Toast.LENGTH_SHORT).show();
            return true;
        }
        case R.id.home: {
            Toast.makeText(getContext(), R.string.home,
                           Toast.LENGTH_SHORT).show();
            return true;
        }
        }
        return false;
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
    private static FileData[] getFiles(
            File dir,
            final boolean showHiddenFiles,
            @SuppressWarnings("SameParameterValue") final boolean showDirs)
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
                    switch (view.getId()) {
                        case R.id.icon: {
                            ImageView iv = (ImageView)view;
                            iv.setImageResource((int) data);
                            iv.setColorFilter(getResources().getColor(R.color.treeview_icons));
                            return true;
                        }
                        case R.id.text: {
                            TextView tv = (TextView)view;
                            tv.setText(textRepresentation);
                            tv.requestLayout();
                            return true;
                        }
                        case R.id.mod_date: {
                            if (data == null) {
                                view.setVisibility(View.GONE);
                                return true;
                            } else {
                                view.setVisibility(View.VISIBLE);
                                return false;
                            }
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

        setListAdapter(adapter);

        // Open the default file
        if (getListAdapter() != null) {
            Activity act = getActivity();
            PasswdSafeApp app = (PasswdSafeApp)act.getApplication();
            if (app.checkOpenDefault()) {
                SharedPreferences prefs = Preferences.getSharedPrefs(act);
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
        SharedPreferences prefs = Preferences.getSharedPrefs(ctx);
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
        SharedPreferences prefs = Preferences.getSharedPrefs(getContext());
        return Preferences.getFileDirPref(prefs);
    }


    /** Set the directory for listing files */
    private void setFileDir(File dir)
    {
        SharedPreferences prefs = Preferences.getSharedPrefs(getContext());
        Preferences.setFileDirPref(dir, prefs);
    }

    /** Async class to load files in a directory */
    private static class FileLoader
            extends AsyncTaskLoader<List<Map<String, Object>>>
    {
        private final File itsDir;
        private final boolean itsIncludeNone;
        private final int itsFolderIcon;
        private final int itsFileIcon;

        /** Constructor */
        public FileLoader(File dir, boolean includeNone, Context context)
        {
            super(context);
            itsDir = dir;
            itsIncludeNone = includeNone;

            Resources.Theme theme = context.getTheme();
            TypedValue attr = new TypedValue();
            theme.resolveAttribute(R.attr.drawableFolder, attr, true);
            itsFolderIcon = attr.resourceId;
            theme.resolveAttribute(R.attr.drawablePasswdsafe, attr, true);
            itsFileIcon = attr.resourceId;
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
                icon = itsFolderIcon;
            } else {
                icon = itsFileIcon;
                item.put(MOD_DATE, Utils.formatDate(file.itsFile.lastModified(),
                                                    getContext()));
            }
            item.put(ICON, icon);
            return item;
        }
    }
}
