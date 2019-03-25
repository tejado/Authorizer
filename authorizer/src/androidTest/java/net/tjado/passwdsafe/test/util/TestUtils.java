/*
 * Copyright (©) 2017 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.test.util;

import com.google.android.material.textfield.TextInputLayout;
import androidx.test.espresso.matcher.BoundedMatcher;
import android.text.TextUtils;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static androidx.test.espresso.intent.Checks.checkNotNull;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;


/**
 * Test utilities
 */
public class TestUtils
{
    /**
     * Convert a string of hex digits to bytes
     */
    public static byte[] hexToBytes(String s)
    {
        byte[] bytes = new byte[s.length() / 2];
        for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = (byte)((Character.digit(s.charAt(i*2), 16) << 4) |
                              Character.digit(s.charAt(i*2+1), 16));
        }
        return bytes;
    }

    /**
     * Match with data in a view adapter
     */
    public static Matcher<View> withAdaptedData(
            final Matcher<Object> dataMatcher)
    {
        checkNotNull(dataMatcher);
        return new TypeSafeMatcher<View>()
        {
            @Override
            public void describeTo(Description description)
            {
                description.appendText("with class name: ");
                dataMatcher.describeTo(description);
            }
            @Override
            public boolean matchesSafely(View view)
            {
                if (!(view instanceof AdapterView)) {
                    return false;
                }
                Adapter adapter = ((AdapterView) view).getAdapter();
                for (int i = 0; i < adapter.getCount(); i++) {
                    if (dataMatcher.matches(adapter.getItem(i))) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * Match with a TextInputLayout's error text
     */
    public static Matcher<View> withTextInputError(String error)
    {
        final Matcher<String> errorMatcher =
                TextUtils.isEmpty(error) ?
                isEmptyOrNullString() : equalTo(error);
        checkNotNull(errorMatcher);
        return new BoundedMatcher<View, TextInputLayout>(TextInputLayout.class)
        {
            @Override
            protected boolean matchesSafely(TextInputLayout item)
            {
                if (item == null) {
                    return false;
                }

                CharSequence error = item.getError();
                return errorMatcher.matches(error) &&
                       (!item.isErrorEnabled() == TextUtils.isEmpty(error));
            }

            @Override
            public void describeTo(Description description)
            {
                description.appendText("with error: ");
                errorMatcher.describeTo(description);
            }
        };
    }
}
