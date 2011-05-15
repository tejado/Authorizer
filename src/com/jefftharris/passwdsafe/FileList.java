/*
 * Copyright (©) 2009-2011 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.LinkedList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class FileList extends ListActivity
{
    private static final String TAG = "FileList";

    private static final int MENU_FILE_NEW = 1;
    private static final int MENU_HOME = 2;
    private static final int MENU_PARENT = 3;
    private static final int MENU_PREFERENCES = 4;
    private static final int MENU_ABOUT = 5;

    private static final int DIALOG_ABOUT = 1;

    private File itsDir;
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

    public static FileData[] getFiles(File dir, final boolean showHiddenFiles)
    {
        File[] files = dir.listFiles(new FileFilter() {
            public final boolean accept(File pathname) {
                String filename = pathname.getName();
                if (pathname.isDirectory()) {
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
                     filename.endsWith(".dat~"))) {
                    return true;
                }
                return false;
            }
        });

        FileData[] data;
        if (files != null) {
            Arrays.sort(files);
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
        Uri.Builder builder = Uri.fromFile(file).buildUpon();
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
        View v = findViewById(R.id.current_group_icon);
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

        if (PasswdSafeApp.DEBUG_AUTO_FILE != null) {
            openFile(new File(PasswdSafeApp.DEBUG_AUTO_FILE));
        }
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
                    return FileList.this;
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
        FileData file = (FileData) l.getItemAtPosition(position);
        if (file == null) {
            return;
        }

        if (file.itsFile.isDirectory()) {
            changeDir(file.itsFile, true);
        } else {
            PasswdSafeApp.dbginfo(TAG, "Open file: " + file.itsFile);
            openFile(file.itsFile);
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuItem item;

        item = menu.add(0, MENU_FILE_NEW, 0, R.string.new_file);
        item.setIcon(android.R.drawable.ic_menu_add);

        item = menu.add(0, MENU_HOME, 0, R.string.home);
        item.setIcon(R.drawable.ic_menu_home);

        item = menu.add(0, MENU_PARENT, 0, R.string.parent_directory);
        item.setIcon(R.drawable.arrow_up);

        item = menu.add(0, MENU_PREFERENCES, 0, R.string.preferences);
        item.setIcon(android.R.drawable.ic_menu_preferences);
        item.setIntent(new Intent(this, Preferences.class));

        item = menu.add(0, MENU_ABOUT, 0, R.string.about);
        item.setIcon(android.R.drawable.ic_menu_info_details);
        return true;
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        MenuItem mi = menu.findItem(MENU_PARENT);
        if (mi != null) {
            mi.setEnabled((itsDir != null) && (itsDir.getParentFile() != null));
        }
        return super.onPrepareOptionsMenu(menu);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case MENU_FILE_NEW:
        {
            if (itsDir != null) {
                startActivity(new Intent(PasswdSafeApp.NEW_INTENT,
                                         Uri.fromFile(itsDir)));
            }
            return true;
        }
        case MENU_HOME:
        {
            doHomePressed();
            return true;
        }
        case MENU_PARENT:
        {
            doParentPressed();
            return true;
        }
        case MENU_ABOUT:
        {
            showDialog(DIALOG_ABOUT);
            return true;
        }
        default:
        {
            return super.onOptionsItemSelected(item);
        }
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


    /* (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog = null;
        switch (id) {
        case DIALOG_ABOUT:
        {
            String version = PasswdSafeApp.getAppVersion(this);
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setIcon(android.R.drawable.ic_menu_info_details)
                .setMessage("Version: " + version +
                            "\n\nBuild ID: " + Rev.BUILD_ID +
                            "\n\nBuild Date: " + Rev.BUILD_DATE)
                .setPositiveButton("Close",
                                   new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });
            dialog = builder.create();
            break;
        }
        }

        return dialog;
    }

    private final FileData[] getFiles(File dir)
    {
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        boolean showHiddenFiles =
            Preferences.getShowHiddenFilesPref(prefs);
        return getFiles(dir, showHiddenFiles);
    }

    private final void openFile(File file)
    {
        startActivity(createOpenIntent(file, null));
    }

    // TODO: need to re-fetch prefs all the time?
    // TODO: directory support in shortcut chooser
    // TODO: show icons
    private final void showFiles()
    {
        ListAdapter adapter = null;
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state) &&
            !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            itsDir = null;
        } else {
            itsDir = Preferences.getFileDirPref(prefs);
            FileData[] data = getFiles(itsDir);
            adapter = new ArrayAdapter<FileData>(
                            this, android.R.layout.simple_list_item_1, data);
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
        if (adapter != null) {
            PasswdSafeApp app = (PasswdSafeApp)getApplication();
            if (app.checkOpenDefault()) {
                String defFileName = Preferences.getDefFilePref(prefs);
                File defFile = new File(itsDir, defFileName);
                if (defFile.isFile() && defFile.canRead()) {
                    openFile(defFile);
                }
            }
        }
    }


    private final void doParentPressed()
    {
        PasswdSafeApp.dbginfo(TAG, "doParentPressed");
        if (itsDir != null) {
            File newdir = itsDir.getParentFile();
            if (newdir != null) {
                changeDir(newdir, true);
            }
        }
    }

    /**
     * @return true if a directory was popped, false to use default behavior
     */
    private final boolean doBackPressed()
    {
        PasswdSafeApp.dbginfo(TAG, "doBackPressed");
        if (itsDirHistory.size() == 0) {
            return false;
        }
        changeDir(itsDirHistory.removeFirst(), false);
        return true;
    }


    private final void doHomePressed()
    {
        changeDir(Environment.getExternalStorageDirectory(), true);
    }


    private final void changeDir(File newDir, boolean saveHistory)
    {
        if (saveHistory && (itsDir != null)) {
            itsDirHistory.addFirst(itsDir);
        }
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        Preferences.setFileDirPref(newDir, prefs);
        showFiles();
    }
}
