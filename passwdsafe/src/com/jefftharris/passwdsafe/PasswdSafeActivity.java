/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
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

import com.jefftharris.passwdsafe.file.ParsedPasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdLocation;
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
                   PasswdSafeOpenFileFragment.Listener,
                   PasswdSafeNewFileFragment.Listener,
                   PasswdSafeListFragment.Listener,
                   PasswdSafeRecordFragment.Listener,
                   PasswdFileActivity
{
    private static final int ACTIVITY_REQUEST_CHOOSE_FILE = 1;

    private static final String TAG = PasswdSafeActivity.class.getName();

    private boolean itsIsTwoPane = false;
    private DrawerLayout itsDrawerLayout;
    private ListView itsDrawerList;
    private ActionBarDrawerToggle itsDrawerToggle;

    private Uri itsPendingOpenNewUri;
    private boolean itsPendingOpenNewIsNew;

    private ActivityPasswdFile itsAppFile = null;
    private PasswdFileData itsFileData = null;
    private ParsedPasswdFileData itsParsedFileData = new ParsedPasswdFileData();
    private PasswdLocation itsLocation = new PasswdLocation();


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
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView)
            {
                super.onDrawerOpened(drawerView);
                supportInvalidateOptionsMenu();
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

        PasswdSafeApp app = (PasswdSafeApp)getApplication();
        ActivityPasswdFile openFile = app.accessOpenFile(this);
        setOpenAppFile(openFile);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdSafeMainFragment.Listener#chooseOpenFile()
     */
    @Override
    public void chooseOpenFile()
    {
        if (PasswdSafeApp.DEBUG_AUTO_FILE == null) {
            Intent intent = new Intent(this, FileChooseActivity.class);
            startActivityForResult(intent, ACTIVITY_REQUEST_CHOOSE_FILE);
        } else {
            setOpenUriView(Uri.fromFile(new File(PasswdSafeApp.DEBUG_AUTO_FILE)));
        }
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdSafeOpenFileFragment.Listener#setOpenFile(com.jefftharris.passwdsafe.file.PasswdFileData)
     * @see com.jefftharris.passwdsafe.PasswdSafeNewFileFragment.Listener#setOpenFile(com.jefftharris.passwdsafe.file.PasswdFileData)
     */
    @Override
    public void setOpenFile(PasswdFileData passwdFile)
    {
        ActivityPasswdFile openFile;
        if (passwdFile != null) {
            PasswdSafeUtil.dbginfo(TAG, "open file %s", passwdFile.getUri());
            PasswdSafeApp app = (PasswdSafeApp)getApplication();
            if (itsAppFile != null) {
                itsAppFile.release();
            }
            openFile = app.accessPasswdFile(passwdFile.getUri(), this);
            openFile.release();
            openFile.setFileData(passwdFile);
        } else {
            PasswdSafeUtil.dbginfo(TAG, "close file");
            openFile = null;
        }
        setOpenAppFile(openFile);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdSafeListFragment.Listener#asyncGetRecordItems(com.jefftharris.passwdsafe.PasswdSafeListFragment.Listener.Mode)
     */
    @Override
    public synchronized List<Map<String, Object>>
    getBackgroundRecordItems(PasswdSafeListFragment.Listener.Mode mode)
    {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        switch (mode) {
        case GROUPS: {
            itsParsedFileData.addGroups(items, this);
            break;
        }
        case RECORDS: {
            itsParsedFileData.addRecords(items, this);
            break;
        }
        case ALL: {
            itsParsedFileData.addGroups(items, this);
            itsParsedFileData.addRecords(items, this);
            break;
        }
        }

        ParsedPasswdFileData.RecordMapComparator comp =
                new ParsedPasswdFileData.RecordMapComparator(false);
        Collections.sort(items, comp);
        return items;
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdSafeListFragment.Listener#changeLocation(com.jefftharris.passwdsafe.file.PasswdLocation)
     */
    @Override
    public void changeLocation(PasswdLocation location)
    {
        if (itsAppFile == null) {
            return;
        }

        PasswdSafeUtil.dbginfo(TAG, "changeLocation loc: %s", location);
        FragmentManager fragMgr = getSupportFragmentManager();
        if (!itsLocation.equals(location)) {
            FragmentTransaction txn = fragMgr.beginTransaction();
            txn.setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

            Fragment contentFrag;
            if (location.getRecord() != null) {
                contentFrag = PasswdSafeRecordFragment.newInstance(location);
            } else {
                contentFrag = PasswdSafeListFragment.newInstance(Mode.RECORDS,
                                                                 location,
                                                                 true);
            }

            txn.replace(R.id.content, contentFrag);
            txn.addToBackStack(null);
            txn.commit();
        }
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdSafeListFragment.Listener#updateLocationView(com.jefftharris.passwdsafe.file.PasswdLocation)
     */
    @Override
    public void updateLocationView(PasswdLocation location)
    {
        PasswdSafeUtil.dbginfo(TAG, "updateLocationView: %s", location);
        itsLocation = location;
        itsParsedFileData.setCurrGroups(itsLocation.getGroups());

        FragmentManager fragMgr = getSupportFragmentManager();
        Fragment contentFrag = fragMgr.findFragmentById(R.id.content);
        if (contentFrag instanceof PasswdSafeListFragment) {
            ((PasswdSafeListFragment)contentFrag).updateLocationView(
                    itsLocation, (itsIsTwoPane ? Mode.RECORDS : Mode.ALL));
        }
        if (itsIsTwoPane) {
            Mode listMode =
                    (itsLocation.getRecord() != null) ? Mode.ALL : Mode.GROUPS;
            Fragment listFrag = fragMgr.findFragmentById(R.id.content_list);
            if (listFrag instanceof PasswdSafeListFragment) {
                ((PasswdSafeListFragment)listFrag).updateLocationView(
                        itsLocation, listMode);
            }
        }
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#getActivity()
     */
    @Override
    public Activity getActivity()
    {
        return this;
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdSafeRecordFragment.Listener#getFileData()
     */
    @Override
    public PasswdFileData getFileData()
    {
        return itsFileData;
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#showProgressDialog()
     */
    @Override
    public void showProgressDialog()
    {
        // TODO save support
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#removeProgressDialog()
     */
    @Override
    public void removeProgressDialog()
    {
        // TODO save support
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#saveFinished(boolean)
     */
    @Override
    public void saveFinished(boolean success)
    {
        // TODO save support
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
                PasswdSafeUtil.dbginfo(TAG, "file choice: %s", data);
                itsPendingOpenNewIsNew =
                    (data.getAction().equals(PasswdSafeUtil.NEW_INTENT));
                itsPendingOpenNewUri = data.getData();
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
     * @see android.support.v4.app.FragmentActivity#onDestroy()
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (itsAppFile != null) {
            itsAppFile.onActivityDestroy();
        }
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onPause()
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        if (itsAppFile != null) {
            itsAppFile.onActivityPause();
        }
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        if (itsPendingOpenNewUri != null) {
            if (itsPendingOpenNewIsNew) {
                setNewUriView(itsPendingOpenNewUri);
            } else {
                setOpenUriView(itsPendingOpenNewUri);
            }
            itsPendingOpenNewUri = null;
            itsPendingOpenNewIsNew = false;
        }

        if (itsAppFile != null) {
            itsAppFile.touch();
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
        case R.id.menu_close: {
            setOpenFile(null);
            return true;
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


    /* (non-Javadoc)
     * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        boolean drawerOpen = itsDrawerLayout.isDrawerOpen(itsDrawerList);
        MenuItem item;

        item = menu.findItem(R.id.menu_passwdsafe);
        item.setVisible(!drawerOpen);

        item = menu.findItem(R.id.menu_close);
        item.setVisible((itsAppFile != null) && !drawerOpen);

        item = menu.findItem(R.id.menu_about);
        item.setVisible(!drawerOpen);

        return super.onPrepareOptionsMenu(menu);
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

    /** Set the open file from the application */
    private void setOpenAppFile(ActivityPasswdFile openFile)
    {
        setMainView();
        if ((openFile != null) && (openFile.getFileData() != null)) {
            itsAppFile = openFile;
            itsFileData = itsAppFile.getFileData();

            FragmentManager fragMgr = getSupportFragmentManager();
            fragMgr.executePendingTransactions();
            FragmentTransaction txn = fragMgr.beginTransaction();
            txn.setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

            Fragment itemsFrag;
            if (itsIsTwoPane) {
                Fragment listFrag = fragMgr.findFragmentById(R.id.content_list);
                if ((listFrag != null) && listFrag.isHidden()) {
                    txn.show(listFrag);
                }
                itemsFrag = PasswdSafeListFragment.newInstance(Mode.RECORDS,
                                                               itsLocation,
                                                               true);
            } else {
                itemsFrag = PasswdSafeListFragment.newInstance(Mode.ALL,
                                                               itsLocation,
                                                               true);
            }

            txn.replace(R.id.content, itemsFrag);
            txn.addToBackStack(null);
            txn.commit();
        } else {
            if (itsAppFile != null) {
                itsAppFile.release();
                itsAppFile.close();
                itsAppFile = null;
                itsFileData = null;
            }
        }
        supportInvalidateOptionsMenu();

        new AsyncTask<PasswdFileData, Void, ParsedPasswdFileData>()
        {
            @Override
            protected ParsedPasswdFileData doInBackground(PasswdFileData... params)
            {
                // TODO: record grouping pref
                // TODO: case sensitivty pref
                return new ParsedPasswdFileData(params[0], true, false,
                                                new ArrayList<String>(),
                                                PasswdSafeActivity.this);
            }

            /* (non-Javadoc)
             * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
             */
            @Override
            protected void onPostExecute(ParsedPasswdFileData result)
            {
                super.onPostExecute(result);
                setParsedFileData(result);
            }
        }.execute(itsFileData);
    }

    /** Set the parsed file data */
    private void setParsedFileData(ParsedPasswdFileData parsedFile)
    {
        itsParsedFileData = parsedFile;
        changeLocation(itsLocation);
        updateLocationView(itsLocation);
    }

    /** Set the views to open a URI */
    private void setOpenUriView(Uri uri)
    {
        setView(PasswdSafeOpenFileFragment.newInstance(uri), true);
    }

    /** Set the views to create a new file URI */
    private void setNewUriView(Uri uri)
    {
        setView(PasswdSafeNewFileFragment.newInstance(uri), true);
    }

    /** Set the views to the main view */
    private void setMainView()
    {
        FragmentManager fragMgr = getSupportFragmentManager();
        while (fragMgr.popBackStackImmediate()) {
            // clear back stack to reset view to main
        }
        setView(new PasswdSafeMainFragment(), false);
    }

    /** Update the view's fragments */
    private void setView(Fragment contentFrag, boolean addBack)
    {
        FragmentManager fragMgr = getSupportFragmentManager();
        FragmentTransaction txn = fragMgr.beginTransaction();
        txn.setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

        if (itsIsTwoPane) {
            Fragment listFrag = fragMgr.findFragmentById(R.id.content_list);
            if ((listFrag != null) && !listFrag.isHidden()) {
                txn.hide(listFrag);
            }
        }

        txn.replace(R.id.content, contentFrag);
        if (addBack) {
            txn.addToBackStack(null);
        }

        txn.commit();
    }
}
