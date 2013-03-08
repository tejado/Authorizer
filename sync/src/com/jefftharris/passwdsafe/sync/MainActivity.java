/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.jefftharris.passwdsafe.lib.PasswdSafeContract;

public class MainActivity extends Activity
{
    private static final String TAG = "MainActivity";

    private static final int CHOOSE_ACCOUNT = 0;

    private static final String[] ACCOUNT_TYPE =
        new String[] {GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE};

    enum AccountState
    {
        INITIAL,
        CHOOSING_ACCOUNT,
        DONE
    }

    private Button itsAccountBtn;
    private AccountState itsAccountState = AccountState.INITIAL;
    private GoogleAccountManager itsAccountMgr;
    private SyncDb itsSyncDb;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate");

        // TODO: create a special google acct for the sync service
        itsAccountBtn = (Button)findViewById(R.id.account);
        itsAccountState = AccountState.INITIAL;
        itsAccountMgr = new GoogleAccountManager(this);
        itsSyncDb = new SyncDb(this);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Account prefAccount = getPreferenceAccount();
        if (prefAccount != null) {
            itsAccountBtn.setText("Account - " + prefAccount.name);
            itsAccountState = AccountState.DONE;
        } else if (itsAccountState == AccountState.INITIAL) {
            chooseAccount();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode) {
        case CHOOSE_ACCOUNT:
            if (data != null) {
                Log.i(TAG,
                      "SELECTED ACCOUNT WITH EXTRA: "
                          + data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
                Bundle b = data.getExtras();

                String accountName = b.getString(AccountManager.KEY_ACCOUNT_NAME);
                Log.i(TAG, "Selected account: " + accountName);
                if (accountName != null && accountName.length() > 0) {
                    Account account =
                        itsAccountMgr.getAccountByName(accountName);
                    setAccount(account);
                }
            } else {
                itsAccountState = AccountState.INITIAL;
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

    public void onChooseAccount(View view)
    {
        chooseAccount();
    }

    public void onClearAccount(View view)
    {
        setAccount(null);
    }

    public void onSync(View view)
    {
        Account acct = getPreferenceAccount();
        if (acct != null) {
            ContentResolver.requestSync(acct, PasswdSafeContract.AUTHORITY,
                                        new Bundle());
        }
    }


    private void chooseAccount()
    {
        itsAccountState = AccountState.CHOOSING_ACCOUNT;
        Intent intent =
            AccountPicker.newChooseAccountIntent(getPreferenceAccount(),
                                                 null, ACCOUNT_TYPE, true,
                                                 null, null, null, null);
        startActivityForResult(intent, CHOOSE_ACCOUNT);
    }

    /** Set the new account to use with the app */
    private void setAccount(Account account)
    {
        try {
            Account oldAccount = getPreferenceAccount();
            // Stop syncing for the previously selected account.
            if (oldAccount != null) {
                ContentResolver.setSyncAutomatically(
                    oldAccount, PasswdSafeContract.AUTHORITY, false);
                GDriveSyncer.deleteProvider(oldAccount, itsSyncDb, this);
                itsAccountBtn.setText("Choose Account");
                itsAccountState = AccountState.DONE;
            }

            if (account != null) {
                GDriveSyncer.addProvider(account, itsSyncDb);
                itsAccountBtn.setText("Account - " + account.name);
                setSyncFrequency(account);
                itsAccountState = AccountState.DONE;
            }
        } catch (SQLException e) {
            Log.e(TAG, "DB error", e);
        }
    }

    /** Set the sync frequency for the selected account */
    private void setSyncFrequency(Account account)
    {
        if (account != null) {
            try {
                int freq = itsSyncDb.getProviderSyncFreq(account.name);
                ContentResolver.setSyncAutomatically(
                    account, PasswdSafeContract.AUTHORITY, true);
                ContentResolver.addPeriodicSync(
                    account, PasswdSafeContract.AUTHORITY, new Bundle(), freq);
            } catch (SQLException e) {
                Log.e(TAG, "DB error", e);
            }
        }
    }

    /** Get the currently preferred account to use with the app */
    private Account getPreferenceAccount()
    {
        return itsAccountMgr.getAccountByName(itsSyncDb.getProviderAccount());
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
