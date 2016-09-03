/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.dropbox;

import android.content.Context;
import android.util.Pair;

import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.sync.ProviderFactory;
import com.jefftharris.passwdsafe.sync.lib.AbstractSyncedFilesActivity;
import com.jefftharris.passwdsafe.sync.lib.ProviderRemoteFile;

import java.util.List;

/**
 *  Activity for managing files synced from Dropbox
 */
public class DropboxFilesActivity extends AbstractSyncedFilesActivity
{
    /** Constructor */
    public DropboxFilesActivity()
    {
        super(ProviderType.DROPBOX, "");
    }


    /** Create a list files task */
    @Override
    protected AbstractListFilesTask createListFilesTask(
            Context ctx,
            AbstractListFilesTask.Callback cb)
    {
        return new ListFilesTask(ctx, cb);
    }


    /** Background task for listing files from Dropbox */
    private static class ListFilesTask extends AbstractListFilesTask
    {
        private final DropboxCoreProvider itsProvider;

        /** Constructor */
        public ListFilesTask(Context ctx, Callback cb)
        {
            super(ctx, cb);
            itsProvider = (DropboxCoreProvider)
                    ProviderFactory.getProvider(ProviderType.DROPBOX, ctx);
        }

        /**
         * Override this method to perform a computation on a background thread.
         * The specified parameters are the parameters passed to
         * {@link #execute} by the caller of this task. */
        @Override
        protected Pair<List<ProviderRemoteFile>, Exception> doInBackground(
                String... params)
        {
            try {
                return Pair.create(itsProvider.listFiles(params[0]), null);
            } catch (Exception e) {
                return Pair.create(null, e);
            }
        }
    }
}
