/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib.view;

import android.content.Context;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * The LongSwitchPreference class is a SwitchPreference with support for
 * a multi-line title
 */
public class LongSwitchPreference extends SwitchPreferenceCompat
{
    /**
     * Constructor
     */
    public LongSwitchPreference(Context context, AttributeSet attrs)
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
