/*
 * Copyright (©) 2018 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.test.util;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import android.view.View;
import android.widget.Checkable;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.isA;

/**
 * View action for ensuring a checkable item is checked or not
 */
public class ChildCheckedViewAction implements ViewAction
{
    private final int itsChildId;
    private final boolean itsIsChecked;
    private boolean itsPrevChecked;

    /**
     * Constructor
     */
    public ChildCheckedViewAction(int childId, boolean checked)
    {
        itsChildId = childId;
        itsIsChecked = checked;
    }

    /**
     * Get whether the view was previously checked
     */
    public boolean isPrevChecked()
    {
        return itsPrevChecked;
    }

    @Override
    public Matcher<View> getConstraints()
    {
        return new BaseMatcher<View>() {
            @Override
            public boolean matches(Object item)
            {
                return isA(Checkable.class).matches(item);
            }

            @Override
            public void describeTo(Description description)
            {
                description.appendText("is checkable");
            }
        };
     }

    @Override
    public String getDescription()
    {
        return "Set whether a child view is checked";
    }

    @Override
    public void perform(UiController uiController, View view)
    {
        Checkable checkableView = view.findViewById(itsChildId);
        itsPrevChecked = checkableView.isChecked();
        checkableView.setChecked(itsIsChecked);
    }
}
