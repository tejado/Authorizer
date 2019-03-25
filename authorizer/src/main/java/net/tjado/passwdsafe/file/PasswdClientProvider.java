/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import androidx.annotation.NonNull;

import net.tjado.passwdsafe.lib.PasswdSafeContract;

/**
 *  The PasswdClientProvider class is a content provider for the PasswdSafe
 *  client password files
 */
public class PasswdClientProvider extends ContentProvider
{
    private static final UriMatcher MATCHER;
    private static final int MATCH_FILES = 1;

    private static PasswdClientProvider itsProvider = null;
    private static final Object itsProviderLock = new Object();
    private final Set<String> itsFiles = new HashSet<>();

    static {
        MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        MATCHER.addURI(PasswdSafeContract.CLIENT_AUTHORITY,
                       PasswdSafeContract.ClientFiles.TABLE + "/*",
                       MATCH_FILES);
    }

    /** Add a file to those provided and return the URI to access it */
    public static Uri addFile(File file)
    {
        String name = file.getAbsolutePath();
        Uri uri = PasswdSafeContract.CLIENT_CONTENT_URI.buildUpon()
                .appendPath(PasswdSafeContract.ClientFiles.TABLE)
                .appendPath(name)
                .build();
        synchronized (itsProviderLock) {
            itsProvider.itsFiles.add(name);
        }
        return uri;
    }

    /** Remove a file from those provided */
    public static void removeFile(File file)
    {
        synchronized (itsProviderLock) {
            itsProvider.itsFiles.remove(file.getAbsolutePath());
        }
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#openFile(android.net.Uri, java.lang.String)
     */
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode)
            throws FileNotFoundException
    {
        if (!mode.equals("r")) {
            throw new IllegalArgumentException("Invalid mode: " + mode);
        }

        switch (MATCHER.match(uri)) {
        case MATCH_FILES: {
            String fileName = uri.getLastPathSegment();
            synchronized (this) {
                if (!itsFiles.contains(fileName)) {
                    throw new FileNotFoundException(fileName);
                }
                File file = new File(fileName);
                return ParcelFileDescriptor.open(
                        file, ParcelFileDescriptor.MODE_READ_ONLY);
            }
        }
        default: {
            return super.openFile(uri, mode);
        }
        }
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
     */
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs)
    {
        return 0;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#getType(android.net.Uri)
     */
    @Override
    public String getType(@NonNull Uri uri)
    {
        return null;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues)
     */
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values)
    {
        return null;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#onCreate()
     */
    @Override
    public boolean onCreate()
    {
        itsProvider = this;
        return true;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
     */
    @Override
    public Cursor query(@NonNull Uri uri,
                        String[] projection,
                        String selection,
                        String[] selectionArgs,
                        String sortOrder)
    {
        return null;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[])
     */
    @Override
    public int update(@NonNull Uri uri,
                      ContentValues values,
                      String selection,
                      String[] selectionArgs)
    {
        return 0;
    }
}
