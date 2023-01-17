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
import android.os.Environment;
import android.view.Gravity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.tjado.passwdsafe.BuildConfig;
import net.tjado.passwdsafe.FileListActivity;
import net.tjado.passwdsafe.R;
import net.tjado.passwdsafe.lib.ApiCompat;
import net.tjado.passwdsafe.lib.DocumentsContractCompat;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.test.util.ChildCheckedViewAction;
import net.tjado.passwdsafe.test.util.TestModeRule;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.DrawerActions.open;
import static androidx.test.espresso.contrib.DrawerMatchers.isClosed;
import static androidx.test.espresso.contrib.NavigationViewActions.navigateTo;
import static androidx.test.espresso.intent.Checks.checkNotNull;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasCategories;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasData;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasType;
import static androidx.test.espresso.intent.matcher.IntentMatchers.toPackage;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static net.tjado.passwdsafe.test.util.RecyclerViewAssertions.hasRecyclerViewItemAtPosition;
import static net.tjado.passwdsafe.test.util.RecyclerViewAssertions.withRecyclerViewCount;
import static net.tjado.passwdsafe.test.util.TestUtils.withAdaptedData;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * UI tests for FileListActivity
 */
@RunWith(AndroidJUnit4.class)
public class FileListActivityTest
{
    public static final File DIR =
            ApplicationProvider.getApplicationContext().getFilesDir();
    private static final File LEGACY_DIR =
            Environment.getExternalStorageDirectory();
    public static final File FILE = new File(DIR, "ZZZtest.psafe3");
    private static final File LEGACY_FILE = new File(LEGACY_DIR,
                                                     FILE.getName());

    @Rule(order=1)
    public TestModeRule itsTestMode = new TestModeRule();

    @Rule(order=2)
    public IntentsTestRule<FileListActivity> itsActivityRule =
            new IntentsTestRule<>(FileListActivity.class);

    @Before
    public void setup()
    {
        if (FILE.exists()) {
            Assert.assertTrue(FILE.delete());
        }
        if (ApiCompat.supportsExternalFilesDirs() && LEGACY_FILE.exists()) {
            Assert.assertTrue(LEGACY_FILE.delete());
        }
    }

    @Test
    public void testLegacyFiles() throws IOException
    {
        if (!ApiCompat.supportsExternalFilesDirs()) {
            return;
        }
        Assert.assertTrue(LEGACY_FILE.createNewFile());

        verifyDrawerClosed();
        setLegacyFileChooser(true);

        onView(withId(android.R.id.list));
        onTestFile(LEGACY_FILE.getName()).check(matches(anything()));

        onView(withId(android.R.id.list))
                .check(matches(withAdaptedData(
                        withFileData(LEGACY_FILE.getName()))));
        onView(withId(android.R.id.list))
                .check(matches(not(withAdaptedData(withFileData("none.psafe3")))));
    }

    @Test
    public void testLegacyFileNav()
    {
        if (!ApiCompat.supportsExternalFilesDirs()) {
            return;
        }
        verifyDrawerClosed();
        setLegacyFileChooser(true);

        Assert.assertTrue(
                LEGACY_DIR.equals(new File("/storage/emulated/0")) ||
                LEGACY_DIR.equals(new File("/mnt/sdcard")));
        onView(withId(R.id.current_group_label))
                .check(matches(withText(LEGACY_DIR.getPath())));
        onView(withId(R.id.current_group_label))
                .perform(click());
        onView(withId(R.id.current_group_label))
                .check(matches(withText(
                        Objects.requireNonNull(
                                LEGACY_DIR.getParentFile()).getPath())));
        onView(withId(R.id.home))
                .perform(click());
        onView(withId(R.id.current_group_label))
                .check(matches(withText(LEGACY_DIR.getPath())));
    }

    @Test
    public void testLegacyLaunchFileNew()
    {
        if (!ApiCompat.supportsExternalFilesDirs()) {
            return;
        }
        verifyDrawerClosed();
        setLegacyFileChooser(true);

        onView(withId(R.id.menu_file_new))
                .perform(click());

        intended(allOf(
                hasAction(equalTo("net.tjado.passwdsafe.action.NEW")),
                toPackage("net.tjado.passwdsafe"),
                hasData("file://" + LEGACY_DIR.getAbsolutePath())));
    }

    @Test
    public void testNewLaunchFileNew()
    {
        verifyDrawerClosed();
        setLegacyFileChooser(false);

        onView(withId(R.id.menu_file_new))
                .perform(click());

        intended(allOf(
                hasAction(equalTo("net.tjado.passwdsafe.action.NEW")),
                toPackage("net.tjado.passwdsafe"),
                hasData(nullValue(Uri.class))));
    }

    @Test
    public void testNewFileOpen() throws IOException
    {
        verifyDrawerClosed();
        setLegacyFileChooser(false);
        clearRecents();

        Uri fileUri = Uri.fromFile(FILE);
        Intent openResponse =
                new Intent().setData(fileUri)
                            .putExtra("__test_display_name", FILE.getName());

        intending(allOf(
                hasAction(equalTo(
                        DocumentsContractCompat.INTENT_ACTION_OPEN_DOCUMENT)),
                hasCategories(
                        Collections.singleton(Intent.CATEGORY_OPENABLE)),
                hasType("application/*")))
                .respondWith(new Instrumentation.ActivityResult(
                        Activity.RESULT_OK, openResponse));

        Assert.assertTrue(FILE.createNewFile());
        onView(withId(R.id.fab))
                .perform(click());
        intended(allOf(hasAction(PasswdSafeUtil.VIEW_INTENT),
                       toPackage("net.tjado.passwdsafe"),
                       hasData(fileUri)));

        closeSoftKeyboard();
        pressBack();

        onNewFileList()
                .check(matches(withEffectiveVisibility(
                        ViewMatchers.Visibility.VISIBLE)));
        onNewFileList()
                .check(withRecyclerViewCount(1));
        onNewFileList()
                .check(hasRecyclerViewItemAtPosition(
                        0,
                        hasDescendant(allOf(withId(R.id.text),
                                            withText(FILE.getName())))));

        onView(withId(R.id.empty))
                .check(matches(withEffectiveVisibility(
                        ViewMatchers.Visibility.GONE)));
    }

     @Test
     public void testNewClearRecents()
     {
         verifyDrawerClosed();
         setLegacyFileChooser(false);
         clearRecents();
     }

    @Test
    public void testAbout()
    {
        onView(withId(R.id.drawer_layout))
                .check(matches(isClosed(Gravity.START)))
                .perform(open());

        onView(withId(R.id.navigation_drawer))
                .perform(navigateTo(R.id.menu_drawer_about));

        onView(withId(R.id.version))
                .check(matches(withText(BuildConfig.VERSION_NAME + " (DEBUG)")));
    }

    private static void clearRecents()
    {
        openActionBarOverflowOrOptionsMenu(
                getInstrumentation().getTargetContext());
        onView(withText(R.string.clear_recent))
                .perform(click());

        onNewFileList()
                .check(matches(withEffectiveVisibility(
                        ViewMatchers.Visibility.VISIBLE)));
        onNewFileList()
                .check(withRecyclerViewCount(0));
        onView(withId(R.id.empty))
                .check(matches(withEffectiveVisibility(
                        ViewMatchers.Visibility.VISIBLE)));
    }

    /**
     * Set whether the legacy file chooser is used
     */
    private static void setLegacyFileChooser(boolean showLegacy)
    {
        verifyDrawerClosed();

        onView(withId(R.id.drawer_layout))
                .check(matches(isClosed(Gravity.START)))
                .perform(open());
        onView(withId(R.id.navigation_drawer))
                .perform(navigateTo(R.id.menu_drawer_preferences));

        onView(withId(R.id.recycler_view))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(allOf(withId(android.R.id.title),
                                            withText(R.string.files))),
                        click()));

        if (ApiCompat.supportsExternalFilesDirs()) {

            ChildCheckedViewAction legacyCheckAction =
                    new ChildCheckedViewAction(android.R.id.checkbox,
                                               showLegacy);
            onView(withId(R.id.recycler_view))
                    .perform(RecyclerViewActions.actionOnItem(
                            hasDescendant(
                                    allOf(withId(android.R.id.title),
                                          withText(R.string.legacy_file_chooser))),
                            legacyCheckAction));
            pressBack();
            if (legacyCheckAction.isPrevChecked() == showLegacy) {
                pressBack();
            }

        } else {
            pressBack();
            pressBack();
        }

        // Ensure files, not a sync provider, is selected
        onView(withId(R.id.drawer_layout))
                .check(matches(isClosed(Gravity.START)))
                .perform(open());

        onView(withId(R.id.navigation_drawer))
                .perform(navigateTo(R.id.menu_drawer_files));
    }

    /**
     * Test on a file in the list
     */
    private static DataInteraction onTestFile(
            @SuppressWarnings("SameParameterValue") String fileTitle)
    {
        return onData(allOf(is(instanceOf(HashMap.class)),
                            withFileData(fileTitle)))
                .inAdapterView(withId(android.R.id.list));
    }

    /**
     * Test on the file list in the new file chooser
     */
    private static ViewInteraction onNewFileList()
    {
        return onView(allOf(withId(R.id.files),
                            instanceOf(RecyclerView.class)));
    }

    /**
     * Match with a FileData entry matching title text
     */
    private static Matcher<Object> withFileData(String expectedText)
    {
        // use preconditions to fail fast when a test is creating an
        // invalid matcher.
        checkNotNull(expectedText);
        return withFileData(equalTo(expectedText));
    }

    /**
     * Match with a FileData entry matching a title
     */
    private static Matcher<Object> withFileData(
            final Matcher<String> titleMatcher)
    {
        // use preconditions to fail fast when a test is creating an
        // invalid matcher.
        checkNotNull(titleMatcher);
        return new BoundedMatcher<>(Map.class)
        {
            @Override
            public boolean matchesSafely(Map data)
            {
                return hasEntry(equalTo("title"), hasToString(titleMatcher))
                        .matches(data);
            }

            @Override
            public void describeTo(Description description)
            {
                description.appendText("with file data: ");
                titleMatcher.describeTo(description);
            }
        };
    }

    /**
     * Verify the nav drawer is closed
     */
    private static void verifyDrawerClosed()
    {
        onView(withId(R.id.drawer_layout))
                .check(matches(isClosed(Gravity.START)));
    }
}
