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

/**
 * Fragment for password expiration information
 */
public class PasswdSafeExpirationsFragment extends Fragment
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
    {
        /** Update the view for expiration info */
        void updateViewExpirations();
    }

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
        return inflater.inflate(R.layout.fragment_passwdsafe_expirations,
                                container, false);
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

    /**
     * Refresh the view
     */
    private void refresh()
    {
    }
}
