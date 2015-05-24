/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
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

import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.sync.MainActivity;
import com.jefftharris.passwdsafe.sync.R;

/**
 *  Utilities for notifications
 */
public final class NotifUtils
{
    public enum Type
    {
        DROPBOX_MIGRATED(1);

        public final int itsNotifId;

        Type(int id)
        {
            itsNotifId = id;
        }
    }

    /** Show a notification */
    public static void showNotif(Type type, Context ctx)
    {
        NotificationManager notifMgr = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent mainIntent = PendingIntent.getActivity(
                ctx, type.itsNotifId, new Intent(ctx, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        String title = "";
        String content = "";
        switch (type) {
        case DROPBOX_MIGRATED: {
            title = ctx.getString(R.string.dropbox_service_updated);
            content = ctx.getString(R.string.open_app_reauthorize);
            break;
        }
        }
        GuiUtils.showSimpleNotification(notifMgr, ctx, R.drawable.ic_stat_app,
                                        title, R.drawable.ic_launcher_sync,
                                        content, mainIntent, type.itsNotifId,
                                        true);
    }
}
