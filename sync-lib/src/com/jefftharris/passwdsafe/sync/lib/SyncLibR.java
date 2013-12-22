/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

/**
 * The SyncLibR class exposes resource strings for use by provider plugins.
 * The standard R class can not be used as the plugin does not contain the
 * correct identifiers from when the resources are generated in the plugin
 * library project vs. the final application.
 */
public final class SyncLibR
{
    public static final class string
    {
        public static final int sync_oper_local_to_remote =
                R.string.sync_oper_local_to_remote;
        public static final int sync_oper_remote_to_local =
                R.string.sync_oper_remote_to_local;
        public static final int sync_oper_rmfile =
                R.string.sync_oper_rmfile;
        public static final int sync_oper_rmfile_local =
                R.string.sync_oper_rmfile_local;
        public static final int sync_oper_rmfile_remote =
                R.string.sync_oper_rmfile_remote;
    }
}
