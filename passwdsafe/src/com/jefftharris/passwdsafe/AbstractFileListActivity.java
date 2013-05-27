/*
 * Copyright (©) 2011-2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;

import com.jefftharris.passwdsafe.view.GuiUtils;

public abstract class AbstractFileListActivity extends FragmentActivity
        implements FileListFragment.Listener,
                   SyncProviderFragment.Listener,
                   SyncProviderFilesFragment.Listener

{
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
