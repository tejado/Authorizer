/*
 * Copyright (©) 2018 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib.view;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.DataSetObserver;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * RecyclerView adapter using a cursor
 */
public abstract class CursorRecyclerViewAdapter
        <ViewHolder extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<ViewHolder>
{
    private Cursor itsCursor;
    private boolean itsIsValid = false;
    private final DataSetObserver itsObserver = new Observer();

    private static final int ID_COLUMN = 0;

    /**
     * Constructor
     */
    protected CursorRecyclerViewAdapter()
    {
        setHasStableIds(true);
        changeCursor(null);
    }

    /**
     * Change the cursor used by the adapter
     */
    @SuppressLint("NotifyDataSetChanged")
    public void changeCursor(Cursor cursor)
    {
        if (cursor != itsCursor) {
            Cursor old = itsCursor;
            if (old != null) {
                old.unregisterDataSetObserver(itsObserver);
            }

            itsCursor = cursor;
            if (itsCursor != null) {
                itsIsValid = true;
                itsCursor.registerDataSetObserver(itsObserver);
            } else {
                itsIsValid = false;
            }
            notifyDataSetChanged();

            if (old != null) {
                old.close();
            }
        }
    }

    @Override
    public int getItemCount()
    {
        if (itsIsValid && (itsCursor != null)) {
            return itsCursor.getCount();
        }
        return 0;
    }

    @Override
    public long getItemId(int position)
    {
        if (itsIsValid &&
            (itsCursor != null) &&
            itsCursor.moveToPosition(position)) {
            return itsCursor.getLong(ID_COLUMN);
        }
        return -1;
    }

    @Override
    public final void onBindViewHolder(@NonNull ViewHolder holder,
                                       int position)
    {
        if (itsIsValid && (itsCursor.moveToPosition(position))) {
            onBindViewHolder(holder, itsCursor);
        }
    }

    /**
     * Update the ViewHolder based on the item at the cursor
     */
    protected abstract void onBindViewHolder(ViewHolder holder, Cursor item);

    /**
     * Cursor data set observer
     */
    private class Observer extends DataSetObserver
    {
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onChanged()
        {
            super.onChanged();
            itsIsValid = true;
            notifyDataSetChanged();
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onInvalidated()
        {
            super.onInvalidated();
            itsIsValid = false;
            notifyDataSetChanged();
        }
    }
}
