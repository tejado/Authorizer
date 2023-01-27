/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import net.tjado.passwdsafe.lib.ApiCompat;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;

import java.util.UUID;

public class LauncherFileShortcuts extends AppCompatActivity
        implements StorageFileListFragment.Listener
{
    public static final String EXTRA_IS_DEFAULT_FILE = "isDefFile";

    private static final String TAG = "LauncherFileShortcuts";

    private boolean itsIsDefaultFile = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        PasswdSafeApp.setupDialogTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher_file_shortcuts);

        FragmentManager fragMgr = getSupportFragmentManager();
        FragmentTransaction txn = fragMgr.beginTransaction();
        txn.commit();

        if (savedInstanceState == null) {
            openFileChooser();
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
        openFileChooser();
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
    }

    @Override
    public void openFile(Uri uri, String fileName)
    {
        if (itsIsDefaultFile || (uri != null)) {
            Intent openIntent = null;
            if (uri != null) {
                openIntent = PasswdSafeUtil.createOpenIntent(uri, null);
            }

            Intent intent = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && !itsIsDefaultFile) {
                ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(this, UUID.randomUUID().toString())
                        .setShortLabel(fileName)
                        .setLongLabel(fileName)
                        .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher_passwdsafe))
                        .setIntent(openIntent)
                        .build();
                ShortcutManager sm = this.getSystemService(ShortcutManager.class);
                intent = sm.createShortcutResultIntent(shortcutInfo);
            } else {
                intent = new Intent();
                intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, openIntent);
                intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, fileName);
                intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                                Intent.ShortcutIconResource.fromContext(
                                        this, R.mipmap.ic_launcher_passwdsafe));
            }

            setResult(RESULT_OK, intent);
        }

        finish();
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
    public void updateViewFiles()
    {
    }

    /**
     * Set the file chooser fragment
     */
    private void openFileChooser()
    {
        Fragment frag = new StorageFileListFragment();
        FragmentManager fragMgr = getSupportFragmentManager();
        FragmentTransaction txn = fragMgr.beginTransaction();
        txn.replace(R.id.files, frag);
        txn.commit();
    }
}
