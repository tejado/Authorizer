/*
 * Copyright (©) 2023 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import net.tjado.passwdsafe.db.BackupFile;
import net.tjado.passwdsafe.lib.Utils;

/**
 * A recycler view adapter for backup files
 */
public class BackupFilesAdapter
        extends ListAdapter<BackupFile, BackupFilesAdapter.ViewHolder>
{
    private SelectionTracker<Long> itsSelTracker;

    /**
     * Constructor
     */
    public BackupFilesAdapter()
    {
        super(new BackupFileDiff());
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.backup_file_list_item,
                                           parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position)
    {
        BackupFile backup = getItem(position);
        boolean selected = (itsSelTracker != null) &&
                           itsSelTracker.isSelected(backup.id);
        holder.bind(backup, selected);
    }

    @Override
    public long getItemId(int position)
    {
        return getItem(position).id;
    }

    /**
     * Create a selection lookup for a view
     */
    public ItemDetailsLookup<Long> createItemLookup(
            final RecyclerView recyclerView)
    {
        return new ItemDetailsLookup<>()
        {
            @Nullable
            @Override
            public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e)
            {
                View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if (view != null) {
                    return ((ViewHolder)recyclerView.getChildViewHolder(view))
                            .createItemDetails();
                }
                return null;
            }
        };
    }

    /**
     * Set the adapter's selection tracker
     */
    public void setSelectionTracker(SelectionTracker<Long> selTracker)
    {
        itsSelTracker = selTracker;
    }

    /**
     * View holder for displaying a backup file
     */
    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        private final TextView itsText;
        private final TextView itsModDate;

        /**
         * Constructor
         */
        protected ViewHolder(View view)
        {
            super(view);
            itsText = view.findViewById(R.id.text);
            itsModDate = view.findViewById(R.id.mod_date);
        }

        /**
         * Bind a backup file to the view
         */
        protected void bind(BackupFile backup, boolean selected)
        {
            itemView.setActivated(selected);

            itsText.setText(backup.title);
            itsText.requestLayout();

            Context ctx = itemView.getContext();
            StringBuilder details = new StringBuilder(
                    Utils.formatDate(backup.date, ctx));
            if (!backup.hasFile) {
                details.append(" (")
                       .append(ctx.getString(R.string.no_backup_file))
                       .append(")");
            }
            if (!backup.hasUriPerm) {
                details.append(" (")
                       .append(ctx.getString(R.string.no_permission))
                       .append(")");
            }
            itsModDate.setText(details);
        }

        /**
         * Create the item details for tracking selection
         */
        protected ItemDetailsLookup.ItemDetails<Long> createItemDetails()
        {
            return new ItemDetailsLookup.ItemDetails<>()
            {

                @Override
                public int getPosition()
                {
                    return getBindingAdapterPosition();
                }

                @Override
                public Long getSelectionKey()
                {
                    return getItemId();
                }
            };
        }
    }

    /**
     * BackupFile difference callbacks
     */
    private static class BackupFileDiff
            extends DiffUtil.ItemCallback<BackupFile>
    {

        @Override
        public boolean areItemsTheSame(@NonNull BackupFile oldItem,
                                       @NonNull BackupFile newItem)
        {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull BackupFile oldItem,
                                          @NonNull BackupFile newItem)
        {
            return oldItem.equals(newItem);
        }
    }
}
