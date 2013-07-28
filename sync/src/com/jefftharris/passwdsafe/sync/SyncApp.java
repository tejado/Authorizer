/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxFileSystem.PathListener;
import com.dropbox.sync.android.DbxPath;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

/**
 *  Application class for PasswdSafe Sync
 */
public class SyncApp extends Application
{
    private static final String DROPBOX_SYNC_APP_KEY = "ncrre47fqpcu42z";
    private static final String DROPBOX_SYNC_APP_SECRET = "7wxt4myb2qut395";

    private static final String TAG = "SyncApp";

    private DbxAccountManager itsDropboxAcctMgr = null;
    private DbxFileSystem itsDropboxFs = null;

    /* (non-Javadoc)
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate()
    {
        PasswdSafeUtil.dbginfo(TAG, "onCreate");
        super.onCreate();

        //  TODO: run when phone starts
        itsDropboxAcctMgr =
                DbxAccountManager.getInstance(getApplicationContext(),
                                              DROPBOX_SYNC_APP_KEY,
                                              DROPBOX_SYNC_APP_SECRET);
        updateDropboxFs();
    }


    /* (non-Javadoc)
     * @see android.app.Application#onTerminate()
     */
    @Override
    public void onTerminate()
    {
        PasswdSafeUtil.dbginfo(TAG, "onTerminate");
        super.onTerminate();
    }


    /** Start the process of linking to a Dropbox account */
    public void startDropboxLink(Activity act, int requestCode)
    {
        itsDropboxAcctMgr.startLink(act, requestCode);
    }


    /** Finish the process of linking a Dropbox account */
    public void finishDropboxLink()
    {
        updateDropboxFs();
    }


    /** Unlink the Dropbox account */
    public void unlinkDropbox()
    {
        itsDropboxAcctMgr.unlink();
        updateDropboxFs();
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


    /** Update the Dropbox filesystem after an account change */
    private void updateDropboxFs()
    {
        DbxAccount acct = itsDropboxAcctMgr.getLinkedAccount();
        if ((acct != null) && (itsDropboxFs == null)) {
            try {
                itsDropboxFs = DbxFileSystem.forAccount(acct);

                itsDropboxFs.addPathListener(new PathListener()
                {
                    @Override
                    public void onPathChange(DbxFileSystem fs,
                                             DbxPath path,
                                             Mode mode)
                    {
                        PasswdSafeUtil.dbginfo(TAG, "Dropbox path change");
                    }
                },
                new DbxPath("/"),
                PathListener.Mode.PATH_OR_DESCENDANT);
            } catch (DbxException e) {
                Log.e(TAG, "updateDropboxFs failure", e);
            }
        } else if ((acct == null) && (itsDropboxFs != null)) {
            itsDropboxFs.shutDown();
            itsDropboxFs = null;
        }
    }
}
