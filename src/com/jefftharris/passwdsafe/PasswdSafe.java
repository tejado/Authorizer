package com.jefftharris.passwdsafe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import org.pwsafe.lib.file.PwsFile;
import org.pwsafe.lib.file.PwsFileFactory;
import org.pwsafe.lib.file.PwsFileV3;
import org.pwsafe.lib.file.PwsRecord;
import org.pwsafe.lib.file.PwsRecordV3;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.SimpleExpandableListAdapter;

public class PasswdSafe extends ExpandableListActivity {
    private static final String TAG = "PasswdSafe";

    private static final int DIALOG_GET_PASSWD = 0;
    private static final int DIALOG_LOADING = 1;

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

                // TODO: click Ok when enter pressed
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
            case DIALOG_LOADING:
            {
                dialog = ProgressDialog.show(this, "", "Loading...", true);
            }
        }
        return dialog;
    }

    private void showFile(String passwd)
    {
        Intent intent = getIntent();
        showDialog(DIALOG_LOADING);

        try {
            // TODO: on pause, close file, clear password, etc.
            // TODO: Remember last file used somewhere?
            Log.i(TAG, "before load file");
            itsPwsFile =
                PwsFileFactory.loadFile(intent.getData().getPath(),
                                        new StringBuilder(passwd));
            Log.i(TAG, "after load file");

            ArrayList<Map<String, String>> groupData =
                new ArrayList<Map<String, String>>();
            ArrayList<ArrayList<HashMap<String, Object>>> childData =
                new ArrayList<ArrayList<HashMap<String, Object>>>();

            TreeMap<String, ArrayList<PwsRecord>> recsByGroup =
                new TreeMap<String, ArrayList<PwsRecord>>();
            Iterator<PwsRecord> recIter = itsPwsFile.getRecords();
            while (recIter.hasNext()) {
                PwsRecord rec = recIter.next();
                String group = getGroup(rec);
                ArrayList<PwsRecord> groupList = recsByGroup.get(group);
                if (groupList == null) {
                    groupList = new ArrayList<PwsRecord>();
                    recsByGroup.put(group, groupList);
                }
                groupList.add(rec);
            }
            Log.i(TAG, "groups sorted");

            RecordMapComparator comp = new RecordMapComparator();
            for (Map.Entry<String, ArrayList<PwsRecord>> entry :
                    recsByGroup.entrySet()) {
                Log.i(TAG, "process group:" + entry.getKey());
                Map<String, String> groupInfo =
                    Collections.singletonMap(GROUP, entry.getKey());
                groupData.add(groupInfo);

                ArrayList<HashMap<String, Object>> children =
                    new ArrayList<HashMap<String, Object>>();
                for (PwsRecord rec : entry.getValue()) {
                    HashMap<String, Object> recInfo = new HashMap<String, Object>();
                    recInfo.put(TITLE, getTitle(rec));
                    recInfo.put(RECORD, rec);
                    children.add(recInfo);
                }

                Collections.sort(children, comp);
                childData.add(children);
            }
            Log.i(TAG, "adapter data created");

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
            Log.i(TAG, "adapter set");


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
        } finally {
            dismissDialog(DIALOG_LOADING);
        }
    }

    private final String getGroup(PwsRecord rec)
    {
        switch (itsPwsFile.getFileVersionMajor()) {
            case PwsFileV3.VERSION:
                return rec.getField(PwsRecordV3.GROUP).toString();
            default:
                return "TODO";
        }
    }

    private final String getTitle(PwsRecord rec)
    {
        switch (itsPwsFile.getFileVersionMajor()) {
            case PwsFileV3.VERSION:
                return rec.getField(PwsRecordV3.TITLE).toString();
            default:
                return "TODO";
        }
    }


    private static final class RecordMapComparator implements
                    Comparator<HashMap<String, Object>>
    {
        public int compare(HashMap<String, Object> arg0,
                           HashMap<String, Object> arg1)
        {
            String title0 = arg0.get(TITLE).toString();
            String title1 = arg1.get(TITLE).toString();
            return title0.compareTo(title1);
        }
    }
}