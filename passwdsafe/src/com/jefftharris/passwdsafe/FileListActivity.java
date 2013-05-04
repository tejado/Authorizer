/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import com.jefftharris.passwdsafe.view.GuiUtils;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

// TODO: update min version to 1.6

/**
 * The FileListActivity is the main PasswdSafe activity for file choosing and
 * top-level options
 */
public class FileListActivity extends FragmentActivity
        implements FileListFragment.Listener,
                   SyncProviderFragment.Listener,
                   SyncProviderFilesFragment.Listener
{
    private static final String TAG = "FileListActivity";


    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        FragmentManager fragMgr = getSupportFragmentManager();
        FragmentTransaction txn = fragMgr.beginTransaction();
        txn.replace(R.id.sync, new SyncProviderFragment());
        txn.commit();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_file_list, menu);

        MenuItem item = menu.findItem(R.id.menu_preferences);
        item.setIntent(new Intent(this, Preferences.class));
        MenuItemCompat.setShowAsAction(item,
                                       MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);

        item = menu.findItem(R.id.menu_about);
        MenuItemCompat.setShowAsAction(item,
                                       MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);

        return super.onCreateOptionsMenu(menu);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_about: {
            AboutDialog dlg = new AboutDialog();
            dlg.show(getSupportFragmentManager(), "AboutDialog");
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.SyncProviderFragment.Listener#showSyncProviderFiles(android.net.Uri)
     */
    @Override
    public void showSyncProviderFiles(Uri uri)
    {
        FragmentManager fragMgr = getSupportFragmentManager();
        Fragment filesFrag = fragMgr.findFragmentById(R.id.files);

        SyncProviderFilesFragment syncFrag =
                SyncProviderFilesFragment.newInstance(uri);

        FragmentTransaction txn = fragMgr.beginTransaction();
        txn.hide(filesFrag);
        txn.replace(R.id.sync, syncFrag);
        txn.addToBackStack(null);
        txn.commit();
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.FileListFragment.Listener#openFile(android.net.Uri)
     * @see com.jefftharris.passwdsafe.SyncProviderFilesFragment.Listener#openFile(android.net.Uri)
     */
    @Override
    public void openFile(Uri uri)
    {
        try {
            startActivity(AbstractFileListActivity.createOpenIntent(uri, null));
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Can't open uri: " + uri, e);
        }
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onKeyDown(int, android.view.KeyEvent)
     */
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
    @TargetApi(Build.VERSION_CODES.ECLAIR)
    @Override
    public void onBackPressed()
    {
        if (!doBackPressed()) {
            super.onBackPressed();
        }
    }


    /**
     * @return true if a directory was popped, false to use default behavior
     */
    private final boolean doBackPressed()
    {
        FragmentManager mgr = getSupportFragmentManager();
        Fragment frag = mgr.findFragmentById(R.id.files);
        if ((frag instanceof FileListFragment) && frag.isVisible()) {
            return ((FileListFragment)frag).doBackPressed();
        }
        return false;
    }
}
