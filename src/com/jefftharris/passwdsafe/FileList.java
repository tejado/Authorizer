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
    private static final int MENU_PARENT = 2;
    private static final int MENU_PREFERENCES = 3;
    private static final int MENU_ABOUT = 4;

    private static final int DIALOG_ABOUT = 1;

    private TextView itsHeader;
    private File itsDir;

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

    public static FileData[] getFiles(File dir, final boolean showBackupFiles)
    {
        File[] files = dir.listFiles(new FileFilter() {
            public final boolean accept(File pathname) {
                if (pathname.isDirectory()) {
                    return true;
                }
                String filename = pathname.getName();
                if (filename.endsWith(".psafe3") || filename.endsWith(".dat")) {
                    return true;
                }
                if (showBackupFiles &&
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

        View v = findViewById(R.id.current_group_panel);
        v.setOnClickListener(new View.OnClickListener()
        {
            public final void onClick(View v)
            {
                doBackPressed();
            }
        });

        itsHeader = new TextView(this);
        getListView().addHeaderView(itsHeader);
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
            changeDir(file.itsFile);
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
        case MENU_PARENT:
        {
            doBackPressed();
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
        boolean showBackupFiles =
            Preferences.getShowBackupFilesPref(prefs);
        return getFiles(dir, showBackupFiles);
    }

    private final void openFile(File file)
    {
        startActivity(createOpenIntent(file, null));
    }

    // TODO: add back key support
    // TODO: need to re-fetch prefs all the time?
    // TODO: home button to goto /sdcard
    private final void showFiles()
    {
        ListAdapter adapter = null;
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state) &&
            !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            itsHeader.setText(R.string.ext_storage_not_mounted);
            itsDir = null;
        } else {
            itsDir = Preferences.getFileDirPref(prefs);
            itsHeader.setText(getString(R.string.file_list_header, itsDir));
            FileData[] data = getFiles(itsDir);
            adapter = new ArrayAdapter<FileData>(
                            this, android.R.layout.simple_list_item_1, data);
        }

        TextView tv = (TextView)findViewById(R.id.current_group_label);
        tv.setText((itsDir == null) ? null : itsDir.toString());

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


    /**
     * @return true if a directory was popped, false to use default behavior
     */
    private final boolean doBackPressed()
    {
        PasswdSafeApp.dbginfo(TAG, "doBackPressed");
        if (itsDir != null) {
            itsDir = itsDir.getParentFile();
            if (itsDir == null) {
                return false;
            }
            changeDir(itsDir);
            return true;
        }
        return false;
    }


    private final void changeDir(File newDir)
    {
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        Preferences.setFileDirPref(newDir, prefs);
        showFiles();
    }
}
