package com.jefftharris.passwdsafe;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
//import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.pwsafe.lib.file.PwsField;
import org.pwsafe.lib.file.PwsFile;
import org.pwsafe.lib.file.PwsFileFactory;
import org.pwsafe.lib.file.PwsFileV3;
import org.pwsafe.lib.file.PwsRecord;
import org.pwsafe.lib.file.PwsRecordV3;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

public class PasswdSafe extends ExpandableListActivity {
    private static final String TAG = "PasswdSafe";

    private static final int DIALOG_GET_PASSWD = 0;

    private static final String GROUP = "group";
    private static final String TITLE = "title";
    private static final String RECORD = "record";

    public static final String INTENT = "com.jefftharris.passwdsafe.action.VIEW";

    private PwsFile itsPwsFile;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showDialog(DIALOG_GET_PASSWD);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog = null;
        switch (id) {
            case DIALOG_GET_PASSWD:
            {
                LayoutInflater factory = LayoutInflater.from(this);
                final View passwdView = factory.inflate(R.layout.passwd_entry,
                                                        null);

                AlertDialog.Builder alert = new AlertDialog.Builder(this)
                .setTitle("Enter Password")
                .setMessage("Password:")
                .setView(passwdView)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        EditText passwdInput =
                            (EditText)passwdView.findViewById(R.id.passwd_edit);
                        dialog.dismiss();
                        showFile(passwdInput.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        PasswdSafe.this.finish();
                    }
                });
                dialog = alert.create();
                break;
            }
        }
        return dialog;
    }

    private void showFile(String passwd)
    {
        Intent intent = getIntent();

        try {
            // TODO: on pause, close file, clear password, etc.
            // TODO: Remember last file used somewhere?
            itsPwsFile =
                PwsFileFactory.loadFile(intent.getData().getPath(),
                                        new StringBuilder(passwd));

            List<Map<String, String>> groupData =
                new ArrayList<Map<String, String>>();
            List<List<Map<String, Object>>> childData =
                new ArrayList<List<Map<String, Object>>>();

            Map<String, List<PwsRecord>> recsByGroup =
                new HashMap<String, List<PwsRecord>>();
            Iterator<PwsRecord> recIter = itsPwsFile.getRecords();
            while (recIter.hasNext()) {
                PwsRecord rec = recIter.next();
                String group = getGroup(rec);
                List<PwsRecord> groupList = recsByGroup.get(group);
                if (groupList == null) {
                    groupList = new ArrayList<PwsRecord>();
                    recsByGroup.put(group, groupList);
                }
                groupList.add(rec);
            }
            // TODO sort group lists

            for (Map.Entry<String, List<PwsRecord>> entry : recsByGroup.entrySet()) {
                Map<String, String> groupInfo = new HashMap<String, String>();
                groupInfo.put(GROUP, entry.getKey());
                groupData.add(groupInfo);

                List<Map<String, Object>> children =
                    new ArrayList<Map<String, Object>>();
                for (PwsRecord rec : entry.getValue()) {
                    Map<String, Object> recInfo = new HashMap<String, Object>();
                    recInfo.put(TITLE, getTitle(rec));
                    recInfo.put(RECORD, rec);
                    children.add(recInfo);
                }
                childData.add(children);
            }

            ExpandableListAdapter adapter =
                new SimpleExpandableListAdapter(this,
                                                groupData,
                                                android.R.layout.simple_expandable_list_item_1,
                                                new String[] { GROUP },
                                                new int[] { android.R.id.text1 },
                                                childData,
                                                android.R.layout.simple_expandable_list_item_1,
                                                new String[] { TITLE },
                                                new int[] { android.R.id.text1 });
            setListAdapter(adapter);



        } catch (Exception e) {
            Log.e(TAG, "Exception", e);
            new AlertDialog.Builder(this)
            .setMessage(e.toString())
            .setCancelable(false)
            .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    PasswdSafe.this.finish();
                }
            })
            .show();
        }
//      PwsFile pwsfile =
//      PwsFileFactory.loadFile("/sdcard/test.psafe3",
//                              new StringBuilder("test123"));
//
//  Iterator<PwsRecord> iter = pwsfile.getRecords();
//  while (iter.hasNext()){
//      PwsRecord rec = iter.next();
//
//      Iterator<Integer> fielditer = rec.getFields();
//      while (fielditer.hasNext()) {
//          PwsField field = rec.getField(fielditer.next());
//          text.append(field.getType() + ": " + field + "\n");
//      }
//  }
    }

    private String getGroup(PwsRecord rec)
    {
        switch (itsPwsFile.getFileVersionMajor()) {
            case PwsFileV3.VERSION:
                return rec.getField(PwsRecordV3.GROUP).toString();
            default:
                return "TODO";
        }
    }

    private String getTitle(PwsRecord rec)
    {
        switch (itsPwsFile.getFileVersionMajor()) {
            case PwsFileV3.VERSION:
                return rec.getField(PwsRecordV3.TITLE).toString();
            default:
                return "TODO";
        }
    }
}