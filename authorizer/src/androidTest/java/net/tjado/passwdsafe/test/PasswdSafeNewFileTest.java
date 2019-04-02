/*
 * Copyright (©) 2019 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.runner.AndroidJUnit4;
import android.view.Gravity;

import net.tjado.passwdsafe.PasswdSafe;
import net.tjado.passwdsafe.R;
import net.tjado.passwdsafe.lib.DocumentsContractCompat;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.DrawerActions.close;
import static androidx.test.espresso.contrib.DrawerActions.open;
import static androidx.test.espresso.contrib.DrawerMatchers.isClosed;
import static androidx.test.espresso.contrib.DrawerMatchers.isOpen;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasCategories;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasType;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static net.tjado.passwdsafe.test.util.ViewActions.waitId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;

/**
 * UI test for creating a new file
 */
@RunWith(AndroidJUnit4.class)
public class PasswdSafeNewFileTest
{
    private static final File FILE = new File(FileListActivityTest.DIR,
                                              "ZZZtest.psafe3");

    @Rule
    public IntentsTestRule<PasswdSafe> itsActivityRule =
            new IntentsTestRule<>(PasswdSafe.class, false, false);

    @Before
    public void setup()
    {
        PasswdSafeUtil.setIsTesting(true);
        if (FILE.exists()) {
            Assert.assertTrue(FILE.delete());
        }
    }

    @After
    public void teardown()
    {
        PasswdSafeUtil.setIsTesting(false);
    }

    @Test
    public void testNewFile()
    {
        itsActivityRule.launchActivity(
                PasswdSafeUtil.createNewFileIntent(
                            Uri.fromFile(FileListActivityTest.DIR)));
        fillNewFileForm();
        clickButton(R.id.ok);
        validateNewFile();
    }

    //@Test
    public void testNewFileSaf() throws IOException
    {
        itsActivityRule.launchActivity(
                PasswdSafeUtil.createNewFileIntent(null));
        fillNewFileForm();

        Uri fileUri = Uri.fromFile(FILE);
        Intent newResponse =
                 new Intent().setData(fileUri)
                             .putExtra("__test_display_name", FILE.getName());
        intending(allOf(
                hasAction(equalTo(
                        DocumentsContractCompat.INTENT_ACTION_CREATE_DOCUMENT)),
                hasCategories(
                        Collections.singleton(Intent.CATEGORY_OPENABLE)),
                hasType("application/psafe3"),
                hasExtra(Intent.EXTRA_TITLE, FILE.getName())))
                .respondWith(new Instrumentation.ActivityResult(
                        Activity.RESULT_OK, newResponse));
        Assert.assertTrue(FILE.createNewFile());
        clickButton(R.id.ok);
        validateNewFile();
    }

    /**
     * Fill in the new file form
     */
    private static void fillNewFileForm()
    {
        PasswdSafeNewFileFragmentTest.onFileNameView()
                .perform(replaceText(FILE.getName()));
        onView(withId(R.id.password))
                .perform(replaceText("test123"));
        onView(withId(R.id.password_confirm))
                .perform(replaceText("test123"));
    }

    /**
     * Click a button
     */
    private static void clickButton(int buttonId)
    {
        onView(withId(buttonId))
                .perform(closeSoftKeyboard(), scrollTo(), click());

    }

    /**
     * Validate a created new file
     */
    private void validateNewFile()
    {
        try {
            Assert.assertTrue(FILE.exists());

            // Verify new file UI
            validateOpenedEmptyFile();

            openActionBarOverflowOrOptionsMenu(
                    getInstrumentation().getTargetContext());
            onView(withText(R.string.close_file))
                    .perform(click());
            Assert.assertTrue(itsActivityRule.getActivity().isFinishing());

            Intents.release();
            Intent openIntent =
                    PasswdSafeUtil.createOpenIntent(Uri.fromFile(FILE), null);
            itsActivityRule.launchActivity(openIntent);
            Assert.assertFalse(itsActivityRule.getActivity().isFinishing());

            // Open file
            onView(withId(R.id.file))
                    .check(matches(withText("Open " + FILE.getName())));
            onView(withId(R.id.passwd_edit))
                    .perform(replaceText("test123"));
            clickButton(R.id.ok);

            // Verify open file UI
            validateOpenedEmptyFile();

            openActionBarOverflowOrOptionsMenu(
                    getInstrumentation().getTargetContext());
            onView(withText(R.string.close_file))
                    .perform(click());
            Assert.assertTrue(itsActivityRule.getActivity().isFinishing());
        } finally {
            Assert.assertTrue(FILE.delete());
        }
    }

    /**
     * Validate the UI for a new file
     */
    private static void validateOpenedEmptyFile()
    {
        onView(withId(R.id.drawer_layout))
                .check(matches(isClosed(Gravity.START)))
                .perform(open());
        onView(allOf(withText(R.string.records),
                     isDescendantOfA(withId(R.id.navigation_drawer))))
                .check(matches(isEnabled()));
        onView(allOf(withText(R.string.preferences),
                     isDescendantOfA(withId(R.id.navigation_drawer))))
                .check(matches(isEnabled()));
        onView(withId(R.id.drawer_layout))
                .check(matches(isOpen(Gravity.START)))
                .perform(close());

        onView(isRoot()).perform(waitId(R.id.menu_search,
                                       TimeUnit.SECONDS.toMillis(100)));
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
                .check(matches(withEffectiveVisibility(
                        ViewMatchers.Visibility.GONE)));
        onView(allOf(withId(android.R.id.empty),
                     withParent(withParent(withId(R.id.content)))))
                .check(matches(withEffectiveVisibility(
                        ViewMatchers.Visibility.VISIBLE)));
    }
}
