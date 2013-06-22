/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.TextView;

import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.Utils;
import com.jefftharris.passwdsafe.lib.view.PasswdCursorLoader;

/**
 * Fragment to show the sync logs
 */
public class SyncLogsFragment extends ListFragment
{
    private static final int LOADER_LOGS = 0;

    private SimpleCursorAdapter itsLogsAdapter;


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        // TODO: Show log fields better
        // TODO: Option to hide successful logs
        itsLogsAdapter = new SimpleCursorAdapter(
               getActivity(), android.R.layout.simple_list_item_2, null,
               new String[] { PasswdSafeContract.SyncLogs.COL_DATE,
                              PasswdSafeContract.SyncLogs.COL_LOG },
               new int[] { android.R.id.text1, android.R.id.text2 }, 0);

        itsLogsAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder()
        {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int colIdx)
            {
                if (colIdx ==
                        PasswdSafeContract.SyncLogs.PROJECTION_IDX_DATE) {
                    long modDate = cursor.getLong(colIdx);
                    TextView tv = (TextView)view;
                    tv.setText(Utils.formatDate(modDate, getActivity()));
                    return true;
                }
                return false;
            }
        });

        setListAdapter(itsLogsAdapter);

        LoaderManager lm = getLoaderManager();
        lm.initLoader(LOADER_LOGS, null, new LoaderCallbacks<Cursor>()
        {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args)
            {
                return new PasswdCursorLoader(
                        getActivity(),
                        PasswdSafeContract.SyncLogs.CONTENT_URI,
                        PasswdSafeContract.SyncLogs.PROJECTION,
                        null, null,
                        PasswdSafeContract.SyncLogs.DATE_SORT_ORDER);

            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
            {
                if (PasswdCursorLoader.checkResult(loader)) {
                    itsLogsAdapter.swapCursor(cursor);
                }
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader)
            {
                if (PasswdCursorLoader.checkResult(loader)) {
                    itsLogsAdapter.swapCursor(null);
                }
            }
        });
    }
}
