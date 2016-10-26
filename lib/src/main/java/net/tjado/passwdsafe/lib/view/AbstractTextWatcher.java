/*
 * Copyright (©) 2011-2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib.view;

import android.text.TextWatcher;

public abstract class AbstractTextWatcher implements TextWatcher
{
    public void beforeTextChanged(CharSequence s, int start,
                                  int count, int after)
    {
    }

    public void onTextChanged(CharSequence s, int start,
                              int before, int count)
    {
    }
}
