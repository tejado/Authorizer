/*
 * Copyright (©) 2023 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.db;

import android.provider.BaseColumns;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Saved password database entry
 */
@Entity(tableName = SavedPassword.TABLE,
        indices = {@Index(value = {SavedPassword.COL_URI}, unique = true)})
public class SavedPassword
{
    public static final String TABLE = "saved_passwords";
    public static final String COL_ID = BaseColumns._ID;
    public static final String COL_URI = "uri";
    public static final String COL_PROVIDER_URI = "provider_uri";
    public static final String COL_DISPLAY_NAME = "display_name";
    public static final String COL_IV = "iv";
    public static final String COL_ENC_PASSWD = "enc_passwd";

    /**
     * Unique id for the record.  The field is a Long for migration as the
     * field is not marked NOT NULL
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = COL_ID)
    public final Long id;

    /**
     * URI of the file
     */
    @ColumnInfo(name = COL_URI)
    @NonNull
    public final String uri;

    /**
     * The URI of the provider of the file (without path, etc.)
     */
    @ColumnInfo(name = COL_PROVIDER_URI)
    @NonNull
    public final String providerUri;

    /**
     * Display name of the file in the provider
     */
    @ColumnInfo(name = COL_DISPLAY_NAME)
    @NonNull
    public final String displayName;

    /**
     * Encryption initialization vector
     */
    @ColumnInfo(name = COL_IV)
    @NonNull
    public final String iv;

    /**
     * Encrypted password
     */
    @ColumnInfo(name = COL_ENC_PASSWD)
    @NonNull
    public final String encPasswd;

    /**
     * Constructor from database entry
     */
    public SavedPassword(long id,
                         @NonNull String uri,
                         @NonNull String providerUri,
                         @NonNull String displayName,
                         @NonNull String iv,
                         @NonNull String encPasswd)
    {
        this.id = id;
        this.uri = uri;
        this.providerUri = providerUri;
        this.displayName = displayName;
        this.iv = iv;
        this.encPasswd = encPasswd;
    }

    /**
     * Constructor for a new saved password
     */
    @Ignore
    public SavedPassword(@NonNull String uri,
                         @NonNull String providerUri,
                         @NonNull String displayName,
                         @NonNull String iv,
                         @NonNull String encPasswd)
    {
        this.id = null;
        this.uri = uri;
        this.providerUri = providerUri;
        this.displayName = displayName;
        this.iv = iv;
        this.encPasswd = encPasswd;
    }
}
