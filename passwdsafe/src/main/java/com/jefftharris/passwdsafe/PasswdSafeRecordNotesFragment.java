/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.view.PasswdLocation;


/**
 * Fragment for showing notes of a password record
 */
public class PasswdSafeRecordNotesFragment
        extends AbstractPasswdSafeRecordFragment
{
    private boolean itsIsWordWrap = true;
    private boolean itsIsMonospace = true;
    private TextView itsNotes;

    private static final String WORD_WRAP_PREF = "wordwrap";
    private static final String MONOSPACE_PREF = "monospace";

    /**
     * Create a new instance of the fragment
     */
    public static PasswdSafeRecordNotesFragment newInstance(
            PasswdLocation location)
    {
        PasswdSafeRecordNotesFragment frag =
                new PasswdSafeRecordNotesFragment();
        frag.setArguments(createArgs(location));
        return frag;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        View root = inflater.inflate(R.layout.fragment_passwdsafe_record_notes,
                                     container, false);
        itsNotes = (TextView)root.findViewById(R.id.notes);
        GuiUtils.setTextSelectable(itsNotes);
        return root;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_copy_notes: {
            PasswdSafeUtil.copyToClipboard(itsNotes.getText().toString(),
                                           getActivity());
            return true;
        }
        case R.id.menu_monospace: {
            itsIsMonospace = !itsIsMonospace;
            item.setChecked(itsIsMonospace);
            saveNotesOptionsPrefs();
            setNotesOptions();
            return true;
        }
        case R.id.menu_word_wrap: {
            itsIsWordWrap = !itsIsWordWrap;
            item.setChecked(itsIsWordWrap);
            saveNotesOptionsPrefs();
            setNotesOptions();
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    /**
     * Set the current view options on the notes field
     */
    public static void setNotesOptions(TextView notes, Activity act)
    {
        SharedPreferences prefs =
                act.getPreferences(Context.MODE_PRIVATE);
        boolean wordWrap = prefs.getBoolean(WORD_WRAP_PREF, true);
        boolean isMonospace = prefs.getBoolean(MONOSPACE_PREF, false);
        setNotesOptions(notes, wordWrap, isMonospace);
    }

    @Override
    protected void doOnCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.fragment_passwdsafe_record_notes, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        MenuItem item = menu.findItem(R.id.menu_monospace);
        item.setChecked(itsIsMonospace);
        item = menu.findItem(R.id.menu_word_wrap);
        item.setChecked(itsIsWordWrap);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void doRefresh()
    {
        RecordInfo info = getRecordInfo();
        if (info == null) {
            return;
        }

        switch (info.itsPasswdRec.getType()) {
        case NORMAL:
        case ALIAS: {
            String notes = info.itsFileData.getNotes(info.itsRec);
            itsNotes.setText(notes);
            break;
        }
        case SHORTCUT: {
            break;
        }
        }

        SharedPreferences prefs =
                getActivity().getPreferences(Context.MODE_PRIVATE);
        itsIsWordWrap = prefs.getBoolean(WORD_WRAP_PREF, true);
        itsIsMonospace = prefs.getBoolean(MONOSPACE_PREF, false);
        setNotesOptions();
        GuiUtils.invalidateOptionsMenu(getActivity());
    }

    /**
     * Save the notes preferences
     */
    private void saveNotesOptionsPrefs()
    {
        SharedPreferences prefs =
                getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(WORD_WRAP_PREF, itsIsWordWrap);
        editor.putBoolean(MONOSPACE_PREF, itsIsMonospace);
        editor.apply();
    }

    /**
     * Set options for the notes
     */
    private void setNotesOptions()
    {
        setNotesOptions(itsNotes, itsIsWordWrap, itsIsMonospace);
    }

    /**
     * Set options for notes
     */
    private static void setNotesOptions(TextView notes, boolean wordWrap,
                                        boolean monospace)
    {
        notes.setHorizontallyScrolling(!wordWrap);
        notes.setTypeface(monospace ? Typeface.MONOSPACE : Typeface.DEFAULT);
    }
}
