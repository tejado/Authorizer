/*
 * Copyright (©) 2009-2010 Jeff Harris <jefftharris@gmail.com>
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class FileList extends ListActivity implements OnClickListener
{
    static {
        Security.removeProvider("BC");
        Security.addProvider(new BCProvider());
    }

    private static final String TAG = "FileList";

    private static final String DIR_PREF = "dir";

    private static final int DIALOG_GET_DIR = 0;

    private String itsCurrDirName = "";

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_list);

        Button dirBtn = (Button)findViewById(R.id.dirBtn);
        dirBtn.setOnClickListener(this);

        PasswdSafeApp app = (PasswdSafeApp)getApplication();
        ActivityPasswdFile file = app.accessPasswdFile(null, this);
        file.close();

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        itsCurrDirName =
            prefs.getString(DIR_PREF,
                            Environment.getExternalStorageDirectory().toString());
        showFiles();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        PasswdSafeApp app = (PasswdSafeApp)getApplication();
        ActivityPasswdFile file = app.accessPasswdFile(null, this);
        file.close();
    }

    /* (non-Javadoc)
     * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        FileData file = (FileData) l.getItemAtPosition(position);
        Log.d(TAG, "Open file: " + file.itsFile);

        startActivity(new Intent(PasswdSafeApp.VIEW_INTENT,
                                 Uri.fromFile(file.itsFile)));
    }

    public void onClick(View v)
    {
        showDialog(DIALOG_GET_DIR);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog = null;
        switch(id) {
        case DIALOG_GET_DIR:
        {
            LayoutInflater factory = LayoutInflater.from(this);
            final View textView = factory.inflate(R.layout.text_entry, null);
            AlertDialog.Builder alert = new AlertDialog.Builder(this)
                .setTitle("Enter Directory")
                .setMessage("Directory:")
                .setView(textView)
                .setPositiveButton("Ok",
                                   new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        EditText textEdit =
                            (EditText)textView.findViewById(R.id.text_entry);
                        setFileDir(textEdit.getText().toString());
                        dialog.dismiss();
                    }
                })
                .setNeutralButton("Reset",
                                  new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        setFileDir(
                            Environment.getExternalStorageDirectory().toString());
                    }
                })
                .setNegativeButton("Cancel",
                                   new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.cancel();
                    }
                });
            dialog = alert.create();
            break;
        }
        default:
        {
            dialog = super.onCreateDialog(id);
            break;
        }
        }
        return dialog;
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
     */
    @Override
    protected void onPrepareDialog(int id, Dialog dialog)
    {
        switch (id) {
        case DIALOG_GET_DIR:
        {
            EditText textEdit = (EditText)dialog.findViewById(R.id.text_entry);
            textEdit.setText(itsCurrDirName);
            break;
        }
        default:
        {
            super.onPrepareDialog(id, dialog);
            break;
        }
        }
    }

    private void setFileDir(String dirName)
    {
        itsCurrDirName = dirName;

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(DIR_PREF, itsCurrDirName);
        editor.commit();

        showFiles();
    }

    private void showFiles()
    {
        File dir = new File(itsCurrDirName);

        TextView dirLabel = (TextView)findViewById(R.id.dirLabel);
        dirLabel.setText("Open file from " + dir);

        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".psafe3") ||
                    filename.endsWith(".dat");
            }
        });
        FileData[] data;
        if (files != null) {
            Arrays.sort(files);
            data = new FileData[files.length];
            for (int i = 0; i < files.length; ++i) {
                data[i] = new FileData(files[i]);
            }
        } else {
            data = new FileData[0];
        }

        setListAdapter(new ArrayAdapter<FileData>(
                        this, android.R.layout.simple_list_item_1, data));
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
