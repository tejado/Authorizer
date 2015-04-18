/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;


/**
 *  The SyncUpdateHandler interface defines callbacks for the UI when the sync
 *  state of a provider changes.
 */
public interface SyncUpdateHandler
{
    public enum GDriveState
    {
        OK,
        AUTH_REQUIRED,
        PENDING_AUTH
    }

    /** Update the state of Google Drive syncing.  Will be called from the
     * main thread */
    public void updateGDriveState(SyncUpdateHandler.GDriveState state);
}
