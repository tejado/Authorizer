/*
 * Copyright (©) 2023 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.test;

import android.content.Intent;
import android.net.Uri;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import net.tjado.passwdsafe.PasswdSafe;
import net.tjado.passwdsafe.R;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.test.util.TestModeRule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasFocus;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
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
    @Rule(order=1)
    public TestModeRule itsTestMode = new TestModeRule();

    @Rule(order=2)
    public ActivityTestRule<PasswdSafe> itsActivityRule =
            new ActivityTestRule<>(PasswdSafe.class)
            {
                @Override
                protected Intent getActivityIntent()
                {
                    return PasswdSafeUtil.createNewFileIntent(
                            Uri.fromFile(FileListActivityTest.DIR));
                }
            };

    @Before
    public void setup()
    {
        if (FileListActivityTest.FILE.exists()) {
            Assert.assertTrue(FileListActivityTest.FILE.delete());
        }
    }

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

        onView(withId(R.id.ok))
                .check(matches(not(isEnabled())));
    }

    @Test
    public void testExistingFile() throws IOException
    {
        Assert.assertTrue(FileListActivityTest.FILE.createNewFile());
        Assert.assertTrue(FileListActivityTest.FILE.exists());
        Assert.assertFalse(
                new File(FileListActivityTest.DIR, "ZZZnotest.psafe3").exists());

        onFileNameView()
                .perform(replaceText("ZZZnotest.psafe3"));
        onView(withId(R.id.file_name_input))
                .check(matches(withTextInputError(null)));

        onView(allOf(withId(R.id.file_name),
                     withParent(withParent(withId(R.id.file_name_input)))))
                .perform(replaceText(FileListActivityTest.FILE.getName()));
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

        for (char c: "1234567890abcxyzABCXYZ-_".toCharArray()) {
            onFileNameView().perform(replaceText("ZZZ" + c + "test.psafe3"));
            onView(withId(R.id.file_name_input))
                    .check(matches(withTextInputError(null)));
        }

        for (char c: "`~!@#$%^&*()+={}[]|\\;:'\"<>,./?".toCharArray()) {
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
        Assert.assertFalse(
                new File(FileListActivityTest.DIR, "ZZZtest.psafe3").exists());
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

    /**
     * Test with the file name text view
     */
    public static ViewInteraction onFileNameView()
    {
        return onView(allOf(
                withId(R.id.file_name),
                withParent(withParent(withId(R.id.file_name_input)))));
    }
}
