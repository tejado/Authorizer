/*
 * Copyright (©) 2013-2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib;

import android.content.Context;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * The type of provider
 */
public enum ProviderType
{
    GDRIVE,
    DROPBOX,
    BOX,
    ONEDRIVE,
    OWNCLOUD;

    /** Set the ImageView to the icon of the provider type */
    public void setIcon(ImageView iv)
    {
        iv.setImageResource(getIconId(false));
    }

    /** Set the ImageView to the icon of the provider type */
    public void setIcon(MenuItem item)
    {
        item.setIcon(getIconId(true));
    }

    /**
     * Get the icon resource id for the provider type
     */
    public int getIconId(boolean forMenu)
    {
        switch (this) {
        case GDRIVE: {
            return R.drawable.google_drive;
        }
        case DROPBOX: {
            /*if (forMenu) {
                return R.drawable.dropbox_trace;
            }*/
            return R.drawable.dropbox;
        }
        case BOX: {
            return R.drawable.box;
        }
        case ONEDRIVE: {
            return R.drawable.onedrive;
        }
        case OWNCLOUD: {
            return R.drawable.owncloud;
        }
        }
        return 0;
    }

    /** Set the TextView to the name of the provider type */
    public void setText(TextView tv)
    {
        tv.setText(getName(tv.getContext()));
    }

    /** Get the name of the provider */
    public String getName(Context context)
    {
        switch (this) {
        case GDRIVE: {
            return context.getString(R.string.google_drive);
        }
        case DROPBOX: {
            return context.getString(R.string.dropbox);
        }
        case BOX: {
            return context.getString(R.string.box);
        }
        case ONEDRIVE: {
            return context.getString(R.string.onedrive);
        }
        case OWNCLOUD: {
            return context.getString(R.string.owncloud);
        }
        }
        return null;
    }

    /** Convert the string name to the ProviderType */
    public static ProviderType fromString(String name)
    {
        try {
            return ProviderType.valueOf(name);
        } catch (Exception e) {
            return null;
        }
    }
}

