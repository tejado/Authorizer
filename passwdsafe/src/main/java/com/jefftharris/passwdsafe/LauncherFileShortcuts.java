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
import android.view.View;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

public class LauncherFileShortcuts extends AbstractFileListActivity
{
    public static final String EXTRA_IS_DEFAULT_FILE = "isDefFile";

    private boolean itsIsDefaultFile = false;

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (!Intent.ACTION_CREATE_SHORTCUT.equals(intent.getAction())) {
            finish();
            return;
        }

        itsIsDefaultFile = intent.getBooleanExtra(EXTRA_IS_DEFAULT_FILE, false);
        if (itsIsDefaultFile) {
            setTitle(R.string.default_file_to_open);
        } else {
            setTitle(R.string.shortcut_choose_file);
        }

        // Remove the extra padding for tablets when used as a dialog style
        View root = findViewById(R.id.content);
        int pad = root.getPaddingTop();
        root.setPadding(pad, pad, pad, pad);
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.FileListFragment.Listener#openFile(android.net.Uri)
     * @see com.jefftharris.passwdsafe.SyncProviderFilesFragment.Listener#openFile(android.net.Uri)
     */
    @Override
    public void openFile(Uri uri, String fileName)
    {
        if (itsIsDefaultFile || (uri != null)) {
            Intent openIntent = null;
            if (uri != null) {
                openIntent = PasswdSafeUtil.createOpenIntent(uri, null);
            }

            Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, openIntent);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, fileName);
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                            Intent.ShortcutIconResource.fromContext(
                                this, R.mipmap.ic_launcher_passwdsafe));
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


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.FileListFragment.Listener#activityHasNoneItem()
     */
    @Override
    public boolean activityHasNoneItem()
    {
        return itsIsDefaultFile;
    }

    @Override
    public void updateViewFiles()
    {
    }

    @Override
    public void updateViewSyncFiles()
    {
    }
}
