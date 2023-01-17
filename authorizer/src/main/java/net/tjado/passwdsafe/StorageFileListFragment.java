/*
 * Copyright (©) 2022 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import net.tjado.passwdsafe.db.PasswdSafeDb;
import net.tjado.passwdsafe.db.RecentFilesDao;
import net.tjado.passwdsafe.file.PasswdFileUri;
import net.tjado.passwdsafe.lib.ActContext;
import net.tjado.passwdsafe.lib.ApiCompat;
import net.tjado.passwdsafe.lib.DocumentsContractCompat;
import net.tjado.passwdsafe.lib.ManagedRef;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.view.GuiUtils;

import java.util.List;

/**
 *  The StorageFileListFragment fragment allows the user to open files using
 *  the storage access framework on Kitkat and higher
 */
@TargetApi(19)
public final class StorageFileListFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>,
                   View.OnClickListener,
                   StorageFileListOps
{
    /** Listener interface for the owning activity */
    public interface Listener
    {
        /** Open a file */
        void openFile(Uri uri, String fileName);

        /** Does the activity have a menu */
        boolean activityHasMenu();

        /** Does the activity have a 'none' item */
        boolean activityHasNoneItem();

        /** Update the view for a list of files */
        void updateViewFiles();
    }

    private static final String TAG = "StorageFileListFragment";

    private static final int OPEN_RC = 1;

    private static final int LOADER_FILES = 0;

    private Listener itsListener;
    private RecentFilesDao itsRecentFilesDao;
    private ManagedRef<RecentFilesDao> itsRecentFilesDaoRef;
    private View itsEmptyText;
    private View itsFab;
    private boolean itsIsFabBounced = false;
    private boolean itsIsDebugOpened = false;
    private StorageFileListAdapter itsFilesAdapter;
    private int itsFileIcon;
    private Uri itsLastOpenUri;


    @Override
    public void onAttach(@NonNull Context ctx)
    {
        super.onAttach(ctx);
        itsListener = (Listener)ctx;

        Resources.Theme theme = ctx.getTheme();
        TypedValue attr = new TypedValue();
        theme.resolveAttribute(R.attr.drawablePasswdsafe, attr, true);
        itsFileIcon = attr.resourceId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        itsRecentFilesDao =
                PasswdSafeDb.get(requireContext()).accessRecentFiles();
        itsRecentFilesDaoRef = new ManagedRef<>(itsRecentFilesDao);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        boolean hasMenu = itsListener.activityHasMenu();
        if (hasMenu) {
            setHasOptionsMenu(true);
        }

        View rootView = inflater.inflate(R.layout.fragment_storage_file_list,
                                         container, false);

        itsEmptyText = rootView.findViewById(R.id.empty);
        GuiUtils.setVisible(itsEmptyText, false);

        itsFilesAdapter = new StorageFileListAdapter(this);
        RecyclerView files = rootView.findViewById(R.id.files);
        files.setAdapter(itsFilesAdapter);

        itsFab = rootView.findViewById(R.id.fab);
        View noDefault = rootView.findViewById(R.id.no_default);
        if (hasMenu) {
            ItemTouchHelper.SimpleCallback swipeCb =
                    new ItemTouchHelper.SimpleCallback(
                            0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT)
                    {
                        @Override
                        public float getSwipeEscapeVelocity(float defaultValue)
                        {
                            return defaultValue * 7f;
                        }

                        @Override
                        public float getSwipeThreshold(
                                @NonNull RecyclerView.ViewHolder viewHolder)
                        {
                            return .75f;
                        }

                        @Override
                        public boolean onMove(
                                @NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder,
                                @NonNull RecyclerView.ViewHolder target)
                        {
                            return false;
                        }

                        @Override
                        public void onSwiped(
                                @NonNull RecyclerView.ViewHolder viewHolder,
                                int direction)
                        {
                            removeFile(((StorageFileListHolder)viewHolder).getUri());
                        }
                    };
            ItemTouchHelper swipeHelper = new ItemTouchHelper(swipeCb);
            swipeHelper.attachToRecyclerView(files);

            itsFab.setOnClickListener(this);
        } else {
            GuiUtils.setVisible(itsFab, false);

            // Wrap content for entries when shown in chooser dialog
            files.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            rootView.getLayoutParams().height =
                    ViewGroup.LayoutParams.WRAP_CONTENT;
        }

        if (itsListener.activityHasNoneItem()) {
            noDefault.setOnClickListener(this);
        } else {
            GuiUtils.setVisible(noDefault, false);
        }

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View fragView, Bundle savedInstanceState)
    {
        super.onViewCreated(fragView, savedInstanceState);
        Context ctx = getContext();
        if (ctx == null) {
            return;
        }

        LoaderManager.getInstance(this).initLoader(LOADER_FILES, null, this);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        LoaderManager.getInstance(this).restartLoader(LOADER_FILES, null, this);
        itsListener.updateViewFiles();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        itsRecentFilesDao = null;
        itsRecentFilesDaoRef.clear();
        itsRecentFilesDaoRef = null;
    }

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

    @Override
    public void onClick(View v)
    {
        int id = v.getId();
        if (id == R.id.fab) {
            startOpenFile();
        } else if (id == R.id.no_default) {
            openUri(null, null);
        }
    }

    @Override
    public void storageFileClicked(String uristr, String title)
    {
        Uri uri = Uri.parse(uristr);
        openUri(uri, title);
    }

    @Override
    public int getStorageFileIcon()
    {
        return itsFileIcon;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.fragment_storage_file_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_file_open) {
            startOpenFile();
            return true;
        } else if (itemId == R.id.menu_file_new) {
            startActivity(new Intent(PasswdSafeUtil.NEW_INTENT));
            return true;
        } else if (itemId == R.id.menu_clear_recent) {
            try {
                Context ctx = getContext();
                if (ctx == null) {
                    return true;
                }
                itsRecentFilesDao.deleteAll();
                LoaderManager.getInstance(this).restartLoader(LOADER_FILES,
                                                              null, this);
            } catch (Exception e) {
                PasswdSafeUtil.showFatalMsg(e, "Clear recent error",
                                            getActivity());
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle)
    {
        return new FileLoader(itsRecentFilesDaoRef, requireContext());
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader,
                               Cursor cursor)
    {
        boolean empty = (cursor == null) || (cursor.getCount() == 0);
        GuiUtils.setVisible(itsEmptyText, empty);
        if (empty && !itsIsFabBounced) {
            bounceView(itsFab);
            itsIsFabBounced = true;
        }
        itsFilesAdapter.changeCursor(cursor);

        //noinspection ConstantConditions
        if ((PasswdSafeApp.DEBUG_AUTO_FILE != null) &&
            !empty && !itsIsDebugOpened && !PasswdSafeUtil.isTesting()) {
            itsIsDebugOpened = true;
            Uri rootUri = ApiCompat.getPrimaryStorageRootUri(
                    requireContext());
            if (rootUri != null) {
                rootUri = rootUri.buildUpon().path(
                        PasswdSafeApp.DEBUG_AUTO_FILE).build();
                openUri(rootUri, PasswdSafeApp.DEBUG_AUTO_FILE);
                return;
            }
        }

        // Open the default file
        Activity act = requireActivity();
        PasswdSafeApp app = (PasswdSafeApp)act.getApplication();
        if (app.checkOpenDefault()) {
            SharedPreferences prefs = Preferences.getSharedPrefs(act);
            Uri defFile = Preferences.getDefFilePref(prefs);
            if (defFile != null) {
                try {
                    itsRecentFilesDao.touchFile(defFile);
                } catch (Exception e) {
                    Log.e(TAG, "Error touching file", e);
                }
                itsListener.openFile(defFile, null);
            }
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader)
    {
        onLoadFinished(cursorLoader, null);
    }

    /** Start the intent to open a file */
    private void startOpenFile()
    {
        Intent intent = new Intent(
                DocumentsContractCompat.INTENT_ACTION_OPEN_DOCUMENT);

        Uri initialUri = (itsLastOpenUri != null) ? itsLastOpenUri :
                ApiCompat.getPrimaryStorageRootUri(requireContext());
        if (initialUri != null) {
            intent.putExtra(DocumentsContractCompat.EXTRA_INITIAL_URI,
                            initialUri);
        }

        intent.putExtra(DocumentsContractCompat.EXTRA_PROMPT,
                        getString(R.string.open_password_file));
        intent.putExtra(DocumentsContractCompat.EXTRA_SHOW_ADVANCED, true);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setType("application/*");

        startActivityForResult(intent, OPEN_RC);
    }


    /** Open a password file URI from an intent */
    private void openUri(Intent openIntent)
    {
        Context ctx = requireContext();

        Uri uri = openIntent.getData();
        if (uri == null) {
            PasswdSafeUtil.showError("No URI to open: " + openIntent, TAG, null,
                                     new ActContext(ctx));
            return;
        }

        int flags = openIntent.getFlags() &
                    (Intent.FLAG_GRANT_READ_URI_PERMISSION |
                     Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        String title = RecentFilesDao.getSafDisplayName(uri, ctx);
        if (isCheckPermissions()) {
            RecentFilesDao.updateOpenedSafFile(uri, flags, ctx);
        } else {
            if (title == null) {
                title = openIntent.getStringExtra("__test_display_name");
            }
        }
        if (title != null) {
            itsLastOpenUri = uri;
            openUri(uri, title);
        }
    }


    /** Open a password file URI */
    private void openUri(Uri uri, String title)
    {
        PasswdSafeUtil.dbginfo(TAG, "openUri %s: %s", uri, title);

        if (uri != null) {
            try {
                itsRecentFilesDao.insertOrUpdate(uri, title);
            } catch (Exception e) {
                Log.e(TAG, "Error inserting recent file", e);
            }
        }

        itsListener.openFile(uri, title);
    }

    /**
     * Remove a recent file entry
     */
    private void removeFile(String uristr)
    {
        try {
            Uri uri = Uri.parse(uristr);
            itsRecentFilesDao.removeUri(uristr);

            if (isCheckPermissions()) {
                ContentResolver cr = requireContext().getContentResolver();
                int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                ApiCompat.releasePersistableUriPermission(cr, uri, flags);
            }

            LoaderManager.getInstance(this).restartLoader(LOADER_FILES,
                                                          null, this);
        } catch (Exception e) {
            PasswdSafeUtil.showFatalMsg(e, "Remove recent file error",
                                        requireActivity());
        }
    }

    /**
     * Bounce a view
     */
    private static void bounceView(View v)
    {
        ObjectAnimator anim = ObjectAnimator.ofFloat(v, "translationY",
                                                     0, -30, 0);
        anim.setInterpolator(new BounceInterpolator());
        anim.setStartDelay(1000);
        anim.setDuration(2500);
        anim.start();
    }

    /**
     *  Whether permissions should be checked
     */
    private static boolean isCheckPermissions()
    {
        return !PasswdSafeUtil.isTesting();
    }

    /**
     * Background file loader
     */
    private static final class FileLoader extends AsyncTaskLoader<Cursor>
    {
        private final ManagedRef<RecentFilesDao> itsRecentFilesDao;

        /**
         * Constructor
         */
        private FileLoader(ManagedRef<RecentFilesDao> recentFilesDao,
                           Context ctx)
        {
            super(ctx.getApplicationContext());
            itsRecentFilesDao = recentFilesDao;
        }

        /** Handle when the loader is reset */
        @Override
        protected void onReset()
        {
            super.onReset();
            onStopLoading();
        }

        /** Handle when the loader is started */
        @Override
        protected void onStartLoading()
        {
            forceLoad();
        }

        /** Handle when the loader is stopped */
        @Override
        protected void onStopLoading()
        {
            cancelLoad();
        }

        /** Load the files in the background */
        @Override
        public Cursor loadInBackground()
        {
            PasswdSafeUtil.dbginfo(TAG, "loadInBackground");

            RecentFilesDao recentFilesDb = itsRecentFilesDao.get();
            if (recentFilesDb == null) {
                return null;
            }

            if (isCheckPermissions()) {
                Context ctx = getContext();
                ContentResolver cr = ctx.getContentResolver();

                // Check default file
                SharedPreferences prefs = Preferences.getSharedPrefs(ctx);
                Uri defaultFile = Preferences.getDefFilePref(prefs);
                if (defaultFile != null) {
                    switch (PasswdFileUri.getUriType(defaultFile)) {
                    case GENERIC_PROVIDER: {
                        checkUriPerm(defaultFile, defaultFile, cr,
                                     recentFilesDb, prefs);
                        break;
                    }
                    case FILE:
                    case EMAIL:
                    case SYNC_PROVIDER:
                    case BACKUP: {
                        break;
                    }
                    }
                }

                // Check any file for which we have permissions
                List<Uri> permUris = ApiCompat.getPersistedUriPermissions(cr);
                for (Uri permUri : permUris) {
                    checkUriPerm(permUri, defaultFile, cr, recentFilesDb,
                                 prefs);
                }
            }

            try {
                return recentFilesDb.getOrderedByDateCursor();
            } catch (Exception e) {
                Log.e(TAG, "Files load error", e);
            }
            return null;
        }

        /**
         * Check permissions on a URI
         */
        private static void checkUriPerm(Uri uri,
                                         Uri defaultFile,
                                         ContentResolver cr,
                                         RecentFilesDao recentFilesDao,
                                         SharedPreferences prefs)
        {
            PasswdSafeUtil.dbginfo(TAG, "Checking persist perm %s", uri);

            boolean doRemove = false;
            try (Cursor ignored = cr.query(uri, null, null, null, null)) {
                // Acquire the cursor to attempt to launch the app providing
                // the file
                ApiCompat.takePersistableUriPermission(
                        cr, uri,
                        (Intent.FLAG_GRANT_READ_URI_PERMISSION |
                         Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
            } catch (Exception e) {
                Log.e(TAG, "Take permission error for: " + uri, e);
                doRemove = true;
            }

            if (doRemove) {
                try {
                    recentFilesDao.removeUri(uri.toString());
                } catch (Exception e) {
                    Log.e(TAG, "Recent files remove error: " + uri, e);
                }

                if (uri.equals(defaultFile)) {
                    Preferences.clearDefFilePref(prefs);
                }
            }
        }
    }
}
