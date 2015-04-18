package com.jefftharris.passwdsafe;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.jefftharris.passwdsafe.lib.ApiCompat;
import com.jefftharris.passwdsafe.lib.DocumentsContractCompat;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

/**
 *  The StorageFileListFragment fragment allows the user to open files using
 *  the storage access framework on Kitkat and higher
 */
@TargetApi(19)
public class StorageFileListFragment extends ListFragment
        implements OnClickListener
{
    // TODO: new file support
    // TODO: remove file support
    // TODO: recent files list

    /** Listener interface for the owning activity */
    public interface Listener
    {
        /** Open a file */
        public void openFile(Uri uri, String fileName);

        /** Does the activity have a menu */
        public boolean activityHasMenu();
    }

    private static final String TAG = "StorageFileListFragment";

    private static final int OPEN_RC = 1;

    private Listener itsListener;


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onAttach(android.app.Activity)
     */
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        itsListener = (Listener)activity;
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        if (itsListener.activityHasMenu()) {
            setHasOptionsMenu(true);
        }

        View view = inflater.inflate(R.layout.fragment_storage_file_list,
                                     container, false);

        Button btn = (Button)view.findViewById(R.id.open);
        btn.setOnClickListener(this);

        return view;
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onResume()
     */
    @Override
    public void onResume()
    {
        super.onResume();
        PasswdSafeApp app = (PasswdSafeApp)getActivity().getApplication();
        app.closeOpenFile();
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode) {
        case OPEN_RC: {
            PasswdSafeUtil.dbginfo(TAG, "onActivityResult open %d: %s",
                                   resultCode, data);
            if ((resultCode == Activity.RESULT_OK) && (data != null)) {
                openUri(data);
            }
            break;
        }
        default: {
            super.onActivityResult(requestCode, resultCode, data);
            break;
        }
        }
    }


    /* (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    public final void onClick(View v)
    {
        switch (v.getId()) {
        case R.id.open: {
            startOpenFile();
            break;
        }
        }
    }


    /** Start the intent to open a file */
    private void startOpenFile()
    {
        Intent intent = new Intent(
                DocumentsContractCompat.INTENT_ACTION_OPEN_DOCUEMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setType("application/octet-stream");

        startActivityForResult(intent, OPEN_RC);
    }


    /** Open a password file URI */
    private void openUri(Intent openIntent)
    {
        ContentResolver cr = getActivity().getContentResolver();
        Uri uri = openIntent.getData();
        int flags = openIntent.getFlags() &
            (Intent.FLAG_GRANT_READ_URI_PERMISSION |
             Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        ApiCompat.takePersistableUriPermission(cr, uri, flags);
        Cursor cursor = cr.query(uri, null, null, null, null);
        try {
            if ((cursor != null) && (cursor.moveToFirst())) {
                String title = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                PasswdSafeUtil.dbginfo(TAG, "openUri %s: %s",
                                       uri, title);
                itsListener.openFile(uri, title);
            }
        } finally {
            cursor.close();
        }
    }
}
