/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.tjado.passwdsafe.lib.view.GuiUtils;
import net.tjado.passwdsafe.util.AboutUtils;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * Fragment for showing app 'about' information
 */
public class ReleaseNotesFragment extends Fragment
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
            extends AbstractPasswdSafeFileDataFragment.Listener
    {
        /** Update the view */
        void updateViewReleaseNotes();
    }

    private Listener itsListener;

    /**
     * Create a new instance
     */
    public static ReleaseNotesFragment newInstance()
    {
        return new ReleaseNotesFragment();
    }

    @Override
    public void onAttach(@NonNull Context ctx)
    {
        super.onAttach(ctx);
        itsListener = (Listener)ctx;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        View rootView = inflater.inflate(R.layout.fragment_app_release_notes,
                                         container, false);

        TextView tv = rootView.findViewById(R.id.release_notes);
        tv.setText(Html.fromHtml(tv.getText().toString().replace("\n", "<br>")));

        return rootView;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        itsListener.updateViewReleaseNotes();
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        itsListener = null;
    }

}
