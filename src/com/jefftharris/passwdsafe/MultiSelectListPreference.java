/*
 * Copyright (©) 2009-2010 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * @author jharris
 * Patterned off of class from http://blog.350nice.com/wp/archives/240
 */
public class MultiSelectListPreference extends ListPreference
{
    /**
     * @param context
     */
    public MultiSelectListPreference(Context context)
    {
        super(context);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param context
     * @param attrs
     */
    public MultiSelectListPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

}
