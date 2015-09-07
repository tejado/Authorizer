/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;
import android.widget.Toast;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.view.PasswdFileDataView;
import com.jefftharris.passwdsafe.view.PasswdLocation;
import com.jefftharris.passwdsafe.view.PasswdRecordListData;

import java.util.List;

/**
 * The main PasswdSafe activity for showing a password file
 */
public class PasswdSafeActivity extends AppCompatActivity
        implements PasswdSafeListFragment.Listener,
                   PasswdSafeOpenFileFragment.Listener,
                   PasswdSafeNavDrawerFragment.Listener
{
    // TODO: new files
    // TODO: rotation support without having to reopen file
    // TODO: search
    // TODO: 3rdparty file open
    // TODO: record view
    // TODO: policies
    // TODO: expired passwords
    // TODO: preferences
    // TODO: about
    // TODO: add record
    // TODO: edit record
    // TODO: expiry notifications
    // TODO: details
    // TODO: file operations
    // TODO: modern theme
    // TODO: file close/lock timeout
    // TODO: autobackup

    enum Mode
    {
        /** Initial mode with no file open */
        INIT,
        /** Opening a file */
        FILE_OPEN,
        /** Initial mode for an open file */
        OPEN_INIT,
        /** An open file */
        OPEN
    }

    /** The open password file */
    PasswdFileData itsFileData;

    /** The open password file view */
    PasswdFileDataView itsFileDataView = new PasswdFileDataView(this);

    /** The location in the password file */
    PasswdLocation itsLocation = new PasswdLocation();

    /** Fragment managing the behaviors, interactions and presentation of the
     * navigation drawer. */
    private PasswdSafeNavDrawerFragment itsNavDrawerFrag;

    /** Used to store the last screen title */
    private CharSequence itsTitle;

    /** Does the UI show two panes */
    private boolean itsIsTwoPane = false;

    /** Logging tag */
    private static final String TAG = "PasswdSafeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passwdsafe);
        itsIsTwoPane = (findViewById(R.id.content_list) != null);

        itsNavDrawerFrag = (PasswdSafeNavDrawerFragment)
                getSupportFragmentManager().findFragmentById(
                        R.id.navigation_drawer);
        itsTitle = getTitle();

        // Set up the drawer.
        itsNavDrawerFrag.setUp((DrawerLayout)findViewById(R.id.drawer_layout));
        setInitialView();

        Intent intent = getIntent();
        PasswdSafeUtil.dbginfo(TAG, "onCreate: %s", intent);
        switch (intent.getAction()) {
        case PasswdSafeUtil.VIEW_INTENT:
        case Intent.ACTION_VIEW: {
            setFileOpenView(intent);
            break;
        }
        default: {
            Log.e(TAG, "Unknown action for intent: " + intent);
            finish();
            break;
        }
        }
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        FragmentManager fragMgr = getSupportFragmentManager();
        Fragment frag = fragMgr.findFragmentById(R.id.content);
        if (frag instanceof PasswdSafeOpenFileFragment) {
            ((PasswdSafeOpenFileFragment)frag).onNewIntent(intent);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        itsNavDrawerFrag.onPostCreate();
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (itsFileData != null) {
            itsFileDataView.clearFileData();
            itsFileData.close();
            itsFileData = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if (!itsNavDrawerFrag.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.activity_passwdsafe, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
        case R.id.menu_close: {
            finish();
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    /**
     * Show the file records
     */
    @Override
    public void showFileRecords()
    {
        Toast.makeText(this, "showFileRecords", Toast.LENGTH_SHORT).show();
    }

    /**
     * Show the file password policies
     */
    @Override
    public void showFilePasswordPolicies()
    {
        Toast.makeText(this, "showFilePasswordPolicies", Toast.LENGTH_SHORT).show();
    }

    /**
     * Show the file expired passwords
     */
    @Override
    public void showFileExpiredPasswords()
    {
        Toast.makeText(this, "showFileExpiredPasswords", Toast.LENGTH_SHORT).show();
    }

    /**
     * Show the preferences
     */
    @Override
    public void showPreferences()
    {
        Toast.makeText(this, "showPreferences", Toast.LENGTH_SHORT).show();
    }

    /**
     * Show the about dialog
     */
    @Override
    public void showAbout()
    {
        Toast.makeText(this, "showAbout", Toast.LENGTH_SHORT).show();
    }

    /**
     * Handle when the file open is canceled
     */
    @Override
    public void handleFileOpenCanceled()
    {
        PasswdSafeUtil.dbginfo(TAG, "handleFileOpenCanceled");
        finish();
    }

    /**
     * Handle when the file was successfully opened
     */
    @Override
    public void handleFileOpen(PasswdFileData fileData, String recToOpen)
    {
        PasswdSafeUtil.dbginfo(TAG, "handleFileOpen: %s, rec: %s",
                               fileData.getUri(), recToOpen);

        // TODO: recToOpen
        if (itsFileData != null) {
            itsFileDataView.clearFileData();
            itsFileData.close();
        }
        itsFileData = fileData;
        itsFileDataView.setFileData(itsFileData);
        setOpenView(itsLocation, true);
    }

    /**
     * Get the current record items in a background thread
     */
    @Override
    public List<PasswdRecordListData> getBackgroundRecordItems(
            PasswdSafeListFragment.Listener.Mode mode)
    {
        if (itsFileDataView == null) {
            return null;
        }

        boolean incRecords = false;
        boolean incGroups = false;
        switch (mode) {
        case GROUPS: {
            incGroups = true;
            break;
        }
        case RECORDS: {
            incRecords = true;
            break;
        }
        case ALL: {
            incGroups = true;
            incRecords = true;
            break;
        }
        }
        return itsFileDataView.getRecords(incRecords, incGroups);
    }

    /**
     * Change the location in the password file
     */
    @Override
    public void changeLocation(PasswdLocation location)
    {
        if (itsFileData == null) {
            return;
        }

        PasswdSafeUtil.dbginfo(TAG, "changeLocation loc: %s", location);
        if (!itsLocation.equals(location)) {
            setOpenView(location, false);
        }
    }

    /**
     * Update the view for the location in the password file
     */
    @Override
    public void updateLocationView(PasswdLocation location)
    {
        PasswdSafeUtil.dbginfo(TAG, "updateLocationView: %s", location);
        itsLocation = location;
        itsFileDataView.setCurrGroups(itsLocation.getGroups());

        String groups = location.getGroupPath();
        if (!TextUtils.isEmpty(groups)) {
            itsTitle = PasswdSafeApp.getAppTitle(groups, this);
        } else {
            itsTitle = PasswdSafeApp.getAppFileTitle(itsFileData.getUri(),
                                                     this);
        }
        restoreActionBar();

        if (itsIsTwoPane) {
            PasswdSafeListFragment.Listener.Mode listMode =
                    (itsLocation.getRecord() != null) ?
                            PasswdSafeListFragment.Listener.Mode.ALL :
                            PasswdSafeListFragment.Listener.Mode.GROUPS;
            FragmentManager fragMgr = getSupportFragmentManager();
            Fragment listFrag = fragMgr.findFragmentById(R.id.content_list);
            if (listFrag instanceof PasswdSafeListFragment) {
                ((PasswdSafeListFragment)listFrag).updateLocationView(
                        itsLocation, listMode);
            }
        }

    }

    /**
     * Set the initial view
     */
    private void setInitialView()
    {
        setView(Mode.INIT, null);
    }

    /**
     * Set the view for opening a file
     */
    private void setFileOpenView(Intent intent)
    {
        Uri openUri = PasswdSafeApp.getOpenUriFromIntent(intent);
        String recToOpen = intent.getData().getQueryParameter("recToOpen");
        Fragment openFrag = PasswdSafeOpenFileFragment.newInstance(openUri,
                                                                   recToOpen);
        setView(Mode.FILE_OPEN, openFrag);
    }

    /**
     * Set the view for an open file
     */
    private void setOpenView(PasswdLocation location, boolean initial)
    {
        PasswdSafeListFragment.Listener.Mode mode =
                itsIsTwoPane ? PasswdSafeListFragment.Listener.Mode.RECORDS :
                        PasswdSafeListFragment.Listener.Mode.ALL;
        Fragment viewFrag = PasswdSafeListFragment.newInstance(
                mode, location, true);

        Mode viewMode = initial ? Mode.OPEN_INIT : Mode.OPEN;
        setView(viewMode, viewFrag);
    }

    /**
     * Set the view of the activity
     */
    private void setView(Mode mode, Fragment contentFrag)
    {
        FragmentManager fragMgr = getSupportFragmentManager();
        //FragmentManager.enableDebugLogging(true);
        FragmentTransaction txn = fragMgr.beginTransaction();

        boolean fileOpen = false;
        boolean showLeftList = false;
        boolean forceLeftListVisibility = false;
        boolean clearBackStack = false;
        boolean supportsBack = false;
        switch (mode) {
        case INIT: {
            clearBackStack = true;
            forceLeftListVisibility = true;
            break;
        }
        case FILE_OPEN: {
            clearBackStack = true;
            break;
        }
        case OPEN_INIT: {
            fileOpen = true;
            showLeftList = true;
            clearBackStack = true;
            break;
        }
        case OPEN: {
            fileOpen = true;
            showLeftList = true;
            supportsBack = true;
            break;
        }
        }
        itsNavDrawerFrag.setFileOpen(fileOpen);
        if (clearBackStack) {
            //noinspection StatementWithEmptyBody
            while (fragMgr.popBackStackImmediate()) {
                // Clear back stack
            }
        }

        txn.setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        if (contentFrag != null) {
            txn.replace(R.id.content, contentFrag);
        } else {
            Fragment currFrag = fragMgr.findFragmentById(R.id.content);
            if (currFrag != null) {
                txn.remove(currFrag);
            }
        }
        setLeftListVisible(showLeftList, forceLeftListVisibility, txn, fragMgr);

        if (supportsBack) {
            txn.addToBackStack(null);
        }

        txn.commit();
    }

    /**
     *  Set whether the left pane is visible
     */
    private void setLeftListVisible(boolean visible,
                                    boolean force,
                                    FragmentTransaction txn,
                                    FragmentManager fragMgr)
    {
        if (itsIsTwoPane) {
            Fragment listFrag = fragMgr.findFragmentById(R.id.content_list);
            if (listFrag != null) {
                if (visible && (force || listFrag.isHidden())) {
                    txn.show(listFrag);
                } else if (!visible && (force || listFrag.isVisible())) {
                    txn.hide(listFrag);
                }
            }
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
