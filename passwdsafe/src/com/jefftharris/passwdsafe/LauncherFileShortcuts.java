/*
 * Copyright (©) 2011-2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

public class LauncherFileShortcuts extends AbstractFileListActivity
{
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setTitle(R.string.shortcut_choose_file);

        Intent intent = getIntent();
        if (!Intent.ACTION_CREATE_SHORTCUT.equals(intent.getAction())) {
            finish();
            return;
        }
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.FileListFragment.Listener#openFile(android.net.Uri)
     * @see com.jefftharris.passwdsafe.SyncProviderFilesFragment.Listener#openFile(android.net.Uri)
     */
    @Override
    public void openFile(Uri uri, String fileName)
    {
        if (uri != null) {
            Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT,
                            PasswdSafeUtil.createOpenIntent(uri, null));
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, fileName);
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                            Intent.ShortcutIconResource.fromContext(
                                this, R.drawable.ic_launcher_passwdsafe));
            setResult(RESULT_OK, intent);
        }

        finish();
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.FileListFragment.Listener#activityHasMenu()
     */
    @Override
    public boolean activityHasMenu()
    {
        return false;
    }
}
