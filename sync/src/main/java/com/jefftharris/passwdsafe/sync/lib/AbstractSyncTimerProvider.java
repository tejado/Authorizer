/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import android.accounts.Account;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;

/**
 *  Abstract provider that uses a system timer to perform syncing
 */
public abstract class AbstractSyncTimerProvider extends AbstractProvider
{
    private final int BROADCAST_REQUEST_SYNC_DROPBOX = 0;
    private final int BROADCAST_REQUEST_SYNC_BOX = 1;
    private final int BROADCAST_REQUEST_SYNC_OWNCLOUD = 2;

    private final ProviderType itsProviderType;
    private final Context itsContext;
    private final String itsTag;
    private Handler itsHandler = null;
    private PendingIntent itsSyncTimeoutIntent = null;
    private SyncRequestTask itsSyncTask = null;

    protected AbstractSyncTimerProvider(ProviderType type,
                                        Context ctx, String tag)
    {
        itsProviderType = type;
        itsContext = ctx;
        itsTag = tag;
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#init()
     */
    @Override
    public void init()
    {
        itsHandler = new Handler(Looper.getMainLooper());
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#fini()
     */
    @Override
    public void fini()
    {
        if (itsSyncTimeoutIntent != null) {
            AlarmManager alarmMgr = (AlarmManager)
                    itsContext.getSystemService(Context.ALARM_SERVICE);
            alarmMgr.cancel(itsSyncTimeoutIntent);
        }
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#updateSyncFreq(android.accounts.Account, int)
     */
    @Override
    public void updateSyncFreq(Account acct, final int freq)
    {
        itsHandler.post(new Runnable() {
            @Override
            public void run()
            {
                String userId = getAccountUserId();
                PasswdSafeUtil.dbginfo(itsTag,
                                       "updateSyncFreq acct %s, freq %d",
                                       userId, freq);

                if ((userId != null) && (freq > 0)) {
                    if (itsSyncTimeoutIntent == null) {
                        Intent timeoutIntent =
                                new Intent(ACTION_SYNC_EXPIRATION_TIMEOUT);
                        timeoutIntent.putExtra(
                                SYNC_EXPIRATION_TIMEOUT_EXTRA_TYPE,
                                itsProviderType.toString());

                        int requestCode;
                        switch (itsProviderType) {
                        case BOX: {
                            requestCode = BROADCAST_REQUEST_SYNC_BOX;
                            break;
                        }
                        case DROPBOX: {
                            requestCode = BROADCAST_REQUEST_SYNC_DROPBOX;
                            break;
                        }
                        case OWNCLOUD: {
                            requestCode = BROADCAST_REQUEST_SYNC_OWNCLOUD;
                            break;
                        }
                        case GDRIVE:
                        case GDRIVE_PLAY:
                            //noinspection UnnecessaryDefault
                        default: {
                            throw new IllegalStateException("GDRIVE not valid");
                        }
                        }

                        itsSyncTimeoutIntent = PendingIntent.getBroadcast(
                                itsContext, requestCode, timeoutIntent,
                                PendingIntent.FLAG_CANCEL_CURRENT);
                    }

                    AlarmManager alarmMgr = (AlarmManager)
                            itsContext.getSystemService(Context.ALARM_SERVICE);
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

    /** Update the sync frequency for this provider */
    protected final void updateProviderSyncFreq(String userId)
            throws SQLException
    {
        SyncDb syncDb = SyncDb.acquire();
        try {
            SQLiteDatabase db = syncDb.beginTransaction();
            DbProvider provider = SyncDb.getProvider(userId, itsProviderType,
                                                     db);
            updateSyncFreq(null,
                           (provider != null) ? provider.itsSyncFreq : 0);
            db.setTransactionSuccessful();
        } finally {
            syncDb.endTransactionAndRelease();
        }
    }

    /** Check whether to start a sync */
    protected synchronized final void doRequestSync(boolean manual)
    {
        if ((itsSyncTask == null) || itsSyncTask.isFinished()) {
            itsSyncTask = new SyncRequestTask(manual);
        }
        itsSyncTask.checkSync();
    }

    /** Get the account user identifier */
    protected abstract String getAccountUserId();

    /** Get the context */
    protected final Context getContext()
    {
        return itsContext;
    }

    /** Get the handler */
    protected final Handler getHandler()
    {
        return itsHandler;
    }

    /** Background sync request for a timer provider */
    private class SyncRequestTask extends AsyncTask<Void, Void, Void>
            implements Runnable
    {
        private final boolean itsIsManual;
        private boolean itsIsTimerPending = false;
        private boolean itsIsRunning = true;

        /** Constructor */
        public SyncRequestTask(boolean manual)
        {
            itsIsManual = manual;
        }

        /** Check the status of the sync */
        public synchronized void checkSync()
        {
            Status status = getStatus();
            PasswdSafeUtil.dbginfo(
                    itsTag, "SyncRequestTask checkSync status %s timer %b",
                    status, itsIsTimerPending);
            long delay = 0;
            switch (status) {
            case PENDING: {
                delay = 3000;
                break;
            }
            case RUNNING:
            case FINISHED: {
                delay = 15000;
                break;
            }
            }
            if (!itsIsTimerPending) {
                itsIsTimerPending = true;
                itsHandler.postDelayed(this, delay);
            }
        }

        /** Get whether the task is finished */
        public synchronized boolean isFinished()
        {
            return !itsIsTimerPending && !itsIsRunning;
        }

        /** Timer expired */
        @Override
        public synchronized void run()
        {
            Status status = getStatus();
            PasswdSafeUtil.dbginfo(itsTag,
                                   "SyncRequestTask timer expired status %s",
                                   status);
            itsIsTimerPending = false;
            switch (status) {
            case PENDING: {
                execute();
                break;
            }
            case RUNNING:
            case FINISHED: {
                doRequestSync(itsIsManual);
                break;
            }
            }
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Void doInBackground(Void... params)
        {
            String acctUserId = getAccountUserId();
            if (acctUserId != null) {
                SyncDb syncDb = SyncDb.acquire();
                SQLiteDatabase db = syncDb.getDb();
                try {
                    Account acct = getAccount(acctUserId);
                    DbProvider provider =
                            SyncHelper.getDbProviderForAcct(acct, db);
                    if (provider != null) {
                        SyncHelper.performSync(acct, provider,
                                               AbstractSyncTimerProvider.this,
                                               itsIsManual, false, db,
                                               itsContext);
                    }
                } finally {
                    syncDb.release();
                }
            }
            return null;
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected synchronized void onPostExecute(Void result)
        {
            super.onPostExecute(result);
            itsIsRunning = false;
        }
    }
}
