/*
 * Copyright (©) 2011 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.File;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class LauncherFileShortcuts extends ListActivity
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

        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        File dir = Preferences.getFileDirPref(prefs);
        FileList.FileData[] data = FileList.getFiles(dir, false);
        setListAdapter(new ArrayAdapter<FileList.FileData>(
                        this, android.R.layout.simple_list_item_1, data));
    }

    /* (non-Javadoc)
     * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        FileList.FileData file =
            (FileList.FileData)l.getItemAtPosition(position);
        if (file != null) {
            Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT,
                            FileList.createOpenIntent(file.itsFile, null));
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, file.itsFile.getName());
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                            Intent.ShortcutIconResource.fromContext(
                                this, R.drawable.icon));
            setResult(RESULT_OK, intent);
        }

        finish();
    }
}
