/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import com.jefftharris.passwdsafe.view.GuiUtils;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

/**
 * The FileListActivity is the main PasswdSafe activity for file choosing and
 * top-level options
 */
public class FileListActivity extends FragmentActivity
{
    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_file_list, menu);

        MenuItem item = menu.findItem(R.id.preferences);
        item.setIntent(new Intent(this, Preferences.class));
        MenuItemCompat.setShowAsAction(item,
                                       MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);

        item = menu.findItem(R.id.about);
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
        case R.id.about: {
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
    @Override
    public void onBackPressed()
    {
        if (!doBackPressed()) {
            finish();
        }
    }


    /**
     * @return true if a directory was popped, false to use default behavior
     */
    private final boolean doBackPressed()
    {
        FragmentManager mgr = getSupportFragmentManager();
        FileListFragment frag =
                (FileListFragment)mgr.findFragmentById(R.id.files);
        return frag.doBackPressed();
    }
}
