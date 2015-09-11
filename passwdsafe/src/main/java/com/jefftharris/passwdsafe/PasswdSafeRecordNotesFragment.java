/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * Fragment for showing notes of a password record
 */
public class PasswdSafeRecordNotesFragment
        extends AbstractPasswdSafeRecordFragment
{
    /**
     * Create a new instance of the fragment
     */
    public static PasswdSafeRecordNotesFragment newInstance(String recUuid)
    {
        PasswdSafeRecordNotesFragment frag =
                new PasswdSafeRecordNotesFragment();
        frag.setArguments(createArgs(recUuid));
        return frag;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_passwdsafe_record_notes,
                                container, false);
    }

    @Override
    protected void doOnCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
    }

    @Override
    protected void doRefresh()
    {
    }
}
