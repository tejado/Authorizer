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
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;

import net.tjado.passwdsafe.lib.ApiCompat;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;

public class LauncherFileShortcuts extends AppCompatActivity
        implements FileListFragment.Listener,
                   StorageFileListFragment.Listener
{
    public static final String EXTRA_IS_DEFAULT_FILE = "isDefFile";

    private static final String TAG = "LauncherFileShortcuts";

    private Boolean itsIsStorageFrag = null;
    private boolean itsIsDefaultFile = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        PasswdSafeApp.setupDialogTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher_file_shortcuts);

        FragmentManager fragMgr = getSupportFragmentManager();
        FragmentTransaction txn = fragMgr.beginTransaction();
        //txn.replace(R.id.sync, new SyncProviderFragment());
        txn.commit();

        if (savedInstanceState == null) {
            setFileChooseFrag();
        }

        Intent intent = getIntent();
        if (!Intent.ACTION_CREATE_SHORTCUT.equals(intent.getAction())) {
            finish();
            return;
        }

        itsIsDefaultFile = intent.getBooleanExtra(EXTRA_IS_DEFAULT_FILE, false);
        if (itsIsDefaultFile) {
            setTitle(R.string.default_file_to_open);
        } else {
            setTitle(R.string.shortcut_file);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        setFileChooseFrag();
    }

    @Override
    public void onBackPressed()
    {
        FragmentManager mgr = getSupportFragmentManager();
        Fragment frag = mgr.findFragmentById(R.id.files);
        boolean handled = (frag instanceof FileListFragment) &&
                          frag.isVisible() &&
                          ((FileListFragment) frag).doBackPressed();

        if (!handled) {
            super.onBackPressed();
        }
    }

    @Override
    public void openFile(Uri uri, String fileName)
    {
        if (itsIsDefaultFile || (uri != null)) {
            Intent openIntent = null;
            if (uri != null) {
                openIntent = PasswdSafeUtil.createOpenIntent(uri, null);
            }

            Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, openIntent);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, fileName);
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                            Intent.ShortcutIconResource.fromContext(
                                this, R.mipmap.ic_launcher_passwdsafe));
            setResult(RESULT_OK, intent);
        }

        finish();
    }

    @Override
    public void createNewFile(Uri dirUri)
    {
    }

    @Override
    public boolean activityHasMenu()
    {
        return false;
    }

    @Override
    public boolean activityHasNoneItem()
    {
        return itsIsDefaultFile;
    }

    @Override
    public boolean appHasFilePermission()
    {
        return false;
    }

    @Override
    public void updateViewFiles()
    {
    }

    /**
     * Set the file chooser fragment
     */
    private void setFileChooseFrag()
    {
        SharedPreferences prefs = Preferences.getSharedPrefs(this);
        boolean storageFrag =
                ((ApiCompat.SDK_VERSION >= ApiCompat.SDK_KITKAT) &&
                 !Preferences.getFileLegacyFileChooserPref(prefs));
        if ((itsIsStorageFrag == null) || (itsIsStorageFrag != storageFrag)) {
            PasswdSafeUtil.dbginfo(TAG, "setFileChooseFrag storage %b",
                                   storageFrag);
            Fragment frag;
            if (storageFrag) {
                frag = new StorageFileListFragment();
            } else {
                frag = new FileListFragment();
            }
            FragmentManager fragMgr = getSupportFragmentManager();
            FragmentTransaction txn = fragMgr.beginTransaction();
            txn.replace(R.id.files, frag);
            txn.commit();
            itsIsStorageFrag = storageFrag;
        }
    }
}
