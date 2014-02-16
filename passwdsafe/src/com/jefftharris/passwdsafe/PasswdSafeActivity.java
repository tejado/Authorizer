/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

/**
 *  Main activity for the PasswdSafe app
 *
 *  TODO: How to handle change of main activity name in manifest?  Launcher
 *  icons disappear
 *
 *  TODO: Keep old PasswdSafe activity for shortcuts and stuff?
 *
 *  TODO: On gingerbread, the single pane layout margins are not used
 */
public class PasswdSafeActivity extends ActionBarActivity
{
    private static final String TAG = PasswdSafeActivity.class.getName();
    private boolean itsIsTwoPane = false;

    /// The state of the views in the activity
    enum ViewState
    {
        MAIN
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passwdsafe);

        PasswdSafeUtil.dbginfo(TAG, "onCreate state: %b", savedInstanceState);
        itsIsTwoPane = findViewById(R.id.content_list) != null;

        setView(ViewState.MAIN);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_passwdsafe, menu);
        return super.onCreateOptionsMenu(menu);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_passwdsafe: {
            Intent intent = new Intent(this, FileListActivity.class);
            startActivity(intent);
            break;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
        return true;
    }


    /// Set the activity's views to the given state
    private void setView(ViewState view)
    {
        FragmentManager fragMgr = getSupportFragmentManager();
        FragmentTransaction txn = fragMgr.beginTransaction();
        switch (view) {
        case MAIN: {
            if (itsIsTwoPane) {
                Fragment listFrag = fragMgr.findFragmentById(R.id.content_list);
                if (listFrag != null) {
                    txn.remove(listFrag);
                }
                View listView = findViewById(R.id.content_list);
                listView.setVisibility(View.GONE);
            }

            txn.replace(R.id.content, new PasswdSafeMainFragment());
            break;
        }
        }

        if (!txn.isEmpty()) {
            txn.commit();
        }
    }
}
