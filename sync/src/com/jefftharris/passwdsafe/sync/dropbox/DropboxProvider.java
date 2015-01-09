/*
 * Copyright (©) 2013-2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.dropbox;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxFileSystem.PathListener;
import com.dropbox.sync.android.DbxPath;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.lib.Utils;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncTimerProvider;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.NewAccountTask;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;

/**
 *  The DropboxProvider class encapsulates Dropbox
 */
public class DropboxProvider extends AbstractSyncTimerProvider
{
    private static final String DROPBOX_SYNC_APP_KEY = "ncrre47fqpcu42z";
    private static final String DROPBOX_SYNC_APP_SECRET = "7wxt4myb2qut395";

    private static final String TAG = "DropboxProvider";

    private DbxAccountManager itsDropboxAcctMgr = null;
    private DbxFileSystem itsDropboxFs = null;
    private PathListener itsDropboxPathListener = null;
    private Runnable itsDropboxSyncEndHandler = null;


    /** Constructor */
    public DropboxProvider(Context ctx)
    {
        super(ProviderType.DROPBOX, ctx, TAG);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#init()
     */
    @Override
    public void init()
    {
        super.init();
        itsDropboxAcctMgr = DbxAccountManager.getInstance(
                getContext(), DROPBOX_SYNC_APP_KEY, DROPBOX_SYNC_APP_SECRET);
        updateDropboxAcct();
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#startAccountLink(android.app.Activity, int)
     */
    @Override
    public void startAccountLink(Activity activity, int requestCode)
    {
        if (itsDropboxAcctMgr.getLinkedAccount() != null) {
            unlinkAccount();
        }
        itsDropboxAcctMgr.startLink(activity, requestCode);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#finishAccountLink()
     */
    @Override
    public NewAccountTask finishAccountLink(int activityResult,
                                            Intent activityData,
                                            Uri acctProviderUri)
    {
        updateDropboxAcct();
        DbxAccount acct = itsDropboxAcctMgr.getLinkedAccount();
        return new NewAccountTask(acctProviderUri,
                                  (acct == null) ? null : acct.getUserId(),
                                  ProviderType.DROPBOX, getContext());
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#unlinkAccount()
     */
    @Override
    public void unlinkAccount()
    {
        // TODO: cleanup unlinkAccount vs. cleanupOnDelete
        itsDropboxAcctMgr.unlink();
        updateDropboxAcct();
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#isAccountAuthorized()
     */
    @Override
    public boolean isAccountAuthorized()
    {
        return (itsDropboxFs != null) && !itsDropboxFs.isShutDown();
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.Provider#getAccount(java.lang.String)
     */
    @Override
    public Account getAccount(String acctName)
    {
        return new Account(acctName, SyncDb.DROPBOX_ACCOUNT_TYPE);
    }


    /** Check whether a provider can be added */
    @Override
    public void checkProviderAdd(SQLiteDatabase db)
            throws Exception
    {
        List<DbProvider> providers = SyncDb.getProviders(db);
        for (DbProvider provider: providers) {
            if (provider.itsType == ProviderType.DROPBOX) {
                throw new Exception("Only one Dropbox account allowed");
            }
        }
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.Provider#cleanupOnDelete(java.lang.String)
     */
    @Override
    public void cleanupOnDelete(String acctName)
    {
        unlinkAccount();
    }


    @Override
    protected String getAccountUserId()
    {
        DbxAccount dbxAcct = itsDropboxAcctMgr.getLinkedAccount();
        return (dbxAcct != null) ? dbxAcct.getUserId() : null;
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.Provider#requestSync(boolean)
     */
    @Override
    public void requestSync(final boolean manual)
    {
        if (itsDropboxFs == null) {
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
                doRequestSync(manual);
            }
        };

        if (itsDropboxSyncEndHandler != null) {
            getHandler().removeCallbacks(itsDropboxSyncEndHandler);
        }
        itsDropboxSyncEndHandler = new Runnable()
        {
            @Override
            public void run()
            {
                PasswdSafeUtil.dbginfo(TAG, "syncDropbox end timer");
                if ((itsDropboxFs != null) &&
                        (itsDropboxPathListener != null)) {
                    itsDropboxFs.removePathListenerForAll(
                        itsDropboxPathListener);
                }
                itsDropboxPathListener = null;
                itsDropboxSyncEndHandler = null;
            }
        };
        getHandler().postDelayed(itsDropboxSyncEndHandler, 60 * 1000);
        itsDropboxFs.addPathListener(itsDropboxPathListener, DbxPath.ROOT,
                                     PathListener.Mode.PATH_OR_DESCENDANT);
        doRequestSync(manual);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.Provider#sync(android.accounts.Account, com.jefftharris.passwdsafe.sync.SyncDb.DbProvider, android.database.sqlite.SQLiteDatabase, com.jefftharris.passwdsafe.sync.SyncLogRecord)
     */
    @Override
    public void sync(Account acct,
                     DbProvider provider,
                     SQLiteDatabase db,
                     boolean manual,
                     boolean full,
                     SyncLogRecord logrec) throws Exception
    {
        if (itsDropboxFs == null) {
            PasswdSafeUtil.dbginfo(TAG, "sync: no fs");
            return;
        }
        new DropboxSyncer(itsDropboxFs, provider, db,
                          logrec, getContext()).sync();
    }


    /** Insert a local file */
    @Override
    public long insertLocalFile(long providerId, String title,
                                SQLiteDatabase db)
            throws Exception
    {
        long fileId = SyncDb.addLocalFile(providerId, title,
                                          System.currentTimeMillis(), db);

        DbxPath path = new DbxPath(DbxPath.ROOT, title);
        SyncDb.updateRemoteFile(fileId, path.toString(), path.getName(), null,
                                -1, null, db);
        return fileId;
    }


    /** Update a local file */
    @Override
    public synchronized void updateLocalFile(DbFile file,
                                             String localFileName,
                                             java.io.File localFile,
                                             SQLiteDatabase db)
            throws Exception
    {
        SyncDb.updateLocalFile(file.itsId, localFileName,
                               file.itsLocalTitle, file.itsLocalFolder,
                               localFile.lastModified(), db);

        DbxPath path = new DbxPath(file.itsRemoteId);
        DbxFile dbxfile = null;
        try {
            if (itsDropboxFs.exists(path)) {
                dbxfile = itsDropboxFs.open(path);
            } else {
                dbxfile = itsDropboxFs.create(path);
            }

            InputStream is = null;
            OutputStream os = null;
            try {
                is = new BufferedInputStream(new FileInputStream(localFile));
                os = new BufferedOutputStream(dbxfile.getWriteStream());
                Utils.copyStream(is, os);
            } finally {
                Utils.closeStreams(is, os);
            }
        } finally {
            if (dbxfile != null) {
                dbxfile.close();
            }
        }
    }


    /** Delete a local file */
    @Override
    public void deleteLocalFile(DbFile file, SQLiteDatabase db)
            throws Exception
    {
        SyncDb.updateLocalFileDeleted(file.itsId, db);

        if (itsDropboxFs == null) {
            PasswdSafeUtil.dbginfo(TAG, "deleteLocalFile: no fs");
            return;
        }

        if (file.itsIsRemoteDeleted || (file.itsRemoteId == null)) {
            return;
        }

        DbxPath path = new DbxPath(file.itsRemoteId);
        if (itsDropboxFs.exists(path)) {
            itsDropboxFs.delete(path);
        }
    }


    /** Update after a Dropbox account change */
    private void updateDropboxAcct()
    {
        DbxAccount acct = itsDropboxAcctMgr.getLinkedAccount();
        boolean shouldHaveFs = (acct != null);
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
                    doRequestSync(false);
                }
            });

            try {
                updateProviderSyncFreq(acct.getUserId());
                itsDropboxFs = DbxFileSystem.forAccount(acct);
                requestSync(false);
            } catch (Exception e) {
                Log.e(TAG, "updateDropboxAcct failure", e);
            }
        } else if (!shouldHaveFs && haveFs) {
            itsDropboxFs = null;
            updateSyncFreq(null, 0);
        }
    }
}
