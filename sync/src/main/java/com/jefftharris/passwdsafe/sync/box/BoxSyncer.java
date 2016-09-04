/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.box;

import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.box.androidsdk.content.BoxApiFolder;
import com.box.androidsdk.content.BoxApiSearch;
import com.box.androidsdk.content.BoxApiUser;
import com.box.androidsdk.content.BoxConstants;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxError;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxJsonObject;
import com.box.androidsdk.content.models.BoxListItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxUser;
import com.box.androidsdk.content.requests.BoxRequestsFolder;
import com.box.androidsdk.content.requests.BoxRequestsSearch;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.lib.AbstractLocalToRemoteSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer;
import com.jefftharris.passwdsafe.sync.lib.AbstractRemoteToLocalSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractRmSyncOper;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper;
import com.jefftharris.passwdsafe.sync.lib.DbFile;
import com.jefftharris.passwdsafe.sync.lib.DbProvider;
import com.jefftharris.passwdsafe.sync.lib.ProviderRemoteFile;
import com.jefftharris.passwdsafe.sync.lib.SyncDb;
import com.jefftharris.passwdsafe.sync.lib.SyncLogRecord;
import com.jefftharris.passwdsafe.sync.lib.SyncRemoteFiles;

/**
 * The BoxSyncer class encapsulates a Box sync operation
 */
public class BoxSyncer extends AbstractProviderSyncer<BoxSession>
{
    private static final String[] FILE_FIELDS = new String[] {
            BoxFile.FIELD_ID, BoxFile.FIELD_TYPE, BoxFile.FIELD_NAME,
            BoxFile.FIELD_PATH_COLLECTION, BoxFile.FIELD_MODIFIED_AT,
            BoxFile.FIELD_ITEM_STATUS, BoxFile.FIELD_SIZE,
            BoxFile.FIELD_SHA1 };

    private static final String TAG = "BoxSyncer";

    /** Constructor */
    public BoxSyncer(BoxSession client, DbProvider provider,
                     SQLiteDatabase db, SyncLogRecord logrec, Context ctx)
    {
        super(client, provider, db, logrec, ctx, TAG);
    }

    /**
     * Get the user for the session
     */
    public static BoxUser getUser(BoxSession client) throws Exception
    {
        BoxApiUser userApi = new BoxApiUser(client);
        return userApi.getCurrentUserInfoRequest().send();
    }

    /** Get the folder for a file */
    public static String getFileFolder(BoxItem file)
    {
        StringBuilder folderStr = new StringBuilder();
        for (BoxFolder folder: file.getPathCollection()) {
            if (folderStr.length() > 0) {
                folderStr.append("/");
            }
            folderStr.append(folder.getName());
        }
        return folderStr.toString();
    }

    /** Perform a sync of the files */
    @Override
    protected List<AbstractSyncOper<BoxSession>> performSync()
            throws Exception
    {
        syncDisplayName();
        updateDbFiles(getBoxFiles());
        return resolveSyncOpers();
    }

    @Override
    protected AbstractLocalToRemoteSyncOper<BoxSession>
    createLocalToRemoteOper(DbFile dbfile)
    {
        return new BoxLocalToRemoteOper(dbfile);
    }

    @Override
    protected AbstractRemoteToLocalSyncOper<BoxSession>
    createRemoteToLocalOper(DbFile dbfile)
    {
        return new BoxRemoteToLocalOper(dbfile);
    }

    @Override
    protected AbstractRmSyncOper<BoxSession>
    createRmFileOper(DbFile dbfile)
    {
        return new BoxRmFileOper(dbfile);
    }


    /** Update an exception thrown during syncing */
    @Override
    protected Exception updateSyncException(Exception e)
    {
        if (e instanceof BoxException) {
            BoxException boxExcept = (BoxException)e;
            // Massage server exceptions to get the error
            BoxError serverError = boxExcept.getAsBoxError();
            if (serverError != null) {
                String msg = boxExcept.getMessage();
                if (TextUtils.isEmpty(msg)) {
                    msg = "Box server error";
                }
                e = new Exception(msg + ": " + boxToString(serverError), e);
            }
        }
        return e;
    }

    /** Sync account display name */
    private void syncDisplayName()
    {
        BoxUser user = itsProviderClient.getUser();
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
    private SyncRemoteFiles getBoxFiles()
            throws BoxException
    {
        BoxApiFolder folderApi = new BoxApiFolder(itsProviderClient);
        SyncRemoteFiles boxfiles = new SyncRemoteFiles();

        // Get root files
        retrieveBoxFolderFiles(BoxConstants.ROOT_FOLDER_ID, FILE_FIELDS,
                               folderApi, boxfiles);

        for (DbFile dbfile: SyncDb.getFiles(itsProvider.itsId, itsDb)) {
            if (dbfile.itsRemoteId != null) {
                continue;
            }

            for (ProviderRemoteFile remfile: boxfiles.getRemoteFiles()) {
                if (TextUtils.equals(dbfile.itsLocalTitle,
                                     remfile.getTitle())) {
                    boxfiles.addRemoteFileForNew(dbfile.itsId, remfile);
                }
            }
        }

        // Get files in folders matching 'passwdsafe' search
        BoxApiSearch searchApi = new BoxApiSearch(itsProviderClient);
        BoxRequestsSearch.Search searchReq =
                searchApi.getSearchRequest("passwdsafe");

        int offset = 0;
        boolean hasMoreFiles = true;
        while (hasMoreFiles) {
            searchReq.setLimit(100);
            searchReq.setOffset(0);
            BoxListItems items = searchReq.send();
            for (BoxItem item: items) {
                PasswdSafeUtil.dbginfo(TAG, "search item %s",
                                       boxToString(item));
                if (item instanceof BoxFolder) {
                    retrieveBoxFolderFiles(item.getId(), FILE_FIELDS, folderApi,
                                           boxfiles);
                }
            }
            offset += items.size();
            hasMoreFiles = (offset < items.fullSize()) && !items.isEmpty();
        }

        return boxfiles;
    }

    /** Retrieve the files in the given folder */
    @SuppressWarnings("SameParameterValue")
    private void retrieveBoxFolderFiles(
            String folderId,
            String[] fileFields,
            BoxApiFolder folderApi,
            SyncRemoteFiles boxfiles)
            throws BoxException
    {
        BoxRequestsFolder.GetFolderItems req =
                folderApi.getItemsRequest(folderId);
        req.setFields(fileFields);

        int offset = 0;
        boolean hasMoreItems = true;
        while (hasMoreItems) {
            req.setLimit(100);
            req.setOffset(0);
            BoxListItems items = req.send();
            for (BoxItem item: items) {
                PasswdSafeUtil.dbginfo(TAG, "item %s", boxToString(item));
                if (item instanceof BoxFile) {
                    BoxFile file = (BoxFile)item;
                    if (file.getName().endsWith(".psafe3")) {
                        boxfiles.addRemoteFile(new BoxProviderFile(file));
                    }
                }
            }
            offset += items.size();
            hasMoreItems = (offset < items.fullSize()) && !items.isEmpty();
        }
    }


    /** Convert a Box object to a string for debugging */
    private String boxToString(BoxJsonObject obj)
    {
        return BoxProvider.boxToString(obj);
    }
}
