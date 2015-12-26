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
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.jefftharris.passwdsafe.file.PasswdRecordFilter;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.view.DatePickerDialogFragment;

import java.util.Calendar;
import java.util.Date;

/**
 * Fragment for password expiration information
 */
public class PasswdSafeExpirationsFragment extends Fragment
        implements AdapterView.OnItemClickListener,
                   DatePickerDialogFragment.Listener
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
    {
        /** Update the view for expiration info */
        void updateViewExpirations();

        /** Set the expiration record filter */
        void setRecordExpiryFilter(PasswdRecordFilter.ExpiryFilter filter,
                                   Date customDate);
    }

    private static final String TAG = "PasswdSafeExpirationsFragment";

    private Listener itsListener;

    /**
     * Create a new instance
     */
    public static PasswdSafeExpirationsFragment newInstance()
    {
        return new PasswdSafeExpirationsFragment();
    }

    @Override
    public void onAttach(Context ctx)
    {
        super.onAttach(ctx);
        itsListener = (Listener)ctx;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View root = inflater.inflate(R.layout.fragment_passwdsafe_expirations,
                                     container, false);

        ListView expirations = (ListView)root.findViewById(R.id.expirations);
        expirations.setOnItemClickListener(this);

        return root;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        itsListener.updateViewExpirations();
        refresh();
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        itsListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> list, View view, int pos, long id)
    {
        PasswdRecordFilter.ExpiryFilter filter =
                PasswdRecordFilter.ExpiryFilter.fromIdx(pos);
        PasswdSafeUtil.dbginfo(TAG, "Filter %s", filter);
        switch (filter) {
        case EXPIRED:
        case TODAY:
        case IN_A_WEEK:
        case IN_TWO_WEEKS:
        case IN_A_MONTH:
        case IN_A_YEAR:
        case ANY: {
            itsListener.setRecordExpiryFilter(filter, null);
            break;
        }
        case CUSTOM: {
            Calendar now = Calendar.getInstance();
            DatePickerDialogFragment picker =
                    DatePickerDialogFragment.newInstance(
                            now.get(Calendar.YEAR),
                            now.get(Calendar.MONTH),
                            now.get(Calendar.DAY_OF_MONTH));
            picker.setTargetFragment(this, 0);
            picker.show(getFragmentManager(), "datePicker");
            break;
        }
        }
    }

    @Override
    public void handleDatePicked(int year, int monthOfYear, int dayOfMonth)
    {
        if (itsListener == null) {
            return;
        }
        Calendar date = Calendar.getInstance();
        date.set(year, monthOfYear, dayOfMonth, 0, 0, 0);
        date.set(Calendar.MILLISECOND, 0);
        date.add(Calendar.DAY_OF_MONTH, 1);
        itsListener.setRecordExpiryFilter(
                PasswdRecordFilter.ExpiryFilter.CUSTOM, date.getTime());
    }

    /**
     * Refresh the view
     */
    private void refresh()
    {
    }
}
