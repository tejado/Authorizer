/*
 * Copyright (©) 2014-2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.box;

import java.util.HashMap;
import java.util.List;

import android.content.Context;
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
import com.jefftharris.passwdsafe.sync.lib.AbstractLocalToRemoteSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer;
import com.jefftharris.passwdsafe.sync.lib.AbstractRemoteToLocalSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractRmSyncOper;
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
        HashMap<String, ProviderRemoteFile> owncloudFiles = getBoxFiles();
        updateDbFiles(owncloudFiles);
        return resolveSyncOpers();
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer#createLocalToRemoteOper(com.jefftharris.passwdsafe.sync.lib.DbFile)
     */
    @Override
    protected AbstractLocalToRemoteSyncOper<BoxClient>
    createLocalToRemoteOper(DbFile dbfile)
    {
        return new BoxLocalToRemoteOper(dbfile);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer#createRemoteToLocalOper(com.jefftharris.passwdsafe.sync.lib.DbFile)
     */
    @Override
    protected AbstractRemoteToLocalSyncOper<BoxClient>
    createRemoteToLocalOper(DbFile dbfile)
    {
        return new BoxRemoteToLocalOper(dbfile);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer#createRmFileOper(com.jefftharris.passwdsafe.sync.lib.DbFile)
     */
    @Override
    protected AbstractRmSyncOper<BoxClient>
    createRmFileOper(DbFile dbfile)
    {
        return new BoxRmFileOper(dbfile);
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
    private final HashMap<String, ProviderRemoteFile> getBoxFiles()
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

        HashMap<String, ProviderRemoteFile> boxfiles =
                new HashMap<String, ProviderRemoteFile>();

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
    private final void retrieveBoxFolderFiles(
            String folderId,
            BoxFoldersManager folderMgr,
            BoxFolderRequestObject folderReq,
            HashMap<String, ProviderRemoteFile> boxfiles)
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
                        BoxProviderFile provFile = new BoxProviderFile(file);
                        boxfiles.put(provFile.getRemoteId(), provFile);
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
