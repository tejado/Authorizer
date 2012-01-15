/*
 * Copyright (©) 2011 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import org.pwsafe.lib.file.PwsRecord;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class LauncherRecordShortcuts extends AbstractPasswdSafeActivity
{
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setTitle(R.string.shortcut_choose_record);

        Intent intent = getIntent();
        if (!Intent.ACTION_CREATE_SHORTCUT.equals(intent.getAction())) {
            finish();
            return;
        }

        PasswdSafeApp app = (PasswdSafeApp)getApplication();
        itsPasswdFile = app.accessOpenFile(this);
        if (itsPasswdFile != null) {
            itsUri = itsPasswdFile.getFileData().getUri();
        } else {
            TextView empty = (TextView)findViewById(android.R.id.empty);
            empty.setText(R.string.no_records_open_file);
        }
        showFileData();
    }


    @Override
    protected void onRecordClick(PwsRecord rec)
    {
        PasswdFileData fileData = itsPasswdFile.getFileData();
        String uuid = fileData.getUUID(rec);
        Intent shortcutIntent = FileList.createOpenIntent(itsUri, uuid);

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, fileData.getTitle(rec));
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                        Intent.ShortcutIconResource.fromContext(
                            this, R.drawable.icon));
        setResult(RESULT_OK, intent);
        finish();
    }

}
