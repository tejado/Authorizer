/*
 * Copyright (©) 2011-2012 Jeff Harris <jefftharris@gmail.com>
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.view.GuiUtils;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public abstract class AbstractFileListActivity extends ListActivity
{
    private static final String TAG = "FileList";

    private static final String TITLE = "title";
    private static final String ICON = "icon";

    private static Comparator<File> FILE_COMP = new Comparator<File>()
    {
        public int compare(File obj1, File obj2)
        {
            if (obj1.isDirectory() && !obj2.isDirectory()) {
                return 1;
            } else if (!obj1.isDirectory() && obj2.isDirectory()) {
                return -1;
            }
            return obj1.compareTo(obj2);
        }
    };


    protected File itsDir;
    private LinkedList<File> itsDirHistory = new LinkedList<File>();


    public static final class FileData
    {
        public final File itsFile;
        public FileData(File f)
        {
            itsFile = f;
        }
        @Override
        public final String toString()
        {
            return itsFile.getName();
        }
    }


    public static FileData[] getFiles(File dir,
                                      final boolean showHiddenFiles,
                                      final boolean showDirs)
    {
        File[] files = dir.listFiles(new FileFilter() {
            public final boolean accept(File pathname) {
                String filename = pathname.getName();
                if (pathname.isDirectory()) {
                    if (!showDirs) {
                        return false;
                    }
                    if (!showHiddenFiles &&
                        (filename.startsWith(".") ||
                         filename.equalsIgnoreCase("LOST.DIR"))) {
                        return false;
                    }
                    return true;
                }
                if (filename.endsWith(".psafe3") || filename.endsWith(".dat")) {
                    return true;
                }
                if (showHiddenFiles &&
                    (filename.endsWith(".psafe3~") ||
                     filename.endsWith(".dat~") ||
                     filename.endsWith(".ibak"))) {
                    return true;
                }
                return false;
            }
        });

        FileData[] data;
        if (files != null) {
            Arrays.sort(files, FILE_COMP);
            data = new FileData[files.length];
            for (int i = 0; i < files.length; ++i) {
                data[i] = new FileData(files[i]);
            }
        } else {
            data = new FileData[0];
        }

        return data;
    }


    public static Intent createOpenIntent(File file, String recToOpen)
    {
        return createOpenIntent(Uri.fromFile(file), recToOpen);
    }


    public static Intent createOpenIntent(Uri uri, String recToOpen)
    {
        Uri.Builder builder = uri.buildUpon();
        if (recToOpen != null) {
            builder.appendQueryParameter("recToOpen", recToOpen);
        }
        return new Intent(PasswdSafeApp.VIEW_INTENT, builder.build());
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_list);

        View.OnClickListener parentListener = new View.OnClickListener()
        {
            public final void onClick(View v)
            {
                doParentPressed();
            }
        };
        View v = findViewById(R.id.up_icon);
        v.setOnClickListener(parentListener);
        v = findViewById(R.id.current_group_label);
        v.setOnClickListener(parentListener);

        v = findViewById(R.id.home);
        v.setOnClickListener(new View.OnClickListener()
        {
            public final void onClick(View v)
            {
                doHomePressed();
            }
        });
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        PasswdSafeApp app = (PasswdSafeApp)getApplication();
        ActivityPasswdFile file =
            app.accessPasswdFile(null, new PasswdFileActivity()
            {
                public void showProgressDialog()
                {
                }

                public void removeProgressDialog()
                {
                }

                public void saveFinished(boolean success)
                {
                }

                public Activity getActivity()
                {
                    return AbstractFileListActivity.this;
                }
            });
        file.close();

        itsDirHistory.clear();
        showFiles();
    }


    /* (non-Javadoc)
     * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
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

        if (file.itsFile.isDirectory()) {
            changeDir(file.itsFile, true);
        } else {
            PasswdSafeUtil.dbginfo(TAG, "Open file: %s", file.itsFile);
            onFileClick(file.itsFile);
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (GuiUtils.isBackKeyDown(keyCode, event)) {
            // Take care of calling this method on earlier versions of
            // the platform where it doesn't exist.
            if (doBackPressed()) {
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed()
    {
        if (!doBackPressed()) {
            finish();
        }
    }


    protected void showFiles()
    {
        ListAdapter adapter = null;
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state) &&
            !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            itsDir = null;
        } else {
            itsDir = getFileDir();
            FileData[] data = getFiles(itsDir);
            List<Map<String, Object>> fileData =
                new ArrayList<Map<String, Object>>();
            for (FileData file: data) {
                HashMap<String, Object> item = new HashMap<String, Object>();
                item.put(TITLE, file);
                item.put(ICON, file.itsFile.isDirectory() ?
                               R.drawable.folder_rev : R.drawable.login_rev);
                fileData.add(item);
            }

            adapter = new SimpleAdapter(this, fileData, R.layout.file_list_item,
                                        new String[] { TITLE, ICON },
                                        new int[] { R.id.text, R.id.icon });
        }

        View groupPanel = findViewById(R.id.current_group_panel);
        TextView groupLabel = (TextView)findViewById(R.id.current_group_label);
        TextView emptyLabel = (TextView)findViewById(android.R.id.empty);
        if (itsDir == null) {
            groupPanel.setVisibility(View.GONE);
            groupLabel.setText("");
            emptyLabel.setText(R.string.ext_storage_not_mounted);
        } else {
            groupPanel.setVisibility(View.VISIBLE);
            groupLabel.setText(itsDir.toString());
            emptyLabel.setText(R.string.no_files);
        }

        View selectFileLabel = findViewById(R.id.select_file_label);
        if ((adapter != null) && !adapter.isEmpty()) {
            selectFileLabel.setVisibility(View.VISIBLE);
        } else {
            selectFileLabel.setVisibility(View.GONE);
        }

        setListAdapter(adapter);
    }


    protected final void doParentPressed()
    {
        PasswdSafeUtil.dbginfo(TAG, "doParentPressed");
        if (itsDir != null) {
            File newdir = itsDir.getParentFile();
            if (newdir != null) {
                changeDir(newdir, true);
            }
        }
    }


    protected abstract void onFileClick(File file);
    protected abstract File getFileDir();
    protected abstract void setFileDir(File dir);


    /**
     * @return true if a directory was popped, false to use default behavior
     */
    private final boolean doBackPressed()
    {
        PasswdSafeUtil.dbginfo(TAG, "doBackPressed");
        if (itsDirHistory.size() == 0) {
            return false;
        }
        changeDir(itsDirHistory.removeFirst(), false);
        return true;
    }


    protected final void doHomePressed()
    {
        changeDir(Environment.getExternalStorageDirectory(), true);
    }


    private final FileData[] getFiles(File dir)
    {
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        boolean showHiddenFiles =
            Preferences.getShowHiddenFilesPref(prefs);
        return getFiles(dir, showHiddenFiles, true);
    }


    private final void changeDir(File newDir, boolean saveHistory)
    {
        if (saveHistory && (itsDir != null)) {
            itsDirHistory.addFirst(itsDir);
        }
        setFileDir(newDir);
        showFiles();
        GuiUtils.invalidateOptionsMenu(this);
    }
}
