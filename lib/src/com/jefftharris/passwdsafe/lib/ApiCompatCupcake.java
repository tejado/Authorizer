/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.lib;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

/**
 * ApiCompat class for Cupcake (v3) and up
 */
public final class ApiCompatCupcake
{
    /** Request a sync of a content provider */
    public static void requestProviderSync(Uri uri, Context ctx)
    {
        ContentResolver res = ctx.getContentResolver();
        res.startSync(uri, new Bundle());
    }
}
