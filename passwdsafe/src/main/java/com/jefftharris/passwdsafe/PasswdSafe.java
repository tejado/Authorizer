/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdFileDataUser;
import com.jefftharris.passwdsafe.file.PasswdFileUri;
import com.jefftharris.passwdsafe.file.PasswdRecordFilter;
import com.jefftharris.passwdsafe.lib.ApiCompat;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.lib.view.ProgressFragment;
import com.jefftharris.passwdsafe.util.ObjectHolder;
import com.jefftharris.passwdsafe.view.ConfirmPromptDialog;
import com.jefftharris.passwdsafe.view.PasswdFileDataView;
import com.jefftharris.passwdsafe.view.PasswdLocation;
import com.jefftharris.passwdsafe.view.PasswdRecordListData;

import org.pwsafe.lib.file.PwsRecord;

import java.io.IOException;
import java.util.BitSet;
import java.util.List;

/**
 * The main PasswdSafe activity for showing a password file
 */
public class PasswdSafe extends AppCompatActivity
        implements AbstractPasswdSafeRecordFragment.Listener,
                   AboutFragment.Listener,
                   View.OnClickListener,
                   ConfirmPromptDialog.Listener,
                   PasswdSafeChangePasswordFragment.Listener,
                   PasswdSafeEditRecordFragment.Listener,
                   PasswdSafeListFragment.Listener,
                   PasswdSafeOpenFileFragment.Listener,
                   PasswdSafePolicyListFragment.Listener,
                   PasswdSafeNavDrawerFragment.Listener,
                   PasswdSafeNewFileFragment.Listener,
                   PasswdSafeRecordFragment.Listener,
                   PreferencesFragment.Listener
{
    // TODO: expired passwords
    // TODO: expiry notifications
    // TODO: recheck all icons (remove use of all built-in ones)
    // TODO: autobackup
    // TODO: shortcuts
    // TODO: check manifest errors regarding icons
    // TODO: storage access framework support (want to keep support?)
    // TODO: recent files db (should that be carried forward? only if SAF kept)

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
        CHANGE_PASSWORD,
        /** View about info */
        VIEW_ABOUT,
        /** View policy list */
        VIEW_POLICY_LIST,
        /** View preferences */
        VIEW_PREFERENCES
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
        CHANGING_PASSWORD,
        /** Viewing about info */
        VIEW_ABOUT,
        /** Viewing a list of policies */
        VIEW_POLICY_LIST,
        /** Viewing preferences */
        VIEW_PREFERENCES
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

    /** Panel for displaying the query */
    private View itsQueryPanel;

    /** The query label */
    private TextView itsQuery;

    /** The search menu item */
    private MenuItem itsSearchItem = null;

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
    private static final String STATE_QUERY = "query";

    private static final String CONFIRM_ARG_ACTION = "action";
    private static final String CONFIRM_ARG_LOCATION = "location";

    private static int MENU_BIT_CAN_ADD = 0;
    private static int MENU_BIT_HAS_FILE_OPS = 1;
    private static int MENU_BIT_HAS_FILE_CHANGE_PASSWORD = 2;
    private static int MENU_BIT_PROTECT_ALL = 3;
    private static int MENU_BIT_HAS_SEARCH = 4;
    private static int MENU_BIT_HAS_CLOSE = 5;

    private static final String TAG = "PasswdSafe";

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

        itsQueryPanel = findViewById(R.id.query_panel);
        View queryClearBtn = findViewById(R.id.query_clear_btn);
        queryClearBtn.setOnClickListener(this);
        itsQuery = (TextView)findViewById(R.id.query);

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
            itsQuery.setText(savedInstanceState.getCharSequence(STATE_QUERY));
        }
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        PasswdSafeUtil.dbginfo(TAG, "onNewIntent: %s", intent);
        switch (intent.getAction()) {
        case PasswdSafeUtil.VIEW_INTENT:
        case Intent.ACTION_VIEW: {
            final Uri openUri = PasswdSafeApp.getOpenUriFromIntent(intent);
            final ObjectHolder<Boolean> reopen = new ObjectHolder<>(true);
            itsFileDataFrag.useFileData(new PasswdFileDataUser()
            {
                @Override
                public void useFileData(@NonNull PasswdFileData fileData)
                {
                    reopen.set(!fileData.getUri().getUri().equals(openUri));
                }
            });
            Boolean reopenVal = reopen.get();
            if ((reopenVal != null) && reopenVal) {
                // Close and reopen the new file
                itsFileDataFrag.setFileData(null);
                doUpdateView(ViewMode.INIT, new PasswdLocation());
                changeInitialView();
                changeFileOpenView(intent);
            }
            break;
        }
        case Intent.ACTION_SEARCH: {
            setRecordFilter(intent.getStringExtra(SearchManager.QUERY));
            break;
        }
        default: {
            FragmentManager fragMgr = getSupportFragmentManager();
            Fragment frag = fragMgr.findFragmentById(R.id.content);
            if (frag instanceof PasswdSafeOpenFileFragment) {
                ((PasswdSafeOpenFileFragment)frag).onNewIntent(intent);
            }
            break;
        }
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
        outState.putCharSequence(STATE_QUERY, itsQuery.getText());
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

            // Get the SearchView and set the searchable configuration
            SearchManager searchManager =
                    (SearchManager)getSystemService(Context.SEARCH_SERVICE);
            itsSearchItem = menu.findItem(R.id.menu_search);
            MenuItemCompat.collapseActionView(itsSearchItem);
            SearchView searchView = (SearchView)
                    MenuItemCompat.getActionView(itsSearchItem);
            searchView.setSearchableInfo(
                    searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(true);

            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        final BitSet options = new BitSet();
        options.set(MENU_BIT_HAS_CLOSE);

        itsFileDataFrag.useFileData(new PasswdFileDataUser()
        {
            @Override
            public void useFileData(@NonNull PasswdFileData fileData)
            {
                boolean fileEditable = fileData.canEdit();

                switch (itsCurrViewMode) {
                case VIEW_LIST: {
                    options.set(MENU_BIT_CAN_ADD, fileEditable);
                    options.set(MENU_BIT_HAS_SEARCH, true);
                    if (fileEditable) {
                        options.set(MENU_BIT_HAS_FILE_OPS, true);
                        options.set(MENU_BIT_HAS_FILE_CHANGE_PASSWORD,
                                    !fileData.isYubikey());
                        options.set(MENU_BIT_PROTECT_ALL,
                                    itsLocation.getGroups().isEmpty());
                    }
                    break;
                }
                case VIEW_RECORD: {
                    options.set(MENU_BIT_CAN_ADD, fileEditable);
                    break;
                }
                case INIT:
                case FILE_OPEN:
                case FILE_NEW:
                case VIEW_ABOUT:
                case VIEW_POLICY_LIST:
                case VIEW_PREFERENCES: {
                    break;
                }
                case EDIT_RECORD:
                case CHANGING_PASSWORD: {
                    options.set(MENU_BIT_HAS_CLOSE, false);
                    break;
                }
                }
            }
        });

        MenuItem item = menu.findItem(R.id.menu_add);
        if (item != null) {
            item.setVisible(options.get(MENU_BIT_CAN_ADD));
        }

        item = menu.findItem(R.id.menu_close);
        if (item != null) {
            item.setVisible(options.get(MENU_BIT_HAS_CLOSE));
        }

        item = menu.findItem(R.id.menu_file_ops);
        if (item != null) {
            item.setVisible(options.get(MENU_BIT_HAS_FILE_OPS));
        }

        item = menu.findItem(R.id.menu_file_change_password);
        if (item != null) {
            item.setEnabled(options.get(MENU_BIT_HAS_FILE_CHANGE_PASSWORD));
        }

        if (options.get(MENU_BIT_HAS_FILE_OPS)) {
            boolean viewProtectAll = options.get(MENU_BIT_PROTECT_ALL);
            item = menu.findItem(R.id.menu_file_protect_records);
            if (item != null) {
                item.setTitle(viewProtectAll ? R.string.protect_all :
                                      R.string.protect_group);
            }
            item = menu.findItem(R.id.menu_file_unprotect_records);
            if (item != null) {
                item.setTitle(viewProtectAll ? R.string.unprotect_all :
                                      R.string.unprotect_group);
            }
        }

        item = menu.findItem(R.id.menu_search);
        if (item != null) {
            item.setVisible(options.get(MENU_BIT_HAS_SEARCH));
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
            final ObjectHolder<String> uriName = new ObjectHolder<>();
            itsFileDataFrag.useFileData(new PasswdFileDataUser()
            {
                @Override
                public void useFileData(@NonNull PasswdFileData fileData)
                {
                    uriName.set(fileData.getUri().getIdentifier(PasswdSafe.this,
                                                                true));
                }
            });
            if (uriName.get() == null) {
                return true;
            }
            Bundle confirmArgs = new Bundle();
            confirmArgs.putString(CONFIRM_ARG_ACTION,
                                  ConfirmAction.DELETE_FILE.name());
            ConfirmPromptDialog dialog = ConfirmPromptDialog.newInstance(
                    getString(R.string.delete_file_msg, uriName.get()),
                    getString(R.string.delete),
                    confirmArgs);
            dialog.show(getSupportFragmentManager(), "Delete file");
            return true;
        }
        case R.id.menu_file_protect_records: {
            protectRecords(true);
            return true;
        }
        case R.id.menu_file_unprotect_records: {
            protectRecords(false);
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
                PasswdSafe.super.onBackPressed();
            }
        });
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
        case R.id.query_clear_btn: {
            setRecordFilter(null);
            break;
        }
        }
    }

    /**
     * Show the file records
     */
    @Override
    public void showFileRecords()
    {
        changeOpenView(itsLocation, false);
    }

    /**
     * Show the file password policies
     */
    @Override
    public void showFilePasswordPolicies()
    {
        doChangeView(ChangeMode.VIEW_POLICY_LIST,
                     PasswdSafePolicyListFragment.newInstance());
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
        doChangeView(ChangeMode.VIEW_PREFERENCES,
                     PreferencesFragment.newInstance());
    }

    /**
     * Show the about dialog
     */
    @Override
    public void showAbout()
    {
        doChangeView(ChangeMode.VIEW_ABOUT, AboutFragment.newInstance());
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
        itsFileDataFrag.setFileData(fileData);
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
        itsFileDataFrag.setFileData(fileData);
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
        return dataView.getRecords(incRecords, incGroups);
    }

    /**
     * Change the location in the password file
     */
    @Override
    public void changeLocation(PasswdLocation location)
    {
        if (isFileOpen()) {
            PasswdSafeUtil.dbginfo(TAG, "changeLocation loc: %s", location);
            if (!itsLocation.equals(location)) {
                changeOpenView(location, false);
            }
        }
    }

    /**
     * Use the file data
     */
    @Override
    public void useFileData(PasswdFileDataUser user)
    {
        itsFileDataFrag.useFileData(user);
    }

    @Override
    public void editRecord(PasswdLocation location)
    {
        if (isFileOpen()) {
            PasswdSafeUtil.dbginfo(TAG, "editRecord loc: %s", location);
            doChangeView(ChangeMode.EDIT_RECORD,
                         PasswdSafeEditRecordFragment.newInstance(location));
        }
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
        saveFile(true, save, newLocation, null);
    }

    @Override
    public void updateViewAbout()
    {
        doUpdateView(ViewMode.VIEW_ABOUT, itsLocation);
    }

    @Override
    public void finishChangePassword()
    {
        saveFile(true, true, null, null);
    }

    @Override
    public void updateViewChangingPassword()
    {
        doUpdateView(ViewMode.CHANGING_PASSWORD, itsLocation);
    }

    @Override
    public void updateViewPolicyList()
    {
        doUpdateView(ViewMode.VIEW_POLICY_LIST, itsLocation);
    }

    @Override
    public void finishPolicyEdit(Runnable postSaveRun)
    {
        saveFile(false, true, null, postSaveRun);
    }

    @Override
    public void updateViewPreferences()
    {
        doUpdateView(ViewMode.VIEW_PREFERENCES, itsLocation);
    }

    @Override
    public void promptConfirmed(Bundle confirmArgs)
    {
        PasswdSafeUtil.dbginfo(TAG, "promptConfirmed: %s", confirmArgs);
        ConfirmAction action;
        try {
            action = ConfirmAction.valueOf(
                    confirmArgs.getString(CONFIRM_ARG_ACTION));
        } catch (Exception e) {
            return;
        }

        switch (action) {
        case DELETE_FILE: {
            final ObjectHolder<PasswdFileUri> uri = new ObjectHolder<>();
            itsFileDataFrag.useFileData(new PasswdFileDataUser()
            {
                @Override
                public void useFileData(@NonNull PasswdFileData fileData)
                {
                    uri.set(fileData.getUri());
                }
            });
            if (uri.get() != null) {
                itsCurrTask = new DeleteTask(uri.get(), this);
                itsCurrTask.execute();
            }
            break;
        }
        case DELETE_RECORD: {
            final PasswdLocation location =
                    confirmArgs.getParcelable(CONFIRM_ARG_LOCATION);
            if (location == null) {
                break;
            }

            final ObjectHolder<Boolean> removed = new ObjectHolder<>(false);
            itsFileDataFrag.useFileData(new PasswdFileDataUser()
            {
                @Override
                public void useFileData(@NonNull PasswdFileData fileData)
                {
                    PwsRecord rec = fileData.getRecord(location.getRecord());
                    if (rec != null) {
                        removed.set(fileData.removeRecord(rec,
                                                          PasswdSafe.this));
                    }
                }
            });
            if (removed.get()) {
                saveFile(true, true, location.selectRecord(null), null);
            }
            break;
        }
        }
    }

    /**
     * Set the record filter from a query string
     */
    private void setRecordFilter(String query)
    {
        PasswdFileDataView fileView = itsFileDataFrag.getFileDataView();
        try {
            fileView.setRecordFilter(query);
        } catch (Exception e) {
            String msg = e.getMessage();
            Log.e(TAG, msg, e);
            PasswdSafeUtil.showErrorMsg(msg, this);
            return;
        }
        PasswdRecordFilter filter = fileView.getRecordFilter();
        if (filter != null) {
            itsQuery.setText(getString(R.string.query_label,
                                       filter.toString(this)));
        }
        GuiUtils.setVisible(itsQueryPanel, (filter != null));

        if ((itsSearchItem != null) && (filter != null) &&
            MenuItemCompat.isActionViewExpanded(itsSearchItem)) {
            MenuItemCompat.collapseActionView(itsSearchItem);
        }

        changeOpenView(new PasswdLocation(), true);
    }

    /**
     * Is there an open file
     */
    private boolean isFileOpen()
    {
        final ObjectHolder<Boolean> isOpen = new ObjectHolder<>(false);
        itsFileDataFrag.useFileData(new PasswdFileDataUser()
        {
            @Override
            public void useFileData(@NonNull PasswdFileData fileData)
            {
                isOpen.set(true);
            }
        });

        return isOpen.get();
    }

    /**
     * Protect or unprotect all records under the current group
     */
    private void protectRecords(final boolean doProtect)
    {
        final ObjectHolder<Boolean> doSave = new ObjectHolder<>(false);
        itsFileDataFrag.useFileData(new PasswdFileDataUser()
        {
            @Override
            public void useFileData(@NonNull final PasswdFileData fileData)
            {
                doSave.set(true);
                itsFileDataFrag.getFileDataView().walkGroupRecords(
                        new PasswdFileDataView.RecordVisitor()
                        {
                            @Override
                            public void visitRecord(PwsRecord record)
                            {
                                fileData.setProtected(doProtect, record);
                            }
                        });
            }
        });

        if (doSave.get()) {
            saveFile(false, true, null, null);
        }
    }

    /**
     * Save the file
     */
    private void saveFile(final boolean popBack, final boolean save,
                          final PasswdLocation newLocation,
                          final Runnable postSaveRun)
    {
        Runnable saveRun = new Runnable()
        {
            @Override
            public void run()
            {
                boolean resetLoc = false;
                if (save) {
                    itsFileDataFrag.refreshFileData();
                    if ((newLocation != null) &&
                        !newLocation.equalGroups(itsLocation)) {
                        resetLoc = true;
                    }
                }

                if (popBack) {
                    FragmentManager fragMgr = getSupportFragmentManager();
                    fragMgr.popBackStackImmediate();
                }

                if (resetLoc) {
                    changeOpenView(new PasswdLocation(), true);
                }

                if (postSaveRun != null) {
                    postSaveRun.run();
                }
            }
        };

        if (save) {
            final ObjectHolder<String> fileId = new ObjectHolder<>("");
            itsFileDataFrag.useFileData(new PasswdFileDataUser()
            {
                @Override
                public void useFileData(@NonNull PasswdFileData fileData)
                {
                    fileId.set(fileData.getUri().getIdentifier(PasswdSafe.this,
                                                               false));
                }
            });
            itsCurrTask = new SaveTask(fileId.get(), saveRun, this);
            itsCurrTask.execute();
        } else {
            saveRun.run();
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
                case CHANGE_PASSWORD:
                case VIEW_ABOUT:
                case VIEW_POLICY_LIST:
                case VIEW_PREFERENCES: {
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
        case CHANGING_PASSWORD:
        case VIEW_ABOUT:
        case VIEW_POLICY_LIST:
        case VIEW_PREFERENCES: {
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
        itsFileDataFrag.setLocation(itsLocation);
        itsCurrViewMode = mode;

        FragmentManager fragMgr = getSupportFragmentManager();
        boolean showLeftList = false;
        boolean queryVisibleForMode = false;
        PasswdSafeNavDrawerFragment.Mode drawerMode =
                PasswdSafeNavDrawerFragment.Mode.INIT;
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
            queryVisibleForMode = true;
            drawerMode = PasswdSafeNavDrawerFragment.Mode.RECORDS_LIST;
            fileTimeoutPaused = false;
            itsTitle = null;
            String groups = itsLocation.getGroupPath();
            if (TextUtils.isEmpty(groups)) {
                itsFileDataFrag.useFileData(new PasswdFileDataUser()
                {
                    @Override
                    public void useFileData(@NonNull PasswdFileData fileData)
                    {
                        itsTitle = PasswdSafeApp.getAppFileTitle(
                                fileData.getUri(), PasswdSafe.this);
                    }
                });
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
                    PasswdSafeNavDrawerFragment.Mode.RECORDS_LIST :
                    PasswdSafeNavDrawerFragment.Mode.RECORDS_SINGLE;
            fileTimeoutPaused = false;
            itsTitle = null;
            itsFileDataFrag.useFileData(new PasswdFileDataUser()
            {
                @Override
                public void useFileData(@NonNull PasswdFileData fileData)
                {
                    if (itsLocation.isRecord()) {
                        PwsRecord rec =
                                fileData.getRecord(itsLocation.getRecord());
                        itsTitle = fileData.getTitle(rec);
                    }
                }
            });
            if (itsTitle == null) {
                itsTitle = getString(R.string.new_entry);
            }
            break;
        }
        case EDIT_RECORD: {
            drawerMode = PasswdSafeNavDrawerFragment.Mode.RECORDS_ACTION;
            itsTitle = null;
            itsFileDataFrag.useFileData(new PasswdFileDataUser()
            {
                @Override
                public void useFileData(@NonNull PasswdFileData fileData)
                {
                    if (itsLocation.isRecord()) {
                        PwsRecord rec =
                                fileData.getRecord(itsLocation.getRecord());
                        itsTitle = getString(R.string.edit_item,
                                             fileData.getTitle(rec));
                    }
                }
            });
            if (itsTitle == null) {
                itsTitle = getString(R.string.new_entry);
            }
            break;
        }
        case CHANGING_PASSWORD: {
            itsTitle = getString(R.string.change_password);
            drawerMode = PasswdSafeNavDrawerFragment.Mode.RECORDS_ACTION;
            break;
        }
        case VIEW_ABOUT: {
            drawerMode = PasswdSafeNavDrawerFragment.Mode.ABOUT;
            fileTimeoutPaused = false;
            itsTitle = PasswdSafeApp.getAppTitle(getString(R.string.about),
                                                 this);
            break;
        }
        case VIEW_POLICY_LIST: {
            drawerMode = PasswdSafeNavDrawerFragment.Mode.POLICIES;
            fileTimeoutPaused = false;
            itsTitle = PasswdSafeApp.getAppTitle(
                    getString(R.string.password_policies), this);
            break;
        }
        case VIEW_PREFERENCES: {
            drawerMode = PasswdSafeNavDrawerFragment.Mode.PREFERENCES;
            fileTimeoutPaused = false;
            itsTitle = PasswdSafeApp.getAppTitle(
                    getString(R.string.preferences), this);
            break;
        }
        }

        GuiUtils.invalidateOptionsMenu(this);
        itsNavDrawerFrag.setMode(drawerMode, isFileOpen());
        restoreActionBar();
        itsTimeoutReceiver.updateTimeout(fileTimeoutPaused);

        PasswdFileDataView fileDataView = itsFileDataFrag.getFileDataView();
        GuiUtils.setVisible(itsQueryPanel,
                            queryVisibleForMode &&
                            (fileDataView.getRecordFilter() != null));

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
        private final Runnable itsPostSaveRun;

        /**
         * Constructor
         */
        public SaveTask(String fileId, Runnable postSaveRun, Context ctx)
        {
            super(ctx.getString(R.string.saving_file, fileId), ctx);
            itsPostSaveRun = postSaveRun;
        }

        @Override
        protected final void handleDoInBackground() throws Exception
        {
            final ObjectHolder<Exception> ex = new ObjectHolder<>();
            itsFileDataFrag.useFileData(new PasswdFileDataUser()
            {
                @Override
                public void useFileData(@NonNull PasswdFileData fileData)
                {
                    try {
                        fileData.save(getContext());
                        PasswdSafeUtil.dbginfo(TAG, "SaveTask finished");
                    } catch (Exception e) {
                        ex.set(e);
                    }
                }
            });
            Exception e = ex.get();
            if (e != null) {
                throw e;
            }
        }

        @Override
        protected final void handlePostExecute()
        {
            if (itsPostSaveRun != null) {
                itsPostSaveRun.run();
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
            PasswdSafe.this.finish();
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
                        handlePostExecute(result);
                    }

                    @Override
                    protected Object doInBackground(Void... params)
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
                PasswdSafeUtil.showFatalMsg(e, msg, PasswdSafe.this, true);
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
