/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
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
import com.jefftharris.passwdsafe.lib.ApiCompat;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.view.ProgressFragment;
import com.jefftharris.passwdsafe.view.PasswdFileDataView;
import com.jefftharris.passwdsafe.view.PasswdLocation;
import com.jefftharris.passwdsafe.view.PasswdRecordListData;

import org.pwsafe.lib.file.PwsRecord;

import java.io.IOException;
import java.util.List;

/**
 * The main PasswdSafe activity for showing a password file
 */
public class PasswdSafeActivity extends AppCompatActivity
        implements AbstractPasswdSafeRecordFragment.Listener,
                   PasswdSafeEditRecordFragment.Listener,
                   PasswdSafeListFragment.Listener,
                   PasswdSafeOpenFileFragment.Listener,
                   PasswdSafeNavDrawerFragment.Listener,
                   PasswdSafeRecordFragment.Listener
{
    // TODO: new files
    // TODO: search
    // TODO: 3rdparty file open
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
    // TODO: keyboard support
    // TODO: shortcuts

    private enum ChangeMode
    {
        /** Initial mode with no file open */
        INIT,
        /** Opening a file */
        FILE_OPEN,
        /** Initial mode for an open file */
        OPEN_INIT,
        /** An open file */
        OPEN,
        /** A record */
        RECORD,
        /** Edit a record */
        EDIT_RECORD
    }

    private enum ViewMode
    {
        /** Initial mode */
        INIT,
        /** Opening a file */
        FILE_OPEN,
        /** Viewing a list of records */
        VIEW_LIST,
        /** Viewing a record */
        VIEW_RECORD,
        /** Editing a record */
        EDIT_RECORD
    }

    /** Fragment holding the open file data */
    private PasswdSafeFileDataFragment itsFileDataFrag;

    /** The location in the password file */
    private PasswdLocation itsLocation = new PasswdLocation();

    /** Fragment managing the behaviors, interactions and presentation of the
     * navigation drawer. */
    private PasswdSafeNavDrawerFragment itsNavDrawerFrag;

    /** Running task to save a file */
    private SaveTask itsSaveTask = null;

    /** Used to store the last screen title */
    private CharSequence itsTitle;

    /** Does the UI show two panes */
    private boolean itsIsTwoPane = false;

    private static final String FRAG_DATA = "data";
    private static final String STATE_TITLE = "title";
    private static final String TAG = "PasswdSafeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        ApiCompat.setRecentAppsVisible(getWindow(), false);
        setContentView(R.layout.activity_passwdsafe);
        itsIsTwoPane = (findViewById(R.id.two_pane) != null);

        itsNavDrawerFrag = (PasswdSafeNavDrawerFragment)
                getSupportFragmentManager().findFragmentById(
                        R.id.navigation_drawer);
        itsNavDrawerFrag.setUp((DrawerLayout)findViewById(R.id.drawer_layout));

        FragmentManager fragMgr = getSupportFragmentManager();
        itsFileDataFrag = (PasswdSafeFileDataFragment)
                fragMgr.findFragmentByTag(FRAG_DATA);
        if (itsFileDataFrag == null) {
            itsFileDataFrag = new PasswdSafeFileDataFragment();
            fragMgr.beginTransaction().add(itsFileDataFrag, FRAG_DATA).commit();
        }
        boolean newFileDataFrag = itsFileDataFrag.checkNew();

        if (newFileDataFrag || (savedInstanceState == null)) {
            itsTitle = getTitle();
            doUpdateView(ViewMode.INIT, new PasswdLocation());
            changeInitialView();

            Intent intent = getIntent();
            PasswdSafeUtil.dbginfo(TAG, "onCreate: %s", intent);
            switch (intent.getAction()) {
            case PasswdSafeUtil.VIEW_INTENT:
            case Intent.ACTION_VIEW: {
                changeFileOpenView(intent);
                break;
            }
            default: {
                Log.e(TAG, "Unknown action for intent: " + intent);
                finish();
                break;
            }
            }
        } else {
            itsTitle = savedInstanceState.getCharSequence(STATE_TITLE);
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
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(STATE_TITLE, itsTitle);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (itsSaveTask != null) {
            itsSaveTask.cancelTask();
            itsSaveTask = null;
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
        switch (item.getItemId()) {
        case android.R.id.home: {
            if (itsNavDrawerFrag.isDrawerEnabled()) {
                return super.onOptionsItemSelected(item);
            }
            onBackPressed();
            return true;
        }
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
        itsFileDataFrag.setFileData(fileData, this);
        changeOpenView(itsLocation, true);
    }

    /**
     * Get the current record items in a background thread
     */
    @Override
    public List<PasswdRecordListData> getBackgroundRecordItems(
            boolean incRecords,
            boolean incGroups)
    {
        PasswdFileDataView dataView = itsFileDataFrag.getFileDataView();
        if (dataView == null) {
            return null;
        }

        return dataView.getRecords(incRecords, incGroups,
                                   getApplicationContext());
    }

    /**
     * Change the location in the password file
     */
    @Override
    public void changeLocation(PasswdLocation location)
    {
        if (itsFileDataFrag.getFileData() == null) {
            return;
        }

        PasswdSafeUtil.dbginfo(TAG, "changeLocation loc: %s", location);
        if (!itsLocation.equals(location)) {
            changeOpenView(location, false);
        }
    }

    /**
     * Get the file data
     */
    @Override
    public PasswdFileData getFileData()
    {
        return itsFileDataFrag.getFileData();
    }

    @Override
    public void editRecord(PasswdLocation location)
    {
        if (itsFileDataFrag.getFileData() == null) {
            return;
        }

        PasswdSafeUtil.dbginfo(TAG, "editRecord loc: %s", location);
        doChangeView(ChangeMode.EDIT_RECORD,
                     PasswdSafeEditRecordFragment.newInstance(location));
    }

    @Override
    public void deleteRecord(PasswdLocation location)
    {
        Toast.makeText(this, "deleteRecord " + location,
                       Toast.LENGTH_SHORT).show();
    }

    /**
     * Update the view for opening a file
     */
    @Override
    public void updateViewFileOpen()
    {
        doUpdateView(ViewMode.FILE_OPEN, new PasswdLocation());
    }

    /**
     * Update the view for a list of records
     */
    @Override
    public void updateViewList(PasswdLocation location)
    {
        doUpdateView(ViewMode.VIEW_LIST, location);
    }

    /**
     * Update the view for a record
     */
    @Override
    public void updateViewRecord(PasswdLocation location)
    {
        doUpdateView(ViewMode.VIEW_RECORD, location);
    }

    /** Update the view for editing a record */
    @Override
    public void updateViewEditRecord(PasswdLocation location)
    {
        doUpdateView(ViewMode.EDIT_RECORD, location);
    }

    @Override
    public boolean isNavDrawerOpen()
    {
        return itsNavDrawerFrag.isDrawerOpen();
    }

    @Override
    public void finishEditRecord(boolean save)
    {
        FragmentManager fragMgr = getSupportFragmentManager();
        fragMgr.popBackStack();

        if (save) {
            itsSaveTask = new SaveTask(itsFileDataFrag.getFileData(), this);
            itsSaveTask.execute();
        }
    }

    /**
     * Change the initial view
     */
    private void changeInitialView()
    {
        doChangeView(ChangeMode.INIT, null);
    }

    /**
     * Change the view for opening a file
     */
    private void changeFileOpenView(Intent intent)
    {
        Uri openUri = PasswdSafeApp.getOpenUriFromIntent(intent);
        String recToOpen = intent.getData().getQueryParameter("recToOpen");
        Fragment openFrag = PasswdSafeOpenFileFragment.newInstance(openUri,
                                                                   recToOpen);
        doChangeView(ChangeMode.FILE_OPEN, openFrag);
    }

    /**
     * Change the view for an open file
     */
    private void changeOpenView(PasswdLocation location, boolean initial)
    {
        Fragment viewFrag;
        ChangeMode viewMode;
        if (location.isRecord()) {
            viewFrag = PasswdSafeRecordFragment.newInstance(location);
            viewMode = ChangeMode.RECORD;
        } else {
            viewFrag = PasswdSafeListFragment.newInstance(location, true);
            viewMode = initial ? ChangeMode.OPEN_INIT : ChangeMode.OPEN;
        }
        doChangeView(viewMode, viewFrag);
    }

    /**
     * Change the view of the activity
     */
    private void doChangeView(ChangeMode mode, Fragment contentFrag)
    {
        FragmentManager fragMgr = getSupportFragmentManager();
        //FragmentManager.enableDebugLogging(true);
        FragmentTransaction txn = fragMgr.beginTransaction();

        boolean clearBackStack = false;
        boolean supportsBack = false;
        switch (mode) {
        case INIT:
        case FILE_OPEN:
        case OPEN_INIT: {
            clearBackStack = true;
            break;
        }
        case OPEN:
        case RECORD:
        case EDIT_RECORD: {
            supportsBack = true;
            break;
        }
        }
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

        if (supportsBack) {
            txn.addToBackStack(null);
        }

        txn.commit();
    }

    /**
     * Update the view mode
     */
    private void doUpdateView(ViewMode mode, @NonNull PasswdLocation location)
    {
        PasswdSafeUtil.dbginfo(TAG, "doUpdateView: mode: %s, loc: %s",
                               mode, location);

        itsLocation = location;
        itsFileDataFrag.getFileDataView().setCurrGroups(itsLocation.getGroups());

        FragmentManager fragMgr = getSupportFragmentManager();
        boolean showLeftList = false;
        PasswdSafeNavDrawerFragment.NavMode drawerMode =
                PasswdSafeNavDrawerFragment.NavMode.INIT;
        switch (mode) {
        case INIT:
        case FILE_OPEN: {
            itsTitle = PasswdSafeApp.getAppTitle(null, this);
            break;
        }
        case VIEW_LIST: {
            showLeftList = true;
            drawerMode = PasswdSafeNavDrawerFragment.NavMode.FILE_OPEN;
            itsTitle = null;
            String groups = itsLocation.getGroupPath();
            if (TextUtils.isEmpty(groups)) {
                PasswdFileData fileData = itsFileDataFrag.getFileData();
                if (fileData != null) {
                    itsTitle = PasswdSafeApp.getAppFileTitle(
                            fileData.getUri(), this);
                }
            }
            if (itsTitle == null) {
                itsTitle = PasswdSafeApp.getAppTitle(groups, this);
            }

            Fragment contentsFrag = fragMgr.findFragmentById(R.id.content);
            if (contentsFrag instanceof PasswdSafeListFragment) {
                PasswdSafeListFragment.Mode contentsMode = itsIsTwoPane ?
                        PasswdSafeListFragment.Mode.RECORDS :
                        PasswdSafeListFragment.Mode.ALL;
                ((PasswdSafeListFragment)contentsFrag).updateLocationView(
                        itsLocation, contentsMode);
            }
            break;
        }
        case VIEW_RECORD:
        case EDIT_RECORD: {
            showLeftList = true;
            drawerMode = itsIsTwoPane ?
                    PasswdSafeNavDrawerFragment.NavMode.FILE_OPEN :
                    PasswdSafeNavDrawerFragment.NavMode.SINGLE_RECORD;
            PasswdFileData fileData = itsFileDataFrag.getFileData();
            if ((fileData != null) && itsLocation.isRecord()) {
                PwsRecord rec = fileData.getRecord(itsLocation.getRecord());
                String title = fileData.getTitle(rec);
                if (mode == ViewMode.VIEW_RECORD) {
                    itsTitle = title;
                } else {
                    itsTitle = getString(R.string.edit_item, title);
                }
            }
            break;
        }
        }

        itsNavDrawerFrag.setMode(drawerMode);
        restoreActionBar();

        if (itsIsTwoPane) {
            PasswdSafeListFragment.Mode listMode = itsLocation.isRecord() ?
                    PasswdSafeListFragment.Mode.ALL :
                    PasswdSafeListFragment.Mode.GROUPS;
            Fragment listFrag = fragMgr.findFragmentById(R.id.content_list);
            if (listFrag instanceof PasswdSafeListFragment) {
                ((PasswdSafeListFragment)listFrag).updateLocationView(
                        itsLocation, listMode);
            }

            if (listFrag != null) {
                FragmentTransaction txn = fragMgr.beginTransaction();
                if (showLeftList) {
                    txn.setTransitionStyle(
                            FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    txn.show(listFrag);
                } else {
                    txn.setTransitionStyle(
                            FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
                    txn.hide(listFrag);
                }
                txn.commit();
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

    /**
     * Task to save a file in the background
     */
    private class SaveTask
    {
        private final PasswdFileData itsFileData;
        private final Context itsContext;
        private final ProgressFragment itsProgressFrag;
        private final AsyncTask<Void, Void, Object> itsTask =
                new AsyncTask<Void, Void, Object>()
        {
            @Override
            protected void onPreExecute()
            {
                itsProgressFrag.show(getSupportFragmentManager(), null);
            }

            @Override
            protected void onPostExecute(Object result)
            {
                SaveTask.this.onPostExecute(result);
            }

            @Override
            protected Object doInBackground(Void... params)
            {
                try {
                    if (itsFileData != null) {
                        itsFileData.save(itsContext);
                        PasswdSafeUtil.dbginfo(TAG, "SaveTask finished");
                    }
                    return null;
                } catch (Exception e) {
                    return e;
                }
            }
        };

        /**
         * Constructor
         */
        public SaveTask(PasswdFileData fileData, Context ctx)
        {
            itsFileData = fileData;
            itsContext = ctx.getApplicationContext();

            String file = itsFileData.getUri().getIdentifier(itsContext, false);
            String msg = itsContext.getString(R.string.saving_file, file);
            itsProgressFrag = ProgressFragment.newInstance(msg);
        }

        /**
         * Execute the task
         */
        public final void execute()
        {
            itsTask.execute();
        }

        /**
         * Cancel the save task
         */
        public final void cancelTask()
        {
            itsTask.cancel(false);
            onPostExecute(null);
        }

        /**
         * Handle the result of executing the task
         */
        private void onPostExecute(Object result)
        {
            itsProgressFrag.dismiss();
            itsSaveTask = null;

            if (result instanceof Exception) {
                Exception e = (Exception)result;
                String msg = e.toString();
                if ((e instanceof IOException) &&
                    (ApiCompat.SDK_VERSION >= ApiCompat.SDK_KITKAT)) {
                    msg = itsContext.getString(R.string.kitkat_sdcard_warning,
                                               msg);
                }
                PasswdSafeUtil.showFatalMsg(e, msg, PasswdSafeActivity.this,
                                            true);
            }
        }
    }
}
