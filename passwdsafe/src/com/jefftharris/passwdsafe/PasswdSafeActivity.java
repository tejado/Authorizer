/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.jefftharris.passwdsafe.lib.AboutDialog;
import com.jefftharris.passwdsafe.lib.ApiCompat;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ReleaseNotesDialog;

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
        implements PasswdSafeMainFragment.Listener,
                   PasswdSafeNewOpenFileFragment.Listener
{
    private static final int ACTIVITY_REQUEST_CHOOSE_FILE = 1;

    private static final String TAG = PasswdSafeActivity.class.getName();

    private boolean itsIsTwoPane = false;
    private DrawerLayout itsDrawerLayout;
    private ListView itsDrawerList;
    private ActionBarDrawerToggle itsDrawerToggle;
    private Uri itsPendingOpenNewUri;

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

        itsDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        itsDrawerToggle = new ActionBarDrawerToggle(this, itsDrawerLayout,
                                                    R.drawable.ic_drawer,
                                                    R.string.open,
                                                    R.string.close) {
            @Override
            public void onDrawerClosed(View drawerView)
            {
                super.onDrawerClosed(drawerView);
                ActivityCompat.invalidateOptionsMenu(PasswdSafeActivity.this);
            }

            @Override
            public void onDrawerOpened(View drawerView)
            {
                super.onDrawerOpened(drawerView);
                ActivityCompat.invalidateOptionsMenu(PasswdSafeActivity.this);
            }
        };
        itsDrawerLayout.setDrawerListener(itsDrawerToggle);
        itsDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
                                        GravityCompat.START);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        itsDrawerList = (ListView)findViewById(R.id.left_drawer);
        itsDrawerList.setAdapter(
                new ArrayAdapter<String>(
                        this, ApiCompat.getOptionalActivatedListItem1(),
                        new String[] { "ITEM1", "ITEM2", "ITEM3" }));
        itsDrawerList.setOnItemClickListener(new OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id)
            {
                itsDrawerList.setItemChecked(position, true);
                itsDrawerLayout.closeDrawer(itsDrawerList);
            }
        });

        setView(ViewState.MAIN);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdSafeMainFragment.Listener#chooseOpenFile()
     */
    @Override
    public void chooseOpenFile()
    {
        Intent intent = new Intent(this, FileChooseActivity.class);
        startActivityForResult(intent, ACTIVITY_REQUEST_CHOOSE_FILE);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data)
    {
        switch (requestCode) {
        case ACTIVITY_REQUEST_CHOOSE_FILE: {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                PasswdSafeUtil.dbginfo(TAG, "file choice: %s", uri);
                itsPendingOpenNewUri = uri;
            }
            break;
        }
        default: {
            super.onActivityResult(requestCode, resultCode, data);
            break;
        }
        }
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onStart()
     */
    @Override
    protected void onStart()
    {
        super.onStart();
        ReleaseNotesDialog.checkNotes(this);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        if (itsPendingOpenNewUri != null) {
            setOpenUriView(itsPendingOpenNewUri);
            itsPendingOpenNewUri = null;
        }
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
        if (itsDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
        case R.id.menu_passwdsafe: {
            Intent intent = new Intent(this, FileListActivity.class);
            startActivity(intent);
            break;
        }
        case R.id.menu_about: {
            AboutDialog dlg = new AboutDialog();
            dlg.show(getSupportFragmentManager(), "AboutDialog");
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
        return true;
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        itsDrawerToggle.onConfigurationChanged(newConfig);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        itsDrawerToggle.syncState();
    }

    /** Set the views to open a URI */
    private void setOpenUriView(Uri uri)
    {
        FragmentManager fragMgr = getSupportFragmentManager();
        FragmentTransaction txn = fragMgr.beginTransaction();

        if (itsIsTwoPane) {
            Fragment listFrag = fragMgr.findFragmentById(R.id.content_list);
            if (listFrag != null) {
                txn.hide(listFrag);
                txn.remove(listFrag);
            }
        }

        Fragment contentFrag = fragMgr.findFragmentById(R.id.content);
        if (contentFrag != null) {
            txn.remove(contentFrag);
        }

        PasswdSafeNewOpenFileFragment fileFrag =
                PasswdSafeNewOpenFileFragment.newInstance(uri);
        txn.replace(R.id.content, fileFrag);
        txn.setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        txn.addToBackStack(null);

        if (!txn.isEmpty()) {
            txn.commit();
        }
    }

    /** Set the activity's views to the given state */
    private void setView(ViewState view)
    {
        FragmentManager fragMgr = getSupportFragmentManager();
        FragmentTransaction txn = fragMgr.beginTransaction();
        switch (view) {
        case MAIN: {
            if (itsIsTwoPane) {
                Fragment listFrag = fragMgr.findFragmentById(R.id.content_list);
                if (listFrag != null) {
                    txn.hide(listFrag);
                    txn.remove(listFrag);
                }
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
