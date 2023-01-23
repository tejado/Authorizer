/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import net.tjado.passwdsafe.file.PasswdFileDataUser;
import net.tjado.passwdsafe.file.PasswdRecordFilter;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.view.GuiUtils;
import net.tjado.passwdsafe.util.Pair;
import net.tjado.passwdsafe.view.CopyField;
import net.tjado.passwdsafe.view.PasswdFileDataView;
import net.tjado.passwdsafe.view.PasswdLocation;
import net.tjado.passwdsafe.view.PasswdRecordListData;

import org.pwsafe.lib.file.PwsRecord;

import java.util.List;
import java.util.UUID;

public class LauncherRecordShortcuts extends AppCompatActivity
        implements PasswdSafeListFragment.Listener,
                   SharedPreferences.OnSharedPreferenceChangeListener
{
    /**
     * Intent flag to apply a filter to show records that have no aliases
     * referencing them
     */
    public static final String FILTER_NO_ALIAS = "filterNoAlias";

    /**
     * Intent flag to apply a filter to show records that have no shortcuts
     * referencing them
     */
    public static final String FILTER_NO_SHORTCUT = "filterNoShortcut";

    private enum Mode
    {
        SHORTCUT,
        CHOOSE_RECORD
    }

    private static final String TAG = "LauncherRecordShortcuts";

    private final PasswdFileDataView itsFileDataView = new PasswdFileDataView();
    private PasswdLocation itsLocation = new PasswdLocation();
    private Mode itsMode;
    private TextView itsFile;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        PasswdSafeApp.setupDialogTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher_record_shortcuts);
        SharedPreferences prefs = Preferences.getSharedPrefs(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        itsFile = findViewById(R.id.file);
        itsFileDataView.onAttach(this, prefs);

        Intent intent = getIntent();
        String s = String.valueOf(intent.getAction());
        if (Intent.ACTION_CREATE_SHORTCUT.equals(s)) {
            setTitle(R.string.shortcut_record);
            itsMode = Mode.SHORTCUT;
        } else if (PasswdSafeApp.CHOOSE_RECORD_INTENT.equals(s)) {
            setTitle(R.string.choose_record);
            itsMode = Mode.CHOOSE_RECORD;
            GuiUtils.setVisible(itsFile, false);
        } else {
            finish();
            return;
        }

        int options = PasswdRecordFilter.OPTS_DEFAULT;
        if (intent.getBooleanExtra(FILTER_NO_ALIAS, false)) {
            options |= PasswdRecordFilter.OPTS_NO_ALIAS;
        }
        if (intent.getBooleanExtra(FILTER_NO_SHORTCUT, false)) {
            options |= PasswdRecordFilter.OPTS_NO_SHORTCUT;
        }
        if (options != PasswdRecordFilter.OPTS_DEFAULT) {
            itsFileDataView.setRecordFilter(new PasswdRecordFilter(null,
                                                                   options));
        }

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
        String fileTitle = PasswdSafeFileDataFragment.useOpenFileData(
                fileData -> {
                    itsFileDataView.setFileData(fileData);
                    return fileData.getUri().getIdentifier(
                            LauncherRecordShortcuts.this, true);
                });
        if (fileTitle != null) {
            itsFile.setText(fileTitle);
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
        SharedPreferences prefs = Preferences.getSharedPrefs(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        itsFileDataView.onDetach();
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs,
                                          @Nullable String key)
    {
        if (itsFileDataView.handleSharedPreferenceChanged(prefs, key)) {
            PasswdSafeFileDataFragment.useOpenFileData(
                    (PasswdFileDataUser<Void>)fileData -> {
                        itsFileDataView.refreshFileData(fileData);
                        return null;
                    });
        }
    }

    @Override
    public void copyField(CopyField field, String recUuid)
    {
        // Not supported
    }

    @Override
    public boolean isCopySupported()
    {
        return false;
    }

    @Override
    public void changeLocation(PasswdLocation location)
    {
        PasswdSafeUtil.dbginfo(TAG, "changeLocation: ", location);
        if (location.isRecord()) {
            selectRecord(location.getRecord());
        } else if (!itsLocation.equals(location)) {
            FragmentManager fragMgr = getSupportFragmentManager();
            FragmentTransaction txn = fragMgr.beginTransaction();
            txn.setTransition(FragmentTransaction.TRANSIT_NONE);
            txn.replace(R.id.contents, PasswdSafeListFragment.newInstance(location, true));
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

    @Override
    public boolean activityHasMenu()
    {
        return false;
    }

    @Override
    public void showRecordPreferences()
    {
    }

    @Override
    public boolean isNavDrawerClosed()
    {
        return true;
    }

    /**
     * Select the given record and return a result
     */
    private void selectRecord(final String uuid)
    {
        if (itsMode == Mode.SHORTCUT) {

            Pair<Uri, String> rc = PasswdSafeFileDataFragment.useOpenFileData(
                    fileData -> {
                        PwsRecord rec = fileData.getRecord(uuid);
                        String title = fileData.getTitle(rec);
                        return new Pair<>(fileData.getUri().getUri(), title);
                    });

            if (rc != null) {
                Intent shortcutIntent = PasswdSafeUtil.createOpenIntent(rc.first, uuid);

                Intent intent = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(this, UUID.randomUUID().toString())
                            .setShortLabel(rc.second)
                            .setLongLabel(rc.second)
                            .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher_passwdsafe))
                            .setIntent(shortcutIntent)
                            .build();
                    ShortcutManager sm = this.getSystemService(ShortcutManager.class);
                    intent = sm.createShortcutResultIntent(shortcutInfo);
                } else {
                    intent = new Intent();
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, rc.second);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                            Intent.ShortcutIconResource.fromContext(
                                    this, R.mipmap.ic_launcher_passwdsafe));
                }

                setResult(RESULT_OK, intent);
            }
        } else if (itsMode == Mode.CHOOSE_RECORD) {
            Intent intent = new Intent();
            intent.putExtra(PasswdSafeApp.RESULT_DATA_UUID, uuid);
            setResult(RESULT_OK, intent);
        }
        finish();
    }
}
