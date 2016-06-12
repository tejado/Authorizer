/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.view;

import android.content.Context;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * The LongCheckBoxPreference class is a CheckBoxPreference with support for
 * a multi-line title
 */
public class LongCheckBoxPreference extends CheckBoxPreference
{
    /**
     * Constructor
     */
    public LongCheckBoxPreference(Context context, AttributeSet attrs)
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
