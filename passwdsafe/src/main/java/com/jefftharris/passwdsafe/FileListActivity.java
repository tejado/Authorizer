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

import com.jefftharris.passwdsafe.lib.ApiCompat;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ReleaseNotesDialog;


/**
 * The FileListActivity is the main PasswdSafe activity for file choosing and
 * top-level options
 */
public class FileListActivity extends AppCompatActivity
        implements FileListFragment.Listener,
                   FileListNavDrawerFragment.Listener,
                   StorageFileListFragment.Listener,
                   SyncProviderFragment.Listener,
                   SyncProviderFilesFragment.Listener
{
    public static final String INTENT_EXTRA_CLOSE_ON_OPEN = "closeOnOpen";

    private static final String STATE_TITLE = "title";

    private static final String TAG = "FileListActivity";

    private FileListNavDrawerFragment itsNavDrawerFrag;
    private boolean itsIsCloseOnOpen = false;
    private CharSequence itsTitle;
    private Boolean itsIsStorageFrag = null;

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

        FragmentManager fragMgr = getSupportFragmentManager();
        FragmentTransaction txn = fragMgr.beginTransaction();
        txn.replace(R.id.sync, new SyncProviderFragment());
        txn.commit();

        if (savedInstanceState == null) {
            setFileChooseFrag();
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
        ReleaseNotesDialog.checkNotes(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(STATE_TITLE, itsTitle);
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
    public void onBackPressed()
    {
        FragmentManager mgr = getSupportFragmentManager();
        Fragment frag = mgr.findFragmentById(R.id.files);
        boolean handled = (frag instanceof FileListFragment) &&
                          frag.isVisible() &&
                          ((FileListFragment) frag).doBackPressed();

        if (!handled) {
            super.onBackPressed();
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

    /**
     * Set the file chooser fragment
     */
    private void setFileChooseFrag()
    {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(this);
        boolean storageFrag =
                ((ApiCompat.SDK_VERSION >= ApiCompat.SDK_KITKAT) &&
                 !Preferences.getFileLegacyFileChooserPref(prefs));
        if ((itsIsStorageFrag == null) || (itsIsStorageFrag != storageFrag)) {
            PasswdSafeUtil.dbginfo(TAG, "setFileChooseFrag storage %b",
                                   storageFrag);
            Fragment frag;
            if (storageFrag) {
                frag = new StorageFileListFragment();
            } else {
                frag = new FileListFragment();
            }
            FragmentManager fragMgr = getSupportFragmentManager();
            FragmentTransaction txn = fragMgr.beginTransaction();
            txn.replace(R.id.files, frag);
            txn.commit();
            itsIsStorageFrag = storageFrag;
        }
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
