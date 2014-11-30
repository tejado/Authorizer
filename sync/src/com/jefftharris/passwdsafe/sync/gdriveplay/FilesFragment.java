/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.gdriveplay;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.ProviderType;
import com.jefftharris.passwdsafe.lib.Utils;
import com.jefftharris.passwdsafe.lib.view.PasswdCursorLoader;
import com.jefftharris.passwdsafe.sync.R;

/**
 *  Fragment to show GDrive files
 */
public class FilesFragment extends ListFragment
{
    /** Listener interface for the owning activity */
    public interface Listener
    {
    }

    private static final int LOADER_TITLE = 0;
    private static final int LOADER_FILES = 1;

    private Uri itsProviderUri;
    private Uri itsFilesUri;
    private SimpleCursorAdapter itsProviderAdapter;
    private Listener itsListener;

    /** Create a new instance of the fragment */
    public static FilesFragment newInstance(Uri providerUri)
    {
        FilesFragment frag = new FilesFragment();
        Bundle args = new Bundle();
        args.putParcelable("providerUri", providerUri);
        frag.setArguments(args);
        return frag;
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onAttach(android.app.Activity)
     */
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        itsListener = (Listener)activity;
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        itsProviderUri = getArguments().getParcelable("providerUri");
        if (itsProviderUri != null) {
            itsFilesUri = itsProviderUri.buildUpon().appendPath(
                    PasswdSafeContract.Files.TABLE).build();
        } else {
            itsFilesUri = null;
        }
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_gdrive_play_files,
                                container, false);
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        itsProviderAdapter = new SimpleCursorAdapter(
               getActivity(), R.layout.sync_provider_file_list_item, null,
               new String[] { PasswdSafeContract.Files.COL_TITLE,
                              PasswdSafeContract.Files.COL_MOD_DATE,
                              PasswdSafeContract.Files.COL_FOLDER },
               new int[] { R.id.title, R.id.mod_date, R.id.folder },
               0);

        itsProviderAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder()
        {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int colIdx)
            {
                switch (colIdx) {
                case PasswdSafeContract.Files.PROJECTION_IDX_MOD_DATE: {
                    long modDate = cursor.getLong(colIdx);
                    TextView tv = (TextView)view;
                    tv.setText(Utils.formatDate(modDate, getActivity()));
                    return true;
                }
                case PasswdSafeContract.Files.PROJECTION_IDX_FOLDER: {
                    String folder = cursor.getString(colIdx);
                    if (TextUtils.isEmpty(folder)) {
                        view.setVisibility(View.GONE);
                    } else {
                        view.setVisibility(View.VISIBLE);
                        ((TextView)view).setText(folder);
                    }
                    return true;
                }
                }
                return false;
            }
        });

        setListAdapter(itsProviderAdapter);

        LoaderManager lm = getLoaderManager();
        lm.initLoader(LOADER_TITLE, null, new LoaderCallbacks<Cursor>()
            {
                @Override
                public Loader<Cursor> onCreateLoader(int id, Bundle args)
                {
                    return new PasswdCursorLoader(
                            getActivity(), itsProviderUri,
                            PasswdSafeContract.Providers.PROJECTION,
                            null, null, null);
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
                {
                    if (PasswdCursorLoader.checkResult(loader)) {
                        updateProvider(cursor);
                    }
                }

                @Override
                public void onLoaderReset(Loader<Cursor> loader)
                {
                    if (PasswdCursorLoader.checkResult(loader)) {
                        updateProvider(null);
                    }
                }

                private void updateProvider(Cursor cursor)
                {
                    View view = getView();
                    if (view == null) {
                        return;
                    }
                    String str;
                    if ((cursor != null) && cursor.moveToFirst()) {
                        str = PasswdSafeContract.Providers.getDisplayName(
                                cursor);
                    } else {
                        str = getString(R.string.no_account);
                    }
                    TextView tv = (TextView)view.findViewById(R.id.title);
                    tv.setText(str);
                }
            });

        lm.initLoader(LOADER_FILES, null, new LoaderCallbacks<Cursor>()
            {
                 @Override
                 public Loader<Cursor> onCreateLoader(int id, Bundle args)
                 {
                     return new PasswdCursorLoader(
                             getActivity(), itsFilesUri,
                             PasswdSafeContract.Files.PROJECTION,
                             PasswdSafeContract.Files.NOT_DELETED_SELECTION,
                             null, PasswdSafeContract.Files.TITLE_SORT_ORDER);
                 }

                 @Override
                 public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
                 {
                     if (PasswdCursorLoader.checkResult(loader)) {
                         itsProviderAdapter.swapCursor(cursor);
                     }
                 }

                 @Override
                 public void onLoaderReset(Loader<Cursor> loader)
                 {
                     if (PasswdCursorLoader.checkResult(loader)) {
                         itsProviderAdapter.swapCursor(null);
                     }
                 }
            });
    }


}
