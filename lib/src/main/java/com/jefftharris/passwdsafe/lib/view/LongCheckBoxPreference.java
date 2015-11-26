/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.lib.view;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/**
 * The LongCheckBoxPreference class is a CheckBoxPreference with support for
 * a multi-line title
 */
public class LongCheckBoxPreference extends CheckBoxPreference
{
    /// Constructor
    public LongCheckBoxPreference(Context context, AttributeSet attrs,
                                  int defStyle)
    {
        super(context, attrs, defStyle);
    }

    /// Constructor
    public LongCheckBoxPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    /// Constructor
    public LongCheckBoxPreference(Context context)
    {
        super(context);
    }

    /* (non-Javadoc)
     * @see android.preference.CheckBoxPreference#onBindView(android.view.View)
     */
    @Override
    protected void onBindView(@NonNull View view)
    {
        super.onBindView(view);
        TextView title = (TextView)view.findViewById(android.R.id.title);
        title.setSingleLine(false);
    }
}
