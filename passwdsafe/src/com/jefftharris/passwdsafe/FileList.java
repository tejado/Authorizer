/*
 * Copyright (©) 2009-2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.File;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

// TODO: delete this class
public class FileList extends AbstractFileListActivity
{
    private static final int MENU_FILE_NEW = 1;
    private static final int MENU_HOME = 2;
    private static final int MENU_PARENT = 3;
    private static final int MENU_PREFERENCES = 4;
    private static final int MENU_ABOUT = 5;

    private static final int DIALOG_ABOUT = 1;

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @TargetApi(Build.VERSION_CODES.ECLAIR)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (PasswdSafeApp.DEBUG_AUTO_FILE != null) {
            openFile(new File(PasswdSafeApp.DEBUG_AUTO_FILE));
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
    protected void onFileClick(File file)
    {
        openFile(file);
    }


    @Override
    protected File getFileDir()
    {
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        return Preferences.getFileDirPref(prefs);
    }

    @Override
    protected void setFileDir(File dir)
    {
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        Preferences.setFileDirPref(dir, prefs);
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
                .setMessage(getString(R.string.about_details,
                                      version, Rev.BUILD_ID, Rev.BUILD_DATE))
                .setPositiveButton(R.string.close,
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

    @Override
    protected void showFiles()
    {
        super.showFiles();
        if (getListAdapter() != null) {
            PasswdSafeApp app = (PasswdSafeApp)getApplication();
            if (app.checkOpenDefault()) {
                SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(this);
                String defFileName = Preferences.getDefFilePref(prefs);
                File defFile = new File(itsDir, defFileName);
                if (defFile.isFile() && defFile.canRead()) {
                    openFile(defFile);
                }
            }
        }
    }

    private final void openFile(File file)
    {
        startActivity(createOpenIntent(file, null));
    }
}
