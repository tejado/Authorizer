/*
 * Copyright (©) 2022 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.database.Cursor;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import net.tjado.passwdsafe.db.RecentFile;
import net.tjado.passwdsafe.lib.Utils;

/**
 * The StorageFileListHolder is a recycler view holder for storage list files
 */
public final class StorageFileListHolder
        extends RecyclerView.ViewHolder
        implements View.OnClickListener
{
    private final StorageFileListOps itsOps;
    private final TextView itsText;
    private final TextView itsModDate;
    private String itsUri;
    private String itsTitle;

    /**
     * Constructor
     */
    public StorageFileListHolder(View view, StorageFileListOps ops)
    {
        super(view);
        itsOps = ops;
        itsText = view.findViewById(R.id.text);
        itsModDate = view.findViewById(R.id.mod_date);

        ImageView icon = view.findViewById(R.id.icon);
        icon.setImageResource(ops.getStorageFileIcon());

        view.setOnClickListener(this);
    }

    /**
     * Update the view for a file item
     */
    public void updateView(Cursor item)
    {
        int titleIdx = item.getColumnIndex(RecentFile.COL_TITLE);
        int dateIdx = item.getColumnIndex(RecentFile.COL_DATE);
        int uriIdx = item.getColumnIndex(RecentFile.COL_URI);
        itsTitle = item.getString(titleIdx);
        long date = item.getLong(dateIdx);
        itsUri = item.getString(uriIdx);

        itsText.setText(itsTitle);
        itsText.requestLayout();

        itsModDate.setText(Utils.formatDate(date, itemView.getContext()));
    }

    @Override
    public void onClick(View v)
    {
        itsOps.storageFileClicked(itsUri, itsTitle);
    }

    /**
     * Get the URI for the file
     */
    public String getUri()
    {
        return itsUri;
    }
}
