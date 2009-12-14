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
import android.os.Handler;
import android.os.Message;
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

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate bundle:" + savedInstanceState + ", intent:" +
              getIntent());
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

        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                dismissDialog(DIALOG_LOADING);
                if (msg.what == LoadFileThread.RESULT_DATA) {
                    LoadFileData data = (LoadFileData)msg.obj;
                    ExpandableListAdapter adapter =
                        new SimpleExpandableListAdapter(PasswdSafe.this,
                                                        data.itsGroupData,
                                                        android.R.layout.simple_expandable_list_item_1,
                                                        new String[] { GROUP },
                                                        new int[] { android.R.id.text1 },
                                                        data.itsChildData,
                                                        android.R.layout.simple_expandable_list_item_1,
                                                        new String[] { TITLE },
                                                        new int[] { android.R.id.text1 });
                    setListAdapter(adapter);
                    Log.i(TAG, "adapter set");

                } else {
                    Exception e = (Exception)msg.obj;
                    new AlertDialog.Builder(PasswdSafe.this)
                    .setMessage(e.toString())
                    .setCancelable(false)
                    .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            PasswdSafe.this.finish();
                        }
                    })
                    .show();
                }
            }
        };

        LoadFileThread thr = new LoadFileThread(intent.getData().getPath(),
                                                new StringBuilder(passwd),
                                                handler);
        thr.start();
    }

    private static final String getGroup(PwsRecord rec, PwsFile file)
    {
        switch (file.getFileVersionMajor()) {
            case PwsFileV3.VERSION:
                return rec.getField(PwsRecordV3.GROUP).toString();
            default:
                return "TODO";
        }
    }

    private static final String getTitle(PwsRecord rec, PwsFile file)
    {
        switch (file.getFileVersionMajor()) {
            case PwsFileV3.VERSION:
                return rec.getField(PwsRecordV3.TITLE).toString();
            default:
                return "TODO";
        }
    }


    private static final class LoadFileData
    {
        public final ArrayList<Map<String, String>> itsGroupData;
        public final ArrayList<ArrayList<HashMap<String, Object>>> itsChildData;

        public LoadFileData(ArrayList<Map<String, String>> groupData,
                            ArrayList<ArrayList<HashMap<String, Object>>> childData)
        {
            itsGroupData = groupData;
            itsChildData = childData;
        }
    }

    private static final class LoadFileThread extends Thread
    {
        private final String itsFile;
        private StringBuilder itsPasswd;
        private final Handler itsMsgHandler;

        public static final int RESULT_DATA = 0;
        public static final int RESULT_EXCEPTION = 1;

        public LoadFileThread(String file,
                              StringBuilder passwd,
                              Handler msgHandler) {
            itsFile = file;
            itsPasswd = passwd;
            itsMsgHandler = msgHandler;
        }

        @Override
        public void run() {
            LoadFileData data = null;
            Exception resultException = null;
            try {
                // TODO: on pause, close file, clear password, etc.
                // TODO: Remember last file used somewhere?
                Log.i(TAG, "before load file");
                PwsFile pwsfile = PwsFileFactory.loadFile(itsFile, itsPasswd);
                itsPasswd = null;
                Log.i(TAG, "after load file");

                ArrayList<Map<String, String>> groupData =
                    new ArrayList<Map<String, String>>();
                ArrayList<ArrayList<HashMap<String, Object>>> childData =
                    new ArrayList<ArrayList<HashMap<String, Object>>>();

                TreeMap<String, ArrayList<PwsRecord>> recsByGroup =
                    new TreeMap<String, ArrayList<PwsRecord>>();
                Iterator<PwsRecord> recIter = pwsfile.getRecords();
                while (recIter.hasNext()) {
                    PwsRecord rec = recIter.next();
                    String group = getGroup(rec, pwsfile);
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
                        recInfo.put(TITLE, getTitle(rec, pwsfile));
                        recInfo.put(RECORD, rec);
                        children.add(recInfo);
                    }

                    Collections.sort(children, comp);
                    childData.add(children);
                }
                Log.i(TAG, "adapter data created");
                data = new LoadFileData(groupData, childData);
            } catch (Exception e) {
                Log.e(TAG, "Exception", e);
                resultException = e;
            }

            Message msg;
            if (data != null) {
                msg = Message.obtain(itsMsgHandler, RESULT_DATA, data);
            } else {
                msg = Message.obtain(itsMsgHandler, RESULT_EXCEPTION,
                                     resultException);
            }
            itsMsgHandler.sendMessage(msg);
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