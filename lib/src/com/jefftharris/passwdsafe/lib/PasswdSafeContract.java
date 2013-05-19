/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.lib;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * The PasswdSafeContract class is the contract for the PasswdSafe client and
 * sync providers
 */
public final class PasswdSafeContract
{
    /** The sync provider's authority */
    public static final String AUTHORITY =
        "com.jefftharris.passwdsafe.sync.provider";

    /** The base URI for the provider */
    public static final Uri CONTENT_URI =
        Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + AUTHORITY);

    /** The table of providers */
    public static final class Providers implements BaseColumns
    {
        public static final String TABLE = "providers";
        public static final Uri CONTENT_URI =
                Uri.withAppendedPath(PasswdSafeContract.CONTENT_URI, TABLE);
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd." + AUTHORITY + "." + TABLE;

        public static final String COL_TYPE = "type";
        public static final String COL_ACCT = "acct";
        public static final String COL_SYNC_FREQ = "sync_freq";

        public static final String[] PROJECTION = {
            Providers._ID,
            Providers.COL_TYPE,
            Providers.COL_ACCT,
            Providers.COL_SYNC_FREQ
        };

        public static final int PROJECTION_IDX_ID = 0;
        public static final int PROJECTION_IDX_TYPE = 1;
        public static final int PROJECTION_IDX_ACCT = 2;
        public static final int PROJECTION_IDX_SYNC_FREQ = 3;

        /** The type of provider */
        public enum Type
        {
            GDRIVE;

            /** Set the ImageView to the icon of the provider type */
            public void setIcon(ImageView iv)
            {
                switch (this) {
                case GDRIVE: {
                    iv.setImageResource(R.drawable.google_drive);
                    break;
                }
                }
            }

            /** Set the TextView to the name of the provider type */
            public void setText(TextView tv)
            {
                switch (this) {
                case GDRIVE: {
                    tv.setText(getName(tv.getContext()));
                    break;
                }
                }
            }

            /** Get the name of the provider */
            public String getName(Context context)
            {
                switch (this) {
                case GDRIVE: {
                    return context.getString(R.string.google_drive);
                }
                }
                return null;
            }
        }
    }

    /** The table of files */
    public static final class Files implements BaseColumns
    {
        public static final String TABLE = "files";
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd." + AUTHORITY + "." + TABLE;

        public static final String COL_PROVIDER = "provider";
        public static final String COL_TITLE = "title";
        public static final String COL_MOD_DATE = "mod_date";
        public static final String COL_FILE = "file";

        public static final String TITLE_SORT_ORDER = "title ASC";

        public static final String[] PROJECTION = {
            Files._ID,
            Files.COL_PROVIDER,
            Files.COL_TITLE,
            Files.COL_MOD_DATE,
            Files.COL_FILE
        };

        public static final int PROJECTION_IDX_ID = 0;
        public static final int PROJECTION_IDX_PROVIDER = 1;
        public static final int PROJECTION_IDX_TITLE = 2;
        public static final int PROJECTION_IDX_MOD_DATE = 3;
        public static final int PROJECTION_IDX_FILE = 4;
    }

    /** The client provider's authority */
    public static final String CLIENT_AUTHORITY =
            "com.jefftharris.passwdsafe.client.provider";

    /** The base URI for the client provider */
    public static final Uri CLIENT_CONTENT_URI =
        Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + CLIENT_AUTHORITY);

    /** The client files */
    public static final class ClientFiles
    {
        public static final String TABLE = "files";
    }
}

