/*
 * Copyright (©) 2023 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.RecyclerView;

import net.tjado.passwdsafe.db.BackupFile;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.view.GuiUtils;
import net.tjado.passwdsafe.view.ConfirmPromptDialog;

import java.util.List;

/**
 * A fragment for backup files
 */
public class BackupFilesFragment extends Fragment
        implements ConfirmPromptDialog.Listener, View.OnClickListener
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
    {
        /** Open a file */
        void openFile(Uri uri);

        /** Update the view for the backup files */
        void updateViewBackupFiles();
    }

    /** Action confirmed via ConfirmPromptDialog */
    private enum ConfirmAction
    {
        /** Delete all backups */
        DELETE_ALL,
        /** Delete selected backups */
        DELETE_SELECTED
    }

    private Listener itsListener;
    private BackupFilesModel itsBackupFiles;
    private BackupFilesAdapter itsBackupFilesAdapter;
    private SelectionKeyProvider itsKeyProvider;
    private SelectionTracker<Long> itsSelTracker;
    private ActionMode itsActionMode;
    private View itsHelp;

    private static final String CONFIRM_ARG_ACTION = "action";

    private static final String TAG = "BackupFilesFragment";

    /**
     * Create a new instance
     */
    public static BackupFilesFragment newInstance()
    {
        return new BackupFilesFragment();
    }

    @Override
    public void onAttach(@NonNull Context ctx)
    {
        super.onAttach(ctx);
        itsListener = (Listener)ctx;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        itsBackupFiles = new ViewModelProvider(requireActivity())
                .get(BackupFilesModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        View rootView =
                inflater.inflate(R.layout.fragment_backup_files_list, container,
                                 false);

        RecyclerView files = rootView.findViewById(R.id.files);
        itsBackupFilesAdapter = new BackupFilesAdapter();
        files.setAdapter(itsBackupFilesAdapter);
        itsBackupFiles.getBackupFiles().observe(
                getViewLifecycleOwner(), itsBackupFilesAdapter::submitList);

        itsKeyProvider = new SelectionKeyProvider();
        itsSelTracker = new SelectionTracker.Builder<>(
                "backup-file-selection",
                files, itsKeyProvider,
                itsBackupFilesAdapter.createItemLookup(files),
                StorageStrategy.createLongStorage())
                .withSelectionPredicate(
                        SelectionPredicates.createSelectSingleAnything())
                .withOnItemActivatedListener((item, e) -> {
                    Long key = item.getSelectionKey();
                    if (key != null) {
                        itsSelTracker.select(key);
                    }
                    return true;
                })
                .build();
        itsBackupFilesAdapter.setSelectionTracker(itsSelTracker);
        itsSelTracker.addObserver(new SelectionObserver());

        itsHelp = rootView.findViewById(R.id.help);
        ImageButton helpCloseBtn = rootView.findViewById(R.id.help_close);
        helpCloseBtn.setOnClickListener(this);

        SharedPreferences prefs = Preferences.getSharedPrefs(requireContext());
        GuiUtils.setVisible(itsHelp,
                            Preferences.getFileBackupShowHelpPref(prefs));

        return rootView;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        itsListener.updateViewBackupFiles();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        if (itsSelTracker != null) {
            itsSelTracker.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState)
    {
        super.onViewStateRestored(savedInstanceState);
        if (itsSelTracker != null) {
            itsSelTracker.onRestoreInstanceState(savedInstanceState);
            onSelChanged(itsSelTracker.hasSelection());
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.fragment_backup_files, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu)
    {
        super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.findItem(R.id.menu_delete_all);
        if (item != null) {
            item.setEnabled(itsBackupFilesAdapter.getItemCount() != 0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_help) {
            GuiUtils.setVisible(itsHelp, true);
        } else if (itemId == R.id.menu_delete_all) {
            showPrompt(ConfirmAction.DELETE_ALL);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v)
    {
        int id = v.getId();
        if (id == R.id.help_close) {
            SharedPreferences prefs =
                    Preferences.getSharedPrefs(requireContext());
            Preferences.setFileBackupShowHelpPref(false, prefs);
            GuiUtils.setVisible(itsHelp, false);
        }
    }

    @Override
    public void promptConfirmed(Bundle confirmArgs)
    {
        PasswdSafeUtil.dbginfo(TAG, "promptConfirmed: %s", confirmArgs);

        switch (ConfirmAction.valueOf(
                confirmArgs.getString(CONFIRM_ARG_ACTION))) {
        case DELETE_ALL: {
            itsBackupFiles.deleteAll();
            break;
        }
        case DELETE_SELECTED: {
            for (Long selected : itsSelTracker.getSelection()) {
                if (selected != null) {
                    PasswdSafeUtil.dbginfo(TAG, "delete %d", selected);
                    itsBackupFiles.delete(selected);
                }
            }
            itsSelTracker.clearSelection();
            break;
        }
        }
    }

    @Override
    public void promptCanceled()
    {
    }

    /**
     * Handle a change in the selection state
     */
    private void onSelChanged(boolean hasSelection)
    {
        if (hasSelection && (itsActionMode == null)) {
            itsActionMode = requireActivity().startActionMode(
                    new ActionModeCallback());
        } else if (itsActionMode != null) {
            if (hasSelection) {
                itsActionMode.invalidate();
            } else {
                itsActionMode.finish();
            }
        }
    }

    /**
     * Open the selected backup file
     */
    private void openSelectedBackup()
    {
        BackupFile backup = getSelectedBackup();
        if (backup != null) {
            PasswdSafeUtil.dbginfo(TAG, "open %d", backup.id);
            itsListener.openFile(backup.createUri());
        }
        itsSelTracker.clearSelection();
    }

    /**
     * Delete the selected backups
     */
    private void deleteSelectedBackups()
    {
        showPrompt(ConfirmAction.DELETE_SELECTED);
    }

    /**
     * Get the selected backup file; null if none
     */
    private @Nullable BackupFile getSelectedBackup()
    {
        for (Long selected : itsSelTracker.getSelection()) {
            if (selected != null) {
                int pos = itsKeyProvider.getPosition(selected);
                if (pos >= 0) {
                    return itsBackupFilesAdapter.getCurrentList().get(pos);
                }
            }
        }
        return null;
    }

    /**
     * Show a confirmation prompt for an action
     */
    private void showPrompt(ConfirmAction action)
    {
        String title = null;
        String confirm = null;
        switch (action) {
        case DELETE_ALL: {
            title = getString(R.string.delete_all_backups_p);
            confirm = getString(R.string.delete_all);
            break;
        }
        case DELETE_SELECTED: {
            title = getString(R.string.delete_backup_p);
            confirm = getString(R.string.delete);
            break;
        }
        }

        Bundle confirmArgs = new Bundle();
        confirmArgs.putString(CONFIRM_ARG_ACTION, action.name());
        ConfirmPromptDialog dialog = ConfirmPromptDialog.newInstance(
                title, null, confirm, confirmArgs);
        dialog.setTargetFragment(this, 0);
        dialog.show(getParentFragmentManager(), "prompt");
    }

    /**
     * Action mode callbacks
     */
    private class ActionModeCallback implements ActionMode.Callback
    {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu)
        {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.fragment_backup_file, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu)
        {
            BackupFile backup = getSelectedBackup();
            MenuItem item = menu.findItem(R.id.menu_file_open);
            if ((backup != null) && (item != null)) {
                GuiUtils.setMenuEnabled(item, backup.hasFile);
            }
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item)
        {
            final int itemId = item.getItemId();
            if (itemId == R.id.menu_file_open) {
                openSelectedBackup();
                return true;
            } else if (itemId == R.id.menu_delete) {
                deleteSelectedBackups();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode)
        {
            itsSelTracker.clearSelection();
            itsActionMode = null;
        }
    }

    /**
     * Selection observer
     */
    private class SelectionObserver
            extends SelectionTracker.SelectionObserver<Long>
    {
        @Override
        public void onSelectionChanged()
        {
            super.onSelectionChanged();
            onSelChanged(itsSelTracker.hasSelection());
        }
    }

    /**
     * Selection key provider.  The number of items should be small, so linear
     * searches shouldn't be too slow.
     */
    private class SelectionKeyProvider extends ItemKeyProvider<Long>
    {
        /**
         * Constructor
         */
        protected SelectionKeyProvider()
        {
            super(ItemKeyProvider.SCOPE_CACHED);
        }

        @Override
        public Long getKey(int position)
        {
            BackupFile file =
                    itsBackupFilesAdapter.getCurrentList().get(position);
            return (file != null) ? file.id : null;
        }

        @Override
        public int getPosition(@NonNull Long key)
        {
            final List<BackupFile> currentList =
                    itsBackupFilesAdapter.getCurrentList();
            for (int pos = 0; pos < currentList.size(); ++pos) {
                if (currentList.get(pos).id == key) {
                    return pos;
                }
            }
            return RecyclerView.NO_POSITION;
        }
    }
}
