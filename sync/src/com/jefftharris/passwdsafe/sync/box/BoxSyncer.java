/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.box;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.box.boxjavalibv2.BoxClient;
import com.box.boxjavalibv2.dao.BoxCollection;
import com.box.boxjavalibv2.dao.BoxFile;
import com.box.boxjavalibv2.dao.BoxTypedObject;
import com.box.boxjavalibv2.dao.BoxUser;
import com.box.boxjavalibv2.requests.requestobjects.BoxDefaultRequestObject;
import com.box.boxjavalibv2.resourcemanagers.BoxSearchManager;
import com.box.boxjavalibv2.resourcemanagers.BoxUsersManager;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.sync.lib.AbstractProviderSyncer;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncOper;
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

    /** Perform a sync of the files */
    @Override
    protected List<AbstractSyncOper<BoxClient>> performSync()
            throws Exception
    {
        // Sync account display name
        BoxUsersManager userMgr = itsProviderClient.getUsersManager();
        BoxDefaultRequestObject userReq = new BoxDefaultRequestObject();
        BoxUser user = userMgr.getCurrentUser(userReq);
        String displayName = null;
        if (user != null) {
            displayName = user.getName() + " (" + user.getLogin() + ")";
        }
        if (!TextUtils.equals(itsProvider.itsDisplayName, displayName)) {
            SyncDb.updateProviderDisplayName(itsProvider.itsId, displayName,
                                             itsDb);
        }

        // Sync files
        BoxSearchManager searchMgr = itsProviderClient.getSearchManager();
        BoxDefaultRequestObject searchReq = new BoxDefaultRequestObject();
        searchReq.addField(BoxFile.FIELD_ID)
                 .addField(BoxFile.FIELD_TYPE)
                 .addField(BoxFile.FIELD_NAME)
                 .addField(BoxFile.FIELD_PATH_COLLECTION)
                 .addField(BoxFile.FIELD_MODIFIED_AT)
                 .addField(BoxFile.FIELD_ITEM_STATUS);
        TreeMap<String, BoxFile> boxfiles = new TreeMap<String, BoxFile>();
        int offset = 0;
        boolean hasMoreFiles = true;
        while (hasMoreFiles) {
            // TODO: bigger page
            searchReq.setPage(3, offset);
            BoxCollection files = searchMgr.search("*.psafe3", searchReq);
            List<BoxTypedObject> entries = files.getEntries();
            for (BoxTypedObject obj: entries) {
                PasswdSafeUtil.dbginfo(TAG, "file %s",
                                       BoxProvider.boxToString(
                                           obj, itsProviderClient));
                if (obj instanceof BoxFile) {
                    boxfiles.put(obj.getId(), (BoxFile)obj);
                }
            }
            offset += entries.size();
            hasMoreFiles =
                    (offset < files.getTotalCount()) && !entries.isEmpty();
        }

        List<AbstractSyncOper<BoxClient>> opers =
                new ArrayList<AbstractSyncOper<BoxClient>>();
        return opers;
    }
}
