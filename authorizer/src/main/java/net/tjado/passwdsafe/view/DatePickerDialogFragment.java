/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.view;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.widget.DatePicker;

import java.util.Calendar;

/**
 * Dialog to pick a date
 */
public class DatePickerDialogFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener
{
    /**
     * Listener interface for the owning fragment
     */
    public interface Listener
    {
        void handleDatePicked(int year, int monthOfYear, int dayOfMonth);
    }

    /**
     * Create a new instance
     */
    public static DatePickerDialogFragment newInstance(int year,
                                                       int monthOfYear,
                                                       int dayOfMonth)
    {
        DatePickerDialogFragment frag = new DatePickerDialogFragment();
        Bundle args = new Bundle();
        args.putInt("year", year);
        args.putInt("monthOfYear", monthOfYear);
        args.putInt("dayOfMonth", dayOfMonth);
        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Bundle args = getArguments();
        Calendar now = Calendar.getInstance();
        int year = args.getInt("year", now.get(Calendar.YEAR));
        int monthOfYear = args.getInt("monthOfYear", now.get(Calendar.MONTH));
        int dayOfMonth = args.getInt("dayOfMonth",
                                     now.get(Calendar.DAY_OF_MONTH));

        return new DatePickerDialog(getContext(), this, year, monthOfYear,
                                    dayOfMonth);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear,
                          int dayOfMonth)
    {
        if (isResumed()) {
            ((Listener)getTargetFragment()).handleDatePicked(year, monthOfYear,
                                                             dayOfMonth);
        }
    }
}
