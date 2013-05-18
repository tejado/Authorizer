/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.file;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pwsafe.lib.exception.EndOfFileException;
import org.pwsafe.lib.exception.InvalidPassphraseException;
import org.pwsafe.lib.exception.UnsupportedFileVersionException;
import org.pwsafe.lib.file.PwsFile;
import org.pwsafe.lib.file.PwsFileFactory;
import org.pwsafe.lib.file.PwsFileStorage;
import org.pwsafe.lib.file.PwsStorage;
import org.pwsafe.lib.file.PwsStreamStorage;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.jefftharris.passwdsafe.Preferences;
import com.jefftharris.passwdsafe.R;
import com.jefftharris.passwdsafe.lib.PasswdSafeContract;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.pref.FileBackupPref;

/**
 * The PasswdFileUri class encapsulates a URI to a password file
 */
public class PasswdFileUri
{
    private static final String TAG = "PasswdFileUri";

    private final Uri itsUri;
    private final Type itsType;
    private final File itsFile;
    private String itsTitle = null;
    private PasswdSafeContract.Providers.Type itsSyncType = null;

    /** The type of URI */
    public enum Type
    {
        FILE,
        SYNC_PROVIDER,
        EMAIL,
        GENERIC_PROVIDER
    }

    /** Constructor */
    public PasswdFileUri(Uri uri)
    {
        itsUri = uri;
        itsType = getUriType(uri);
        switch (itsType) {
        case FILE: {
            itsFile = new File(uri.getPath());
            break;
        }
        case SYNC_PROVIDER:
        case EMAIL:
        case GENERIC_PROVIDER:
        default: {
            itsFile = null;
            break;
        }
        }
    }


    /** Constructor from a File */
    public PasswdFileUri(File file)
    {
        itsUri = Uri.fromFile(file);
        itsType = Type.FILE;
        itsFile = file;
    }


    /** Load the password file */
    public PwsFile load(StringBuilder passwd, Context context)
            throws NoSuchAlgorithmException, EndOfFileException,
                InvalidPassphraseException, IOException,
                UnsupportedFileVersionException
    {
        switch (itsType) {
        case FILE: {
            return PwsFileFactory.loadFile(itsFile.getAbsolutePath(), passwd);
        }
        case SYNC_PROVIDER: {
            ContentResolver cr = context.getContentResolver();
            InputStream is = cr.openInputStream(itsUri);
            String id = getIdentifier(context, false);
            PwsStorage storage = new SyncStorage(itsUri, id, is);
            return PwsFileFactory.loadFromStorage(storage, passwd);
        }
        case EMAIL:
        case GENERIC_PROVIDER: {
            ContentResolver cr = context.getContentResolver();
            InputStream is = cr.openInputStream(itsUri);
            String id = getIdentifier(context, false);
            PwsStorage storage = new PwsStreamStorage(id, is);
            return PwsFileFactory.loadFromStorage(storage, passwd);
        }
        }
        return null;
    }


    /** Create a new file */
    public PwsFile createNew(StringBuilder passwd, Context context)
            throws IOException
    {
        switch (itsType) {
        case FILE: {
            PwsFile file = PwsFileFactory.newFile();
            file.setPassphrase(passwd);
            file.setStorage(new PwsFileStorage(itsFile.getAbsolutePath(),
                                               null));
            return file;
        }
        case SYNC_PROVIDER:
        case EMAIL:
        case GENERIC_PROVIDER: {
            throw new IOException("no file");
        }
        }
        return null;
    }


    /** Is the file writable */
    public boolean isWritable()
    {
        switch (itsType) {
        case FILE: {
            return (itsFile != null) && itsFile.canWrite();
        }
        case SYNC_PROVIDER: {
            return true;
        }
        case EMAIL:
        case GENERIC_PROVIDER: {
            return false;
        }
        }
        return false;
    }


    /** Get the URI of the file */
    public Uri getUri()
    {
        return itsUri;
    }


    /** Get the file associated with the URI if it exists */
    public File getFile()
    {
        return itsFile;
    }


    /** Get an identifier for the URI */
    public String getIdentifier(Context context, boolean shortId)
    {
        switch (itsType) {
        case FILE: {
            if (shortId) {
                return itsUri.getLastPathSegment();
            } else {
                return itsUri.getPath();
            }
        }
        case SYNC_PROVIDER: {
            resolveSyncUri(context);
            return String.format("%s - %s",
                                 itsSyncType.getName(context), itsTitle);
        }
        case EMAIL: {
            return context.getString(R.string.email_attachment);
        }
        case GENERIC_PROVIDER: {
            return context.getString(R.string.content_file);
        }
        }
        return "";
    }


    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof PasswdFileUri)) {
            return false;
        }
        PasswdFileUri uri = (PasswdFileUri)o;
        return itsUri.equals(uri.itsUri);
    }


    /** Convert the URI to a string */
    @Override
    public String toString()
    {
        return itsUri.toString();
    }


    /** Resolve fields for a sync URI */
    private void resolveSyncUri(Context context)
    {
        switch (itsType) {
        case FILE:
        case EMAIL:
        case GENERIC_PROVIDER: {
            break;
        }
        case SYNC_PROVIDER: {
            if (itsTitle != null) {
                break;
            }
            ContentResolver cr = context.getContentResolver();
            Cursor fileCursor = cr.query(itsUri,
                                         PasswdSafeContract.Files.PROJECTION,
                                         null, null, null);
            Cursor providerCursor = null;
            try {
                if (!fileCursor.moveToFirst()) {
                    break;
                }
                String title = fileCursor.getString(
                        PasswdSafeContract.Files.PROJECTION_IDX_TITLE);
                long provider = fileCursor.getLong(
                        PasswdSafeContract.Files.PROJECTION_IDX_PROVIDER);

                Uri providerUri = ContentUris.withAppendedId(
                        PasswdSafeContract.Providers.CONTENT_URI, provider);
                providerCursor = cr.query(
                        providerUri,
                        PasswdSafeContract.Providers.PROJECTION,
                        null, null, null);
                if (!providerCursor.moveToFirst()) {
                    break;
                }
                String typeStr = providerCursor.getString(
                        PasswdSafeContract.Providers.PROJECTION_IDX_TYPE);
                try {
                    itsSyncType =
                            PasswdSafeContract.Providers.Type.valueOf(typeStr);
                    itsTitle = title;
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Unknown provider type: " + typeStr);
                }
            } finally {
                fileCursor.close();
                if (providerCursor != null) {
                    providerCursor.close();
                }
            }
            break;
        }
        }
    }


    /** Get the URI type */
    private static final Type getUriType(Uri uri)
    {
        if (uri.getScheme().equals(ContentResolver.SCHEME_FILE)) {
            return Type.FILE;
        }
        String auth = uri.getAuthority();
        if (PasswdSafeContract.AUTHORITY.equals(auth)) {
            return Type.SYNC_PROVIDER;
        } else if (auth.indexOf("mail") != -1) {
            return Type.EMAIL;
        }
        return Type.GENERIC_PROVIDER;
    }


    /** A PwsStorage save helper for files */
    public static class SaveHelper implements PwsStorage.SaveHelper
    {
        private final Context itsContext;

        public SaveHelper(Context context)
        {
            itsContext = context;
        }

        /** Get the save context */
        public Context getContext()
        {
            return itsContext;
        }

        /* (non-Javadoc)
         * @see org.pwsafe.lib.file.PwsStorage.SaveHelper#getSaveFileName(java.io.File, boolean)
         */
        @Override
        public String getSaveFileName(File file, boolean isV3)
        {
            String name = file.getName();
            Pattern pat = Pattern.compile("^(.*)_\\d{8}_\\d{6}\\.ibak$");
            Matcher match = pat.matcher(name);
            if ((match != null) && match.matches()) {
                name = match.group(1);
                if (isV3) {
                    name += ".psafe3";
                } else {
                    name += ".dat";
                }
            }
            return name;
        }

        /* (non-Javadoc)
         * @see org.pwsafe.lib.file.PwsStorage.SaveHelper#createBackupFile(java.io.File, java.io.File)
         */
        @Override
        public void createBackupFile(File fromFile, File toFile)
                throws IOException
        {
            SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(itsContext);
            FileBackupPref backupPref = Preferences.getFileBackupPref(prefs);

            File dir = toFile.getParentFile();
            String fileName = toFile.getName();
            int dotpos = fileName.lastIndexOf('.');
            if (dotpos != -1) {
                fileName = fileName.substring(0, dotpos);
            }

            final Pattern pat = Pattern.compile(
                    "^" + Pattern.quote(fileName) + "_\\d{8}_\\d{6}\\.ibak$");
            File[] backupFiles = dir.listFiles(new FileFilter() {
                public boolean accept(File f)
                {
                    return f.isFile() && pat.matcher(f.getName()).matches();
                }
            });
            if (backupFiles != null) {
                Arrays.sort(backupFiles);

                int numBackups = backupPref.getNumBackups();
                if (numBackups > 0) {
                    --numBackups;
                }
                for (int i = 0, numFiles = backupFiles.length;
                        numFiles > numBackups; ++i, --numFiles) {
                    if (!backupFiles[i].equals(fromFile)) {
                        backupFiles[i].delete();
                    }
                }
            }

            if (backupPref != FileBackupPref.BACKUP_NONE) {
                SimpleDateFormat bakTime =
                        new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
                StringBuilder bakName = new StringBuilder(fileName);
                bakName.append("_");
                bakName.append(bakTime.format(new Date()));
                bakName.append(".ibak");

                File bakFile = new File(dir, bakName.toString());
                if (!toFile.renameTo(bakFile)) {
                    throw new IOException("Can not create backup file: " +
                                          bakFile);
                }
            }
        }
    }


    /** A PwsStreamStorage implementation for sync providers */
    private static class SyncStorage extends PwsStreamStorage
    {
        private final Uri itsUri;

        /** Constructor */
        public SyncStorage(Uri uri, String id, InputStream stream)
        {
            super(id, stream);
            itsUri = uri;
        }

        /** Save the file contents */
        @Override
        public boolean save(byte[] data, boolean isV3)
        {
            File file = null;
            try {
                PasswdFileUri.SaveHelper helper =
                        (PasswdFileUri.SaveHelper)getSaveHelper();
                Context ctx = helper.getContext();
                file = File.createTempFile("passwd", ".tmp", ctx.getCacheDir());
                PwsFileStorage.writeFile(file, data);

                Uri fileUri = PasswdClientProvider.addFile(file);
                ContentResolver cr = ctx.getContentResolver();
                ContentValues values = new ContentValues();
                values.put(PasswdSafeContract.Files.COL_FILE,
                           fileUri.toString());
                cr.update(itsUri, values, null, null);

                PasswdSafeUtil.dbginfo(TAG, "SyncStorage update %s with %s",
                                       itsUri, file);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error saving " + itsUri, e);
                return false;
            } finally {
                if (file != null) {
                    PasswdClientProvider.removeFile(file);
                    file.delete();
                }
            }
        }
    }
}