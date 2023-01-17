/*
 * Copyright (©) 2022 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;

import net.tjado.passwdsafe.lib.view.CursorRecyclerViewAdapter;

/**
 * The StorageFileListAdapter is a recycler view adapter for storage list files
 */
public final class StorageFileListAdapter
        extends CursorRecyclerViewAdapter<StorageFileListHolder>
{
    private final StorageFileListOps itsFileOps;

    /**
     * Constructor
     */
    public StorageFileListAdapter(StorageFileListOps ops)
    {
        itsFileOps = ops;
    }

    @Override
    protected void onBindViewHolder(StorageFileListHolder holder, Cursor item)
    {
        holder.updateView(item);
    }

    @NonNull
    @Override
    public StorageFileListHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                    int viewType)
    {
        View v = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.file_list_item, parent, false);
        return new StorageFileListHolder(v, itsFileOps);
    }
}
