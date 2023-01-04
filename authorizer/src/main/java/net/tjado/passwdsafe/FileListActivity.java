/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import net.tjado.passwdsafe.file.PasswdFileDataUser;
import net.tjado.passwdsafe.lib.AboutUtils;
import net.tjado.passwdsafe.lib.ApiCompat;
import net.tjado.passwdsafe.lib.DynamicPermissionMgr;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.view.GuiUtils;

import java.util.Objects;


/**
 * The FileListActivity is the main PasswdSafe activity for file choosing and
 * top-level options
 */
public class FileListActivity extends AppCompatActivity
        implements AboutFragment.Listener,
                   FileListFragment.Listener,
                   FileListNavDrawerFragment.Listener,
                   BackupFilesFragment.Listener,
                   PreferencesFragment.Listener,
                   SharedPreferences.OnSharedPreferenceChangeListener,
                   StorageFileListFragment.Listener,
                   PreferenceFragmentCompat.OnPreferenceStartScreenCallback
{
    public static final String INTENT_EXTRA_CLOSE_ON_OPEN = "closeOnOpen";

    private static final int REQUEST_STORAGE_PERM = 1;
    private static final int REQUEST_APP_SETTINGS = 2;

    private static final String STATE_TITLE = "title";

    private static final String TAG = "AuthFileListActivity";

    private enum ViewChange
    {
        /** View about info */
        ABOUT,
        /** View backup files */
        BACKUP_FILES,
        /** View files */
        FILES,
        /** Initial view of files */
        FILES_INIT,
        /** View preferences */
        PREFERENCES
    }

    private enum ViewMode
    {
        /** Viewing about info */
        ABOUT,
        /** Viewing backup files */
        BACKUP_FILES,
        /** Viewing files */
        FILES,
        /** Viewing preferences */
        PREFERENCES
    }

    private FileListNavDrawerFragment itsNavDrawerFrag;
    private DynamicPermissionMgr itsPermissionMgr;
    private View itsFiles;
    private View itsSync;
    private View itsNoPermGroup;
    private boolean itsIsCloseOnOpen = false;
    private CharSequence itsTitle;
    private boolean itsIsLegacyChooser =
            Preferences.PREF_FILE_LEGACY_FILE_CHOOSER_DEF;
    private boolean itsIsLegacyChooserChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        PasswdSafeApp.setupTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        itsNavDrawerFrag = (FileListNavDrawerFragment)
                getSupportFragmentManager().findFragmentById(
                        R.id.navigation_drawer);
        Objects.requireNonNull(itsNavDrawerFrag)
               .setUp(findViewById(R.id.drawer_layout));
        itsFiles = findViewById(R.id.files);
        itsNoPermGroup = findViewById(R.id.no_permission_group);

        Intent intent = getIntent();
        itsIsCloseOnOpen = intent.getBooleanExtra(INTENT_EXTRA_CLOSE_ON_OPEN,
                                                  false);

        SharedPreferences prefs = Preferences.getSharedPrefs(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(prefs,
                                  Preferences.PREF_FILE_LEGACY_FILE_CHOOSER);

        itsPermissionMgr = new DynamicPermissionMgr(
                this, REQUEST_STORAGE_PERM, REQUEST_APP_SETTINGS,
                BuildConfig.APPLICATION_ID, R.id.reload, R.id.app_settings);
        itsPermissionMgr.addPerm(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                 true);
        itsPermissionMgr.addPerm(DynamicPermissionMgr.PERM_POST_NOTIFICATIONS,
                                 false);
        showFiles(true, savedInstanceState);
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
        if (AboutUtils.checkShowNotes(this)) {
            showAbout();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(STATE_TITLE, itsTitle);
    }

    @Override
    protected void onDestroy()
    {
        SharedPreferences prefs = Preferences.getSharedPrefs(this);
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
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data)
    {
        if (!itsPermissionMgr.handleActivityResult(requestCode)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        if (!itsPermissionMgr.handlePermissionsResult(
                requestCode, permissions, grantResults)) {
            super.onRequestPermissionsResult(requestCode, permissions,
                                             grantResults);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, @Nullable String key)
    {
        boolean updateLegacyFileChooser = false;
        if (key == null) {
            updateLegacyFileChooser = true;
        } else {
            switch (key) {
            case Preferences.PREF_FILE_LEGACY_FILE_CHOOSER: {
                updateLegacyFileChooser = true;
                break;
            }
            }
        }
        if (updateLegacyFileChooser) {
            boolean legacy =
                    ((ApiCompat.SDK_VERSION < ApiCompat.SDK_KITKAT) ||
                     Preferences.getFileLegacyFileChooserPref(prefs));
            if (legacy != itsIsLegacyChooser) {
                itsIsLegacyChooser = legacy;
                itsIsLegacyChooserChanged = true;
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
                showFiles(true, null);
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller,
                                           PreferenceScreen pref)
    {
        doChangeView(ViewChange.PREFERENCES,
                     PreferencesFragment.newInstance(pref.getKey()));
        return true;
    }

    @Override
    public void openFile(Uri uri, String fileName)
    {
        openFile(uri);
    }

    @Override
    public void openFile(Uri uri)
    {
        try {
            Intent intent = PasswdSafeUtil.createOpenIntent(uri, null);
            if (itsIsCloseOnOpen) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            }
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Can't open uri: " + uri, e);
        }
    }

    @Override
    public void createNewFile(Uri dirUri)
    {
        startActivity(PasswdSafeUtil.createNewFileIntent(dirUri));
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
    public boolean appHasFilePermission()
    {
        return itsPermissionMgr.hasRequiredPerms();
    }

    @Override
    public void updateViewBackupFiles()
    {
        doUpdateView(ViewMode.BACKUP_FILES);
    }

    @Override
    public void updateViewFiles()
    {
        doUpdateView(ViewMode.FILES);
    }

    @Override
    public boolean isNavDrawerClosed()
    {
        return !itsNavDrawerFrag.isDrawerOpen();
    }

    @Override
    public <RetT> RetT useFileData(PasswdFileDataUser<RetT> user)
    {
        // No file data for about fragment
        return null;
    }


    @Override
    public void updateViewAbout()
    {
        doUpdateView(ViewMode.ABOUT);
    }

    @Override
    public void updateViewPreferences()
    {
        doUpdateView(ViewMode.PREFERENCES);
    }

    @Override
    public void showAbout()
    {
        doChangeView(ViewChange.ABOUT, AboutFragment.newInstance());
    }

    @Override
    public void showBackupFiles()
    {
        doChangeView(ViewChange.BACKUP_FILES,
                     BackupFilesFragment.newInstance());
    }

    @Override
    public void showFiles()
    {
        showFiles(itsIsLegacyChooserChanged, null);
    }

    @Override
    public void showPreferences()
    {
        doChangeView(ViewChange.PREFERENCES,
                     PreferencesFragment.newInstance(null));
    }

    /**
     * View files
     */
    private void showFiles(boolean initial, Bundle savedState)
    {
        itsIsLegacyChooserChanged = false;
        if (savedState == null) {
            Fragment filesFrag;
            if (itsIsLegacyChooser) {
                filesFrag = new FileListFragment();
            } else {
                filesFrag = new StorageFileListFragment();
            }

            doChangeView(initial ? ViewChange.FILES_INIT : ViewChange.FILES,
                         filesFrag);
        } else {
            itsTitle = savedState.getCharSequence(STATE_TITLE);
        }

        itsPermissionMgr.checkPerms();
    }

    /**
     * Change the view of the activity
     */
    private void doChangeView(ViewChange mode, Fragment filesFrag)
    {
        boolean clearBackStack = false;
        boolean supportsBack = false;
        switch (mode) {
        case FILES_INIT: {
            clearBackStack = true;
            break;
        }
        case ABOUT:
        case BACKUP_FILES:
        case FILES:
        case PREFERENCES: {
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
        boolean hasPermission = true;
        switch (mode) {
        case ABOUT: {
            drawerMode = FileListNavDrawerFragment.Mode.ABOUT;
            itsTitle = PasswdSafeApp.getAppTitle(getString(R.string.about),
                                                 this);
            break;
        }
        case BACKUP_FILES: {
            drawerMode = FileListNavDrawerFragment.Mode.BACKUP_FILES;
            itsTitle = PasswdSafeApp.getAppTitle(
                    getString(R.string.file_backups), this);
            break;
        }
        case FILES: {
            drawerMode = FileListNavDrawerFragment.Mode.FILES;
            itsTitle = getString(R.string.app_name);
            hasPermission = itsPermissionMgr.hasRequiredPerms();
            break;
        }
        case PREFERENCES: {
            drawerMode = FileListNavDrawerFragment.Mode.PREFERENCES;
            itsTitle = PasswdSafeApp.getAppTitle(
                    getString(R.string.preferences), this);
            break;
        }
        }

        invalidateOptionsMenu();
        itsNavDrawerFrag.updateView(drawerMode);
        GuiUtils.setVisible(itsNoPermGroup, !hasPermission);
        GuiUtils.setVisible(itsFiles, hasPermission);
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
