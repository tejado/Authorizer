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
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.jefftharris.passwdsafe.Preferences;
import com.jefftharris.passwdsafe.R;
import com.jefftharris.passwdsafe.pref.FileBackupPref;

/**
 * The PasswdFileUri class encapsulates a URI to a password file
 */
public class PasswdFileUri
{
    private final Uri itsUri;
    private final File itsFile;

    /** Constructor */
    public PasswdFileUri(Uri uri)
    {
        itsUri = uri;
        itsFile = getUriAsFile(itsUri);
    }


    /** Load the password file */
    public PwsFile load(StringBuilder passwd, Context context)
            throws NoSuchAlgorithmException, EndOfFileException,
                InvalidPassphraseException, IOException,
                UnsupportedFileVersionException
    {
        if (itsFile != null) {
            return PwsFileFactory.loadFile(itsFile.getAbsolutePath(), passwd);
        } else {
            ContentResolver cr = context.getContentResolver();
            InputStream is = cr.openInputStream(itsUri);
            String id = getUriIdentifier(itsUri, context, false);
            PwsStorage storage = new PwsStreamStorage(id, is);
            return PwsFileFactory.loadFromStorage(storage, passwd);
        }
    }


    /** Create a new file */
    public PwsFile createNew(StringBuilder passwd, Context context)
            throws IOException
    {
        if (itsFile == null) {
            throw new IOException("no file");
        }
        PwsFile file = PwsFileFactory.newFile();
        file.setPassphrase(passwd);
        file.setStorage(new PwsFileStorage(itsFile.getAbsolutePath(), null));
        return file;
    }


    /** Is the file writable */
    public boolean isWritable()
    {
        return (itsFile != null) && itsFile.canWrite();
    }


    /** Get the URI of the file */
    public Uri getUri()
    {
        return itsUri;
    }


    /** Is the URI a file URI */
    public static final boolean isFileUri(Uri uri)
    {
        return uri.getScheme().equals(ContentResolver.SCHEME_FILE);
    }


    /** Get the file for the URI */
    public static File getUriAsFile(Uri uri)
    {
        if (isFileUri(uri)) {
            return new File(uri.getPath());
        }
        return null;
    }


    /** Get an identifier for the given URI */
    public static String getUriIdentifier(Uri uri, Context context,
                                          boolean shortId)
    {
        String id;
        if (isFileUri(uri)) {
            if (shortId) {
                id = uri.getLastPathSegment();
            } else {
                id = uri.getPath();
            }
        } else {
            if (uri.getAuthority().indexOf("mail") != -1) {
                id = context.getString(R.string.email_attachment);
            } else {
                id = context.getString(R.string.content_file);
            }
        }
        return id;
    }


    /** A PwsStorage save helper for files */
    public static class SaveHelper implements PwsStorage.SaveHelper
    {
        private final Context itsContext;

        public SaveHelper(Context context)
        {
            itsContext = context;
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
}
