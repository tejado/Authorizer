/*
 * Copyright (©) 2017 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.Nullable;

/**
 * Wrapper for a context that must be an Activity.  Used to ensure UI elements
 * created from a context are backed by an activity.
 */
public final class ActContext
{
    private final ManagedRef<Context> itsContext;

    /**
     * Constructor
     */
    public ActContext(Context ctx)
    {
        if (!(ctx instanceof Activity)) {
            throw new ClassCastException("Activity");
        }

        itsContext = new ManagedRef<>(ctx);
    }

    /**
     * Get the context
     */
    public @Nullable Context getContext()
    {
        return itsContext.get();
    }
}
