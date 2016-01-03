/*
 * Copyright (©) 2013-2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.jefftharris.passwdsafe.file.PasswdFileDataUser;
import com.jefftharris.passwdsafe.lib.ApiCompat;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ReleaseNotesDialog;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;


/**
 * The FileListActivity is the main PasswdSafe activity for file choosing and
 * top-level options
 */
public class FileListActivity extends AppCompatActivity
        implements AboutFragment.Listener,
                   FileListFragment.Listener,
                   FileListNavDrawerFragment.Listener,
                   PreferencesFragment.Listener,
                   SharedPreferences.OnSharedPreferenceChangeListener,
                   StorageFileListFragment.Listener,
                   SyncProviderFragment.Listener,
                   SyncProviderFilesFragment.Listener
{
    public static final String INTENT_EXTRA_CLOSE_ON_OPEN = "closeOnOpen";

    private static final String STATE_TITLE = "title";

    private static final String TAG = "FileListActivity";

    private enum ChangeMode
    {
        /** View about info */
        VIEW_ABOUT,
        /** View files */
        VIEW_FILES,
        /** Initial view of files */
        VIEW_FILES_INIT,
        /** View preferences */
        VIEW_PREFERENCES
    }

    private enum ViewMode
    {
        /** Viewing about info */
        VIEW_ABOUT,
        /** Viewing files */
        VIEW_FILES,
        /** Viewing preferences */
        VIEW_PREFERENCES
    }

    private FileListNavDrawerFragment itsNavDrawerFrag;
    private boolean itsIsCloseOnOpen = false;
    private CharSequence itsTitle;
    private boolean itsIsLegacyChooser =
            Preferences.PREF_FILE_LEGACY_FILE_CHOOSER_DEF;
    private boolean itsIsLegacyChooserChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        itsNavDrawerFrag = (FileListNavDrawerFragment)
                getSupportFragmentManager().findFragmentById(
                        R.id.navigation_drawer);
        itsNavDrawerFrag.setUp((DrawerLayout)findViewById(R.id.drawer_layout));

        Intent intent = getIntent();
        itsIsCloseOnOpen = intent.getBooleanExtra(INTENT_EXTRA_CLOSE_ON_OPEN,
                                                  false);

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(prefs,
                                  Preferences.PREF_FILE_LEGACY_FILE_CHOOSER);

        if (savedInstanceState == null) {
            showFiles(true);
        } else {
            itsTitle = savedInstanceState.getCharSequence(STATE_TITLE);
        }
        if (itsTitle == null) {
            itsTitle = getTitle();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        itsNavDrawerFrag.onPostCreate();
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if (ReleaseNotesDialog.checkShowNotes(this)) {
            showAbout();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(STATE_TITLE, itsTitle);
    }

    @Override
    protected void onDestroy()
    {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if (itsNavDrawerFrag.isDrawerOpen()) {
            return super.onCreateOptionsMenu(menu);
        }

        // Only show items in the action bar relevant to this screen
        // if the drawer is not showing. Otherwise, let the drawer
        // decide what to show in the action bar.
        getMenuInflater().inflate(R.menu.activity_file_list, menu);

        restoreActionBar();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case android.R.id.home: {
            if (itsNavDrawerFrag.isDrawerEnabled()) {
                return super.onOptionsItemSelected(item);
            }
            onBackPressed();
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        switch (key) {
        case Preferences.PREF_FILE_LEGACY_FILE_CHOOSER: {
            boolean legacy =
                    ((ApiCompat.SDK_VERSION < ApiCompat.SDK_KITKAT) ||
                     Preferences.getFileLegacyFileChooserPref(prefs));
            if (legacy != itsIsLegacyChooser) {
                itsIsLegacyChooser = legacy;
                itsIsLegacyChooserChanged = true;
            }
            PasswdSafeUtil.dbginfo(TAG, "onSharedPreferenceChanged legacy %b",
                                   itsIsLegacyChooser);
            break;
        }
        }
    }

    @Override
    public void onBackPressed()
    {
        FragmentManager mgr = getSupportFragmentManager();
        Fragment frag = mgr.findFragmentById(R.id.files);
        boolean handled = (frag instanceof FileListFragment) &&
                          frag.isVisible() &&
                          ((FileListFragment) frag).doBackPressed();

        if (!handled) {
            if (itsIsLegacyChooserChanged) {
                showFiles(true);
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public void showSyncProviderFiles(Uri uri)
    {
        FragmentManager fragMgr = getSupportFragmentManager();
        Fragment syncFrag = fragMgr.findFragmentById(R.id.sync);

        SyncProviderFilesFragment syncFilesFrag =
                SyncProviderFilesFragment.newInstance(uri);

        FragmentTransaction txn = fragMgr.beginTransaction();
        txn.remove(syncFrag);
        txn.replace(R.id.files, syncFilesFrag);
        txn.addToBackStack(null);
        txn.commit();
    }

    @Override
    public void openFile(Uri uri, String fileName)
    {
        try {
            Intent intent = PasswdSafeUtil.createOpenIntent(uri, null);
            if (itsIsCloseOnOpen) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(ApiCompat.INTENT_FLAG_ACTIVITY_CLEAR_TASK);
            }
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Can't open uri: " + uri, e);
        }
    }

    @Override
    public boolean activityHasMenu()
    {
        return true;
    }

    @Override
    public boolean activityHasNoneItem()
    {
        return false;
    }

    @Override
    public void updateViewFiles()
    {
        doUpdateView(ViewMode.VIEW_FILES);
    }

    @Override
    public void updateViewSyncFiles()
    {
        doUpdateView(ViewMode.VIEW_FILES);
    }

    @Override
    public boolean isNavDrawerClosed()
    {
        return !itsNavDrawerFrag.isDrawerOpen();
    }

    @Override
    public void useFileData(PasswdFileDataUser user)
    {
        // No file data for about fragment
    }

    @Override
    public void updateViewAbout()
    {
        doUpdateView(ViewMode.VIEW_ABOUT);
    }

    @Override
    public void updateViewPreferences()
    {
        doUpdateView(ViewMode.VIEW_PREFERENCES);
    }

    @Override
    public void showAbout()
    {
        doChangeView(ChangeMode.VIEW_ABOUT, AboutFragment.newInstance(), null);
    }

    @Override
    public void showFiles()
    {
        showFiles(itsIsLegacyChooserChanged);
    }

    @Override
    public void showPreferences()
    {
        doChangeView(ChangeMode.VIEW_PREFERENCES,
                     PreferencesFragment.newInstance(), null);
    }

    /**
     * View files
     */
    private void showFiles(boolean initial)
    {
        Fragment filesFrag;
        if (itsIsLegacyChooser) {
            filesFrag = new FileListFragment();
        } else {
            filesFrag = new StorageFileListFragment();
        }
        itsIsLegacyChooserChanged = false;

        doChangeView(initial ?
                     ChangeMode.VIEW_FILES_INIT : ChangeMode.VIEW_FILES,
                     filesFrag, new SyncProviderFragment());
    }

    /**
     * Change the view of the activity
     */
    private void doChangeView(ChangeMode mode,
                              Fragment filesFrag,
                              Fragment syncFrag)
    {
        boolean clearBackStack = false;
        boolean supportsBack = false;
        switch (mode) {
        case VIEW_FILES_INIT: {
            clearBackStack = true;
            break;
        }
        case VIEW_ABOUT:
        case VIEW_FILES:
        case VIEW_PREFERENCES: {
            supportsBack = true;
            break;
        }
        }

        FragmentManager fragMgr = getSupportFragmentManager();
        FragmentTransaction txn = fragMgr.beginTransaction();
        txn.setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

        if (clearBackStack) {
            //noinspection StatementWithEmptyBody
            while (fragMgr.popBackStackImmediate()) {
                // Clear back stack
            }
        }

        if (filesFrag != null) {
            txn.replace(R.id.files, filesFrag);
        } else {
            Fragment currFrag = fragMgr.findFragmentById(R.id.files);
            if ((currFrag != null) && currFrag.isAdded()) {
                txn.remove(currFrag);
            }
        }

        if (syncFrag != null) {
            txn.replace(R.id.sync, syncFrag);
        } else {
            Fragment currFrag = fragMgr.findFragmentById(R.id.sync);
            if ((currFrag != null) && currFrag.isAdded()) {
                txn.remove(currFrag);
            }
        }

        if (supportsBack) {
            txn.addToBackStack(null);
        }

        txn.commit();
    }

    /**
     * Update the view mode
     */
    private void doUpdateView(ViewMode mode)
    {
        PasswdSafeUtil.dbginfo(TAG, "doUpdateView mode: %s", mode);

        FileListNavDrawerFragment.Mode drawerMode =
                FileListNavDrawerFragment.Mode.INIT;
        switch (mode) {
        case VIEW_ABOUT: {
            drawerMode = FileListNavDrawerFragment.Mode.ABOUT;
            itsTitle = PasswdSafeApp.getAppTitle(getString(R.string.about),
                                                 this);
            break;
        }
        case VIEW_FILES: {
            drawerMode = FileListNavDrawerFragment.Mode.FILES;
            itsTitle = getString(R.string.app_name);
            break;
        }
        case VIEW_PREFERENCES: {
            drawerMode = FileListNavDrawerFragment.Mode.PREFERENCES;
            itsTitle = PasswdSafeApp.getAppTitle(
                    getString(R.string.preferences), this);
            break;
        }
        }

        GuiUtils.invalidateOptionsMenu(this);
        itsNavDrawerFrag.updateView(drawerMode);
        restoreActionBar();
    }

    /**
     * Restore the action bar from the nav drawer
     */
    private void restoreActionBar()
    {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(itsTitle);
        }
    }
}
