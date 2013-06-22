/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableNotifiedException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Changes;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

/**
 *  The GDriveSyncer class syncs password files from Google Drive
 */
public class GDriveSyncer
{
    private static final String TAG = "GDriveSyncer";

    private static final String ABOUT_FIELDS = "largestChangeId";
    public static final String FILE_FIELDS =
            "id,title,mimeType,labels,fileExtension,modifiedDate,downloadUrl";

    private final Context itsContext;
    private final Account itsAccount;
    private final Drive itsDrive;
    private final String itsDriveToken;
    private final SyncDb itsSyncDb;

    /** Constructor */
    public GDriveSyncer(Context context,
                        ContentProviderClient provider,
                        Account account)
    {
        itsContext = context;
        itsAccount = account;
        Pair<Drive, String> driveInfo = getDriveService();
        itsDrive = driveInfo.first;
        itsDriveToken = driveInfo.second;
        itsSyncDb = new SyncDb(itsContext);
    }


    /** Add a provider for an account */
    public static long addProvider(String acctName, SQLiteDatabase db,
                                   Context ctx)
        throws SQLException
    {
        Log.i(TAG, "Add provider: " + acctName);
        int freq = ProviderSyncFreqPref.DEFAULT.getFreq();
        long id = SyncDb.addProvider(acctName, freq, db);

        GoogleAccountManager acctMgr = new GoogleAccountManager(ctx);
        Account acct = acctMgr.getAccountByName(acctName);
        if (acct != null) {
            ContentResolver.setSyncAutomatically(
                    acct, PasswdSafeContract.AUTHORITY, true);
            ContentResolver.addPeriodicSync(
                    acct, PasswdSafeContract.AUTHORITY, new Bundle(), freq);
            ContentResolver.requestSync(acct, PasswdSafeContract.AUTHORITY,
                                        new Bundle());
        }
        ctx.getContentResolver().notifyChange(
                PasswdSafeContract.Providers.CONTENT_URI, null);
        return id;
    }


    /** Delete the provider for the account */
    public static void deleteProvider(SyncDb.DbProvider provider,
                                      SQLiteDatabase db,
                                      Context ctx,
                                      GoogleAccountManager acctMgr)
        throws SQLException
    {
        List<SyncDb.DbFile> dbfiles = SyncDb.getFiles(provider.itsId, db);
        for (SyncDb.DbFile dbfile: dbfiles) {
            ctx.deleteFile(dbfile.itsLocalFile);
        }

        SyncDb.deleteProvider(provider.itsId, db);

        try {
            GoogleAccountCredential credential =
                    GoogleAccountCredential.usingOAuth2(ctx, DriveScopes.DRIVE);
            String token = GoogleAuthUtil.getToken(ctx, provider.itsAcct,
                                                   credential.getScope());
            PasswdSafeUtil.dbginfo(TAG, "Remove token for %s: %s",
                                   provider.itsAcct, token);
            if (token != null) {
                GoogleAuthUtil.invalidateToken(ctx, token);
            }
        } catch (Exception e) {
            PasswdSafeUtil.dbginfo(TAG, e, "No auth token for %s",
                                   provider.itsAcct);
        }

        if (acctMgr == null) {
            acctMgr = new GoogleAccountManager(ctx);
        }
        Account acct = acctMgr.getAccountByName(provider.itsAcct);
        if (acct != null) {
            ContentResolver.removePeriodicSync(acct,
                                               PasswdSafeContract.AUTHORITY,
                                               new Bundle());
            ContentResolver.setSyncAutomatically(acct,
                                                 PasswdSafeContract.AUTHORITY,
                                                 false);
        }
        ctx.getContentResolver().notifyChange(PasswdSafeContract.CONTENT_URI,
                                              null);
    }


    /** Update the sync frequency for a provider */
    public static void updateSyncFreq(SyncDb.DbProvider provider,
                                      int freq,
                                      SQLiteDatabase db,
                                      Context ctx)
            throws SQLException
    {
        SyncDb.updateProviderSyncFreq(provider.itsId, freq, db);

        GoogleAccountManager acctMgr = new GoogleAccountManager(ctx);
        Account acct = acctMgr.getAccountByName(provider.itsAcct);
        if (acct != null) {
            ContentResolver.removePeriodicSync(acct,
                                               PasswdSafeContract.AUTHORITY,
                                               new Bundle());
            if (freq > 0) {
                ContentResolver.addPeriodicSync(acct,
                                                PasswdSafeContract.AUTHORITY,
                                                new Bundle(), freq);
            }
        }
    }


    /** Validate the provider accounts */
    public static void validateAccounts(SQLiteDatabase db, Context ctx)
            throws SQLException
    {
        PasswdSafeUtil.dbginfo(TAG, "Validating accounts");
        GoogleAccountManager acctMgr = new GoogleAccountManager(ctx);

        List<SyncDb.DbProvider> providers = SyncDb.getProviders(db);
        for (SyncDb.DbProvider provider: providers) {
            Account acct = acctMgr.getAccountByName(provider.itsAcct);
            if (acct == null) {
                deleteProvider(provider, db, ctx, acctMgr);
            }
        }
        ctx.getContentResolver().notifyChange(PasswdSafeContract.CONTENT_URI,
                                              null);
    }

    /** Get the filename for a local file */
    public static String getLocalFileName(long fileId)
    {
        return "syncfile-" + Long.toString(fileId);
    }

    /** Close the syncer */
    public void close()
    {
        itsSyncDb.close();
    }


    /** Perform synchronization */
    public void performSync(boolean manual)
    {
        if (itsDrive == null) {
            return;
        }

        // TODO: sync log

        PasswdSafeUtil.dbginfo(TAG, "Performing sync for %s, manual: %b",
                               itsAccount.name, manual);
        SyncLogRecord logrec = new SyncLogRecord(itsAccount.name);

        try {
            SQLiteDatabase db = itsSyncDb.getDb();
            List<GDriveSyncOper> opers = null;
            try {
                db.beginTransaction();
                SyncDb.DbProvider provider = SyncDb.getProvider(itsAccount.name,
                                                                db);
                if (provider != null) {
                    long changeId = provider.itsSyncChange;
                    PasswdSafeUtil.dbginfo(TAG, "largest change %d", changeId);
                    Pair<Long, List<GDriveSyncOper>> syncrc;
                    if (changeId == -1) {
                        logrec.setFullSync(true);
                        syncrc = performFullSync(provider, db);
                    } else {
                        logrec.setFullSync(false);
                        syncrc = performSyncSince(provider, changeId, db);
                    }
                    long newChangeId = syncrc.first;
                    opers = syncrc.second;
                    if (changeId != newChangeId) {
                        SyncDb.updateProviderSyncChange(provider, newChangeId,
                                                        db);
                    }
                } else {
                    validateAccounts(db, itsContext);
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            if (opers != null) {
                for (GDriveSyncOper oper: opers) {
                    try {
                        logrec.addEntry(oper.getDescription(itsContext));
                        oper.doOper(itsDrive, itsContext);
                        try {
                            db.beginTransaction();
                            oper.doPostOperUpdate(db, itsContext);
                            db.setTransactionSuccessful();
                        } finally {
                            db.endTransaction();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Sync error for file " + oper.getFile(), e);
                        logrec.addFailure(e);
                    }
                }
            }

            itsContext.getContentResolver().notifyChange(
                     PasswdSafeContract.CONTENT_URI, null, false);
        } catch (UserRecoverableAuthIOException e) {
            PasswdSafeUtil.dbginfo(TAG, e, "Recoverable google auth error");
            GoogleAuthUtil.invalidateToken(itsContext, itsDriveToken);
        } catch (GoogleAuthIOException e) {
            Log.e(TAG, "Google auth error", e);
            logrec.addFailure(e);
            GoogleAuthUtil.invalidateToken(itsContext, itsDriveToken);
        } catch (Exception e) {
            Log.e(TAG, "Sync error", e);
            logrec.addFailure(e);
        }
        PasswdSafeUtil.dbginfo(TAG, "Sync finished for %s", itsAccount.name);
        logrec.setEndTime();

        SQLiteDatabase db = itsSyncDb.getDb();
        try {
            db.beginTransaction();
            String log = logrec.toString(itsContext);
            Log.i(TAG, log);
            SyncDb.deleteSyncLogs(
                    System.currentTimeMillis() - 2 * DateUtils.WEEK_IN_MILLIS,
                    db);
            SyncDb.addSyncLog(log, db);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Sync write log error", e);
        } finally {
            db.endTransaction();
        }
    }

    // TODO: moving file to different folder doesn't update modDate
    // TODO: show folders


    /** Perform a full sync of the files */
    private Pair<Long, List<GDriveSyncOper>>
    performFullSync(SyncDb.DbProvider provider, SQLiteDatabase db)
            throws SQLException, IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "Perform full sync");
        About about = itsDrive.about().get()
                .setFields(ABOUT_FIELDS).execute();
        long largestChangeId = about.getLargestChangeId();

        HashMap<String, File> allRemFiles = new HashMap<String, File>();
        Files.List request = itsDrive.files().list()
                .setQ("not trashed")
                .setFields("nextPageToken,items("+FILE_FIELDS+")");
        do {
            FileList files = request.execute();
            PasswdSafeUtil.dbginfo(TAG, "num files: %d",
                                   files.getItems().size());
            for (File file: files.getItems()) {
                if (!isSyncFile(file)) {
                    continue;
                }
                PasswdSafeUtil.dbginfo(TAG, "File id: %s, title: %s, mime: %s",
                                       file.getId(), file.getTitle(),
                                       file.getMimeType());
                allRemFiles.put(file.getId(), file);
            }
            request.setPageToken(files.getNextPageToken());
        } while((request.getPageToken() != null) &&
                (request.getPageToken().length() > 0));

        List<GDriveSyncOper> opers = performSync(allRemFiles, provider, db);
        return new Pair<Long, List<GDriveSyncOper>>(largestChangeId, opers);
    }


    /** Perform a sync of files since the given change id */
    private Pair<Long, List<GDriveSyncOper>>
    performSyncSince(SyncDb.DbProvider provider,
                     long changeId,
                     SQLiteDatabase db)
        throws SQLException, IOException
    {
        PasswdSafeUtil.dbginfo(TAG, "performSyncSince %d", changeId);
        HashMap<String, File> changedFiles = new HashMap<String, File>();
        Changes.List request =
            itsDrive.changes().list().setStartChangeId(changeId + 1)
            .setFields("largestChangeId,nextPageToken," +
                    "items(deleted,fileId,file("+FILE_FIELDS+"))");
        do {
            ChangeList changes = request.execute();
            long changesLargestId = changes.getLargestChangeId().longValue();

            for (Change change: changes.getItems()) {
                File file = change.getFile();
                if (change.getDeleted() || !isSyncFile(file)) {
                    file = null;
                }
                changedFiles.put(change.getFileId(), file);
                PasswdSafeUtil.dbginfo(TAG, "performSyncSince changed %s: %s",
                                       change.getFileId(), file);
            }

            if (changesLargestId > changeId) {
                changeId = changesLargestId;
            }
            request.setPageToken(changes.getNextPageToken());
        } while((request.getPageToken() != null) &&
                (request.getPageToken().length() > 0));

        List<GDriveSyncOper> opers = performSync(changedFiles, provider, db);
        return new Pair<Long, List<GDriveSyncOper>>(changeId, opers);
    }


    /** Perform a sync of the files */
    private List<GDriveSyncOper> performSync(HashMap<String, File> remfiles,
                                            SyncDb.DbProvider provider,
                                            SQLiteDatabase db)
            throws SQLException
    {
        HashMap<String, File> fileCache = new HashMap<String, File>(remfiles);
        List<SyncDb.DbFile> dbfiles = SyncDb.getFiles(provider.itsId, db);
        for (SyncDb.DbFile dbfile: dbfiles) {
            if (remfiles.containsKey(dbfile.itsRemoteId)) {
                File remfile = remfiles.get(dbfile.itsRemoteId);
                if (remfile != null) {
                    PasswdSafeUtil.dbginfo(TAG, "performSync update remote %s",
                                           dbfile.itsRemoteId);
                    SyncDb.updateRemoteFile(
                            dbfile.itsId, dbfile.itsRemoteId,
                            remfile.getTitle(),
                            remfile.getModifiedDate().getValue(), db);
                } else {
                    PasswdSafeUtil.dbginfo(TAG, "performSync remove remote %s",
                                           dbfile.itsRemoteId);
                    SyncDb.updateRemoteFileDeleted(dbfile.itsId, db);
                }
                remfiles.remove(dbfile.itsRemoteId);
            }
        }

        for (File remfile: remfiles.values()) {
            if (remfile == null) {
                continue;
            }
            String fileId = remfile.getId();
            PasswdSafeUtil.dbginfo(TAG, "performSync add remote %s", fileId);
            SyncDb.addRemoteFile(provider.itsId, fileId, remfile.getTitle(),
                                 remfile.getModifiedDate().getValue(), db);
        }

        List<GDriveSyncOper> opers = new ArrayList<GDriveSyncOper>();
        dbfiles = SyncDb.getFiles(provider.itsId, db);
        for (SyncDb.DbFile dbfile: dbfiles) {
            if (isRemoteNewer(dbfile)) {
                if (dbfile.itsIsRemoteDeleted) {
                    opers.add(new GDriveRmFileOper(dbfile));
                } else if (dbfile.itsIsLocalDeleted) {
                    PasswdSafeUtil.dbginfo(
                            TAG, "performSync recreate local removed %s",
                            dbfile);
                    opers.add(new GDriveRemoteToLocalOper(dbfile, fileCache));
                } else {
                    opers.add(new GDriveRemoteToLocalOper(dbfile, fileCache));
                }
            } else if (dbfile.itsLocalModDate > dbfile.itsRemoteModDate) {
                if (dbfile.itsIsLocalDeleted) {
                    opers.add(new GDriveRmFileOper(dbfile));
                } else if (dbfile.itsIsRemoteDeleted) {
                    PasswdSafeUtil.dbginfo(
                            TAG, "performSync recreate remote removed %s",
                            dbfile);
                    opers.add(new GDriveLocalToRemoteOper(dbfile, fileCache,
                                                          true));
                } else {
                    opers.add(new GDriveLocalToRemoteOper(dbfile, fileCache,
                                                          false));
                }
            } else if (dbfile.itsIsRemoteDeleted ||
                    dbfile.itsIsLocalDeleted) {
                opers.add(new GDriveRmFileOper(dbfile));
            }
        }
        return opers;
    }


    /** Is the remote file considered newer than the local */
    private boolean isRemoteNewer(SyncDb.DbFile dbfile)
    {
        if (dbfile.itsRemoteId == null) {
            return false;
        }
        if (dbfile.itsRemoteModDate > dbfile.itsLocalModDate) {
            return true;
        }
        if (TextUtils.isEmpty(dbfile.itsLocalFile)) {
            return true;
        }
        java.io.File localFile =
                itsContext.getFileStreamPath(dbfile.itsLocalFile);
        if (!localFile.exists()) {
            return true;
        }
        return false;
    }


    /** Should the file be synced */
    private static boolean isSyncFile(File file)
    {
        if (file.getLabels().getTrashed()) {
            return false;
        }
        String ext = file.getFileExtension();
        return (ext != null) && ext.equals("psafe3");
    }

    /**
     * Retrieve a authorized service object to send requests to the Google Drive
     * API. On failure to retrieve an access token, a notification is sent to
     * the user requesting that authorization be granted for the
     * {@code https://www.googleapis.com/auth/drive} scope.
     *
     * @return An authorized service object.
     */
    private Pair<Drive, String> getDriveService()
    {
        Drive drive = null;
        String token = null;
        try {
            GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(itsContext,
                                                    DriveScopes.DRIVE);
            credential.setSelectedAccountName(itsAccount.name);

            token = GoogleAuthUtil.getTokenWithNotification(
                itsContext, itsAccount.name, credential.getScope(),
                null, PasswdSafeContract.AUTHORITY, null);

            drive = new Drive.Builder(AndroidHttp.newCompatibleTransport(),
                                      new GsonFactory(), credential).build();
        } catch (UserRecoverableNotifiedException e) {
            // User notified
            PasswdSafeUtil.dbginfo(TAG, e, "User notified auth exception");
        } catch (GoogleAuthException e) {
            // Unrecoverable
            Log.e(TAG, "Unrecoverable auth exception", e);
        }
        catch (IOException e) {
            // Transient
            PasswdSafeUtil.dbginfo(TAG, e, "Transient error");
        } catch (Exception e) {
            Log.e(TAG, "Token exception", e);
        }
        return new Pair<Drive, String>(drive, token);
    }
}
