/*
 * Copyright (©) 2023 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;

import net.tjado.passwdsafe.file.PasswdFileDataUser;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.view.GuiUtils;

import org.pwsafe.lib.exception.RecordLoadException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

/**
 * Fragment showing a list of record errors
 */
public class PasswdSafeRecordErrorsFragment extends ListFragment
{
    /** Listener interface for owning activity */
    public interface Listener
            extends AbstractPasswdSafeFileDataFragment.Listener
    {
        /** Update the view for a list of record errors */
        void updateViewRecordErrors();

        void recoverRecordErrors();

        void shareFile();
    }

    private View itsNote;
    private View itsSeparator;
    private Listener itsListener;

    /**
     * Create a new instance
     */
    public static PasswdSafeRecordErrorsFragment newInstance()
    {
        return new PasswdSafeRecordErrorsFragment();
    }

    @Override
    public void onAttach(@NonNull Context ctx)
    {
        super.onAttach(ctx);
        itsListener = (Listener)ctx;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        View root = inflater.inflate(R.layout.fragment_passwdsafe_record_errors,
                                container, false);
        itsNote = root.findViewById(R.id.note);
        itsSeparator = root.findViewById(R.id.separator);
        return root;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        itsListener.updateViewRecordErrors();
        refresh();
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        itsListener = null;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu,
                                    @NonNull MenuInflater inflater)
    {
        if ((itsListener != null)) {
            inflater.inflate(R.menu.fragment_passwdsafe_record_errors, menu);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int menuId = item.getItemId();
        if (menuId == R.id.menu_save) {
            itsListener.recoverRecordErrors();
            return true;
        } else if (menuId == R.id.menu_share) {
            itsListener.shareFile();
            return true;
        } else if (menuId == R.id.menu_copy_clipboard) {
            copyErrorsToClipboard();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Refresh the list of errors
     */
    private void refresh()
    {
        var errors = new ArrayList<RecordError>();
        var ctx = requireContext();

        itsListener.useFileData((PasswdFileDataUser<Void>) fileData -> {
            var recordErrors = fileData.getRecordErrors();
            if (recordErrors != null) {
                for (var err: recordErrors) {
                    errors.add(new RecordError(err, ctx));
                }
            }
            return null;
        });

        GuiUtils.setVisible(itsNote, !errors.isEmpty());
        GuiUtils.setVisible(itsSeparator, !errors.isEmpty());
        setListAdapter(new RecordErrorAdapter(errors, ctx));
        requireActivity().invalidateOptionsMenu();
    }

    /**
     * Copy the errors to the clipboard
     */
    private void copyErrorsToClipboard()
    {
        var ctx = requireContext();
        StringBuilder sb = new StringBuilder();
        itsListener.useFileData((PasswdFileDataUser<Void>) fileData -> {
            var recordErrors = fileData.getRecordErrors();
            if (recordErrors != null) {
                for (var err: recordErrors) {
                    sb.append(RecordError.errorToString(err, ctx));
                    sb.append("\n\n");
                }
            }
            return null;
        });
        PasswdSafeUtil.copyToClipboard(sb.toString(), false, ctx);
    }

    /**
     * A record error to show in the list
     */
    private static class RecordError
    {
        private final String itsError;

        protected RecordError(RecordLoadException rle, Context ctx)
        {
            itsError = errorToString(rle, ctx);
        }

        protected static String errorToString(RecordLoadException rle,
                                              Context ctx)
        {
            StringWriter sb = new StringWriter();
            PrintWriter printer = new PrintWriter(sb);
            sb.append(ctx.getString(R.string.record)).append(": ")
              .append(rle.itsRecord.toString());
            for (var err: rle.itsErrors) {
                sb.append("\n\n");
                err.printStackTrace(printer);
            }
            return sb.toString();
        }

        @NonNull
        @Override
        public String toString()
        {
            return itsError;
        }
    }

    /**
     * Array list adaptor for RecordErrors
     */
    private static class RecordErrorAdapter extends ArrayAdapter<RecordError>
    {
        protected RecordErrorAdapter(@NonNull ArrayList<RecordError> errors,
                                     @NonNull Context ctx)
        {
            super(ctx, android.R.layout.simple_list_item_1, errors);
        }
    }
}
