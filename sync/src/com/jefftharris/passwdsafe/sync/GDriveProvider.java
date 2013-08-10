/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.accounts.Account;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
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
 * The GDriveProvider class encapsulates Google Drive
 */
public class GDriveProvider extends Provider
{
    // TODO: moving file to different folder doesn't update modDate

    public static final String ABOUT_FIELDS = "largestChangeId";
    public static final String FILE_FIELDS =
            "id,title,mimeType,labels,fileExtension,modifiedDate,downloadUrl";

    private static final String TAG = "GDriveProvider";

    private final Context itsContext;


    /** Constructor */
    public GDriveProvider(Context ctx)
    {
        itsContext = ctx;
    }


    /** Get the account for the named provider */
    @Override
    public Account getAccount(String acctName)
    {
        GoogleAccountManager acctMgr = new GoogleAccountManager(itsContext);
        return acctMgr.getAccountByName(acctName);
    }


    /** Cleanup a provider when deleted */
    @Override
    public void cleanupOnDelete(String acctName)
    {
        try {
            GoogleAccountCredential credential = getAcctCredential(itsContext);
            String token = GoogleAuthUtil.getToken(itsContext, acctName,
                                                   credential.getScope());
            PasswdSafeUtil.dbginfo(TAG, "Remove token for %s", acctName);
            if (token != null) {
                GoogleAuthUtil.invalidateToken(itsContext, token);
            }
        } catch (Exception e) {
            PasswdSafeUtil.dbginfo(TAG, e, "No auth token for %s", acctName);
        }
    }


    /** Sync a provider */
    @Override
    public void sync(Account acct, SyncDb.DbProvider provider,
                     SQLiteDatabase db,
                     boolean manual, SyncLogRecord logrec) throws Exception
    {
        new Syncer(acct, provider, db, logrec, itsContext).sync();
    }


    /** Insert a local file */
    @Override
    public long insertLocalFile(long providerId, String title,
                                SQLiteDatabase db)
            throws Exception
    {
        return SyncDb.addLocalFile(providerId, title,
                                   System.currentTimeMillis(), db);
    }


    /** Update a local file */
    @Override
    public void updateLocalFile(SyncDb.DbFile file,
                                String localFileName,
                                java.io.File localFile,
                                SQLiteDatabase db)
            throws Exception
    {
        SyncDb.updateLocalFile(file.itsId, localFileName,
                               file.itsLocalTitle,
                               localFile.lastModified(), db);
    }


    /** Delete a local file */
    @Override
    public void deleteLocalFile(SyncDb.DbFile file, SQLiteDatabase db)
    {
        SyncDb.updateLocalFileDeleted(file.itsId, db);
    }


    /** Get the Google account credential */
    private static GoogleAccountCredential getAcctCredential(Context ctx)
    {
        return GoogleAccountCredential.usingOAuth2(
                ctx, Collections.singletonList(DriveScopes.DRIVE));
    }


    /** The Syncer class encapsulates a sync operation */
    private static class Syncer
    {
        private final Drive itsDrive;
        private final String itsDriveToken;
        private final SyncDb.DbProvider itsProvider;
        private final SQLiteDatabase itsDb;
        private final SyncLogRecord itsLogrec;
        private final Context itsContext;


        /** Constructor */
        public Syncer(Account acct, SyncDb.DbProvider provider,
                      SQLiteDatabase db, SyncLogRecord logrec, Context ctx)
        {
            Pair<Drive, String> drive = getDriveService(acct, ctx);
            itsDrive = drive.first;
            itsDriveToken = drive.second;
            itsProvider = provider;
            itsDb = db;
            itsLogrec = logrec;
            itsContext = ctx;
        }


        /** Sync the provider */
        public final void sync() throws Exception
        {
            if (itsDrive == null) {
                return;
            }

            try {
                List<GDriveSyncOper> opers = null;
                try {
                    itsDb.beginTransaction();
                    long changeId = itsProvider.itsSyncChange;
                    PasswdSafeUtil.dbginfo(TAG, "largest change %d", changeId);
                    Pair<Long, List<GDriveSyncOper>> syncrc;
                    if (changeId == -1) {
                        itsLogrec.setFullSync(true);
                        syncrc = performFullSync();
                    } else {
                        itsLogrec.setFullSync(false);
                        syncrc = performSyncSince(changeId);
                    }
                    long newChangeId = syncrc.first;
                    opers = syncrc.second;
                    if (changeId != newChangeId) {
                        SyncDb.updateProviderSyncChange(itsProvider,
                                                        newChangeId, itsDb);
                    }
                    itsDb.setTransactionSuccessful();
                } finally {
                    itsDb.endTransaction();
                }

                if (opers != null) {
                    for (GDriveSyncOper oper: opers) {
                        try {
                            itsLogrec.addEntry(oper.getDescription(itsContext));
                            oper.doOper(itsDrive, itsContext);
                            try {
                                itsDb.beginTransaction();
                                oper.doPostOperUpdate(itsDb, itsContext);
                                itsDb.setTransactionSuccessful();
                            } finally {
                                itsDb.endTransaction();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Sync error for file " + oper.getFile(),
                                  e);
                            itsLogrec.addFailure(e);
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
                GoogleAuthUtil.invalidateToken(itsContext, itsDriveToken);
                throw e;
            }
        }


        /** Perform a full sync of the files */
        private final Pair<Long, List<GDriveSyncOper>> performFullSync()
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
                    PasswdSafeUtil.dbginfo(TAG,
                                           "File id: %s, title: %s, mime: %s",
                                           file.getId(), file.getTitle(),
                                           file.getMimeType());
                    allRemFiles.put(file.getId(), file);
                }
                request.setPageToken(files.getNextPageToken());
            } while((request.getPageToken() != null) &&
                    (request.getPageToken().length() > 0));

            List<GDriveSyncOper> opers = performSync(allRemFiles);
            return new Pair<Long, List<GDriveSyncOper>>(largestChangeId, opers);
        }


        /** Perform a sync of files since the given change id */
        private final Pair<Long, List<GDriveSyncOper>>
        performSyncSince(long changeId)
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
                long changesLargestId =
                        changes.getLargestChangeId().longValue();

                for (Change change: changes.getItems()) {
                    File file = change.getFile();
                    if (change.getDeleted() || !isSyncFile(file)) {
                        file = null;
                    }
                    changedFiles.put(change.getFileId(), file);
                    PasswdSafeUtil.dbginfo(TAG,
                                           "performSyncSince changed %s: %s",
                                           change.getFileId(), file);
                }

                if (changesLargestId > changeId) {
                    changeId = changesLargestId;
                }
                request.setPageToken(changes.getNextPageToken());
            } while((request.getPageToken() != null) &&
                    (request.getPageToken().length() > 0));

            List<GDriveSyncOper> opers = performSync(changedFiles);
            return new Pair<Long, List<GDriveSyncOper>>(changeId, opers);
        }


        /** Perform a sync of the files */
        private final List<GDriveSyncOper>
        performSync(HashMap<String, File> remfiles)
                throws SQLException
        {
            HashMap<String, File> fileCache =
                    new HashMap<String, File>(remfiles);
            List<SyncDb.DbFile> dbfiles = SyncDb.getFiles(itsProvider.itsId,
                                                          itsDb);
            for (SyncDb.DbFile dbfile: dbfiles) {
                if (remfiles.containsKey(dbfile.itsRemoteId)) {
                    File remfile = remfiles.get(dbfile.itsRemoteId);
                    if (remfile != null) {
                        PasswdSafeUtil.dbginfo(TAG,
                                               "performSync update remote %s",
                                               dbfile.itsRemoteId);
                        SyncDb.updateRemoteFile(
                                dbfile.itsId, dbfile.itsRemoteId,
                                remfile.getTitle(),
                                remfile.getModifiedDate().getValue(), itsDb);
                    } else {
                        PasswdSafeUtil.dbginfo(TAG,
                                               "performSync remove remote %s",
                                               dbfile.itsRemoteId);
                        SyncDb.updateRemoteFileDeleted(dbfile.itsId, itsDb);
                    }
                    remfiles.remove(dbfile.itsRemoteId);
                }
            }

            for (File remfile: remfiles.values()) {
                if (remfile == null) {
                    continue;
                }
                String fileId = remfile.getId();
                PasswdSafeUtil.dbginfo(TAG, "performSync add remote %s",
                                       fileId);
                SyncDb.addRemoteFile(itsProvider.itsId, fileId,
                                     remfile.getTitle(),
                                     remfile.getModifiedDate().getValue(),
                                     itsDb);
            }

            List<GDriveSyncOper> opers = new ArrayList<GDriveSyncOper>();
            dbfiles = SyncDb.getFiles(itsProvider.itsId, itsDb);
            for (SyncDb.DbFile dbfile: dbfiles) {
                if (isRemoteNewer(dbfile)) {
                    if (dbfile.itsIsRemoteDeleted) {
                        opers.add(new GDriveRmFileOper(dbfile));
                    } else if (dbfile.itsIsLocalDeleted) {
                        PasswdSafeUtil.dbginfo(
                                TAG, "performSync recreate local removed %s",
                                dbfile);
                        opers.add(new GDriveRemoteToLocalOper(dbfile,
                                                              fileCache));
                    } else {
                        opers.add(new GDriveRemoteToLocalOper(dbfile,
                                                              fileCache));
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
        private final boolean isRemoteNewer(SyncDb.DbFile dbfile)
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
         * Retrieve a authorized service object to send requests to the Google
         * Drive API. On failure to retrieve an access token, a notification is
         * sent to the user requesting that authorization be granted for the
         * {@code https://www.googleapis.com/auth/drive} scope.
         *
         * @return An authorized service object and its auth token.
         */
        private static Pair<Drive, String> getDriveService(Account acct,
                                                           Context ctx)
        {
            Drive drive = null;
            String token = null;
            try {
                GoogleAccountCredential credential = getAcctCredential(ctx);
                credential.setSelectedAccountName(acct.name);

                token = GoogleAuthUtil.getTokenWithNotification(
                    ctx, acct.name, credential.getScope(),
                    null, PasswdSafeContract.AUTHORITY, null);

                Drive.Builder builder =
                        new Drive.Builder(AndroidHttp.newCompatibleTransport(),
                                          new GsonFactory(), credential);
                builder.setApplicationName(ctx.getString(R.string.app_name));
                drive = builder.build();
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
}
