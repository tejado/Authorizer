/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.view;

import com.google.android.material.textfield.TextInputLayout;
import android.text.TextUtils;

/**
 * Utilities for TextInputLayout
 */
public class TextInputUtils
{
    /**
     * Set the error message on a TextInputLayout
     * @param errorMsg The error message; null if no error
     * @param field The input field
     * @return Whether there was an error
     */
    public static boolean setTextInputError(String errorMsg,
                                            TextInputLayout field)
    {
        boolean isError = !TextUtils.isEmpty(errorMsg);

        // Set fields only if error changes to prevent flashing
        CharSequence currErrorMsg = field.getError();
        if (!TextUtils.equals(errorMsg, currErrorMsg)) {
            if (TextUtils.isEmpty(errorMsg)) {
                field.setErrorEnabled(false);
            } else {
                // Ensure the error message changes
                field.setError(null);
                field.setError(errorMsg);
                field.setErrorEnabled(true);
            }
        }

        return isError;
    }
}
