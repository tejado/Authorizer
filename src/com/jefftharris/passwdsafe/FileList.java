/*
 * Copyright (©) 2009-2010 Jeff Harris <jeffharris@users.sourceforge.net>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.File;
import java.io.FilenameFilter;
import java.security.Security;
import java.util.Arrays;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class FileList extends ListActivity
{
    static {
        Security.removeProvider("BC");
        Security.addProvider(new BCProvider());
    }

    private static final String TAG = "FileList";

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        PasswdSafeApp app = (PasswdSafeApp)getApplication();
        app.setFileData(null);

        File dir = new File("/sdcard");
        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".psafe3");
            }
        });

        TextView header = new TextView(this);
        header.setText("Open file from " + dir);
        getListView().addHeaderView(header);

        if (files != null) {
            Arrays.sort(files);
            FileData[] data = new FileData[files.length];
            for (int i = 0; i < files.length; ++i) {
                data[i] = new FileData(files[i]);
            }

            setListAdapter(new ArrayAdapter<FileData>(this,
                            android.R.layout.simple_list_item_1,
                            data));
        }
        // TODO: What if no files?
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        PasswdSafeApp app = (PasswdSafeApp)getApplication();
        app.setFileData(null);
    }

    /* (non-Javadoc)
     * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        FileData file = (FileData) l.getItemAtPosition(position);
        Log.d(TAG, "Open file: " + file.itsFile);

        startActivity(new Intent(PasswdSafe.INTENT,
                                 Uri.fromFile(file.itsFile)));
    }

    private static final class FileData
    {
        public final File itsFile;

        public FileData(File f)
        {
            itsFile = f;
        }

        @Override
        public final String toString()
        {
            return itsFile.getName();
        }
    }
}
