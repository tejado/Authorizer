/*
 * Copyright (©) 2017 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.test;

import android.content.Intent;
import android.net.Uri;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.Gravity;

import net.tjado.passwdsafe.PasswdSafe;
import net.tjado.passwdsafe.R;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.close;
import static android.support.test.espresso.contrib.DrawerActions.open;
import static android.support.test.espresso.contrib.DrawerMatchers.isClosed;
import static android.support.test.espresso.contrib.DrawerMatchers.isOpen;
import static android.support.test.espresso.matcher.ViewMatchers.hasFocus;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static net.tjado.passwdsafe.test.util.TestUtils.withTextInputError;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;

/**
 * UI test for PasswdSafeNewFileFragment
 */
@RunWith(AndroidJUnit4.class)
public class PasswdSafeNewFileFragmentTest
{
    @Rule
    public ActivityTestRule<PasswdSafe> itsActivityRule =
            new ActivityTestRule<PasswdSafe>(PasswdSafe.class)
            {
                @Override
                protected Intent getActivityIntent()
                {
                    return PasswdSafeUtil.createNewFileIntent(
                            Uri.fromFile(FileListActivityTest.DIR));
                }
            };

    @Test
    public void testInitialState()
    {
        onFileNameView()
                .check(matches(allOf(withText(".psafe3"), hasFocus())));
        onView(withId(R.id.file_name_input))
                .check(matches(withTextInputError("Empty file name")));

        onView(withId(R.id.password))
                .check(matches(withText(isEmptyString())));
        onView(withId(R.id.password_input))
                .check(matches(withTextInputError("Empty password")));

        onView(withId(R.id.password_confirm))
                .check(matches(withText(isEmptyString())));
        onView(withId(R.id.password_confirm_input))
                .check(matches(withTextInputError(null)));

        onView(withId(R.id.cancel))
                .check(matches(isEnabled()));
        onView(withId(R.id.ok))
                .check(matches(not(isEnabled())));
    }

    @Test
    public void testExistingFile()
    {
        Assert.assertTrue(
                new File(FileListActivityTest.DIR, "test.psafe3").exists());
        Assert.assertTrue(
                !new File(FileListActivityTest.DIR, "ZZZtest.psafe3").exists());

        onFileNameView()
                .perform(replaceText("ZZZtest.psafe3"));
        onView(withId(R.id.file_name_input))
                .check(matches(withTextInputError(null)));

        onView(allOf(withId(R.id.file_name),
                     withParent(withParent(withId(R.id.file_name_input)))))
                .perform(replaceText("test.psafe3"));
        onView(withId(R.id.file_name_input))
                .check(matches(withTextInputError("File exists")));
    }

    @Test
    public void testFileName()
    {
        onFileNameView()
                .check(matches(withText(".psafe3")));
        onView(withId(R.id.file_name_input))
                .check(matches(withTextInputError("Empty file name")));

        onFileNameView()
                .perform(replaceText("ZZZtest.psafe3"));
        onView(withId(R.id.file_name_input))
                .check(matches(withTextInputError(null)));

        for (char c: "1234567890abcxyzABCXYZ".toCharArray()) {
            onFileNameView().perform(replaceText("ZZZ" + c + "test.psafe3"));
            onView(withId(R.id.file_name_input))
                    .check(matches(withTextInputError(null)));
        }

        for (char c: "`~!@#$%^&*()_+-={}[]|\\;:'\"<>,./?".toCharArray()) {
            onFileNameView()
                    .perform(replaceText("ZZZ" + c + "test.psafe3"));
            onView(withId(R.id.file_name_input))
                    .check(matches(withTextInputError("Invalid file name")));
        }
    }

    @Test
    public void testFileNameSuffix()
    {
        onFileNameView()
                .check(matches(withText(".psafe3")));
        onView(withId(R.id.file_name_input))
                .check(matches(withTextInputError("Empty file name")));

        onView(allOf(withId(R.id.file_name),
                     withParent(withParent(withId(R.id.file_name_input)))))
                .perform(replaceText(".psafe"));

        onView(allOf(withId(R.id.file_name),
                     withParent(withParent(withId(R.id.file_name_input)))))
                .check(matches(withText(".psafe3")));
        onView(withId(R.id.file_name_input))
                .check(matches(withTextInputError("Empty file name")));
    }

    @Test
    public void testPassword()
    {
        // Check initial with valid file name
        Assert.assertTrue(
                !new File(FileListActivityTest.DIR, "ZZZtest.psafe3").exists());
        onFileNameView()
                .perform(replaceText("ZZZtest.psafe3"));
        onView(withId(R.id.file_name_input))
                .check(matches(withTextInputError(null)));
        onView(withId(R.id.password))
                .check(matches(withText(isEmptyString())));
        onView(withId(R.id.password_input))
                .check(matches(withTextInputError("Empty password")));
        onView(withId(R.id.password_confirm))
                .check(matches(withText(isEmptyString())));
        onView(withId(R.id.password_confirm_input))
                .check(matches(withTextInputError(null)));
        onView(withId(R.id.ok))
                .check(matches(not(isEnabled())));

        onView(withId(R.id.password))
                .perform(replaceText("test123"));
        onView(withId(R.id.password_input))
                .check(matches(withTextInputError(null)));
        onView(withId(R.id.password_confirm))
                .check(matches(withText(isEmptyString())));
        onView(withId(R.id.password_confirm_input))
                .check(matches(withTextInputError("Passwords do not match")));
        onView(withId(R.id.ok))
                .check(matches(not(isEnabled())));

        onView(withId(R.id.password_confirm))
                .perform(replaceText("test123"));
        onView(withId(R.id.password_input))
                .check(matches(withTextInputError(null)));
        onView(withId(R.id.password_confirm_input))
                .check(matches(withTextInputError(null)));
        onView(withId(R.id.ok))
                .check(matches(isEnabled()));

        onView(withId(R.id.password_confirm))
                .perform(replaceText("test1234"));
        onView(withId(R.id.password_input))
                .check(matches(withTextInputError(null)));
        onView(withId(R.id.password_confirm_input))
                .check(matches(withTextInputError("Passwords do not match")));
        onView(withId(R.id.ok))
                .check(matches(not(isEnabled())));

        onView(withId(R.id.password))
                .perform(replaceText("test1234"));
        onView(withId(R.id.password_input))
                .check(matches(withTextInputError(null)));
        onView(withId(R.id.password_confirm_input))
                .check(matches(withTextInputError(null)));
        onView(withId(R.id.ok))
                .check(matches(isEnabled()));

        onView(withId(R.id.password))
                .perform(replaceText(""));
        onView(withId(R.id.password_input))
                .check(matches(withTextInputError("Empty password")));
        onView(withId(R.id.password_confirm_input))
                .check(matches(withTextInputError("Passwords do not match")));
        onView(withId(R.id.ok))
                .check(matches(not(isEnabled())));
    }

    @Test
    public void testNewFile() throws InterruptedException
    {
        File file = new File(FileListActivityTest.DIR, "ZZZtest.psafe3");
        if (file.exists()) {
            Assert.assertTrue(file.delete());
        }
        onFileNameView()
                .perform(replaceText(file.getName()));
        onView(withId(R.id.password))
                .perform(replaceText("test123"));
        onView(withId(R.id.password_confirm))
                .perform(replaceText("test123"));
        onView(withId(R.id.ok))
                .perform(closeSoftKeyboard(), scrollTo(), click());

        try {
            Assert.assertTrue(file.exists());

            onView(withId(R.id.drawer_layout))
                    .check(matches(isClosed(Gravity.START)))
                    .perform(open());
            onView(allOf(withText(R.string.records),
                         isDescendantOfA(withId(R.id.navigation_drawer))))
                    .check(matches(isEnabled()));
            onView(allOf(withText(R.string.about),
                         isDescendantOfA(withId(R.id.navigation_drawer))))
                    .check(matches(isEnabled()));
            onView(withId(R.id.drawer_layout))
                    .check(matches(isOpen(Gravity.START)))
                    .perform(close());
            Thread.sleep(1000); // Wait for menu to be refreshed

            onView(withId(R.id.menu_search))
                    .check(matches(isEnabled()));
            onView(withId(R.id.menu_add))
                    .check(matches(isEnabled()));
            openActionBarOverflowOrOptionsMenu(
                    getInstrumentation().getTargetContext());
            onView(withText(R.string.file_operations))
                    .check(matches(isEnabled()));
            //onView(withText(R.string.sort))
            //        .check(matches(isEnabled()));
            onView(withText(R.string.close_file))
                    .check(matches(isEnabled()));
            pressBack();

            onView(withId(R.id.content))
                    .check(matches(isEnabled()));
            onView(allOf(withId(android.R.id.list),
                         withParent(withParent(withId(R.id.content)))))
                    .check(matches(
                            withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
            onView(allOf(withId(android.R.id.empty),
                         withParent(withParent(withId(R.id.content)))))
                    .check(matches(
                            withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

            openActionBarOverflowOrOptionsMenu(
                    getInstrumentation().getTargetContext());
            onView(withText(R.string.close_file))
                    .perform(click());
        } finally {
            Assert.assertTrue(file.delete());
        }
    }

    /**
     * Test with the file name text view
     */
    private static ViewInteraction onFileNameView()
    {
        return onView(allOf(
                withId(R.id.file_name),
                withParent(withParent(withId(R.id.file_name_input)))));
    }
}
