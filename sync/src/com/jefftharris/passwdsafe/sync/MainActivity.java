/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.jefftharris.passwdsafe.lib.PasswdSafeContract;

public class MainActivity extends FragmentActivity
{
    private static final String TAG = "MainActivity";

    private static final int CHOOSE_ACCOUNT = 0;

    private static final String[] ACCOUNT_TYPE =
        new String[] {GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE};

    private GoogleAccountManager itsAccountMgr;
    private SyncDb itsSyncDb;
    private Account itsNewAccount = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate");

        // TODO: create a special google acct for the sync service
        itsAccountMgr = new GoogleAccountManager(this);
        itsSyncDb = new SyncDb(this);

        updateGdriveAccount(getPreferenceAccount());
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

    @Override
    public void onResume()
    {
        super.onResume();

        // TODO: do this always on resume vs. perhaps create?
        // Check the state of Google Play services
        int rc = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (rc != ConnectionResult.SUCCESS) {
            Dialog dlg = GooglePlayServicesUtil.getErrorDialog(rc, this, 0);
            dlg.show();
        }
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
                    Account account =
                        itsAccountMgr.getAccountByName(accountName);
                    itsNewAccount = account;
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


    /** Button onClick handler to choose a GDrive account */
    public void onGdriveChoose(View view)
    {
        Intent intent =
            AccountPicker.newChooseAccountIntent(getPreferenceAccount(),
                                                 null, ACCOUNT_TYPE, true,
                                                 null, null, null, null);
        startActivityForResult(intent, CHOOSE_ACCOUNT);
    }


    /** Button onClick handler to sync a GDrive account */
    public void onGdriveSync(View view)
    {
        Account acct = getPreferenceAccount();
        if (acct != null) {
            ContentResolver.requestSync(acct, PasswdSafeContract.AUTHORITY,
                                        new Bundle());
        }
    }


    /** Button onClick handler to clear a GDrive account */
    public void onGdriveClear(View view)
    {
        setAccount(null);
    }


    /** Update the UI when the GDrive account is changed */
    private final void updateGdriveAccount(Account acct)
    {
        View chooseBtn = findViewById(R.id.gdrive_choose);
        TextView acctView = (TextView)findViewById(R.id.gdrive_acct);
        View btns = findViewById(R.id.gdrive_btns);
        if (acct != null) {
            chooseBtn.setVisibility(View.GONE);
            acctView.setVisibility(View.VISIBLE);
            acctView.setText("Account - " + acct.name);
            btns.setVisibility(View.VISIBLE);
        } else {
            chooseBtn.setVisibility(View.VISIBLE);
            acctView.setVisibility(View.GONE);
            btns.setVisibility(View.GONE);
        }
    }

    /** Set the new account to use with the app */
    private void setAccount(Account account)
    {
        new AccountTask(account);
    }

    /** Set the sync frequency for the selected account */
    private void setSyncFrequency(Account account)
    {
        if (account != null) {
            ContentResolver.setSyncAutomatically(
                    account, PasswdSafeContract.AUTHORITY, true);
        }
    }

    /** Get the currently preferred account to use with the app */
    private Account getPreferenceAccount()
    {
        return itsAccountMgr.getAccountByName(itsSyncDb.getProviderAccount());
    }


    /** Async task to set the account */
    private final class AccountTask extends AsyncTask<Account, Void, Account>
    {
        ProgressFragment itsProgressFrag;
        Account itsOldAccount;

        /** Constructor */
        public AccountTask(Account acct)
        {
            String msg = getString((acct == null) ?
                    R.string.removing_account : R.string.adding_account);
            itsProgressFrag = ProgressFragment.newInstance(msg);
            itsProgressFrag.show(getSupportFragmentManager(), null);
            itsOldAccount = getPreferenceAccount();
            execute(acct);
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Account doInBackground(Account... params)
        {
            Account account = params[0];
            try {
                // Stop syncing for the previously selected account.
                if (itsOldAccount != null) {
                    ContentResolver.setSyncAutomatically(
                        itsOldAccount, PasswdSafeContract.AUTHORITY, false);
                    GDriveSyncer.deleteProvider(itsOldAccount, itsSyncDb,
                                                MainActivity.this);
                }

                if (account != null) {
                    GDriveSyncer.addProvider(account, itsSyncDb,
                                             MainActivity.this);
                    setSyncFrequency(account);
                    ContentResolver.requestSync(account,
                                                PasswdSafeContract.AUTHORITY,
                                                new Bundle());
                }
            } catch (SQLException e) {
                Log.e(TAG, "DB error", e);
            }
            return account;
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Account account)
        {
            super.onPostExecute(account);
            updateGdriveAccount(account);
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
