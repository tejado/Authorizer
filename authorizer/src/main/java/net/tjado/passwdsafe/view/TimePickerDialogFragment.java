/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.view;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import java.util.Calendar;

/**
 * Dialog to pick a time
 */
public class TimePickerDialogFragment extends DialogFragment
        implements TimePickerDialog.OnTimeSetListener
{
    /**
     * Listener interface for the owning fragment
     */
    public interface Listener
    {
        void handleTimePicked(int hourOfDay, int minute);
    }

    /**
     * Create a new instance
     */
    public static TimePickerDialogFragment newInstance(int hourOfDay,
                                                       int minute)
    {
        TimePickerDialogFragment frag = new TimePickerDialogFragment();
        Bundle args = new Bundle();
        args.putInt("hourOfDay", hourOfDay);
        args.putInt("minute", minute);
        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Bundle args = getArguments();
        Calendar now = Calendar.getInstance();
        int hourOfDay = args.getInt("hourOfDay", now.get(Calendar.HOUR_OF_DAY));
        int minute = args.getInt("minute", now.get(Calendar.MINUTE));

        return new TimePickerDialog(getContext(), this, hourOfDay, minute,
                                    DateFormat.is24HourFormat(getContext()));
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute)
    {
        if (isResumed()) {
            ((Listener)getTargetFragment()).handleTimePicked(hourOfDay, minute);
        }
    }
}
