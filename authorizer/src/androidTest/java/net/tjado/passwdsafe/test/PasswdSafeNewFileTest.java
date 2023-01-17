/*
 * Copyright (©) 2023 Jeff Harris <jefftharris@gmail.com>
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
import android.view.Gravity;
import android.view.View;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.tjado.passwdsafe.PasswdSafe;
import net.tjado.passwdsafe.R;
import net.tjado.passwdsafe.lib.ApiCompat;
import net.tjado.passwdsafe.lib.DocumentsContractCompat;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.test.util.TestModeRule;

import org.hamcrest.Matcher;
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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
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
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static net.tjado.passwdsafe.test.util.ViewActions.waitId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;

/**
 * UI test for creating a new file
 */
@RunWith(AndroidJUnit4.class)
public class PasswdSafeNewFileTest
{
    private static final File FILE = FileListActivityTest.FILE;

    @Rule(order=1)
    public TestModeRule itsTestMode = new TestModeRule();

    @Rule(order=2)
    public IntentsTestRule<PasswdSafe> itsActivityRule =
            new IntentsTestRule<>(PasswdSafe.class, false, false);

    @Before
    public void setup()
    {
        //PasswdSafeUtil.setIsTesting(true);
        if (FILE.exists()) {
            Assert.assertTrue(FILE.delete());
        }
    }

    @After
    @SuppressWarnings("EmptyMethod")
    public void teardown()
    {
        //PasswdSafeUtil.setIsTesting(false);
    }

    @Test
    public void testNewFile()
    {
        if (!ApiCompat.supportsExternalFilesDirs()) {
            return;
        }
        itsActivityRule.launchActivity(
                PasswdSafeUtil.createNewFileIntent(
                            Uri.fromFile(FileListActivityTest.DIR)));
        fillNewFileForm();
        clickButton(R.id.ok);
        validateNewFile();
    }

    @Test
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
            validateOpenedEmptyFile(true);
            closeFile();

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
            clickButton(R.id.open);

            // Verify open file UI
            validateOpenedEmptyFile(false);
            closeFile();
        } finally {
            Assert.assertTrue(FILE.delete());
        }
    }

    /**
     * Close an open file
     */
    private void closeFile()
    {
        onView(withId(R.id.menu_close))
                .check(matches(isEnabled()))
                .perform(click());
        Assert.assertTrue(itsActivityRule.getActivity().isFinishing());
    }

    /**
     * Validate the UI for a new file.  The file is left in read-only mode.
     */
    private static void validateOpenedEmptyFile(boolean newFile)
    {
        if (!newFile) {
            validateMenus(false);
            setWritable(true);
        }
        validateMenus(true);
        setWritable(false);
        validateMenus(false);

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

    /**
     * Validate the menus of an open file
     */
    private static void validateMenus(boolean writable)
    {
        // Validate nav drawer
        onView(withId(R.id.drawer_layout))
                .check(matches(isClosed(Gravity.START)))
                .perform(open());

        onView(withParent(withId(R.id.navigation_drawer)))
               .check(matches(hasChildCount(1 /*header*/ + 6 /*menu items*/)));

        for (int id : new int[]
                { R.string.writable, R.string.records,
                  R.string.password_policies, R.string.password_expiration,
                  R.string.preferences, R.string.about }) {
            onView(allOf(withText(id),
                         isDescendantOfA(withId(R.id.navigation_drawer))))
                    .check(matches(isEnabled()));
        }

        onView(withWritableSw())
                .check(matches(writable ? isChecked() : isNotChecked()));

        onView(withId(R.id.drawer_layout))
                .check(matches(isOpen(Gravity.START)))
                .perform(close());

        // Validate main menu
        onView(isRoot()).perform(waitId(R.id.menu_search,
                                        TimeUnit.SECONDS.toMillis(15)));
        onView(withId(R.id.menu_search))
                .check(matches(isEnabled()));
        if (writable) {
            onView(withId(R.id.menu_add))
                    .check(matches(isEnabled()));
            openActionBarOverflowOrOptionsMenu(
                    getInstrumentation().getTargetContext());
            onView(withText(R.string.file_operations))
                    .check(matches(isEnabled()));
            onView(withText(R.string.sort))
                    .check(matches(isEnabled()));
            onView(withText(R.string.close_file))
                    .check(matches(isEnabled()));
            pressBack();
        } else {
            onView(withId(R.id.menu_sort))
                    .check(matches(isEnabled()));
            onView(withId(R.id.menu_close))
                    .check(matches(isEnabled()));
            onView(withId(R.id.menu_add))
                    .check(doesNotExist());
        }
    }

    /**
     * Set the writable state of the file.  The file must be in the opposite
     * state already.
     */
    private static void setWritable(boolean writable)
    {
        onView(withId(R.id.drawer_layout))
                .check(matches(isClosed(Gravity.START)))
                .perform(open());

        onView(withWritableSw())
                .check(matches(writable ? isNotChecked() : isChecked()))
                .perform(click());

        // Wait for main menu
        onView(isRoot()).perform(waitId(R.id.menu_search,
                                        TimeUnit.SECONDS.toMillis(15)));
        onView(withId(R.id.drawer_layout))
                .check(matches(isClosed(Gravity.START)));
    }

    /**
     * Get a matcher for the writable switch
     */
    private static Matcher<View> withWritableSw()
    {
        return allOf(
                withId(R.id.switch_item),
                withParent(withParent(hasSibling(
                        allOf(withText(R.string.writable),
                              isDescendantOfA(withId(R.id.navigation_drawer))
                        )))
                ));
    }
}
