/*
 * Copyright (©) 2023 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import net.tjado.passwdsafe.file.PasswdFileUri;
import net.tjado.passwdsafe.util.Pair;

/**
 * Access to the saved passwords database
 */
@Dao
public abstract class SavedPasswordsDao
{
    private static final String TAG = "SavedPasswordsDao";

    /**
     * Get a saved password for a file
     * @return The saved password if found; null otherwise
     */
    @Transaction
    public SavedPassword get(PasswdFileUri fileUri, Context ctx)
    {
        switch (fileUri.getType()) {
        case GENERIC_PROVIDER: {
            SavedPassword saved = queryByUri(fileUri.toString());
            if (saved != null) {
                return saved;
            }
            Pair<String, String> provdisp = getProviderAndDisplay(fileUri, ctx);
            return queryByProvUriAndDispName(provdisp.first, provdisp.second);
        }
        case BACKUP: {
            BackupFile backup = fileUri.getBackupFile();
            return (backup != null) ? queryByUri(backup.fileUri) : null;
        }
        case FILE:
        case SYNC_PROVIDER:
        case EMAIL: {
            return queryByUri(fileUri.toString());
        }
        }
        return null;
    }

    /**
     * Add a saved password for a file
     */
    public void add(PasswdFileUri fileUri,
                    String ivStr,
                    String encStr,
                    Context ctx)
    {
        Pair<String, String> provdisp = getProviderAndDisplay(fileUri, ctx);
        insert(new SavedPassword(fileUri.toString(), provdisp.first,
                                 provdisp.second, ivStr, encStr));
    }

    /**
     * Remove a saved password
     */
    public void remove(Uri uri)
    {
        deleteByUri(uri.toString());
    }

    /**
     * Remove all saved passwords
     */
    @Query("DELETE FROM " + SavedPassword.TABLE)
    public abstract void removeAll();

    /**
     * Upgrade the database storage
     */
    public void processDbUpgrade(Context ctx)
    {
        // Upgrade from preferences storage
        SharedPreferences prefs =
                ctx.getSharedPreferences("saved", Context.MODE_PRIVATE);
        for (String pref : prefs.getAll().keySet()) {
            if (!pref.startsWith("key_")) {
                continue;
            }
            String uri = pref.substring("key_".length());
            String encPasswd = prefs.getString(pref, null);
            String iv = prefs.getString("iv_" + pref, null);
            if ((encPasswd == null) || (iv == null)) {
                continue;
            }
            try {
                insert(new SavedPassword(uri, "", "", iv, encPasswd));
            } catch (Exception e) {
                Log.e(TAG, "Error upgrading keys", e);
            }
        }
        prefs.edit().clear().apply();
    }

    /**
     * Get the provider URI and display name for a file URI
     */
    private static Pair<String, String> getProviderAndDisplay(
            PasswdFileUri fileUri,
            Context ctx)
    {
        Uri uri = fileUri.getUri();
        String providerUri = uri.buildUpon().path(null).query(null).toString();
        String displayName = fileUri.getIdentifier(ctx, true);
        return new Pair<>(providerUri, displayName);
    }

    /**
     * Query a saved password by URI
     */
    @Query("SELECT * FROM " + SavedPassword.TABLE +
           " WHERE " + SavedPassword.COL_URI + " = :uri")
    protected abstract SavedPassword queryByUri(String uri);

    /**
     * Query a saved password by provider URI and display name
     */
    @Query("SELECT * FROM " + SavedPassword.TABLE +
           " WHERE " + SavedPassword.COL_PROVIDER_URI + " = :providerUri " +
           " AND " + SavedPassword.COL_DISPLAY_NAME + " = :displayName")
    protected abstract SavedPassword queryByProvUriAndDispName(
            String providerUri,
            String displayName);

    /**
     * Insert a saved password
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract void insert(SavedPassword savedPasswd);

    /**
     * Delete a saved password by URI
     */
    @Query("DELETE FROM " + SavedPassword.TABLE + " WHERE " +
           SavedPassword.COL_URI + " = :uri")
    protected abstract void deleteByUri(String uri);
}
