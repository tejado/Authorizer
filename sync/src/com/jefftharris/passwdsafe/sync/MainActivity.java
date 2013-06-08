/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

public class MainActivity extends FragmentActivity
        implements LoaderCallbacks<Cursor>
{
    private static final String TAG = "MainActivity";

    private static final int CHOOSE_ACCOUNT = 0;

    private static final int LOADER_PROVIDERS = 0;

    private static final String[] ACCOUNT_TYPE =
        new String[] {GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE};

    private SyncDb itsSyncDb;
    private Account itsGdriveAccount = null;
    private Uri itsGdriveUri = null;
    private String itsNewAccount = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate");

        itsSyncDb = new SyncDb(this);

        // TODO: google drive intents to open from drive app

        Spinner freqSpin = (Spinner)findViewById(R.id.gdrive_interval);
        freqSpin.setOnItemSelectedListener(new OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id)
            {
                onGdriveFreqChanged(pos);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });

        // Check the state of Google Play services
        int rc = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (rc != ConnectionResult.SUCCESS) {
            Dialog dlg = GooglePlayServicesUtil.getErrorDialog(rc, this, 0);
            dlg.show();
        }

        updateGdriveAccount(null);
        LoaderManager lm = getSupportLoaderManager();
        lm.initLoader(LOADER_PROVIDERS, null, this);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        itsSyncDb.close();
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onResumeFragments()
     */
    @Override
    protected void onResumeFragments()
    {
        super.onResumeFragments();
        if (itsNewAccount != null) {
            setAccount(itsNewAccount);
            itsNewAccount = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode) {
        case CHOOSE_ACCOUNT:
            if (data != null) {
                Bundle b = data.getExtras();
                String accountName =
                        b.getString(AccountManager.KEY_ACCOUNT_NAME);
                Log.i(TAG, "Selected account: " + accountName);
                if (accountName != null && accountName.length() > 0) {
                    itsNewAccount = accountName;
                }
            }
            break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_settings: {
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }


    /** Button onClick handler to launch PasswdSafe */
    public void onLaunchPasswdSafeClick(View view)
    {
        PasswdSafeUtil.startMainActivity("com.jefftharris.passwdsafe", this);
    }


    /** Button onClick handler to choose a GDrive account */
    public void onGdriveChoose(View view)
    {
        Intent intent =
            AccountPicker.newChooseAccountIntent(itsGdriveAccount,
                                                 null, ACCOUNT_TYPE, true,
                                                 null, null, null, null);
        startActivityForResult(intent, CHOOSE_ACCOUNT);
    }


    /** Button onClick handler to sync a GDrive account */
    public void onGdriveSync(View view)
    {
        if (itsGdriveAccount != null) {
            ContentResolver.requestSync(itsGdriveAccount,
                                        PasswdSafeContract.AUTHORITY,
                                        new Bundle());
        }
    }


    /** Button onClick handler to clear a GDrive account */
    public void onGdriveClear(View view)
    {
        DialogFragment prompt = new ClearPromptDlg();
        prompt.show(getSupportFragmentManager(), null);
    }


    /** GDrive sync frequency spinner changed */
    private void onGdriveFreqChanged(int pos)
    {
        ProviderSyncFreqPref freq = ProviderSyncFreqPref.displayValueOf(pos);
        new AccountTask(freq);
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
        boolean hasGdrive = false;
        for (boolean more = cursor.moveToFirst(); more;
                more = cursor.moveToNext()) {
            String typeStr = cursor.getString(
                    PasswdSafeContract.Providers.PROJECTION_IDX_TYPE);
            try {
                PasswdSafeContract.Providers.Type type =
                        PasswdSafeContract.Providers.Type.valueOf(typeStr);
                switch (type) {
                case GDRIVE: {
                    hasGdrive = true;
                    updateGdriveAccount(cursor);
                    break;
                }
                }
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Unknown type: " + typeStr);
            }
        }
        if (!hasGdrive) {
            updateGdriveAccount(null);
        }
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoaderReset(android.support.v4.content.Loader)
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader)
    {
        updateGdriveAccount(null);
    }

    /** Update the UI when the GDrive account is changed */
    private final void updateGdriveAccount(Cursor cursor)
    {
        View chooseBtn = findViewById(R.id.gdrive_choose);
        TextView acctView = (TextView)findViewById(R.id.gdrive_acct);
        View btns = findViewById(R.id.gdrive_controls);
        if (cursor != null) {
            long id = cursor.getLong(
                    PasswdSafeContract.Providers.PROJECTION_IDX_ID);
            String acct = cursor.getString(
                    PasswdSafeContract.Providers.PROJECTION_IDX_ACCT);
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
            acctView.setVisibility(View.VISIBLE);
            btns.setVisibility(View.VISIBLE);

            boolean haveAccount = (itsGdriveAccount != null);
            if (haveAccount) {
                acctView.setText(getString(R.string.account_label,
                                           itsGdriveAccount.name));
            } else {
                acctView.setText(getString(R.string.account_not_exists_label,
                                           acct));
            }
            freqSpin.setEnabled(haveAccount);
            freqSpinLabel.setEnabled(haveAccount);
            syncBtn.setEnabled(haveAccount);
        } else {
            itsGdriveAccount = null;
            itsGdriveUri = null;
            chooseBtn.setVisibility(View.VISIBLE);
            acctView.setVisibility(View.GONE);
            btns.setVisibility(View.GONE);
        }
    }

    /** Set the new account to use with the app */
    private void setAccount(String account)
    {
        new AccountTask(account);
    }

    /** Dialog to prompt when an account is cleared */
    public static class ClearPromptDlg extends DialogFragment
    {
        /** Constructor */
        public ClearPromptDlg()
        {
        }

        /* (non-Javadoc)
         * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
         */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
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
                    act.setAccount(null);
                }
            })
            .setNegativeButton(android.R.string.no, null);
            return builder.create();
        }
    }

    /** The type of async account task */
    private enum AccountTaskType
    {
        ADD_REMOVE,
        UPDATE_SYNC_FREQ
    }

    /** Async task to set the account */
    private class AccountTask extends AsyncTask<Void, Void, Void>
    {
        private final AccountTaskType itsType;
        private final Uri itsCurrAccount;
        private ProgressFragment itsProgressFrag;
        private final String itsNewAccount;
        private final ProviderSyncFreqPref itsUpdateFreq;

        /** Constructor for add/remove */
        public AccountTask(String acct)
        {
            itsType = AccountTaskType.ADD_REMOVE;
            itsCurrAccount = itsGdriveUri;
            itsNewAccount = acct;
            itsUpdateFreq = null;
            String msg = getString((acct == null) ?
                    R.string.removing_account : R.string.adding_account);
            init(msg);
        }

        /** Constructor for updating the sync frequency */
        public AccountTask(ProviderSyncFreqPref freq)
        {
            itsType = AccountTaskType.UPDATE_SYNC_FREQ;
            itsCurrAccount = itsGdriveUri;
            itsNewAccount = null;
            itsUpdateFreq = freq;
            init(getString(R.string.updating_account));
        }

        /** Initialize the task */
        private void init(String msg)
        {
            itsProgressFrag = ProgressFragment.newInstance(msg);
            itsProgressFrag.show(getSupportFragmentManager(), null);
            execute();
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Void doInBackground(Void... params)
        {
            try {
                ContentResolver cr = MainActivity.this.getContentResolver();

                switch (itsType) {
                case ADD_REMOVE: {
                    // Stop syncing for the previously selected account.
                    if (itsCurrAccount != null) {
                        cr.delete(itsCurrAccount, null, null);
                    }

                    if (itsNewAccount != null) {
                        ContentValues values = new ContentValues();
                        values.put(PasswdSafeContract.Providers.COL_ACCT,
                                   itsNewAccount);
                        cr.insert(PasswdSafeContract.Providers.CONTENT_URI,
                                  values);
                    }
                    break;
                }
                case UPDATE_SYNC_FREQ: {
                    ContentValues values = new ContentValues();
                    values.put(PasswdSafeContract.Providers.COL_SYNC_FREQ,
                               itsUpdateFreq.getFreq());
                    cr.update(itsCurrAccount, values, null, null);
                    break;
                }
                }
            } catch (Exception e) {
                Log.e(TAG, "Account update error", e);
            }
            return null;
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Void arg)
        {
            super.onPostExecute(arg);
            itsProgressFrag.dismiss();
        }
    }
/*

    static final int REQUEST_ACCOUNT_PICKER = 1;
    static final int REQUEST_AUTHORIZATION = 2;
    static final int CAPTURE_IMAGE = 3;

    private static Uri fileUri;
    private static Drive service;
    private GoogleAccountCredential credential;

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      credential = GoogleAccountCredential.usingOAuth2(this, DriveScopes.DRIVE);
      startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
      switch (requestCode) {
      case REQUEST_ACCOUNT_PICKER:
        if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
          String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
          if (accountName != null) {
            credential.setSelectedAccountName(accountName);
            service = getDriveService(credential);
            startCameraIntent();
          }
        }
        break;
      case REQUEST_AUTHORIZATION:
        if (resultCode == Activity.RESULT_OK) {
          saveFileToDrive();
        } else {
          startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
        }
        break;
      case CAPTURE_IMAGE:
        if (resultCode == Activity.RESULT_OK) {
          saveFileToDrive();
        }
      }
    }

    private void startCameraIntent() {
      String mediaStorageDir = Environment.getExternalStoragePublicDirectory(
          Environment.DIRECTORY_PICTURES).getPath();
      String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
      fileUri = Uri.fromFile(new java.io.File(mediaStorageDir + java.io.File.separator + "IMG_"
          + timeStamp + ".jpg"));

      Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
      cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
      startActivityForResult(cameraIntent, CAPTURE_IMAGE);
    }

    private void saveFileToDrive() {
      Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            // File's binary content
            java.io.File fileContent = new java.io.File(fileUri.getPath());
            FileContent mediaContent = new FileContent("image/jpeg", fileContent);

            // File's metadata.
            File body = new File();
            body.setTitle(fileContent.getName());
            body.setMimeType("image/jpeg");

            File file = service.files().insert(body, mediaContent).execute();
            if (file != null) {
              showToast("Photo uploaded: " + file.getTitle());
              startCameraIntent();
            }
          } catch (UserRecoverableAuthIOException e) {
            startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      });
      t.start();
    }

    private Drive getDriveService(GoogleAccountCredential credential) {
      return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
          .build();
    }

    public void showToast(final String toast) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
        }
      });
    }
*/
}
