/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdRecord;
import com.jefftharris.passwdsafe.lib.Utils;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.view.PasswdLocation;

import org.pwsafe.lib.file.PwsRecord;

import java.util.Date;

/**
 * Base class for showing fields of a password record
 */
public abstract class AbstractPasswdSafeRecordFragment extends Fragment
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
    {
        /** Get the file data */
        PasswdFileData getFileData();

        /** Change the location in the password file */
        void changeLocation(PasswdLocation location);

        /** Is the navigation drawer open */
        boolean isNavDrawerOpen();
    }

    protected String itsRecUuid;
    protected Listener itsListener;

    /**
     * Create arguments for new instance
     */
    protected static Bundle createArgs(String recUuid)
    {
        Bundle args = new Bundle();
        args.putString("recUuid", recUuid);
        return args;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            itsRecUuid = args.getString("recUuid");
        }
    }

    @Override
    public void onAttach(Context ctx)
    {
        super.onAttach(ctx);
        itsListener = (Listener)ctx;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        refresh();
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        itsListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        if ((itsListener != null) && !itsListener.isNavDrawerOpen()) {
            doOnCreateOptionsMenu(menu, inflater);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Derived-class create options menu
     */
    protected abstract void doOnCreateOptionsMenu(Menu menu,
                                                  MenuInflater inflater);

    /**
     * Derived-class refresh
     */
    protected abstract void doRefresh();

    /**
     * Get the record information
     */
    protected RecordInfo getRecordInfo()
    {
        if (isAdded() && (itsListener != null)) {
            PasswdFileData fileData = itsListener.getFileData();
            if (fileData != null) {
                PwsRecord rec = fileData.getRecord(itsRecUuid);
                if (rec != null) {
                    PasswdRecord passwdRec = fileData.getPasswdRecord(rec);
                    if (passwdRec != null) {
                        return new RecordInfo(rec, passwdRec, fileData);
                    }
                }
            }
        }
        return null;
    }


    /**
     * Set the value of a text field.  The field's row is visible if the text
     * isn't null.
     */
    protected static void setFieldText(TextView field,
                                       View fieldRow,
                                       String text)
    {
        field.setText(text);

        if (fieldRow != null) {
            GuiUtils.setVisible(fieldRow, (text != null));
        }
    }

    /**
     * Set the value of a date field.  The field's row is visible if the date
     * isn't null
     */
    protected void setFieldDate(TextView field, View fieldRow, Date date)
    {
        String str =
                (date != null) ? Utils.formatDate(date, getActivity()) : null;
        setFieldText(field, fieldRow, str);
    }

    /**
     * Refresh the view
     */
    private void refresh()
    {
        doRefresh();

        final View root = getView();
        if (root != null) {
            root.post(new Runnable()
            {
                @Override
                public void run()
                {
                    root.scrollTo(0, 0);
                }
            });
        }
    }

    /**
     * Wrapper class for record information
     */
    protected static class RecordInfo
    {
        public final PwsRecord itsRec;
        public final PasswdRecord itsPasswdRec;
        public final PasswdFileData itsFileData;

        /**
         * Constructor
         */
        public RecordInfo(@NonNull PwsRecord rec,
                          @NonNull PasswdRecord passwdRec,
                          @NonNull PasswdFileData fileData)
        {
            itsRec = rec;
            itsPasswdRec = passwdRec;
            itsFileData = fileData;
        }
    }
}
