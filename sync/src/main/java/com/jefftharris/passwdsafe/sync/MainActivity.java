/*
 * Copyright (©) 2013-2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.jefftharris.passwdsafe.lib.AboutDialog;
import com.jefftharris.passwdsafe.lib.ApiCompat;
import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.lib.ReleaseNotesDialog;
import com.jefftharris.passwdsafe.sync.dropbox.DropboxFilesActivity;
import com.jefftharris.passwdsafe.sync.gdriveplay.GDrivePlayMainActivity;
import com.jefftharris.passwdsafe.sync.lib.AccountUpdateTask;
import com.jefftharris.passwdsafe.sync.lib.NewAccountTask;
import com.jefftharris.passwdsafe.sync.lib.Provider;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.owncloud.OwncloudFilesActivity;
import com.jefftharris.passwdsafe.sync.owncloud.OwncloudProvider;

public class MainActivity extends FragmentActivity
        implements LoaderCallbacks<Cursor>, SyncUpdateHandler
{
    private static final String TAG = "MainActivity";

    private static final int CHOOSE_ACCOUNT_RC = 0;
    private static final int DROPBOX_LINK_RC = 1;
    private static final int BOX_AUTH_RC = 2;
    private static final int GDRIVE_PLAY_LINK_RC = 3;
    private static final int OWNCLOUD_LINK_RC = 4;

    private static final int LOADER_PROVIDERS = 0;

    private static final boolean GDRIVE_PLAY_ENABLED = false;

    private Account itsGdriveAccount = null;
    private Uri itsGdrivePlayUri = null;
    private String itsGdrivePlayAcct = null;
    private String itsGdrivePlayDisplay = null;
    private Uri itsGdriveUri = null;
    private Uri itsDropboxUri = null;
    private boolean itsDropboxPendingAcctLink = false;
    private Uri itsBoxUri = null;
    private Uri itsOwncloudUri = null;

    private NewAccountTask itsNewAccountTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate");

        OnItemSelectedListener freqSelListener = new OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id)
            {
                switch (parent.getId()) {
                case R.id.gdrive_interval: {
                    onGdriveFreqChanged(position);
                    break;
                }
                case R.id.dropbox_interval: {
                    onDropboxFreqChanged(position);
                    break;
                }
                case R.id.box_interval: {
                    onBoxFreqChanged(position);
                    break;
                }
                case R.id.owncloud_interval: {
                    onOwncloudFreqChanged(position);
                    break;
                }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        };

        Spinner freqSpin = (Spinner)findViewById(R.id.gdrive_interval);
        freqSpin.setOnItemSelectedListener(freqSelListener);

        freqSpin = (Spinner)findViewById(R.id.dropbox_interval);
        freqSpin.setOnItemSelectedListener(freqSelListener);

        freqSpin = (Spinner)findViewById(R.id.box_interval);
        freqSpin.setOnItemSelectedListener(freqSelListener);

        freqSpin = (Spinner)findViewById(R.id.owncloud_interval);
        freqSpin.setOnItemSelectedListener(freqSelListener);

        CheckBox httpsCb = (CheckBox)findViewById(R.id.owncloud_use_https);
        httpsCb.setOnCheckedChangeListener(new OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked)
            {
                onOwncloudUseHttpsChanged(isChecked);
            }
        });

        // Check the state of Google Play services
        int rc = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (rc != ConnectionResult.SUCCESS) {
            Dialog dlg = GooglePlayServicesUtil.getErrorDialog(rc, this, 0);
            dlg.show();
        }

        //noinspection PointlessBooleanExpression
        if (!GDRIVE_PLAY_ENABLED) {
            // TODO play: Remove flag
            findViewById(R.id.gdrive_play_container).setVisibility(View.GONE);
            findViewById(R.id.gdrive_play_separator).setVisibility(View.GONE);
        }

        updateGdrivePlayAccount(null);
        updateGdriveAccount(null);
        updateDropboxAccount(null);
        updateBoxAccount(null);
        updateOwncloudAccount(null);
        LoaderManager lm = getSupportLoaderManager();
        lm.initLoader(LOADER_PROVIDERS, null, this);
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
     * @see android.support.v4.app.FragmentActivity#onPause()
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        SyncApp.get(this).setSyncUpdateHandler(null);
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onResumeFragments()
     */
    @Override
    protected void onResumeFragments()
    {
        super.onResumeFragments();
        if (itsDropboxPendingAcctLink) {
            itsDropboxPendingAcctLink = false;
            itsNewAccountTask = getDbxProvider().finishAccountLink(
                    Activity.RESULT_OK, null, itsDropboxUri);
        }
        if (itsNewAccountTask != null) {
            itsNewAccountTask.startTask(this);
            itsNewAccountTask = null;
        }
        LoaderManager lm = getSupportLoaderManager();
        lm.restartLoader(LOADER_PROVIDERS, null, this);
        SyncApp.get(this).setSyncUpdateHandler(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode) {
        case CHOOSE_ACCOUNT_RC:
            itsNewAccountTask =
                    getGdriveProvider().finishAccountLink(resultCode, data,
                                                          itsGdriveUri);
            break;
        case BOX_AUTH_RC: {
            itsNewAccountTask =
                    getBoxProvider().finishAccountLink(resultCode, data,
                                                       itsBoxUri);
            break;
        }
        case GDRIVE_PLAY_LINK_RC: {
            itsNewAccountTask = getGdrivePlayProvider().finishAccountLink(
                    resultCode, data, itsGdrivePlayUri);
            break;
        }
        case OWNCLOUD_LINK_RC: {
            itsNewAccountTask = getOwncloudProvider().finishAccountLink(
                    resultCode, data, itsOwncloudUri);
            break;
        }
        default: {
            super.onActivityResult(requestCode, resultCode, data);
            break;
        }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_main, menu);

        MenuItem item = menu.findItem(R.id.menu_about);
        MenuItemCompat.setShowAsAction(item,
                                       MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);

        item = menu.findItem(R.id.menu_logs);
        MenuItemCompat.setShowAsAction(item,
                                       MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_about: {
            String extraLicenses =
                GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(this);
            AboutDialog dlg = AboutDialog.newInstance(extraLicenses);
            dlg.show(getSupportFragmentManager(), "AboutDialog");
            return true;
        }
        case R.id.menu_logs: {
            Intent intent = new Intent();
            intent.setClass(this, SyncLogsActivity.class);
            startActivity(intent);
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }


    /** Button onClick handler to launch PasswdSafe */
    @SuppressWarnings("UnusedParameters")
    public void onLaunchPasswdSafeClick(View view)
    {
        PasswdSafeUtil.startMainActivity("com.jefftharris.passwdsafe", this);
    }


    /** Button onClick handler to choose a GDrive account */
    @SuppressWarnings("UnusedParameters")
    public void onGdriveChoose(View view)
    {
        Intent intent = AccountPicker.newChooseAccountIntent(
                itsGdriveAccount, null,
                new String[] { SyncDb.GDRIVE_ACCOUNT_TYPE },
                true, null, null, null, null);
        try {
            startActivityForResult(intent, CHOOSE_ACCOUNT_RC);
        } catch (ActivityNotFoundException e) {
            String msg = getString(R.string.google_acct_not_available);
            Log.e(TAG, msg, e);
            PasswdSafeUtil.showErrorMsg(msg, this);
        }
    }


    /** Button onClick handler to sync a GDrive account */
    @SuppressWarnings("UnusedParameters")
    public void onGdriveSync(View view)
    {
        if (itsGdriveAccount != null) {
            Bundle extras = new Bundle();
            extras.putBoolean(SyncAdapter.SYNC_EXTRAS_FULL, true);
            ApiCompat.requestManualSync(itsGdriveAccount,
                                        PasswdSafeContract.CONTENT_URI,
                                        this, extras);
        }
    }


    /** Button onClick handler to clear a GDrive account */
    @SuppressWarnings("UnusedParameters")
    public void onGdriveClear(View view)
    {
        DialogFragment prompt = ClearPromptDlg.newInstance(itsGdriveUri);
        prompt.show(getSupportFragmentManager(), null);
    }


    /** GDrive sync frequency spinner changed */
    private void onGdriveFreqChanged(int pos)
    {
        ProviderSyncFreqPref freq = ProviderSyncFreqPref.displayValueOf(pos);
        updateSyncFreq(freq, itsGdriveUri);
    }

    @SuppressWarnings("UnusedParameters")
    public void onGdrivePlayClear(View view)
    {
        DialogFragment prompt = ClearPromptDlg.newInstance(itsGdrivePlayUri);
        prompt.show(getSupportFragmentManager(), null);
    }

    @SuppressWarnings("UnusedParameters")
    public void onGdrivePlayChoose(View view)
    {
        if (itsGdrivePlayUri == null) {
            Provider gdriveProvider = getGdrivePlayProvider();
            try {
                gdriveProvider.startAccountLink(this, GDRIVE_PLAY_LINK_RC);
            } catch (Exception e) {
                Log.e(TAG, "onGdrivePlayChoose failed", e);
                gdriveProvider.unlinkAccount();
            }
        } else {
            Intent intent = new Intent();
            intent.putExtra(GDrivePlayMainActivity.INTENT_PROVIDER_URI,
                            itsGdrivePlayUri);
            intent.putExtra(GDrivePlayMainActivity.INTENT_PROVIDER_ACCT,
                            itsGdrivePlayAcct);
            intent.putExtra(GDrivePlayMainActivity.INTENT_PROVIDER_DISPLAY,
                            itsGdrivePlayDisplay);
            intent.setClass(this, GDrivePlayMainActivity.class);
            startActivity(intent);
        }
    }

    /** Button onClick handler to choose a Dropbox account */
    @SuppressWarnings("UnusedParameters")
    public void onDropboxChoose(View view)
    {
        Provider dbxProvider = getDbxProvider();
        try {
            dbxProvider.startAccountLink(this, DROPBOX_LINK_RC);
            itsDropboxPendingAcctLink = true;
        } catch (Exception e) {
            Log.e(TAG, "startDropboxLink failed", e);
            dbxProvider.unlinkAccount();
        }
    }


    /** Button onClick handler to sync a Dropbox account */
    @SuppressWarnings("UnusedParameters")
    public void onDropboxSync(View view)
    {
        getDbxProvider().requestSync(true);
    }


    /** Button onClick handler to clear a Dropbox account */
    @SuppressWarnings("UnusedParameters")
    public void onDropboxClear(View view)
    {
        DialogFragment prompt = ClearPromptDlg.newInstance(itsDropboxUri);
        prompt.show(getSupportFragmentManager(), null);
    }


    /** Dropbox sync frequency spinner changed */
    private void onDropboxFreqChanged(int pos)
    {
        ProviderSyncFreqPref freq = ProviderSyncFreqPref.displayValueOf(pos);
        updateSyncFreq(freq, itsDropboxUri);
    }


    /** Button onClick handler to select Dropbox files */
    @SuppressWarnings("UnusedParameters")
    public void onDropboxChooseFiles(View view)
    {
        Intent intent = new Intent();
        intent.putExtra(DropboxFilesActivity.INTENT_PROVIDER_URI,
                        itsDropboxUri);
        intent.setClass(this, DropboxFilesActivity.class);
        startActivity(intent);
    }

    /** Button onClick handler to choose a Box account */
    @SuppressWarnings("UnusedParameters")
    public void onBoxChoose(View view)
    {
        Provider boxProvider = getBoxProvider();
        try {
            boxProvider.startAccountLink(this, BOX_AUTH_RC);
        } catch (Exception e) {
            Log.e(TAG, "Box startAccountLink failed", e);
            boxProvider.unlinkAccount();
        }
    }


    /** Button onClick handler to sync a Box account */
    @SuppressWarnings("UnusedParameters")
    public void onBoxSync(View view)
    {
        getBoxProvider().requestSync(true);
    }


    /** Button onClick handler to clear a Box account */
    @SuppressWarnings("UnusedParameters")
    public void onBoxClear(View view)
    {
        DialogFragment prompt = ClearPromptDlg.newInstance(itsBoxUri);
        prompt.show(getSupportFragmentManager(), null);
    }


    /** Box sync frequency spinner changed */
    private void onBoxFreqChanged(int pos)
    {
        ProviderSyncFreqPref freq = ProviderSyncFreqPref.displayValueOf(pos);
        updateSyncFreq(freq, itsBoxUri);
    }


    /** Button onClick handler to choose an ownCloud account */
    @SuppressWarnings("UnusedParameters")
    public void onOwncloudChoose(View view)
    {
        Provider owncloudProvider = getOwncloudProvider();
        try {
            owncloudProvider.startAccountLink(this, OWNCLOUD_LINK_RC);
        } catch (Exception e) {
            Log.e(TAG, "ownCloud startAccountLink failed", e);
            owncloudProvider.unlinkAccount();
        }
    }


    /** Button onClick handler to sync an ownCloud account */
    @SuppressWarnings("UnusedParameters")
    public void onOwncloudSync(View view)
    {
        getOwncloudProvider().requestSync(true);
    }


    /** Button onClick handler to clear an ownCloud account */
    @SuppressWarnings("UnusedParameters")
    public void onOwncloudClear(View view)
    {
        DialogFragment prompt = ClearPromptDlg.newInstance(itsOwncloudUri);
        prompt.show(getSupportFragmentManager(), null);
    }


    /** ownCloud sync frequency spinner changed */
    private void onOwncloudFreqChanged(int pos)
    {
        ProviderSyncFreqPref freq = ProviderSyncFreqPref.displayValueOf(pos);
        updateSyncFreq(freq, itsOwncloudUri);
    }


    /** ownCloud use HTTPS changed */
    private void onOwncloudUseHttpsChanged(boolean useHttps)
    {
        getOwncloudProvider().setUseHttps(useHttps);
    }


    /** Button onClick handler to select ownCloud files */
    @SuppressWarnings("UnusedParameters")
    public void onOwncloudChooseFiles(View view)
    {
        Intent intent = new Intent();
        intent.putExtra(OwncloudFilesActivity.INTENT_PROVIDER_URI,
                        itsOwncloudUri);
        intent.setClass(this, OwncloudFilesActivity.class);
        startActivity(intent);
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onCreateLoader(int, android.os.Bundle)
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args)
    {
        return new CursorLoader(this, PasswdSafeContract.Providers.CONTENT_URI,
                                PasswdSafeContract.Providers.PROJECTION,
                                null, null, null);
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoadFinished(android.support.v4.content.Loader, java.lang.Object)
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
    {
        boolean hasGdrivePlay = false;
        boolean hasGdrive = false;
        boolean hasDropbox = false;
        boolean hasBox = false;
        boolean hasOwncloud = false;
        for (boolean more = cursor.moveToFirst(); more;
                more = cursor.moveToNext()) {
            String typeStr = cursor.getString(
                    PasswdSafeContract.Providers.PROJECTION_IDX_TYPE);
            try {
                ProviderType type = ProviderType.valueOf(typeStr);
                switch (type) {
                case GDRIVE_PLAY: {
                    hasGdrivePlay = true;
                    updateGdrivePlayAccount(cursor);
                    break;
                }
                case GDRIVE: {
                    hasGdrive = true;
                    updateGdriveAccount(cursor);
                    break;
                }
                case DROPBOX: {
                    hasDropbox = true;
                    updateDropboxAccount(cursor);
                    break;
                }
                case BOX: {
                    hasBox = true;
                    updateBoxAccount(cursor);
                    break;
                }
                case OWNCLOUD: {
                    hasOwncloud = true;
                    updateOwncloudAccount(cursor);
                    break;
                }
                }
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Unknown type: " + typeStr);
            }
        }
        if (!hasGdrivePlay) {
            updateGdrivePlayAccount(null);
        }
        if (!hasGdrive) {
            updateGdriveAccount(null);
        }
        if (!hasDropbox) {
            updateDropboxAccount(null);
        }
        if (!hasBox) {
            updateBoxAccount(null);
        }
        if (!hasOwncloud) {
            updateOwncloudAccount(null);
        }
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoaderReset(android.support.v4.content.Loader)
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader)
    {
        updateGdrivePlayAccount(null);
        updateGdriveAccount(null);
        updateDropboxAccount(null);
        updateBoxAccount(null);
        updateOwncloudAccount(null);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.SyncApp.SyncUpdateHandler#updateGDriveState(com.jefftharris.passwdsafe.sync.SyncApp.SyncUpdateHandler.GDriveState)
     */
    @Override
    public void updateGDriveState(GDriveState state)
    {
        TextView warning = (TextView)findViewById(R.id.gdrive_sync_warning);
        switch (state) {
        case OK: {
            warning.setVisibility(View.GONE);
            break;
        }
        case AUTH_REQUIRED: {
            warning.setVisibility(View.VISIBLE);
            warning.setText(R.string.gdrive_state_auth_required);
            break;
        }
        case PENDING_AUTH: {
            warning.setVisibility(View.VISIBLE);
            warning.setText(R.string.gdrive_state_pending_auth);
            break;
        }
        }
    }

    /** Update the UI when the GDrive play account is changed */
    private void updateGdrivePlayAccount(Cursor cursor)
    {
        Button chooseBtn = (Button)findViewById(R.id.gdrive_play_choose);
        TextView acctView = (TextView)findViewById(R.id.gdrive_play_acct);
        // TODO play: controls?
        View clearBtn = findViewById(R.id.gdrive_play_clear);
        if (cursor != null) {
            long id = cursor.getLong(
                    PasswdSafeContract.Providers.PROJECTION_IDX_ID);
            itsGdrivePlayAcct = cursor.getString(
                    PasswdSafeContract.Providers.PROJECTION_IDX_ACCT);
            itsGdrivePlayDisplay =
                    PasswdSafeContract.Providers.getDisplayName(cursor);
            itsGdrivePlayUri = ContentUris.withAppendedId(
                    PasswdSafeContract.Providers.CONTENT_URI, id);

            acctView.setText(itsGdrivePlayDisplay);
            acctView.setVisibility(View.VISIBLE);

            // TODO play: string
            chooseBtn.setText("Choose files");
            clearBtn.setVisibility(View.VISIBLE);
        } else {
            itsGdrivePlayUri = null;
            itsGdrivePlayAcct = null;
            itsGdrivePlayDisplay = null;
            chooseBtn.setText(R.string.choose_account);
            acctView.setVisibility(View.GONE);
            clearBtn.setVisibility(View.GONE);
        }
    }

    /** Update the UI when the GDrive account is changed */
    private void updateGdriveAccount(Cursor cursor)
    {
        View chooseBtn = findViewById(R.id.gdrive_choose);
        TextView acctView = (TextView)findViewById(R.id.gdrive_acct);
        View controls = findViewById(R.id.gdrive_controls);
        View clearBtn = findViewById(R.id.gdrive_clear);
        if (cursor != null) {
            long id = cursor.getLong(
                    PasswdSafeContract.Providers.PROJECTION_IDX_ID);
            String acct = PasswdSafeContract.Providers.getDisplayName(cursor);
            int freqVal = cursor.getInt(
                    PasswdSafeContract.Providers.PROJECTION_IDX_SYNC_FREQ);
            ProviderSyncFreqPref freq =
                    ProviderSyncFreqPref.freqValueOf(freqVal);
            GoogleAccountManager acctMgr = new GoogleAccountManager(this);
            itsGdriveAccount = acctMgr.getAccountByName(acct);
            itsGdriveUri = ContentUris.withAppendedId(
                    PasswdSafeContract.Providers.CONTENT_URI, id);

            View freqSpinLabel = findViewById(R.id.gdrive_interval_label);
            Spinner freqSpin = (Spinner)findViewById(R.id.gdrive_interval);
            freqSpin.setSelection(freq.getDisplayIdx());
            View syncBtn = findViewById(R.id.gdrive_sync);
            chooseBtn.setVisibility(View.GONE);
            controls.setVisibility(View.VISIBLE);
            clearBtn.setVisibility(View.VISIBLE);

            boolean haveAccount = (itsGdriveAccount != null);
            if (haveAccount) {
                acctView.setText(itsGdriveAccount.name);
            } else {
                acctView.setText(getString(R.string.account_not_exists_label,
                                           acct));
            }
            acctView.setVisibility(View.VISIBLE);

            freqSpin.setEnabled(haveAccount);
            freqSpinLabel.setEnabled(haveAccount);
            syncBtn.setEnabled(haveAccount);
        } else {
            itsGdriveAccount = null;
            itsGdriveUri = null;
            chooseBtn.setVisibility(View.VISIBLE);
            acctView.setVisibility(View.GONE);
            controls.setVisibility(View.GONE);
            clearBtn.setVisibility(View.GONE);
        }
    }

    /** Update the UI when the Dropbox account is changed */
    private void updateDropboxAccount(Cursor cursor)
    {
        View chooseBtn = findViewById(R.id.dropbox_choose);
        TextView acctView = (TextView)findViewById(R.id.dropbox_acct);
        View controls = findViewById(R.id.dropbox_controls);
        View clearBtn = findViewById(R.id.dropbox_clear);
        View acctWarning = findViewById(R.id.dropbox_acct_unlink);
        if (cursor != null) {
            long id = cursor.getLong(
                    PasswdSafeContract.Providers.PROJECTION_IDX_ID);
            String acct = PasswdSafeContract.Providers.getDisplayName(cursor);
            int freqVal = cursor.getInt(
                    PasswdSafeContract.Providers.PROJECTION_IDX_SYNC_FREQ);
            ProviderSyncFreqPref freq =
                    ProviderSyncFreqPref.freqValueOf(freqVal);
            itsDropboxUri = ContentUris.withAppendedId(
                    PasswdSafeContract.Providers.CONTENT_URI, id);
            boolean authorized = getDbxProvider().isAccountAuthorized();

            View freqSpinLabel = findViewById(R.id.dropbox_interval_label);
            Spinner freqSpin = (Spinner)findViewById(R.id.dropbox_interval);
            freqSpin.setSelection(freq.getDisplayIdx());
            View syncBtn = findViewById(R.id.dropbox_sync);
            chooseBtn.setVisibility(View.GONE);
            acctView.setVisibility(View.VISIBLE);
            acctView.setText(acct);
            controls.setVisibility(View.VISIBLE);
            clearBtn.setVisibility(View.VISIBLE);
            acctWarning.setVisibility(authorized ? View.GONE : View.VISIBLE);

            freqSpin.setEnabled(true);
            freqSpinLabel.setEnabled(true);
            syncBtn.setEnabled(authorized);
        } else {
            itsDropboxUri = null;
            chooseBtn.setVisibility(View.VISIBLE);
            acctView.setVisibility(View.GONE);
            controls.setVisibility(View.GONE);
            clearBtn.setVisibility(View.GONE);
            acctWarning.setVisibility(View.GONE);
        }
    }

    /** Update the UI when the Box account is changed */
    private void updateBoxAccount(Cursor cursor)
    {
        View chooseBtn = findViewById(R.id.box_choose);
        TextView acctView = (TextView)findViewById(R.id.box_acct);
        View controls = findViewById(R.id.box_controls);
        View clearBtn = findViewById(R.id.box_clear);
        if (cursor != null) {
            long id = cursor.getLong(
                    PasswdSafeContract.Providers.PROJECTION_IDX_ID);
            String acct = PasswdSafeContract.Providers.getDisplayName(cursor);
            int freqVal = cursor.getInt(
                    PasswdSafeContract.Providers.PROJECTION_IDX_SYNC_FREQ);
            ProviderSyncFreqPref freq =
                    ProviderSyncFreqPref.freqValueOf(freqVal);
            itsBoxUri = ContentUris.withAppendedId(
                    PasswdSafeContract.Providers.CONTENT_URI, id);
            boolean authorized = getBoxProvider().isAccountAuthorized();

            View freqSpinLabel = findViewById(R.id.box_interval_label);
            Spinner freqSpin = (Spinner)findViewById(R.id.box_interval);
            freqSpin.setSelection(freq.getDisplayIdx());
            View syncBtn = findViewById(R.id.box_sync);
            chooseBtn.setVisibility(View.GONE);
            acctView.setVisibility(View.VISIBLE);
            acctView.setText(acct);
            controls.setVisibility(View.VISIBLE);
            clearBtn.setVisibility(View.VISIBLE);

            freqSpin.setEnabled(true);
            freqSpinLabel.setEnabled(true);
            syncBtn.setEnabled(authorized);
        } else {
            itsBoxUri = null;
            chooseBtn.setVisibility(View.VISIBLE);
            acctView.setVisibility(View.GONE);
            controls.setVisibility(View.GONE);
            clearBtn.setVisibility(View.GONE);
        }
    }

    /** Update the UI when the ownCloud account is changed */
    private void updateOwncloudAccount(Cursor cursor)
    {
        View chooseBtn = findViewById(R.id.owncloud_choose);
        TextView acctView = (TextView)findViewById(R.id.owncloud_acct);
        View controls = findViewById(R.id.owncloud_controls);
        View clearBtn = findViewById(R.id.owncloud_clear);
        View authReqWarning = findViewById(R.id.owncloud_auth_required);
        if (cursor != null) {
            long id = cursor.getLong(
                    PasswdSafeContract.Providers.PROJECTION_IDX_ID);
            String acct = PasswdSafeContract.Providers.getDisplayName(cursor);
            int freqVal = cursor.getInt(
                    PasswdSafeContract.Providers.PROJECTION_IDX_SYNC_FREQ);
            ProviderSyncFreqPref freq =
                    ProviderSyncFreqPref.freqValueOf(freqVal);
            itsOwncloudUri = ContentUris.withAppendedId(
                    PasswdSafeContract.Providers.CONTENT_URI, id);

            OwncloudProvider provider = getOwncloudProvider();
            boolean authorized = provider.isAccountAuthorized();
            boolean useHttps = provider.useHttps();

            View freqSpinLabel = findViewById(R.id.owncloud_interval_label);
            Spinner freqSpin = (Spinner)findViewById(R.id.owncloud_interval);
            CheckBox httpsCb = (CheckBox)findViewById(R.id.owncloud_use_https);

            freqSpin.setSelection(freq.getDisplayIdx());
            httpsCb.setChecked(useHttps);
            chooseBtn.setVisibility(View.GONE);
            acctView.setVisibility(View.VISIBLE);
            acctView.setText(acct);
            controls.setVisibility(View.VISIBLE);
            clearBtn.setVisibility(View.VISIBLE);
            authReqWarning.setVisibility(authorized ? View.GONE : View.VISIBLE);

            freqSpin.setEnabled(true);
            freqSpinLabel.setEnabled(true);
        } else {
            itsOwncloudUri = null;
            chooseBtn.setVisibility(View.VISIBLE);
            acctView.setVisibility(View.GONE);
            controls.setVisibility(View.GONE);
            clearBtn.setVisibility(View.GONE);
            authReqWarning.setVisibility(View.GONE);
        }
    }

    /** Remove an account */
    private void removeAccount(Uri currAcct)
    {
        new AccountUpdateTask(currAcct, getString(R.string.removing_account))
        {
            @Override
            protected void doAccountUpdate(ContentResolver cr)
            {
                if (itsAccountUri != null) {
                    cr.delete(itsAccountUri, null, null);
                }
            }
        }.startTask(this);
    }

    /** Update the sync frequency for an account */
    private void updateSyncFreq(final ProviderSyncFreqPref freq, Uri acct)
    {
        new AccountUpdateTask(acct, getString(R.string.updating_account))
        {
            @Override
            protected void doAccountUpdate(ContentResolver cr)
            {
                ContentValues values = new ContentValues();
                values.put(PasswdSafeContract.Providers.COL_SYNC_FREQ,
                           freq.getFreq());
                cr.update(itsAccountUri, values, null, null);
            }
        }.startTask(this);
    }

    /** Get the Google Drive Play provider */
    private Provider getGdrivePlayProvider()
    {
        return ProviderFactory.getProvider(ProviderType.GDRIVE_PLAY, this);
    }

    /** Get the Google Drive provider */
    private Provider getGdriveProvider()
    {
        return ProviderFactory.getProvider(ProviderType.GDRIVE, this);
    }

    /** Get the Dropbox provider */
    private Provider getDbxProvider()
    {
        return ProviderFactory.getProvider(ProviderType.DROPBOX, this);
    }

    /** Get the Box provider */
    private Provider getBoxProvider()
    {
        return ProviderFactory.getProvider(ProviderType.BOX, this);
    }

    /** Get the ownCloud provider */
    private OwncloudProvider getOwncloudProvider()
    {
        return (OwncloudProvider)
                ProviderFactory.getProvider(ProviderType.OWNCLOUD, this);
    }

    /** Dialog to prompt when an account is cleared */
    public static class ClearPromptDlg extends DialogFragment
    {
        /** Create an instance of the dialog */
        public static ClearPromptDlg newInstance(Uri currAcct)
        {
            ClearPromptDlg dlg = new ClearPromptDlg();
            Bundle args = new Bundle();
            args.putParcelable("currAcct", currAcct);
            dlg.setArguments(args);
            return dlg;
        }

        /* (non-Javadoc)
         * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
         */
        @Override
        public @NonNull
        Dialog onCreateDialog(Bundle savedInstanceState)
        {
            Bundle args = getArguments();
            final Uri currAcct = args.getParcelable("currAcct");

            AlertDialog.Builder builder =
                    new AlertDialog.Builder(getActivity());
            builder
            .setMessage(R.string.remove_account)
            .setPositiveButton(android.R.string.yes,
                               new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    MainActivity act = (MainActivity)getActivity();
                    act.removeAccount(currAcct);
                }
            })
            .setNegativeButton(android.R.string.no, null);
            return builder.create();
        }
    }
}
