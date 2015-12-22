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

import com.jefftharris.passwdsafe.lib.AboutDialog;

/**
 * Fragment for showing app 'about' information
 */
public class AboutFragment extends Fragment
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
            extends AbstractPasswdSafeFileDataFragment.Listener
    {
        /**
         * Update the view for the about fragment
         */
        void updateViewAbout();
    }

    private Listener itsListener;

    /**
     * Create a new instance
     */
    public static AboutFragment newInstance()
    {
        return new AboutFragment();
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
        setHasOptionsMenu(true);
        View rootView = inflater.inflate(R.layout.fragment_about,
                                         container, false);

        AboutDialog.updateAboutFields(rootView, null, getContext());
        return rootView;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        itsListener.updateViewAbout();
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        itsListener = null;
    }
}
