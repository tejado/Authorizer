/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
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
import com.jefftharris.passwdsafe.file.PasswdFileUri;
import com.jefftharris.passwdsafe.lib.ApiCompat;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.lib.view.ProgressFragment;
import com.jefftharris.passwdsafe.view.ConfirmPromptDialog;
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
                   ConfirmPromptDialog.Listener,
                   PasswdSafeChangePasswordFragment.Listener,
                   PasswdSafeEditRecordFragment.Listener,
                   PasswdSafeListFragment.Listener,
                   PasswdSafeOpenFileFragment.Listener,
                   PasswdSafeNavDrawerFragment.Listener,
                   PasswdSafeNewFileFragment.Listener,
                   PasswdSafeRecordFragment.Listener
{
    // TODO: search
    // TODO: 3rdparty file open
    // TODO: policies
    // TODO: expired passwords
    // TODO: preferences
    // TODO: about
    // TODO: expiry notifications
    // TODO: details
    // TODO: protect / unprotect all
    // TODO: modern theme
    // TODO: recheck all icons (remove use of all built-in ones)
    // TODO: autobackup
    // TODO: keyboard support
    // TODO: shortcuts
    // TODO: check manifest errors regarding icons
    // TODO: storage access framework support (want to keep support?)
    // TODO: recent files db (should that be carried forward? only if SAF kept)
    // TODO: add checked get file data to show fatal error if none

    private enum ChangeMode
    {
        /** Initial mode with no file open */
        INIT,
        /** Opening a file */
        FILE_OPEN,
        /** Creating a new file */
        FILE_NEW,
        /** Initial mode for an open file */
        OPEN_INIT,
        /** An open file */
        OPEN,
        /** A record */
        RECORD,
        /** Edit a record */
        EDIT_RECORD,
        /** Change a password */
        CHANGE_PASSWORD
    }

    private enum ViewMode
    {
        /** Initial mode */
        INIT,
        /** Opening a file */
        FILE_OPEN,
        /** Creating a new file */
        FILE_NEW,
        /** Viewing a list of records */
        VIEW_LIST,
        /** Viewing a record */
        VIEW_RECORD,
        /** Editing a record */
        EDIT_RECORD,
        /** Changing a password */
        CHANGING_PASSWORD
    }

    /** Action conformed via ConfirmPromptDialog */
    private enum ConfirmAction
    {
        /** Delete a file */
        DELETE_FILE,
        /** Delete a record */
        DELETE_RECORD
    }

    /** Fragment holding the open file data */
    private PasswdSafeFileDataFragment itsFileDataFrag;

    /** The location in the password file */
    private PasswdLocation itsLocation = new PasswdLocation();

    /** Fragment managing the behaviors, interactions and presentation of the
     * navigation drawer. */
    private PasswdSafeNavDrawerFragment itsNavDrawerFrag;

    /** Currently running task */
    private AbstractTask itsCurrTask = null;

    /** Used to store the last screen title */
    private CharSequence itsTitle;

    /** Does the UI show two panes */
    private boolean itsIsTwoPane = false;

    /** Receiver for file timeout notifications */
    private FileTimeoutReceiver itsTimeoutReceiver;

    /** Current view mode */
    private ViewMode itsCurrViewMode = ViewMode.INIT;

    private static final String FRAG_DATA = "data";
    private static final String STATE_TITLE = "title";

    private static final String CONFIRM_ARG_ACTION = "action";
    private static final String CONFIRM_ARG_LOCATION = "location";

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

        itsTimeoutReceiver = new FileTimeoutReceiver(this);

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
            case PasswdSafeUtil.NEW_INTENT: {
                changeFileNewView(intent);
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
        if (itsCurrTask != null) {
            itsCurrTask.cancelTask();
            itsCurrTask = null;
        }
    }

    @Override
    protected void onDestroy()
    {
        itsTimeoutReceiver.onDestroy();
        super.onDestroy();
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
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        PasswdFileData fileData = itsFileDataFrag.getFileData();
        boolean fileEditable = (fileData != null) && fileData.canEdit();

        boolean viewCanAdd = false;
        boolean viewHasFileOps = false;
        boolean viewHasFileChangePassword = false;
        boolean viewHasClose = true;
        switch (itsCurrViewMode) {
        case VIEW_LIST: {
            viewCanAdd = fileEditable;
            if (itsLocation.getGroups().isEmpty() && fileEditable) {
                viewHasFileOps = true;
                viewHasFileChangePassword = !fileData.isYubikey();
            }
            break;
        }
        case VIEW_RECORD: {
            viewCanAdd = fileEditable;
            break;
        }
        case INIT:
        case FILE_OPEN:
        case FILE_NEW: {
            break;
        }
        case EDIT_RECORD:
        case CHANGING_PASSWORD: {
            viewHasClose = false;
            break;
        }
        }

        MenuItem item = menu.findItem(R.id.menu_add);
        if (item != null) {
            item.setVisible(viewCanAdd);
        }

        item = menu.findItem(R.id.menu_close);
        if (item != null) {
            item.setVisible(viewHasClose);
        }

        item = menu.findItem(R.id.menu_file_ops);
        if (item != null) {
            item.setVisible(viewHasFileOps);
        }

        item = menu.findItem(R.id.menu_file_change_password);
        if (item != null) {
            item.setEnabled(viewHasFileChangePassword);
        }

        return super.onPrepareOptionsMenu(menu);
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
        case R.id.menu_add: {
            editRecord(itsLocation.selectRecord(null));
            return true;
        }
        case R.id.menu_close: {
            checkNavigation(false, new Runnable()
            {
                @Override
                public void run()
                {
                    finish();
                }
            });
            return true;
        }
        case R.id.menu_file_change_password: {
            PasswdSafeUtil.dbginfo(TAG, "change password");
            doChangeView(ChangeMode.CHANGE_PASSWORD,
                         PasswdSafeChangePasswordFragment.newInstance());
            return true;
        }
        case R.id.menu_file_delete: {
            PasswdFileData fileData = itsFileDataFrag.getFileData();
            if (fileData == null) {
                return true;
            }
            String uriName = fileData.getUri().getIdentifier(this, true);

            Bundle confirmArgs = new Bundle();
            confirmArgs.putString(CONFIRM_ARG_ACTION,
                                  ConfirmAction.DELETE_FILE.name());
            ConfirmPromptDialog dialog = ConfirmPromptDialog.newInstance(
                    getString(R.string.delete_file_msg, uriName),
                    getString(R.string.delete),
                    confirmArgs);
            dialog.show(getSupportFragmentManager(), "Delete file");
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
        checkNavigation(false, new Runnable()
        {
            @Override
            public void run()
            {
                PasswdSafeActivity.super.onBackPressed();
            }
        });
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
     * Handle when the file new is canceled
     */
    @Override
    public void handleFileNewCanceled()
    {
        PasswdSafeUtil.dbginfo(TAG, "handleFileNewCanceled");
        finish();
    }

    /**
     * Handle when the file was successfully created
     */
    @Override
    public void handleFileNew(PasswdFileData fileData)
    {
        PasswdSafeUtil.dbginfo(TAG, "handleFileNew: %s", fileData.getUri());
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
    public void deleteRecord(PasswdLocation location, String title)
    {
        PasswdSafeUtil.dbginfo(TAG, "deleteRecord loc: %s", location);
        Bundle confirmArgs = new Bundle();
        confirmArgs.putString(CONFIRM_ARG_ACTION,
                              ConfirmAction.DELETE_RECORD.name());
        confirmArgs.putParcelable(CONFIRM_ARG_LOCATION, location);
        ConfirmPromptDialog dialog = ConfirmPromptDialog.newInstance(
                getString(R.string.delete_record_msg, title),
                getString(R.string.delete),
                confirmArgs);
        dialog.show(getSupportFragmentManager(), "Delete record");
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
     * Update the view for creating a new file
     */
    @Override
    public void updateViewFileNew()
    {
        doUpdateView(ViewMode.FILE_NEW, new PasswdLocation());
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
    public void finishEditRecord(boolean save, PasswdLocation newLocation)
    {
        boolean resetLoc = false;
        if (save) {
            itsFileDataFrag.refreshFileData(this);
            if ((newLocation != null) &&
                !newLocation.equalGroups(itsLocation)) {
                resetLoc = true;
            }
        }

        FragmentManager fragMgr = getSupportFragmentManager();
        fragMgr.popBackStackImmediate();

        if (save) {
            itsCurrTask = new SaveTask(itsFileDataFrag.getFileData(),
                                       resetLoc, this);
            itsCurrTask.execute();
        }
    }

    @Override
    public void finishChangePassword()
    {
        finishEditRecord(true, null);
    }

    @Override
    public void updateViewChangingPassword()
    {
        doUpdateView(ViewMode.CHANGING_PASSWORD, itsLocation);
    }

    @Override
    public void promptConfirmed(Bundle confirmArgs)
    {
        PasswdSafeUtil.dbginfo(TAG, "promptConfirmed: %s", confirmArgs);
        PasswdFileData fileData = itsFileDataFrag.getFileData();
        if (fileData == null) {
            return;
        }

        ConfirmAction action;
        try {
            action = ConfirmAction.valueOf(
                    confirmArgs.getString(CONFIRM_ARG_ACTION));
        } catch (Exception e) {
            return;
        }

        switch (action) {
        case DELETE_FILE: {
            itsCurrTask = new DeleteTask(fileData.getUri(), this);
            itsCurrTask.execute();
            break;
        }
        case DELETE_RECORD: {
            PasswdLocation location =
                    confirmArgs.getParcelable(CONFIRM_ARG_LOCATION);
            if (location == null) {
                break;
            }

            PwsRecord rec = fileData.getRecord(location.getRecord());
            if (rec == null) {
                break;
            }

            boolean removed = fileData.removeRecord(rec, this);
            if (removed) {
                finishEditRecord(true, location.selectRecord(null));
            }
            break;
        }
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
     * Change the view for creating a new file
     */
    private void changeFileNewView(Intent intent)
    {
        Uri newUri = PasswdSafeApp.getOpenUriFromIntent(intent);
        Fragment newFrag = PasswdSafeNewFileFragment.newInstance(newUri);
        doChangeView(ChangeMode.FILE_NEW, newFrag);
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
    private void doChangeView(final ChangeMode mode, final Fragment contentFrag)
    {
        checkNavigation(true, new Runnable()
        {
            public void run()
            {
                FragmentManager fragMgr = getSupportFragmentManager();
                FragmentTransaction txn = fragMgr.beginTransaction();

                boolean clearBackStack = false;
                boolean supportsBack = false;
                switch (mode) {
                case INIT:
                case FILE_OPEN:
                case FILE_NEW:
                case OPEN_INIT: {
                    clearBackStack = true;
                    break;
                }
                case OPEN:
                case RECORD:
                case EDIT_RECORD:
                case CHANGE_PASSWORD: {
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

                txn.setTransitionStyle(
                        FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
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
        });
    }

    /**
     * Check whether to confirm before performing a navigation change
     */
    private void checkNavigation(final boolean popOnConfirm,
                                 final Runnable navRun)
    {
        boolean doPrompt = false;
        switch (itsCurrViewMode) {
        case EDIT_RECORD: {
            doPrompt = true;
            break;
        }
        case INIT:
        case FILE_OPEN:
        case FILE_NEW:
        case VIEW_LIST:
        case VIEW_RECORD:
        case CHANGING_PASSWORD: {
            break;
        }
        }

        if (doPrompt) {
            DialogInterface.OnClickListener continueListener =
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.dismiss();
                            if (popOnConfirm) {
                                FragmentManager fragMgr =
                                        getSupportFragmentManager();
                                fragMgr.popBackStackImmediate();
                            }
                            navRun.run();
                        }
                    };
            new AlertDialog.Builder(this)
                    .setTitle(R.string.continue_p)
                    .setMessage(R.string.any_changes_will_be_lost)
                    .setPositiveButton(R.string.continue_str, continueListener)
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            navRun.run();
        }
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
        itsCurrViewMode = mode;

        FragmentManager fragMgr = getSupportFragmentManager();
        boolean showLeftList = false;
        PasswdSafeNavDrawerFragment.NavMode drawerMode =
                PasswdSafeNavDrawerFragment.NavMode.INIT;
        boolean fileTimeoutPaused = true;
        switch (mode) {
        case INIT:
        case FILE_OPEN:
        case FILE_NEW: {
            itsTitle = PasswdSafeApp.getAppTitle(null, this);
            break;
        }
        case VIEW_LIST: {
            showLeftList = true;
            drawerMode = PasswdSafeNavDrawerFragment.NavMode.FILE_OPEN;
            fileTimeoutPaused = false;
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
        case VIEW_RECORD: {
            showLeftList = true;
            drawerMode = itsIsTwoPane ?
                    PasswdSafeNavDrawerFragment.NavMode.FILE_OPEN :
                    PasswdSafeNavDrawerFragment.NavMode.SINGLE_RECORD;
            fileTimeoutPaused = false;
            PasswdFileData fileData = itsFileDataFrag.getFileData();
            if ((fileData != null) && itsLocation.isRecord()) {
                PwsRecord rec = fileData.getRecord(itsLocation.getRecord());
                itsTitle = fileData.getTitle(rec);
            } else {
                itsTitle = getString(R.string.new_entry);
            }
            break;
        }
        case EDIT_RECORD: {
            drawerMode = PasswdSafeNavDrawerFragment.NavMode.CANCELABLE_ACTION;
            PasswdFileData fileData = itsFileDataFrag.getFileData();
            if ((fileData != null) && itsLocation.isRecord()) {
                PwsRecord rec = fileData.getRecord(itsLocation.getRecord());
                itsTitle = getString(R.string.edit_item,
                                     fileData.getTitle(rec));
            } else {
                itsTitle = getString(R.string.new_entry);
            }
            break;
        }
        case CHANGING_PASSWORD: {
            itsTitle = getString(R.string.change_password);
            drawerMode = PasswdSafeNavDrawerFragment.NavMode.CANCELABLE_ACTION;
            fileTimeoutPaused = false;
            break;
        }
        }

        GuiUtils.invalidateOptionsMenu(this);
        itsNavDrawerFrag.setMode(drawerMode);
        restoreActionBar();
        itsTimeoutReceiver.updateTimeout(fileTimeoutPaused);

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
    private final class SaveTask extends AbstractTask
    {
        private final PasswdFileData itsFileData;
        private final boolean itsIsResetLoc;

        /**
         * Constructor
         */
        public SaveTask(PasswdFileData fileData, boolean resetLoc, Context ctx)
        {
            super(ctx.getString(R.string.saving_file,
                                fileData.getUri().getIdentifier(ctx, false)),
                  ctx);
            itsFileData = fileData;
            itsIsResetLoc = resetLoc;
        }

        @Override
        protected final void handleDoInBackground() throws Exception
        {
            if (itsFileData != null) {
                itsFileData.save(getContext());
                PasswdSafeUtil.dbginfo(TAG, "SaveTask finished");
            }
        }

        @Override
        protected final void handlePostExecute()
        {
            if (itsIsResetLoc) {
                changeOpenView(new PasswdLocation(), true);
            }
        }

        @Override
        protected final String getExceptionMsg(Exception e)
        {
            String msg = super.getExceptionMsg(e);
            if ((e instanceof IOException) &&
                (ApiCompat.SDK_VERSION >= ApiCompat.SDK_KITKAT)) {
                msg = getContext().getString(R.string.kitkat_sdcard_warning,
                                             msg);
            }
            return msg;
        }
    }

    /**
     * Task to delete a file in the background
     */
    private final class DeleteTask extends AbstractTask
    {
        private final PasswdFileUri itsFileUri;

        /**
         * Constructor
         */
        public DeleteTask(PasswdFileUri uri, Context ctx)
        {
            super(ctx.getString(R.string.delete_file), ctx);
            itsFileUri = uri;
        }

        @Override
        protected void handleDoInBackground() throws Exception
        {
            itsFileUri.delete(getContext());
        }

        @Override
        protected void handlePostExecute()
        {
            PasswdSafeActivity.this.finish();
        }
    }

    /**
     * Abstract task for background operations
     */
    private abstract class AbstractTask
    {
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
                        // TODO: fix use of file by other threads while saving.
                        // Causing RuntimeCryptoException when view frag is refreshing
                        result = doAction();
                        handlePostExecute(result);
                    }

                    @Override
                    protected Object doInBackground(Void... params)
                    {
                        return null;
                    }
                    private Object doAction()
                    {
                        try {
                            handleDoInBackground();
                            return new Object();
                        } catch (Exception e) {
                            return e;
                        }
                    }
                };

        /**
         * Constructor
         */
        protected AbstractTask(String msg, Context ctx) 
        {
            itsContext = ctx.getApplicationContext();
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
            handlePostExecute(null);
        }

        /**
         * Handle the result of executing the task
         */
        private void handlePostExecute(Object result)
        {
            itsProgressFrag.dismiss();
            itsCurrTask = null;

            if (result instanceof Exception) {
                Exception e = (Exception)result;
                String msg = getExceptionMsg(e);
                PasswdSafeUtil.showFatalMsg(e, msg, PasswdSafeActivity.this,
                                            true);
            } else if (result != null) {
                handlePostExecute();
            }
        }

        /**
         * Execute the task in a background thread
         */
        protected abstract void handleDoInBackground() throws Exception;

        /**
         * Execute a task in the main thread after the background operation
         * completes successfully
         */
        protected abstract void handlePostExecute();

        /**
         * Get a message for the exception
         */
        protected String getExceptionMsg(Exception e)
        {
            return e.toString();
        }

        /**
         * Get a context
         */
        protected final Context getContext()
        {
            return itsContext;
        }
    }
}
