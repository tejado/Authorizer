/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import androidx.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import net.tjado.passwdsafe.lib.Utils;
import net.tjado.passwdsafe.lib.view.GuiUtils;
import net.tjado.passwdsafe.view.CopyField;
import net.tjado.passwdsafe.view.PasswdLocation;

import java.util.Date;

/**
 * Base class for showing fields of a password record
 */
public abstract class AbstractPasswdSafeRecordFragment
        extends AbstractPasswdSafeLocationFragment
                        <AbstractPasswdSafeRecordFragment.Listener>
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
            extends AbstractPasswdSafeFileDataFragment.Listener
    {
        /** Copy a field */
        void copyField(CopyField field, String recUuid);

        /** Change the location in the password file */
        void changeLocation(PasswdLocation location);

        /** Finish editing a record */
        void finishEditRecord(boolean save, PasswdLocation newLocation, boolean popBack);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        refresh();
    }

    /**
     * Derived-class refresh
     */
    protected abstract void doRefresh(@NonNull RecordInfo info);

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
        useRecordInfo(new RecordInfoUser()
        {
            @Override
            public void useRecordInfo(@NonNull RecordInfo info)
            {
                doRefresh(info);
            }
        });

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
}
