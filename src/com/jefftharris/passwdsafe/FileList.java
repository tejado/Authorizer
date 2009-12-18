package com.jefftharris.passwdsafe;

import java.io.File;
import java.io.FilenameFilter;
import java.security.Security;

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
        header.setText("Open file from " + dir + "...");
        getListView().addHeaderView(header);

        if (files != null) {
            // TODO: Only show file name, not whole path
            setListAdapter(new ArrayAdapter<File>(this,
                            android.R.layout.simple_list_item_1,
                            files));
        }
        // TODO: What if no files?
    }

    /* (non-Javadoc)
     * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        File file = (File) l.getItemAtPosition(position);
        Log.d(TAG, "Open file: " + file);

        startActivity(new Intent(PasswdSafe.INTENT, Uri.fromFile(file)));
    }


}
