/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
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
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.box.boxjavalibv2.BoxClient;
import com.box.boxjavalibv2.dao.BoxCollection;
import com.box.boxjavalibv2.dao.BoxFile;
import com.box.boxjavalibv2.dao.BoxItem;
import com.box.boxjavalibv2.dao.BoxObject;
import com.box.boxjavalibv2.dao.BoxServerError;
import com.box.boxjavalibv2.dao.BoxTypedObject;
import com.box.boxjavalibv2.dao.BoxUser;
import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.box.boxjavalibv2.exceptions.BoxServerException;
import com.box.boxjavalibv2.requests.requestobjects.BoxDefaultRequestObject;
import com.box.boxjavalibv2.resourcemanagers.BoxSearchManager;
import com.box.boxjavalibv2.resourcemanagers.BoxUsersManager;
import com.box.restclientv2.exceptions.BoxRestException;
import com.box.restclientv2.exceptions.BoxSDKException;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
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
    private static final String TAG = "BoxSyncer";

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
        try {
            return doPerformSync();
        } catch (BoxServerException e) {
            // Massage server exceptions to get the error
            BoxServerError serverError = e.getError();
            if (serverError != null) {
                String msg = e.getCustomMessage();
                if (TextUtils.isEmpty(msg)) {
                    msg = "Box server error";
                }
                throw new Exception(msg + ": " + boxToString(serverError), e);
            } else {
                throw e;
            }
        }
    }

    /** Delegate method to perform a sync with raw Box errors */
    private final List<AbstractSyncOper<BoxClient>> doPerformSync()
            throws BoxSDKException
    {
        syncDisplayName();

        // Sync files
        TreeMap<String, BoxFile> boxfiles = getBoxFiles();
        TreeMap<String, BoxFile> allboxfiles =
                new TreeMap<String, BoxFile>(boxfiles);
        List<DbFile> dbfiles = SyncDb.getFiles(itsProvider.itsId, itsDb);
        for (DbFile dbfile: dbfiles) {
            if (dbfile.itsRemoteId == null) {
                continue;
            }
            BoxFile boxfile = boxfiles.get(dbfile.itsRemoteId);
            if (boxfile != null) {
                PasswdSafeUtil.dbginfo(TAG, "performSync update remote %s",
                                       dbfile.itsRemoteId);
                String folder = getFileFolder(boxfile);
                SyncDb.updateRemoteFile(dbfile.itsId, dbfile.itsRemoteId,
                                        boxfile.getName(), folder,
                                        boxfile.dateModifiedAt().getTime(),
                                        itsDb);
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
                                 boxfile.dateModifiedAt().getTime(), itsDb);
        }

        List<AbstractSyncOper<BoxClient>> opers =
                new ArrayList<AbstractSyncOper<BoxClient>>();
        dbfiles = SyncDb.getFiles(itsProvider.itsId, itsDb);
        for (DbFile dbfile: dbfiles) {
            if (dbfile.itsIsRemoteDeleted || dbfile.itsIsLocalDeleted) {
                opers.add(new BoxRmFileOper(dbfile));
            } else if (isRemoteNewer(dbfile, allboxfiles)) {
                opers.add(new BoxRemoteToLocalOper(dbfile));
            } else if (isLocalNewer(dbfile, allboxfiles)) {
                // TODO: handle local mod and remote deleted like gdrive?
                opers.add(new BoxLocalToRemoteOper(dbfile));
            }
        }
        return opers;
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
            throws BoxRestException, BoxServerException,
                   AuthFatalFailureException
    {
        BoxSearchManager searchMgr = itsProviderClient.getSearchManager();
        BoxDefaultRequestObject searchReq = new BoxDefaultRequestObject();
        searchReq.addField(BoxFile.FIELD_ID)
                 .addField(BoxFile.FIELD_TYPE)
                 .addField(BoxFile.FIELD_NAME)
                 .addField(BoxFile.FIELD_PATH_COLLECTION)
                 .addField(BoxFile.FIELD_MODIFIED_AT)
                 .addField(BoxFile.FIELD_ITEM_STATUS)
                 .addField(BoxFile.FIELD_SIZE);
        TreeMap<String, BoxFile> boxfiles = new TreeMap<String, BoxFile>();
        int offset = 0;
        boolean hasMoreFiles = true;
        while (hasMoreFiles) {
            // TODO: bigger page
            // TODO: use search?? Can take a while for Box to update

            // TODO: handle box website delete where confirm box is still
            // visible and cause the total count to include the extra item
            // but a request failure
            searchReq.setPage(10, offset);
            BoxCollection files = searchMgr.search("*.psafe3", searchReq);
            PasswdSafeUtil.dbginfo(TAG, "total count %d",
                                   files.getTotalCount());
            List<BoxTypedObject> entries = files.getEntries();
            for (BoxTypedObject obj: entries) {
                PasswdSafeUtil.dbginfo(TAG, "file %s", boxToString(obj));
                if (obj instanceof BoxFile) {
                    boxfiles.put(obj.getId(), (BoxFile)obj);
                }
            }
            offset += entries.size();
            hasMoreFiles =
                    (offset < files.getTotalCount()) && !entries.isEmpty();
        }
        return boxfiles;
    }

    /** Is the remote file newer than the local */
    private final boolean isRemoteNewer(DbFile dbfile,
                                        Map<String, BoxFile> boxfiles)
    {
        if (dbfile.itsRemoteId == null) {
            return false;
        }
        if (dbfile.itsRemoteModDate > dbfile.itsLocalModDate) {
            return true;
        }
        if (!TextUtils.equals(dbfile.itsLocalFolder, dbfile.itsRemoteFolder)) {
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

        BoxFile boxfile = boxfiles.get(dbfile.itsRemoteId);
        if (boxfile == null) {
            return true;
        }

        // TODO: sha1 checksum, etag?
        return false;
    }

    /** Is the local file newer than the remote */
    private final boolean isLocalNewer(DbFile dbfile,
                                       Map<String, BoxFile> boxfiles)
    {
        if (dbfile.itsLocalModDate > dbfile.itsRemoteModDate) {
            return true;
        }
        return false;
    }

    /** Convert a Box object to a string for debugging */
    private final String boxToString(BoxObject obj)
    {
        return BoxProvider.boxToString(obj, itsProviderClient);
    }
}
