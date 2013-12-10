/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxFileSystem.PathListener;
import com.dropbox.sync.android.DbxPath;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;

/**
 *  Application class for PasswdSafe Sync
 */
public class SyncApp extends Application
{
    public static final String ACTION_SYNC_EXPIRATION_TIMEOUT =
            "com.jefftharris.passwdsafe.action.SYNC_EXPIRATION_TIMEOUT";

    private static final String DROPBOX_SYNC_APP_KEY = "ncrre47fqpcu42z";
    private static final String DROPBOX_SYNC_APP_SECRET = "7wxt4myb2qut395";

    private static final String TAG = "SyncApp";

    private SyncDb itsSyncDb = null;
    private Handler itsHandler = null;
    private DbxAccountManager itsDropboxAcctMgr = null;
    private DbxFileSystem itsDropboxFs = null;
    private DropboxSyncer itsDropboxSyncer = null;
    private PathListener itsDropboxPathListener = null;
    private Runnable itsDropboxSyncEndHandler = null;
    private PendingIntent itsSyncTimeoutIntent = null;

    private SyncUpdateHandler itsSyncUpdateHandler;
    private SyncUpdateHandler.GDriveState itsSyncGDriveState =
            SyncUpdateHandler.GDriveState.OK;

    /* (non-Javadoc)
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate()
    {
        PasswdSafeUtil.dbginfo(TAG, "onCreate");
        super.onCreate();

        itsSyncDb = new SyncDb(this);
        itsHandler = new Handler(Looper.getMainLooper());
        itsDropboxAcctMgr =
                DbxAccountManager.getInstance(getApplicationContext(),
                                              DROPBOX_SYNC_APP_KEY,
                                              DROPBOX_SYNC_APP_SECRET);
        updateDropboxAcct();
    }


    /* (non-Javadoc)
     * @see android.app.Application#onTerminate()
     */
    @Override
    public void onTerminate()
    {
        PasswdSafeUtil.dbginfo(TAG, "onTerminate");
        if (itsSyncTimeoutIntent != null) {
            AlarmManager alarmMgr =
                    (AlarmManager)getSystemService(Context.ALARM_SERVICE);
            alarmMgr.cancel(itsSyncTimeoutIntent);
        }
        itsSyncDb.close();
        super.onTerminate();
    }


    /** Get the Sync application */
    public static SyncApp get(Context ctx)
    {
        return (SyncApp)ctx.getApplicationContext();
    }


    /** Acquire the SyncDb */
    public static SyncDb acquireSyncDb(Context ctx)
    {
        SyncApp app = SyncApp.get(ctx);
        app.itsSyncDb.acquire();
        return app.itsSyncDb;
    }


    /** Start the process of linking to a Dropbox account */
    public void startDropboxLink(Activity act, int requestCode)
    {
        itsDropboxAcctMgr.startLink(act, requestCode);
    }


    /** Finish the process of linking a Dropbox account */
    public void finishDropboxLink()
    {
        updateDropboxAcct();
    }


    /** Unlink the Dropbox account */
    public void unlinkDropbox()
    {
        itsDropboxAcctMgr.unlink();
        updateDropboxAcct();
    }


    /** Update a Dropbox's sync frequency */
    public void updateDropboxSyncFreq(final int freq)
    {
        itsHandler.post(new Runnable() {
            @Override
            public void run()
            {
                DbxAccount acct = itsDropboxAcctMgr.getLinkedAccount();
                PasswdSafeUtil.dbginfo(TAG,
                                       "updateDropboxSyncFreq acct %s, freq %d",
                                       acct, freq);

                if ((acct != null) && (freq > 0)) {
                    if (itsSyncTimeoutIntent == null) {
                        Intent timeoutIntent =
                                new Intent(ACTION_SYNC_EXPIRATION_TIMEOUT);
                        itsSyncTimeoutIntent = PendingIntent.getBroadcast(
                                SyncApp.this, 0, timeoutIntent,
                                PendingIntent.FLAG_CANCEL_CURRENT);
                    }

                    AlarmManager alarmMgr =
                            (AlarmManager)getSystemService(Context.ALARM_SERVICE);
                    long interval = freq * 1000;
                    alarmMgr.setInexactRepeating(
                            AlarmManager.RTC,
                            System.currentTimeMillis() + interval,
                            interval, itsSyncTimeoutIntent);
                } else {
                    if (itsSyncTimeoutIntent != null) {
                        itsSyncTimeoutIntent.cancel();
                        itsSyncTimeoutIntent = null;
                    }
                }
            }
        });
    }


    /** Sync Dropbox */
    public void syncDropbox(final boolean manual)
    {
        DbxFileSystem fs = getDropboxFs();
        if (fs == null) {
            PasswdSafeUtil.dbginfo(TAG, "syncDropbox no fs");
            return;
        }

        PasswdSafeUtil.dbginfo(TAG, "syncDropbox");
        if (itsDropboxPathListener != null) {
            return;
        }
        itsDropboxPathListener = new PathListener()
        {
            @Override
            public void onPathChange(DbxFileSystem fs,
                                     DbxPath path, Mode mode)
            {
                PasswdSafeUtil.dbginfo(TAG, "syncDropbox path change");
                doDropboxSync(manual);
            }
        };

        if (itsDropboxSyncEndHandler != null) {
            itsHandler.removeCallbacks(itsDropboxSyncEndHandler);
        }
        itsDropboxSyncEndHandler = new Runnable()
        {
            @Override
            public void run()
            {
                PasswdSafeUtil.dbginfo(TAG, "syncDropbox end timer");
                DbxFileSystem fs = getDropboxFs();
                if ((fs != null) && (itsDropboxPathListener != null)) {
                    fs.removePathListenerForAll(itsDropboxPathListener);
                }
                itsDropboxPathListener = null;
                itsDropboxSyncEndHandler = null;
            }
        };
        itsHandler.postDelayed(itsDropboxSyncEndHandler, 60 * 1000);
        fs.addPathListener(itsDropboxPathListener, DbxPath.ROOT,
                           PathListener.Mode.PATH_OR_DESCENDANT);
        doDropboxSync(manual);
    }


    /** Get the Dropbox account; null if no account is linked */
    public DbxAccount getDropboxAcct()
    {
        return itsDropboxAcctMgr.getLinkedAccount();
    }


    /** Get the Dropbox filesystem; null if no account is linked */
    public DbxFileSystem getDropboxFs()
    {
        return itsDropboxFs;
    }


    public void setSyncUpdateHandler(SyncUpdateHandler handler)
    {
        itsSyncUpdateHandler = handler;
        if (itsSyncUpdateHandler != null) {
            itsSyncUpdateHandler.updateGDriveState(itsSyncGDriveState);
        }
    }

    /** Update the state of a Google Drive sync.  This method can be
     * called from a background thread. */
    public void updateGDriveSyncState(final SyncUpdateHandler.GDriveState state)
    {
        itsHandler.post(new Runnable() {
            @Override
            public void run()
            {
                itsSyncGDriveState = state;
                if (itsSyncUpdateHandler != null) {
                    itsSyncUpdateHandler.updateGDriveState(state);
                }
            }
        });
    }


    /** Update after a Dropbox account change */
    private void updateDropboxAcct()
    {
        DbxAccount acct = itsDropboxAcctMgr.getLinkedAccount();
        boolean shouldHaveFs= (acct != null);
        boolean haveFs = (itsDropboxFs != null);

        PasswdSafeUtil.dbginfo(TAG, "updateDropboxAcct should %b have %b",
                               shouldHaveFs, haveFs);
        if (shouldHaveFs && !haveFs) {
            acct.addListener(new DbxAccount.Listener()
            {
                @Override
                public void onAccountChange(DbxAccount acct)
                {
                    PasswdSafeUtil.dbginfo(TAG, "Dropbox acct change");
                    doDropboxSync(false);
                }
            });

            SyncDb syncDb = acquireSyncDb(this);
            try {
                SQLiteDatabase db = syncDb.beginTransaction();
                DbProvider provider = SyncDb.getProvider(acct.getUserId(),
                                                         ProviderType.DROPBOX,
                                                         db);
                updateDropboxSyncFreq(
                        (provider != null) ? provider.itsSyncFreq : 0);

                itsDropboxFs = DbxFileSystem.forAccount(acct);
                syncDropbox(false);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "updateDropboxAcct failure", e);
            } finally {
                syncDb.endTransactionAndRelease();
            }
        } else if (!shouldHaveFs && haveFs) {
            itsDropboxFs = null;
            updateDropboxSyncFreq(0);
        }
    }


    /** Check whether to start a dropbox sync */
    private void doDropboxSync(boolean manual)
    {
        if (itsDropboxSyncer == null) {
            itsDropboxSyncer = new DropboxSyncer(manual);
        }
        itsDropboxSyncer.checkSync();
    }


    /** Background syncer for Dropbox */
    private class DropboxSyncer extends AsyncTask<Void, Void, Void>
            implements Runnable
    {
        private final boolean itsIsManual;
        private boolean itsIsTimerPending = false;
        private boolean itsIsRunning = true;

        /** Constructor */
        public DropboxSyncer(boolean manual)
        {
            itsIsManual = manual;
        }

        /** Check the status of the sync */
        public void checkSync()
        {
            switch (getStatus()) {
            case PENDING: {
                PasswdSafeUtil.dbginfo(TAG, "DropboxSyncer start");
                execute();
                break;
            }
            case RUNNING:
            case FINISHED: {
                if (!itsIsTimerPending) {
                    PasswdSafeUtil.dbginfo(TAG, "DropboxSyncer start timer");
                    itsIsTimerPending = true;
                    itsHandler.postDelayed(this, 15000);
                }
                break;
            }
            }
        }

        /** Timer expired */
        @Override
        public void run()
        {
            PasswdSafeUtil.dbginfo(TAG, "DropboxSyncer timer expired");
            itsIsTimerPending = false;
            checkSyncerDone();
            doDropboxSync(itsIsManual);
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Void doInBackground(Void... params)
        {
            DbxAccount acct = getDropboxAcct();
            if (acct != null) {
                ProviderSyncer syncer = new ProviderSyncer(
                        SyncApp.this, new Account(acct.getUserId(),
                                                  SyncDb.DROPBOX_ACCOUNT_TYPE));
                syncer.performSync(itsIsManual);
            }
            return null;
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);
            itsIsRunning = false;
            checkSyncerDone();
        }

        /** Check whether the DropboxSyncer is finished */
        private void checkSyncerDone()
        {
            if (!itsIsTimerPending && !itsIsRunning) {
                itsDropboxSyncer = null;
            }
        }
    }
}
