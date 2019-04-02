/*
 * Copyright (©) 2018 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.test.util;

import androidx.test.espresso.ViewAssertion;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import org.hamcrest.Matcher;
import org.junit.Assert;

import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static org.hamcrest.CoreMatchers.is;


/**
 * View assertions for a RecyclerView
 */
public final class RecyclerViewAssertions
{
    /**
     * Assert the number of entries in the view
     */
    public static ViewAssertion withRecyclerViewCount(int count)
    {
        return withRecyclerViewCount(is(count));
    }

    /**
     * Match against the number of entries in the view
     */
    public static ViewAssertion withRecyclerViewCount(
            final Matcher<Integer> matcher)
    {
        return (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }

            RecyclerView rv = (RecyclerView)view;
            RecyclerView.Adapter adapter = rv.getAdapter();
            Assert.assertNotNull(adapter);
            assertThat(adapter.getItemCount(), matcher);
        };
    }

    /**
     * Match against the view of the RecyclerView item at a position
     */
    public static ViewAssertion hasRecyclerViewItemAtPosition(
            final int index,
            final Matcher<View> viewMatcher)
    {
        return (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }

            RecyclerView rv = (RecyclerView)view;
            RecyclerView.ViewHolder holder =
                    rv.findViewHolderForAdapterPosition(index);
            Assert.assertNotNull(holder);
            Assert.assertThat(holder.itemView, viewMatcher);
        };
    }
}
