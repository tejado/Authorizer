/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.net.Uri;

/**
 * The NotificationMgr class encapsulates the notifications provided by the app
 */
public class NotificationMgr
{
    Set<Uri> itsExpiryUris = new HashSet<Uri>();

    /** Constructor */
    public NotificationMgr()
    {
    }


    /** Are notifications enabled for a URI */
    public boolean hasPasswdExpiryNotif(Uri uri)
    {
        return itsExpiryUris.contains(uri);
    }


    /** Toggle whether notifications are enabled for a URI */
    public void togglePasswdExpiryNotif(Uri uri, Activity act)
    {
        if (itsExpiryUris.contains(uri)) {
            itsExpiryUris.remove(uri);
        } else {
            itsExpiryUris.add(uri);
        }
    }
}
