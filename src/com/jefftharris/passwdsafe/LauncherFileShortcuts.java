/*
 * Copyright (©) 2011 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.File;

import android.content.Intent;
import android.os.Bundle;

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

    @Override
    protected void onFileClick(File file)
    {
        if (file != null) {
            Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT,
                            FileList.createOpenIntent(file, null));
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, file.getName());
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                            Intent.ShortcutIconResource.fromContext(
                                this, R.drawable.icon));
            setResult(RESULT_OK, intent);
        }

        finish();
    }
}
