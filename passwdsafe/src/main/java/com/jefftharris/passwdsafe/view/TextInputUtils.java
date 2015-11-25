/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.view;

import android.support.design.widget.TextInputLayout;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.text.TextUtils;
import android.view.View;

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
            field.setError(errorMsg);
            if (!isError && (field.getChildCount() > 1)) {
                // Assume the TextInputLayout has its error message as the
                // second child.  Use animation so visibility occurs after
                // the TextInputLayout's animation that turns it invisible
                View error = field.getChildAt(1);
                ViewCompat.animate(error)
                          .setListener(new ViewPropertyAnimatorListenerAdapter()
                          {
                              @Override
                              public void onAnimationEnd(View view)
                              {
                                  view.setVisibility(View.GONE);
                              }
                          })
                          .start();
            }
        }

        return isError;
    }
}
