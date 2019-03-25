/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.view;

import android.content.Context;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * The LongListPreference class is a ListPreference with support for a
 * multi-line title
 */
public class LongListPreference extends ListPreference
{
    /**
     * Constructor
     */
    public LongListPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder)
    {
        super.onBindViewHolder(holder);
        TextView title = (TextView)holder.findViewById(android.R.id.title);
        title.setSingleLine(false);
    }
}
