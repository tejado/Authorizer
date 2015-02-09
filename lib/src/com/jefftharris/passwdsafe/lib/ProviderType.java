/*
 * Copyright (©) 2013-2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.lib;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * The type of provider
 */
public enum ProviderType
{
    GDRIVE,
    GDRIVE_PLAY,
    DROPBOX,
    BOX,
    OWNCLOUD;

    /** Set the ImageView to the icon of the provider type */
    public void setIcon(ImageView iv)
    {
        switch (this) {
        case GDRIVE:
        case GDRIVE_PLAY: {
            iv.setImageResource(R.drawable.google_drive);
            break;
        }
        case DROPBOX: {
            iv.setImageResource(R.drawable.dropbox);
            break;
        }
        case BOX: {
            iv.setImageResource(R.drawable.box);
            break;
        }
        case OWNCLOUD: {
            iv.setImageResource(R.drawable.owncloud);
            break;
        }
        }
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
        case GDRIVE:
        case GDRIVE_PLAY: {
            return context.getString(R.string.google_drive);
        }
        case DROPBOX: {
            return context.getString(R.string.dropbox);
        }
        case BOX: {
            return context.getString(R.string.box);
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
