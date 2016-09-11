/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.sync.MainActivity;
import com.jefftharris.passwdsafe.sync.R;
import com.jefftharris.passwdsafe.sync.SyncLogsActivity;

/**
 *  Utilities for notifications
 */
public final class NotifUtils
{
    public enum Type
    {
        OWNCLOUD_CERT_TRUSTED(0),
        DROPBOX_MIGRATED(1),
        BOX_MIGRATGED(2),
        SYNC_PROGRESS(3),
        SYNC_RESULTS(4),
        SYNC_CONFLICT(5);

        public final int itsNotifId;

        Type(int id)
        {
            itsNotifId = id;
        }
    }

    /** Show a notification */
    public static void showNotif(Type type, Context ctx)
    {
        String content = "";
        switch (type) {
        case OWNCLOUD_CERT_TRUSTED:
        case SYNC_PROGRESS:
        case SYNC_RESULTS:
        case SYNC_CONFLICT: {
            break;
        }
        case DROPBOX_MIGRATED:
        case BOX_MIGRATGED: {
            content = ctx.getString(R.string.open_app_reauthorize);
            break;
        }
        }
        showNotif(type, content, ctx);
    }


    /** Show a notification with a custom content*/
    public static void showNotif(Type type, String content, Context ctx)
    {
        String title = getTitle(type, ctx);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx)
                .setContentTitle(title)
                .setContentText(content)
                .setTicker(title)
                .setAutoCancel(true);
        showNotif(builder, type, null, ctx);
    }

    /**
     * Get the intent activity class for the notification
     */

    /**
     * Show a notification with a custom builder
     */
    public static void showNotif(NotificationCompat.Builder builder,
                                 Type type,
                                 String tag,
                                 Context ctx)
    {
        Class activityClass = null;
        switch (type) {
        case OWNCLOUD_CERT_TRUSTED:
        case DROPBOX_MIGRATED:
        case BOX_MIGRATGED:
        case SYNC_PROGRESS: {
            activityClass = MainActivity.class;
            break;
        }
        case SYNC_RESULTS:
        case SYNC_CONFLICT: {
            activityClass = SyncLogsActivity.class;
        }
        }
        PendingIntent intent = PendingIntent.getActivity(
                ctx, type.itsNotifId, new Intent(ctx, activityClass),
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(intent);

        builder.setSmallIcon(R.drawable.ic_stat_app);
        GuiUtils.showNotification(getNotifMgr(ctx), builder,
                                  R.mipmap.ic_launcher_sync,
                                  type.itsNotifId, tag, ctx);
    }

    /**
     * Cancel a notification
     */
    public static void cancelNotif(Type type, String tag, Context ctx)
    {
        getNotifMgr(ctx).cancel(tag, type.itsNotifId);
    }

    /**
     * Get the title of a notification type
     */
    public static String getTitle(Type type, Context ctx)
    {
        switch (type) {
        case OWNCLOUD_CERT_TRUSTED: {
            return ctx.getString(R.string.owncloud_cert_trusted);
        }
        case DROPBOX_MIGRATED: {
            return ctx.getString(R.string.dropbox_service_updated);
        }
        case BOX_MIGRATGED: {
            return ctx.getString(R.string.box_service_updated);
        }
        case SYNC_PROGRESS: {
            return ctx.getString(R.string.syncing);
        }
        case SYNC_RESULTS: {
            return ctx.getString(R.string.sync_results);
        }
        case SYNC_CONFLICT: {
            return ctx.getString(R.string.sync_conflict);
        }
        }
        return null;
    }

    /**
     * Get the notification manager
     */
    private static NotificationManager getNotifMgr(Context ctx)
    {
        return (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
