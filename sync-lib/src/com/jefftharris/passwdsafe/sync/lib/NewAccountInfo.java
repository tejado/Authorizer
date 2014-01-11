/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

import android.net.Uri;

import com.jefftharris.passwdsafe.lib.ProviderType;

/** Information for a new account */
public class NewAccountInfo
{
    public final ProviderType itsProviderType;
    public final String itsAccount;
    public final Uri itsProviderAccountUri;

    /** Constructor */
    public NewAccountInfo(ProviderType type, String acct, Uri acctProviderUri)
    {
        itsProviderType = type;
        itsAccount = acct;
        itsProviderAccountUri = acctProviderUri;
    }
}