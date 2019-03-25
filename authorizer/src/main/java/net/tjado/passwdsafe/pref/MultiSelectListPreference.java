/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.pref;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import androidx.annotation.NonNull;
import android.util.AttributeSet;

/**
 * @author jharris
 * Patterned off of class from http://blog.350nice.com/wp/archives/240
 */
@SuppressWarnings("unused")
public class MultiSelectListPreference extends ListPreference
{
    private static final String SEPARATOR = "|";

    private boolean[] itsSelectedItems;

    /** Constructor */
    @SuppressWarnings("unused")
    public MultiSelectListPreference(Context context)
    {
        this(context, null);
    }

    /** Constructor */
    @SuppressWarnings("unused")
    public MultiSelectListPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        int len = 0;
        if (getEntries() != null) {
            len = getEntries().length;
        }
        itsSelectedItems = new boolean[len];
    }

    /* (non-Javadoc)
     * @see android.preference.ListPreference#setEntries(java.lang.CharSequence[])
     */
    @Override
    public void setEntries(CharSequence[] entries)
    {
        super.setEntries(entries);
        int len = 0;
        if (entries != null) {
            len = entries.length;
        }
        itsSelectedItems = new boolean[len];
    }

    /* (non-Javadoc)
     * @see android.preference.ListPreference#onPrepareDialogBuilder(android.app.AlertDialog.Builder)
     */
    @Override
    protected void onPrepareDialogBuilder(@NonNull Builder builder)
    {
        CharSequence[] entries = getEntries();
        CharSequence[] entryValues = getEntryValues();

        if ((entries == null) || (entryValues == null) ||
            (entries.length != entryValues.length)) {
            throw new IllegalStateException(
                "ListPreference requires an entries array and an entryValues " +
                "array which are both the same length");
        }

        String value = getValue();
        String[] values = null;
        if (value != null) {
            values = value.split("\\Q" + SEPARATOR + "\\E");
        }
        for (int entryIdx = 0; entryIdx < entryValues.length; ++entryIdx) {
            itsSelectedItems[entryIdx] = false;
            if (values != null) {
                for (String val : values) {
                    if (entryValues[entryIdx].equals(val)) {
                        itsSelectedItems[entryIdx] = true;
                        break;
                    }
                }
            }
        }

        builder.setMultiChoiceItems(
            entries, itsSelectedItems,
            new DialogInterface.OnMultiChoiceClickListener()
            {
                public void onClick(DialogInterface dialog, int which,
                                    boolean isChecked)
                {
                    itsSelectedItems[which] = isChecked;
                }
            });
    }

    /* (non-Javadoc)
     * @see android.preference.ListPreference#onDialogClosed(boolean)
     */
    @Override
    protected void onDialogClosed(boolean positiveResult)
    {
        CharSequence[] entryValues = getEntryValues();

        if (positiveResult && (entryValues != null)) {
            boolean first = true;
            StringBuilder newValue = new StringBuilder();
            for (int i = 0; i < entryValues.length; ++i) {
                if (itsSelectedItems[i]) {
                    if (first) {
                        first = false;
                    } else {
                        newValue.append(SEPARATOR);
                    }
                    newValue.append(entryValues[i]);
                }
            }

            String newStr = newValue.toString();
            if (callChangeListener(newStr)) {
                setValue(newStr);
            }
        }
    }
}
