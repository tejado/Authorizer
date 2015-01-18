/*
 * Copyright (©) 2014-2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.box;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.box.boxjavalibv2.BoxClient;
import com.box.boxjavalibv2.dao.BoxCollection;
import com.box.boxjavalibv2.dao.BoxFile;
import com.box.boxjavalibv2.dao.BoxFolder;
import com.box.boxjavalibv2.dao.BoxItem;
import com.box.boxjavalibv2.dao.BoxObject;
import com.box.boxjavalibv2.dao.BoxServerError;
import com.box.boxjavalibv2.dao.BoxTypedObject;
import com.box.boxjavalibv2.dao.BoxUser;
import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.box.boxjavalibv2.exceptions.BoxServerException;
import com.box.boxjavalibv2.requests.requestobjects.BoxDefaultRequestObject;
import com.box.boxjavalibv2.requests.requestobjects.BoxFolderRequestObject;
import com.box.boxjavalibv2.resourcemanagers.BoxFoldersManager;
import com.box.boxjavalibv2.resourcemanagers.BoxSearchManager;
import com.box.boxjavalibv2.resourcemanagers.BoxUsersManager;
import com.box.restclientv2.exceptions.BoxRestException;
import com.box.restclientv2.exceptions.BoxSDKException;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.R;
import com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;

/**
 * The BoxSyncer class encapsulates a Box sync operation
 */
public class BoxSyncer extends AbstractProviderSyncer<BoxClient>
{
    public static final String ROOT_FOLDER = "0";

    private static final String TAG = "BoxSyncer";

    // TODO refactor to use base class sync opers

    /** Constructor */
    public BoxSyncer(BoxClient client, DbProvider provider,
                     SQLiteDatabase db, SyncLogRecord logrec, Context ctx)
    {
        super(client, provider, db, logrec, ctx, TAG);
    }

    /** Get the folder for a file */
    public static String getFileFolder(BoxItem file)
    {
        StringBuilder folderStr = new StringBuilder();
        for (BoxTypedObject folder: file.getPathCollection().getEntries()) {
            if (folderStr.length() > 0) {
                folderStr.append("/");
            }
            folderStr.append(((BoxItem)folder).getName());
        }
        return folderStr.toString();
    }

    /** Perform a sync of the files */
    @Override
    protected List<AbstractSyncOper<BoxClient>> performSync()
            throws Exception
    {
        syncDisplayName();

        // Sync files
        TreeMap<String, BoxFile> boxfiles = getBoxFiles();
        List<DbFile> dbfiles = SyncDb.getFiles(itsProvider.itsId, itsDb);
        for (DbFile dbfile: dbfiles) {
            if ((dbfile.itsRemoteId == null) ||
                    (dbfile.itsLocalChange == DbFile.FileChange.ADDED)) {
                continue;
            }
            BoxFile boxfile = boxfiles.get(dbfile.itsRemoteId);
            if (boxfile != null) {
                checkRemoteFileChange(dbfile, boxfile);
                boxfiles.remove(dbfile.itsRemoteId);
            } else {
                PasswdSafeUtil.dbginfo(TAG, "performSync remove remote %s",
                                       dbfile.itsRemoteId);
                SyncDb.updateRemoteFileDeleted(dbfile.itsId, itsDb);
            }
        }

        for (Map.Entry<String, BoxFile> entry: boxfiles.entrySet()) {
            String fileId = entry.getKey();
            BoxFile boxfile = entry.getValue();
            PasswdSafeUtil.dbginfo(TAG, "performSync add remote %s", fileId);
            SyncDb.addRemoteFile(itsProvider.itsId, fileId,
                                 boxfile.getName(), getFileFolder(boxfile),
                                 boxfile.dateModifiedAt().getTime(),
                                 boxfile.getSha1(), itsDb);
        }

        List<AbstractSyncOper<BoxClient>> opers =
                new ArrayList<AbstractSyncOper<BoxClient>>();
        dbfiles = SyncDb.getFiles(itsProvider.itsId, itsDb);
        for (DbFile dbfile: dbfiles) {
            resolveSyncOper(dbfile, opers);
        }
        return opers;
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer#createLocalToRemoteOper(com.jefftharris.passwdsafe.sync.lib.DbFile)
     */
    @Override
    protected AbstractSyncOper<BoxClient>
    createLocalToRemoteOper(DbFile dbfile)
    {
        return new BoxLocalToRemoteOper(dbfile);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer#createRemoteToLocalOper(com.jefftharris.passwdsafe.sync.lib.DbFile)
     */
    @Override
    protected AbstractSyncOper<BoxClient>
    createRemoteToLocalOper(DbFile dbfile)
    {
        return new BoxRemoteToLocalOper(dbfile);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer#createRmFileOper(com.jefftharris.passwdsafe.sync.lib.DbFile)
     */
    @Override
    protected AbstractSyncOper<BoxClient>
    createRmFileOper(DbFile dbfile)
    {
        return new BoxRmFileOper(dbfile);
    }


    /** Check for a remote file change and update */
    private final void checkRemoteFileChange(DbFile dbfile, BoxFile remfile)
    {
        String remTitle = remfile.getName();
        String remFolder = getFileFolder(remfile);
        long remModDate = remfile.dateModifiedAt().getTime();
        String remHash = remfile.getSha1();
        boolean changed = true;
        do {
            if (!TextUtils.equals(dbfile.itsRemoteTitle, remTitle) ||
                    !TextUtils.equals(dbfile.itsRemoteFolder, remFolder) ||
                    (dbfile.itsRemoteModDate != remModDate) ||
                    !TextUtils.equals(dbfile.itsRemoteHash, remHash) ||
                    TextUtils.isEmpty(dbfile.itsLocalFile)) {
                break;
            }

            java.io.File localFile =
                    itsContext.getFileStreamPath(dbfile.itsLocalFile);
            if (!localFile.exists()) {
                break;
            }

            changed = false;
        } while(false);

        if (!changed) {
            return;
        }

        PasswdSafeUtil.dbginfo(TAG, "performSync update remote %s", dbfile);
        SyncDb.updateRemoteFile(dbfile.itsId, dbfile.itsRemoteId,
                                remTitle, remFolder, remModDate, remHash,
                                itsDb);
        switch (dbfile.itsRemoteChange) {
        case NO_CHANGE:
        case REMOVED: {
            SyncDb.updateRemoteFileChange(dbfile.itsId,
                                          DbFile.FileChange.MODIFIED, itsDb);
            break;
        }
        case ADDED:
        case MODIFIED: {
            break;
        }
        }
    }


    /** Resolve the sync operations for a file */
    private final void resolveSyncOper(DbFile dbfile,
                                       List<AbstractSyncOper<BoxClient>> opers)
            throws SQLException
    {
        if ((dbfile.itsLocalChange != DbFile.FileChange.NO_CHANGE) ||
                (dbfile.itsRemoteChange != DbFile.FileChange.NO_CHANGE)) {
            PasswdSafeUtil.dbginfo(TAG, "resolveSyncOper %s", dbfile);
        }

        switch (dbfile.itsLocalChange) {
        case ADDED: {
            switch (dbfile.itsRemoteChange) {
            case ADDED:
            case MODIFIED: {
                logConflictFile(dbfile, true);
                splitConflictedFile(dbfile, opers);
                break;
            }
            case NO_CHANGE: {
                opers.add(new BoxLocalToRemoteOper(dbfile));
                break;
            }
            case REMOVED: {
                logConflictFile(dbfile, true);
                recreateRemoteRemovedFile(dbfile, opers);
                break;
            }
            }
            break;
        }
        case MODIFIED: {
            switch (dbfile.itsRemoteChange) {
            case ADDED:
            case MODIFIED: {
                logConflictFile(dbfile, true);
                splitConflictedFile(dbfile, opers);
                break;
            }
            case NO_CHANGE: {
                opers.add(new BoxLocalToRemoteOper(dbfile));
                break;
            }
            case REMOVED: {
                logConflictFile(dbfile, true);
                recreateRemoteRemovedFile(dbfile, opers);
                break;
            }
            }
            break;
        }
        case NO_CHANGE: {
            switch (dbfile.itsRemoteChange) {
            case ADDED:
            case MODIFIED: {
                opers.add(new BoxRemoteToLocalOper(dbfile));
                break;
            }
            case NO_CHANGE: {
                // Nothing
                break;
            }
            case REMOVED: {
                opers.add(new BoxRmFileOper(dbfile));
                break;
            }
            }
            break;
        }
        case REMOVED: {
            switch (dbfile.itsRemoteChange) {
            case ADDED:
            case MODIFIED: {
                logConflictFile(dbfile, false);
                DbFile newRemfile = splitRemoteToNewFile(dbfile);
                DbFile updatedLocalFile = SyncDb.getFile(dbfile.itsId, itsDb);

                opers.add(new BoxRemoteToLocalOper(newRemfile));
                opers.add(new BoxRmFileOper(updatedLocalFile));
                break;
            }
            case NO_CHANGE:
            case REMOVED: {
                opers.add(new BoxRmFileOper(dbfile));
                break;
            }
            }
            break;
        }
        }
    }


    /** Split the file.  A new added remote file is created with the remote id,
     * and the file is updated to resemble a new local file with the same id but
     * a different name indicating a conflict
     */
    private final void splitConflictedFile
    (
            DbFile dbfile,
            List<AbstractSyncOper<BoxClient>> opers
    )
            throws SQLException
    {
        DbFile newRemfile = splitRemoteToNewFile(dbfile);
        DbFile updatedLocalFile = updateFileAsLocallyAdded(
                dbfile, itsContext.getString(R.string.conflicted_local_copy));

        opers.add(new BoxRemoteToLocalOper(newRemfile));
        opers.add(new BoxLocalToRemoteOper(updatedLocalFile));
    }


    /** Recreate a remotely deleted file from local updates */
    private final void recreateRemoteRemovedFile
    (
            DbFile dbfile,
            List<AbstractSyncOper<BoxClient>> opers
    )
            throws SQLException
    {
        resetRemoteFields(dbfile);
        DbFile updatedLocalFile = updateFileAsLocallyAdded(
                dbfile, itsContext.getString(R.string.recreated_local_copy));
        opers.add(new BoxLocalToRemoteOper(updatedLocalFile));
    }


    /** Update an exception thrown during syncing */
    @Override
    protected Exception updateSyncException(Exception e)
    {
        if (e instanceof BoxServerException) {
            BoxServerException boxExcept = (BoxServerException)e;
            // Massage server exceptions to get the error
            BoxServerError serverError = boxExcept.getError();
            if (serverError != null) {
                String msg = boxExcept.getCustomMessage();
                if (TextUtils.isEmpty(msg)) {
                    msg = "Box server error";
                }
                e = new Exception(msg + ": " + boxToString(serverError), e);
            }
        }
        return e;
    }

    /** Sync account display name */
    private final void syncDisplayName()
            throws BoxRestException, BoxServerException,
                   AuthFatalFailureException
    {
        BoxUsersManager userMgr = itsProviderClient.getUsersManager();
        BoxDefaultRequestObject userReq = new BoxDefaultRequestObject();
        userReq.addField(BoxUser.FIELD_ID)
               .addField(BoxUser.FIELD_NAME)
               .addField(BoxUser.FIELD_LOGIN);
        BoxUser user = userMgr.getCurrentUser(userReq);
        PasswdSafeUtil.dbginfo(TAG, "user %s", boxToString(user));
        String displayName = null;
        if (user != null) {
            displayName = user.getName() + " (" + user.getLogin() + ")";
        }
        if (!TextUtils.equals(itsProvider.itsDisplayName, displayName)) {
            SyncDb.updateProviderDisplayName(itsProvider.itsId, displayName,
                                             itsDb);
        }
    }

    /** Get the files from Box */
    private final TreeMap<String, BoxFile> getBoxFiles()
            throws BoxSDKException
    {
        BoxFoldersManager folderMgr = itsProviderClient.getFoldersManager();
        BoxFolderRequestObject folderReq =
                BoxFolderRequestObject.getFolderItemsRequestObject(100, 0);
        folderReq.addField(BoxFile.FIELD_ID)
                 .addField(BoxFile.FIELD_TYPE)
                 .addField(BoxFile.FIELD_NAME)
                 .addField(BoxFile.FIELD_PATH_COLLECTION)
                 .addField(BoxFile.FIELD_MODIFIED_AT)
                 .addField(BoxFile.FIELD_ITEM_STATUS)
                 .addField(BoxFile.FIELD_SIZE)
                 .addField(BoxFile.FIELD_SHA1);

        TreeMap<String, BoxFile> boxfiles = new TreeMap<String, BoxFile>();

        // Get root files
        retrieveBoxFolderFiles(ROOT_FOLDER, folderMgr, folderReq, boxfiles);

        // Get files in folders matching 'passwdsafe' search
        BoxSearchManager searchMgr = itsProviderClient.getSearchManager();
        BoxDefaultRequestObject searchReq = new BoxDefaultRequestObject();
        searchReq.addField(BoxFolder.FIELD_ID)
                 .addField(BoxFolder.FIELD_TYPE)
                 .addField(BoxFolder.FIELD_NAME);
        int offset = 0;
        boolean hasMoreFiles = true;
        while (hasMoreFiles) {
            searchReq.setPage(100, offset);
            BoxCollection items = searchMgr.search("passwdsafe", searchReq);
            List<BoxTypedObject> entries = items.getEntries();
            for (BoxTypedObject obj: entries) {
                PasswdSafeUtil.dbginfo(TAG, "search item %s", boxToString(obj));
                if (obj instanceof BoxFolder) {
                    retrieveBoxFolderFiles(obj.getId(), folderMgr, folderReq,
                                           boxfiles);
                }
            }
            offset += entries.size();
            hasMoreFiles =
                    (offset < items.getTotalCount()) && !entries.isEmpty();
        }

        return boxfiles;
    }

    /** Retrieve the files in the given folder */
    private final void retrieveBoxFolderFiles(String folderId,
                                              BoxFoldersManager folderMgr,
                                              BoxFolderRequestObject folderReq,
                                              TreeMap<String, BoxFile> boxfiles)
            throws BoxSDKException
    {
        int offset = 0;
        boolean hasMoreItems = true;
        while (hasMoreItems) {
            folderReq.setPage(100, offset);
            BoxCollection items = folderMgr.getFolderItems(folderId, folderReq);
            List<BoxTypedObject> entries = items.getEntries();
            for (BoxTypedObject obj: entries) {
                PasswdSafeUtil.dbginfo(TAG, "item %s", boxToString(obj));
                if (obj instanceof BoxFile) {
                    BoxFile file = (BoxFile)obj;
                    if (file.getName().endsWith(".psafe3")) {
                        boxfiles.put(file.getId(), file);
                    }
                }
            }
            offset += entries.size();
            hasMoreItems =
                    (offset < items.getTotalCount()) && !entries.isEmpty();
        }
    }


    /** Convert a Box object to a string for debugging */
    private final String boxToString(BoxObject obj)
    {
        return BoxProvider.boxToString(obj, itsProviderClient);
    }
}
