/*
 * Copyright (©) 2011-2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdFileDataUser;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.util.ObjectHolder;
import com.jefftharris.passwdsafe.util.Pair;
import com.jefftharris.passwdsafe.view.PasswdFileDataView;
import com.jefftharris.passwdsafe.view.PasswdLocation;
import com.jefftharris.passwdsafe.view.PasswdRecordListData;

import org.pwsafe.lib.file.PwsRecord;

import java.util.List;

public class LauncherRecordShortcuts extends AppCompatActivity
        implements PasswdSafeListFragment.Listener
{
    private static final String TAG = "LauncherRecordShortcuts";

    private final PasswdFileDataView itsFileDataView = new PasswdFileDataView();
    private PasswdLocation itsLocation = new PasswdLocation();
    private TextView itsFile;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher_record_shortcuts);
        setTitle(R.string.shortcut_record);
        itsFile = (TextView)findViewById(R.id.file);

        Intent intent = getIntent();
        if (!Intent.ACTION_CREATE_SHORTCUT.equals(intent.getAction())) {
            finish();
            return;
        }

        itsFileDataView.onAttach(this);

        if (savedInstanceState == null) {
            FragmentManager fragMgr = getSupportFragmentManager();
            FragmentTransaction txn = fragMgr.beginTransaction();
            txn.replace(R.id.contents,
                        PasswdSafeListFragment.newInstance(itsLocation, true));
            txn.commit();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        itsFileDataView.clearFileData();
        final ObjectHolder<String> fileTitle = new ObjectHolder<>();
        PasswdSafeFileDataFragment.useOpenFileData(
                new PasswdFileDataUser()
                {
                    @Override
                    public void useFileData(
                            @NonNull PasswdFileData fileData)
                    {
                        itsFileDataView.setFileData(fileData);
                        fileTitle.set(fileData.getUri().getIdentifier(
                                LauncherRecordShortcuts.this, true));
                    }
                });
        String fileTitleVal = fileTitle.get();
        if (fileTitleVal != null) {
            itsFile.setText(fileTitleVal);
        } else {
            itsFile.setText(R.string.no_records_open_file);
            GuiUtils.setVisible(findViewById(R.id.contents), false);
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        itsFileDataView.clearFileData();
    }

    @Override
    public void onDestroy()
    {
        itsFileDataView.onDetach();
        super.onDestroy();
    }

    @Override
    public void changeLocation(PasswdLocation location)
    {
        PasswdSafeUtil.dbginfo(TAG, "changeLocation: ", location);
        if (location.isRecord()) {
            final String uuid = location.getRecord();
            final ObjectHolder<Pair<Uri, String>> rc = new ObjectHolder<>();
            PasswdSafeFileDataFragment.useOpenFileData(
                    new PasswdFileDataUser()
                    {
                        @Override
                        public void useFileData(
                                @NonNull PasswdFileData fileData)
                        {
                            PwsRecord rec = fileData.getRecord(uuid);
                            String title = fileData.getTitle(rec);
                            rc.set(new Pair<>(fileData.getUri().getUri(),
                                              title));
                        }
                    });
            Pair<Uri, String> rcval = rc.get();
            if (rcval != null) {
                Intent shortcutIntent = PasswdSafeUtil.createOpenIntent(
                        rcval.first, uuid);

                Intent intent = new Intent();
                intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, rcval.second);
                intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                                Intent.ShortcutIconResource.fromContext(
                                        this, R.mipmap.ic_launcher_passwdsafe));
                setResult(RESULT_OK, intent);
            }
            finish();
        } else if (!itsLocation.equals(location)) {
            FragmentManager fragMgr = getSupportFragmentManager();
            FragmentTransaction txn = fragMgr.beginTransaction();
            txn.setTransition(FragmentTransaction.TRANSIT_NONE);
            txn.replace(R.id.contents,
                        PasswdSafeListFragment.newInstance(location, true));
            txn.addToBackStack(null);
            txn.commit();
        }
    }

    @Override
    public List<PasswdRecordListData> getBackgroundRecordItems(
            boolean incRecords, boolean incGroups)
    {
        return itsFileDataView.getRecords(incRecords, incGroups);
    }

    @Override
    public void updateViewList(PasswdLocation location)
    {
        PasswdSafeUtil.dbginfo(TAG, "updateViewList: ", location);
        itsLocation = location;
        itsFileDataView.setCurrGroups(itsLocation.getGroups());

        FragmentManager fragMgr = getSupportFragmentManager();
        Fragment contentsFrag = fragMgr.findFragmentById(R.id.contents);
        if (contentsFrag instanceof PasswdSafeListFragment) {
            ((PasswdSafeListFragment)contentsFrag).updateLocationView(
                    itsLocation, PasswdSafeListFragment.Mode.ALL);
        }
    }
}
