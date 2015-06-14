/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.onedrive;

import android.content.Context;
import android.util.Pair;

import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.sync.ProviderFactory;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncedFilesActivity;
import com.jefftharris.passwdsafe.sync.lib.ProviderRemoteFile;
import com.microsoft.onedriveaccess.IOneDriveService;
import com.microsoft.onedriveaccess.model.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *  Activity for managing files synced from OneDrive
 */
public class OnedriveFilesActivity extends AbstractSyncedFilesActivity
{
    private final IOneDriveService itsService;

    private static final Map<String, String> QUERY_OPTIONS =
            Collections.singletonMap("expand", "children");

    /**
     * Constructor
     */
    public OnedriveFilesActivity()
    {
        super(ProviderType.ONEDRIVE);
        OnedriveProvider provider = (OnedriveProvider)
                ProviderFactory.getProvider(ProviderType.ONEDRIVE, this);
        itsService = provider.getOnedriveService();
    }

    /**
     * Create a list files task
     */
    @Override
    protected AbstractListFilesTask createListFilesTask(
            Context ctx,
            AbstractListFilesTask.Callback cb)
    {
        return new ListFilesTask(itsService, ctx, cb);
    }


    /** Background task for listing files from OneDrive */
    private static class ListFilesTask extends AbstractListFilesTask
    {
        private final IOneDriveService itsService;


        /** Constructor */
        public ListFilesTask(IOneDriveService service, Context ctx, Callback cb)
        {
            super(ctx, cb);
            itsService = service;
        }


        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
         */
        @Override
        protected Pair<List<ProviderRemoteFile>, Exception>
        doInBackground(String... params)
        {
            List<ProviderRemoteFile> files = new ArrayList<>();
            Pair<List<ProviderRemoteFile>, Exception> result =
                    Pair.create(files, (Exception)null);
            if (itsService == null) {
                return result;
            }

            try {
                Item item = itsService.getItemByPath(params[0], QUERY_OPTIONS);
                for (Item child: item.Children) {
                    files.add(new OnedriveProviderFile(child));
                }
            } catch (Exception e) {
                result = Pair.create(null, e);
            }
            return result;
        }
    }
}
