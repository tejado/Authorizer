/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.pwsafe.lib.file.PwsRecord;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.util.Log;

import net.tjado.passwdsafe.file.PasswdExpiration;
import net.tjado.passwdsafe.file.PasswdFileData;
import net.tjado.passwdsafe.file.PasswdFileDataObserver;
import net.tjado.passwdsafe.file.PasswdFileUri;
import net.tjado.passwdsafe.file.PasswdRecord;
import net.tjado.passwdsafe.file.PasswdRecordFilter;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.Utils;
import net.tjado.passwdsafe.lib.view.GuiUtils;
import net.tjado.passwdsafe.util.LongReference;
import net.tjado.passwdsafe.view.ConfirmPromptDialog;

/**
 * The NotificationMgr class encapsulates the notifications provided by the app
 */
@SuppressWarnings("TryFinallyCanBeTryWithResources")
public class NotificationMgr implements PasswdFileDataObserver
{
    private static final String TAG = "NotificationMgr";

    private static final String DB_TABLE_URIS = "uris";
    private static final String DB_COL_URIS_ID = BaseColumns._ID;
    private static final String DB_COL_URIS_URI = "uri";
    private static final String DB_MATCH_URIS_ID = DB_COL_URIS_ID + " = ?";
    private static final String DB_MATCH_URIS_URI = DB_COL_URIS_URI + " = ?";

    private static final String DB_TABLE_EXPIRYS = "expirations";
    private static final String DB_COL_EXPIRYS_ID = BaseColumns._ID;
    private static final String DB_COL_EXPIRYS_URI = "uri";
    private static final String DB_COL_EXPIRYS_UUID = "rec_uuid";
    private static final String DB_COL_EXPIRYS_TITLE = "rec_title";
    private static final String DB_COL_EXPIRYS_GROUP = "rec_group";
    private static final String DB_COL_EXPIRYS_EXPIRE = "rec_expire";
    private static final String DB_MATCH_EXPIRYS_URI =
        DB_COL_EXPIRYS_URI + " = ?";
    private static final String DB_MATCH_EXPIRYS_ID =
        DB_COL_EXPIRYS_ID + " = ?";

    private final Context itsCtx;
    private final AlarmManager itsAlarmMgr;
    private final NotificationManager itsNotifyMgr;
    private final DbHelper itsDbHelper;
    private final HashMap<Long, UriNotifInfo> itsUriNotifs = new HashMap<>();
    private final HashSet<Uri> itsNotifUris = new HashSet<>();
    private int itsNextNotifId = 1;
    private PasswdRecordFilter.ExpiryFilter itsExpiryFilter = null;
    private PendingIntent itsTimerIntent;

    /** Constructor */
    public NotificationMgr(Context ctx,
                           AlarmManager alarmMgr,
                           PasswdRecordFilter.ExpiryFilter expiryFilter)
    {
        itsCtx = ctx;
        itsAlarmMgr = alarmMgr;
        itsNotifyMgr = (NotificationManager)
            ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        itsExpiryFilter = expiryFilter;

        itsDbHelper = new DbHelper(ctx);
        PasswdFileData.addObserver(this);

        loadEntries();
    }


    /** Are notifications enabled for a URI */
    public boolean hasPasswdExpiryNotif(PasswdFileUri uri)
    {
        return itsNotifUris.contains(uri.getUri());
    }

    /**
     * Set whether notifications are enabled for a password file
     */
    public void setPasswdExpiryNotif(@NonNull PasswdFileData fileData,
                                     boolean enabled)
    {
        try {
            SQLiteDatabase db = itsDbHelper.getWritableDatabase();
            try {
                db.beginTransaction();
                Long uriId = getDbUriId(fileData.getUri(), db);
                if (enabled) {
                    if (uriId == null) {
                        enablePasswdExpiryNotif(fileData, db);
                    }
                } else {
                    if (uriId != null) {
                        removeUri(uriId, db);
                        loadEntries(db);
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (SQLException e) {
            Log.e(TAG, "Database error", e);
        }
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.file.PasswdFileDataObserver#passwdFileDataChanged(com.jefftharris.passwdsafe.file.PasswdFileData)
     */
    public void passwdFileDataChanged(PasswdFileData fileData)
    {
        try {
            SQLiteDatabase db = itsDbHelper.getWritableDatabase();
            try {
                db.beginTransaction();
                Long id = getDbUriId(fileData.getUri(), db);
                if (id != null) {
                    doUpdatePasswdFileData(id, fileData, db);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (SQLException e) {
            Log.e(TAG, "Database error", e);
        }
    }

    /**
     * Create a confirm prompt for clearing all notifications
     */
    public DialogFragment createClearAllPrompt(Context ctx, Bundle args)
    {
        return ConfirmPromptDialog.newInstance(
                ctx.getString(R.string.clear_password_notifications),
                ctx.getString(R.string.erase_all_expiration_notifications),
                ctx.getString(R.string.clear), args);
    }

    /**
     * Clear all notifications after being confirmed
     */
    public void handleClearAllConfirmed()
    {
        try {
            SQLiteDatabase db = itsDbHelper.getWritableDatabase();
            try {
                db.beginTransaction();
                db.delete(DB_TABLE_EXPIRYS, null, null);
                db.delete(DB_TABLE_URIS, null, null);
                loadEntries(db);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (SQLException e) {
            Log.e(TAG, "Database error", e);
        }
    }


    /** Cancel the notification for a URI */
    public void cancelNotification(PasswdFileUri uri)
    {
        if (uri != null) {
            try {
                SQLiteDatabase db = itsDbHelper.getReadableDatabase();
                Long id = getDbUriId(uri, db);
                UriNotifInfo info = itsUriNotifs.get(id);
                if (info != null) {
                    itsNotifyMgr.cancel(info.getNotifId());
                }
            } catch (SQLException e) {
                Log.e(TAG, "Database error for uri: " + uri, e);
            }
        }
    }


    /** Set the password expiration filter */
    public void setPasswdExpiryFilter(PasswdRecordFilter.ExpiryFilter filter)
    {
        itsExpiryFilter = filter;
        loadEntries();
    }


    /** Handle an expiration timeout */
    public void handleExpirationTimeout()
    {
        loadEntries();
    }


    /** Return whether notifications are supported for the URI */
    public static boolean notifSupported(PasswdFileUri uri)
    {
        if (uri == null) {
            return false;
        }

        switch (uri.getType()) {
        case FILE: {
            Uri fileUri = uri.getUri();
            String path = fileUri.getPath();
            return (!path.contains("/data/com.google.android.apps.") &&
                    !path.contains("/data/com.dropbox.android"));
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


    /** Enable notifications for the password file */
    private void enablePasswdExpiryNotif(@NonNull PasswdFileData fileData,
                                         SQLiteDatabase db)
            throws SQLException
    {
        ContentValues values = new ContentValues(1);
        values.put(DB_COL_URIS_URI, fileData.getUri().toString());
        long id = db.insertOrThrow(DB_TABLE_URIS, null, values);
        doUpdatePasswdFileData(id, fileData, db);
    }


    /** Update the notification expirations for a password file */
    private void doUpdatePasswdFileData(long uriId,
                                        PasswdFileData fileData,
                                        SQLiteDatabase db)
        throws SQLException
    {
        PasswdSafeUtil.dbginfo(TAG, "Update %s, id: %d",
                               fileData.getUri(), uriId);

        TreeMap<ExpiryEntry, Long> entries = new TreeMap<>();
        Cursor cursor =
            db.query(DB_TABLE_EXPIRYS,
                     new String[] { DB_COL_EXPIRYS_ID, DB_COL_EXPIRYS_UUID,
                                    DB_COL_EXPIRYS_TITLE, DB_COL_EXPIRYS_GROUP,
                                    DB_COL_EXPIRYS_EXPIRE },
                     DB_MATCH_EXPIRYS_URI,
                     new String[] { Long.toString(uriId) }, null, null, null);
        try {
            while (cursor.moveToNext()) {
                ExpiryEntry entry =
                    new ExpiryEntry(cursor.getString(1), cursor.getString(2),
                                    cursor.getString(3), cursor.getLong(4));
                entries.put(entry, cursor.getLong(0));
            }
        } finally {
            cursor.close();
        }

        boolean dbchanged = false;
        ContentValues values = null;
        for (PasswdRecord rec: fileData.getPasswdRecords()) {
            PasswdExpiration expiry = rec.getPasswdExpiry();
            if (expiry == null) {
                continue;
            }

            PwsRecord pwsrec = rec.getRecord();
            ExpiryEntry entry = new ExpiryEntry(rec.getUUID(),
                                                fileData.getTitle(pwsrec),
                                                fileData.getGroup(pwsrec),
                                                expiry.itsExpiration.getTime());
            if (entries.remove(entry) == null) {
                if (values == null) {
                    values = new ContentValues();
                    values.put(DB_COL_EXPIRYS_URI, uriId);
                }
                values.put(DB_COL_EXPIRYS_UUID, entry.itsUuid);
                values.put(DB_COL_EXPIRYS_TITLE, entry.itsTitle);
                values.put(DB_COL_EXPIRYS_GROUP, entry.itsGroup);
                values.put(DB_COL_EXPIRYS_EXPIRE, entry.itsExpiry);
                db.insertOrThrow(DB_TABLE_EXPIRYS, null, values);
                dbchanged = true;
            }
        }

        for (Long rmId: entries.values()) {
            db.delete(DB_TABLE_EXPIRYS, DB_MATCH_EXPIRYS_ID,
                      new String[] { rmId.toString() });
            dbchanged = true;
        }

        if (dbchanged) {
            loadEntries(db);
        }
    }


    /** Load the expiration entries */
    private void loadEntries()
    {
        SQLiteDatabase db = itsDbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            loadEntries(db);
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, "Database error", e);
        } finally {
            db.endTransaction();
        }
    }


    /** Load the expiration entries from the database */
    private void loadEntries(SQLiteDatabase db)
        throws SQLException
    {
        long expiration;
        if (itsExpiryFilter != null) {
            expiration = itsExpiryFilter.getExpiryFromNow(null);
        } else {
            expiration = Long.MIN_VALUE;
        }
        LongReference nextExpiration = new LongReference(Long.MAX_VALUE);

        itsNotifUris.clear();
        HashSet<Long> uris = new HashSet<>();
        ArrayList<Long> removeUriIds = new ArrayList<>();
        Cursor uriCursor =
            db.query(DB_TABLE_URIS,
                     new String[] { DB_COL_URIS_ID, DB_COL_URIS_URI },
                     null, null, null, null, null);
        try {
            while (uriCursor.moveToNext()) {
                long id = uriCursor.getLong(0);
                Uri uri = Uri.parse(uriCursor.getString(1));
                boolean exists =
                        loadUri(id, uri, uris, expiration, nextExpiration, db);
                if (!exists) {
                    removeUriIds.add(id);
                }
            }
        } finally {
            uriCursor.close();
        }

        Iterator<HashMap.Entry<Long, UriNotifInfo>> iter =
            itsUriNotifs.entrySet().iterator();
        while (iter.hasNext()) {
            HashMap.Entry<Long, UriNotifInfo> entry = iter.next();
            if (!uris.contains(entry.getKey())) {
                itsNotifyMgr.cancel(entry.getValue().getNotifId());
                iter.remove();
            }
        }

        for (Long removeId: removeUriIds) {
            removeUri(removeId, db);
        }

        PasswdSafeUtil.dbginfo(TAG, "nextExpiration: %tc",
                               nextExpiration.itsValue);

        if ((nextExpiration.itsValue != Long.MAX_VALUE) &&
            (itsExpiryFilter != null)) {
            if (itsTimerIntent == null) {
                Intent intent =
                    new Intent(PasswdSafeApp.EXPIRATION_TIMEOUT_INTENT);
                itsTimerIntent = PendingIntent.getBroadcast(
                    itsCtx, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            }
            long nextTimer = System.currentTimeMillis() +
                (nextExpiration.itsValue - expiration);
            PasswdSafeUtil.dbginfo(TAG, "nextTimer: %tc", nextTimer);
            itsAlarmMgr.set(AlarmManager.RTC, nextTimer, itsTimerIntent);
        } else if (itsTimerIntent != null) {
            PasswdSafeUtil.dbginfo(TAG, "cancel expiration timer");
            itsAlarmMgr.cancel(itsTimerIntent);
        }
    }


    /**
     * Handle the expiration entries for a URI in the database. Return whether
     * the URI exists.
     */
    private boolean loadUri(final long uriId,
                            final Uri uri,
                            final HashSet<Long> expiredUris,
                            final long expiration,
                            final LongReference nextExpiration,
                            final SQLiteDatabase db)
        throws SQLException
    {
        PasswdFileUri.Creator creator = new PasswdFileUri.Creator(uri, itsCtx);
        PasswdFileUri passwdUri = creator.finishCreate();
        if ((passwdUri == null) || !passwdUri.exists()) {
            PasswdSafeUtil.dbginfo(TAG, "Notif file doesn't exist: %s", uri);
            return false;
        }

        itsNotifUris.add(uri);
        PasswdSafeUtil.dbginfo(TAG, "Load %s", uri);

        TreeSet<ExpiryEntry> expired =
            loadUriEntries(uriId, expiration, nextExpiration, db);

        if (expired.isEmpty()) {
            return true;
        }

        expiredUris.add(uriId);
        UriNotifInfo info = itsUriNotifs.get(uriId);
        if (info == null) {
            info = new UriNotifInfo(itsNextNotifId++);
            itsUriNotifs.put(uriId, info);
        }

        // Skip the notification if the entries are the same
        if (info.getEntries().equals(expired))
        {
            PasswdSafeUtil.dbginfo(TAG, "No expiry changes");
            return true;
        }

        info.setEntries(expired);
        int numExpired = info.getEntries().size();
        ArrayList<String> strs = new ArrayList<>(numExpired);
        for (ExpiryEntry entry: info.getEntries()) {
            strs.add(entry.toString(itsCtx));
        }

        String record = null;
        if (numExpired == 1) {
            ExpiryEntry entry = info.getEntries().first();
            record = entry.itsUuid;
        }

        PendingIntent intent = PendingIntent.getActivity(
            itsCtx, 0, PasswdSafeUtil.createOpenIntent(uri, record),
            PendingIntent.FLAG_UPDATE_CURRENT);

        String title = itsCtx.getResources().getQuantityString(
            R.plurals.expiring_passwords, numExpired, numExpired);
        GuiUtils.showNotification(
            itsNotifyMgr, itsCtx, R.drawable.ic_stat_app,
            itsCtx.getString(R.string.expiring_password),
            title, R.mipmap.ic_launcher_passwdsafe,
            passwdUri.getIdentifier(itsCtx, false),
            strs, intent, info.getNotifId(), null, false);
        return true;
    }


    /** Load the expiration entries for a URI from the database */
    private TreeSet<ExpiryEntry>
    loadUriEntries(final long uriId,
                   final long expiration,
                   final LongReference nextExpiration,
                   final SQLiteDatabase db)
        throws SQLException
    {
        TreeSet<ExpiryEntry> expired = new TreeSet<>();
        Cursor cursor = db.query(DB_TABLE_EXPIRYS,
                                 new String[] { DB_COL_EXPIRYS_UUID,
                                                DB_COL_EXPIRYS_TITLE,
                                                DB_COL_EXPIRYS_GROUP,
                                                DB_COL_EXPIRYS_EXPIRE },
                                 DB_MATCH_EXPIRYS_URI,
                                 new String[] { Long.toString(uriId) },
                                 null, null, null);
        try {
            while (cursor.moveToNext()) {
                long expiry = cursor.getLong(3);
                if (expiry <= expiration) {
                    ExpiryEntry entry = new ExpiryEntry(cursor.getString(0),
                                                        cursor.getString(1),
                                                        cursor.getString(2),
                                                        expiry);
                    PasswdSafeUtil.dbginfo(TAG, "expired entry: %s/%s, at: %tc",
                                           entry.itsGroup, entry.itsTitle,
                                           entry.itsExpiry);
                    expired.add(entry);
                }
                else if (expiry < nextExpiration.itsValue) {
                    nextExpiration.itsValue = expiry;
                }
            }
        } finally {
            cursor.close();
        }

        return expired;
    }


    /** Remove the URI from the database */
    private static void removeUri(Long id, SQLiteDatabase db)
        throws SQLException
    {
        String[] idarg = new String[] { id.toString() };
        db.delete(DB_TABLE_EXPIRYS, DB_MATCH_EXPIRYS_URI, idarg);
        db.delete(DB_TABLE_URIS, DB_MATCH_URIS_ID, idarg);
    }


    /** Get the id for a URI or null if not found */
    private static Long getDbUriId(PasswdFileUri uri, SQLiteDatabase db)
        throws SQLException
    {
        Cursor cursor = db.query(DB_TABLE_URIS, new String[] { DB_COL_URIS_ID },
                                 DB_MATCH_URIS_URI,
                                 new String[] { uri.getUri().toString() },
                                 null, null, null);
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return cursor.getLong(0);
        } finally {
            cursor.close();
        }
    }


    /** Database helper class to manage the database tables */
    private static final class DbHelper extends SQLiteOpenHelper
    {
        private static final String DB_NAME = "notifications.db";
        private static final int DB_VERSION = 1;

        /** Constructor */
        public DbHelper(Context context)
        {
            super(context, DB_NAME, null, DB_VERSION);
        }


        /* (non-Javadoc)
         * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
         */
        @Override
        public void onCreate(SQLiteDatabase db)
        {
            PasswdSafeUtil.dbginfo(TAG, "Create DB");
            enableForeignKey(db);
            db.execSQL("CREATE TABLE " + DB_TABLE_URIS + " (" +
                       DB_COL_URIS_ID + " INTEGER PRIMARY KEY," +
                       DB_COL_URIS_URI + " TEXT NOT NULL" +
                       ");");
            db.execSQL("CREATE TABLE " + DB_TABLE_EXPIRYS + " (" +
                       DB_COL_EXPIRYS_ID + " INTEGER PRIMARY KEY, " +
                       DB_COL_EXPIRYS_URI + " INTEGER REFERENCES " +
                           DB_TABLE_URIS + "(" + DB_COL_URIS_ID +") NOT NULL, " +
                       DB_COL_EXPIRYS_UUID + " TEXT NOT NULL, " +
                       DB_COL_EXPIRYS_TITLE + " TEXT NOT NULL, " +
                       DB_COL_EXPIRYS_GROUP + " TEXT, " +
                       DB_COL_EXPIRYS_EXPIRE + " INTEGER NOT NULL" +
                       ");");
        }


        /* (non-Javadoc)
         * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            enableForeignKey(db);
        }


        /* (non-Javadoc)
         * @see android.database.sqlite.SQLiteOpenHelper#onOpen(android.database.sqlite.SQLiteDatabase)
         */
        @Override
        public void onOpen(SQLiteDatabase db)
        {
            enableForeignKey(db);
            super.onOpen(db);
        }


        /** Enable support for foreign keys on the open database connection */
        private void enableForeignKey(SQLiteDatabase db)
            throws SQLException
        {
            if (!db.isReadOnly()) {
                db.execSQL("PRAGMA foreign_keys = ON;");
            }
        }
    }


    /** The ExpiryEntry class represents an expiration entry for notifications */
    private static final class ExpiryEntry implements Comparable<ExpiryEntry>
    {
        public final String itsUuid;
        public final String itsTitle;
        public final String itsGroup;
        public final long itsExpiry;

        /** Constructor */
        public ExpiryEntry(String uuid, String title, String group, long expiry)
        {
            itsUuid = uuid;
            itsTitle = title;
            itsGroup = group;
            itsExpiry = expiry;
        }


        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(@NonNull ExpiryEntry another)
        {
            int rc;
            // Reverse sort on expiration time
            if (itsExpiry < another.itsExpiry) {
                rc = 1;
            } else if (itsExpiry > another.itsExpiry) {
                rc = -1;
            } else {
                rc = compareStrs(itsGroup, another.itsGroup);
                if (rc == 0) {
                    rc = compareStrs(itsTitle, another.itsTitle);
                    if (rc == 0) {
                        rc = itsUuid.compareTo(another.itsUuid);
                    }
                }
            }
            return rc;
        }


        /** Convert the entry to a string for users */
        public final String toString(Context ctx)
        {
            return PasswdRecord.getRecordId(itsGroup, itsTitle, null) +
                   " (" + Utils.formatDate(itsExpiry, ctx, false, true, true) +
                   ")";
        }


        /** Compare two strings, accounting for null */
        private static int compareStrs(String str1, String str2)
        {
            if (str1 != null) {
                if (str2 != null) {
                    return str1.compareTo(str2);
                }
                return 1;
            } else if (str2 != null) {
                return -1;
            }
            return 0;
        }
    }


    /** The UriNotifInfo contains the parsed notification data for a URI */
    private static final class UriNotifInfo
    {
        private final int itsNotifId;
        private final TreeSet<ExpiryEntry> itsEntries = new TreeSet<>();

        /** Constructor */
        public UriNotifInfo(int notifId)
        {
            itsNotifId = notifId;
        }

        /** Get the notification id */
        public int getNotifId()
        {
            return itsNotifId;
        }

        /** Get the expired entries */
        public SortedSet<ExpiryEntry> getEntries()
        {
            return itsEntries;
        }

        /** Set the expired entries */
        public void setEntries(Set<ExpiryEntry> entries)
        {
            itsEntries.clear();
            itsEntries.addAll(entries);
        }
    }
}
