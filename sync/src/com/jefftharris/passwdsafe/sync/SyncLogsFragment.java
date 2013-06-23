/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

import java.util.Locale;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
    private static final String STATE_SHOW_ALL = "showAll";

    private boolean itsIsShowAll = false;
    private SimpleCursorAdapter itsLogsAdapter;
    private LoaderCallbacks<Cursor> itsLogsCbs;


    /* (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            itsIsShowAll = savedInstanceState.getBoolean(STATE_SHOW_ALL, false);
        } else {
            itsIsShowAll = false;
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        itsLogsAdapter = new SimpleCursorAdapter(
               getActivity(), android.R.layout.simple_list_item_2, null,
               new String[] { PasswdSafeContract.SyncLogs.COL_START,
                              PasswdSafeContract.SyncLogs.COL_LOG },
               new int[] { android.R.id.text1, android.R.id.text2 }, 0);

        itsLogsAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder()
        {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int colIdx)
            {
                switch (colIdx) {
                case PasswdSafeContract.SyncLogs.PROJECTION_IDX_START: {
                    long start = cursor.getLong(
                            PasswdSafeContract.SyncLogs.PROJECTION_IDX_START);
                    long end = cursor.getLong(
                            PasswdSafeContract.SyncLogs.PROJECTION_IDX_END);
                    String acct = cursor.getString(
                            PasswdSafeContract.SyncLogs.PROJECTION_IDX_ACCT);
                    TextView tv = (TextView)view;

                    String str =
                        String.format(Locale.US, "%s (%ds) - %s",
                                      Utils.formatDate(start, getActivity()),
                                      (end - start) / 1000,
                                      acct);
                    tv.setText(str);
                    return true;
                }
                case PasswdSafeContract.SyncLogs.PROJECTION_IDX_LOG: {
                    int flags = cursor.getInt(
                            PasswdSafeContract.SyncLogs.PROJECTION_IDX_FLAGS);
                    String log = cursor.getString(
                            PasswdSafeContract.SyncLogs.PROJECTION_IDX_LOG);

                    StringBuilder str = new StringBuilder();
                    if ((flags &
                            PasswdSafeContract.SyncLogs.FLAGS_IS_FULL) != 0) {
                        str.append(getString(R.string.full_sync));
                    } else {
                        str.append(getString(R.string.incremental_sync));
                    }

                    str.append(", ");
                    if ((flags &
                            PasswdSafeContract.SyncLogs.FLAGS_IS_MANUAL) != 0) {
                        str.append(getString(R.string.manual));
                    } else {
                        str.append(getString(R.string.automatic));
                    }

                    if (log.length() != 0) {
                        str.append("\n");
                    }
                    str.append(log);
                    TextView tv = (TextView)view;
                    tv.setText(str.toString());
                    return true;
                }
                }
                return false;
            }
        });

        setListAdapter(itsLogsAdapter);

        itsLogsCbs = new LoaderCallbacks<Cursor>()
        {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args)
            {
                String selection = itsIsShowAll ? null :
                    PasswdSafeContract.SyncLogs.DEFAULT_SELECTION;
                return new PasswdCursorLoader(
                        getActivity(),
                        PasswdSafeContract.SyncLogs.CONTENT_URI,
                        PasswdSafeContract.SyncLogs.PROJECTION,
                        selection, null,
                        PasswdSafeContract.SyncLogs.START_SORT_ORDER);
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
        };
        LoaderManager lm = getLoaderManager();
        lm.initLoader(LOADER_LOGS, null, itsLogsCbs);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_SHOW_ALL, itsIsShowAll);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onCreateOptionsMenu(android.view.Menu, android.view.MenuInflater)
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.fragment_sync_logs, menu);
        super.onCreateOptionsMenu(menu, inflater);

        MenuItem item = menu.findItem(R.id.menu_show_all);
        item.setChecked(itsIsShowAll);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_show_all: {
            itsIsShowAll = !item.isChecked();
            item.setChecked(itsIsShowAll);
            LoaderManager lm = getLoaderManager();
            lm.restartLoader(LOADER_LOGS, null, itsLogsCbs);
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }
}
