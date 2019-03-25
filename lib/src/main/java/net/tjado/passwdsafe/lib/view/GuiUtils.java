/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib.view;

import java.util.List;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.google.android.material.textfield.TextInputLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.core.app.NotificationCompat;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import net.tjado.passwdsafe.lib.ApiCompat;

/**
 * @author jharris
 *
 */
@SuppressWarnings("SameParameterValue")
public final class GuiUtils
{
    private static final int INPUT_TEXT_PASSWORD =
        InputType.TYPE_CLASS_TEXT |
        InputType.TYPE_TEXT_VARIATION_PASSWORD;
    private static final int INPUT_TEXT_PASSWORD_VISIBLE =
        InputType.TYPE_CLASS_TEXT |
        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;


    /**
     * Set the height of a ListView based on all of its children
     */
    public static void setListViewHeightBasedOnChildren(final ListView listView)
    {
        final ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        // Defer measurement so listview is rendered to get its width
        listView.post(new Runnable()
        {
            @Override
            public void run()
            {
                int width = View.MeasureSpec.makeMeasureSpec(
                        listView.getMeasuredWidth(), View.MeasureSpec.AT_MOST);
                int height = View.MeasureSpec.makeMeasureSpec(
                        0, View.MeasureSpec.UNSPECIFIED);
                int totalHeight = 0;
                int numItems = listAdapter.getCount();
                for (int i = 0; i < numItems; i++) {
                    View listItem = listAdapter.getView(i, null, listView);
                    listItem.measure(width, height);
                    totalHeight += listItem.getMeasuredHeight();
                }

                ViewGroup.LayoutParams params = listView.getLayoutParams();
                params.height = totalHeight;
                if (numItems > 0) {
                    params.height +=
                            listView.getDividerHeight() * (numItems - 1);
                }
                listView.setLayoutParams(params);
            }
        });
    }


    public static boolean isPasswordVisible(TextView tv)
    {
        return tv.getInputType() == INPUT_TEXT_PASSWORD_VISIBLE;
    }


    /**
     * Set whether the view shows a visible password
     */
    public static void setPasswordVisible(TextView tv,
                                          boolean visible,
                                          Context ctx)
    {
        int pos = tv.getSelectionStart();
        tv.setInputType(visible ? INPUT_TEXT_PASSWORD_VISIBLE :
                                INPUT_TEXT_PASSWORD);
        // Reset monospace as the input type change resets to default monospace
        // font
        TypefaceUtils.setMonospace(tv, ctx);
        if (tv instanceof EditText) {
            // Keep selection location after the type change
            ((EditText)tv).setSelection(pos);
        }
    }


    /** Set whether a view is visible */
    public static void setVisible(View view, boolean visible)
    {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }


    /**
     * Set whether a TextInputLayout is visible
     */
    public static void setTextInputVisible(final TextInputLayout view,
                                           final boolean visible)
    {
        // Use a delayed post to prevent stack overflow errors on gingerbread
        // from repeated toggles on Gingerbread
        view.post(new Runnable()
        {
            @Override
           public void run()
            {
                view.setVisibility(visible ? View.VISIBLE : View.GONE);
           }
       });
    }


    /**
     * Setup the keyboard on a form.  The final field clicks the supplied OK
     * button when enter is pressed.
     */
    public static void setupFormKeyboard(TextView firstField,
                                         TextView finalField,
                                         final Button okBtn,
                                         Context ctx)
    {
        setupFormKeyboard(firstField, finalField, ctx, new Runnable()
        {
            @Override
            public void run()
            {
                if (okBtn.isEnabled()) {
                    okBtn.performClick();
                }
            }
        });
    }


    /**
     * Setup the keyboard on a form.  The final field performs the supplied
     * runnable when enter is pressed.
     */
    public static void setupFormKeyboard(TextView firstField,
                                         TextView finalField,
                                         Context ctx,
                                         final Runnable enterRunnable)
    {
        if (firstField != null) {
            GuiUtilsFroyo.showKeyboard(firstField, ctx);
        }

        finalField.setOnKeyListener(new OnKeyListener()
        {
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER: {
                        enterRunnable.run();
                        return true;
                    }
                    }
                }
                return false;
            }
        });
    }


    /**
     * Set the keyboard visibility on a view
     */
    public static void setKeyboardVisible(View v, Context ctx, boolean visible)
    {
        InputMethodManager imm = (InputMethodManager)
            ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (visible) {
            if (ApiCompat.SDK_VERSION >= ApiCompat.SDK_HONEYCOMB) {
                imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
            } else {
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,
                                    InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        } else {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }


    /**
     * Invalidate the options menu on an activity
     */
    public static void invalidateOptionsMenu(Activity act)
    {
        if (act instanceof FragmentActivity) {
            ((FragmentActivity)act).supportInvalidateOptionsMenu();
        } else if (ApiCompat.SDK_VERSION >= ApiCompat.SDK_HONEYCOMB) {
            GuiUtilsHoneycomb.invalidateOptionsMenu(act);
        }
    }


    /** Set the text in a TextView as selectable */
    public static void setTextSelectable(TextView tv)
    {
        if (ApiCompat.SDK_VERSION >= ApiCompat.SDK_HONEYCOMB) {
            GuiUtilsHoneycomb.setTextSelectable(tv);
        }
    }


    /**
     * Get a drawable resource
     */
    public static Drawable getDrawable(Resources res, int id)
    {
        if (ApiCompat.SDK_VERSION >= ApiCompat.SDK_LOLLIPOP) {
            return GuiUtilsLollipop.getDrawable(res, id);
        } else {
            return GuiUtilsFroyo.getDrawable(res, id);
        }
    }


    /** Show a notification */
    public static void showNotification(NotificationManager notifyMgr,
                                        Context ctx,
                                        int iconId,
                                        String tickerText,
                                        String title,
                                        int bigIcon,
                                        String content,
                                        List<String> bigLines,
                                        PendingIntent intent,
                                        int notifyId,
                                        String notifyTag,
                                        boolean autoCancel)
    {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(intent)
                .setSmallIcon(iconId)
                .setTicker(tickerText)
                .setAutoCancel(autoCancel);
        setInboxStyle(builder, title, content, bigLines);
        showNotification(notifyMgr, builder, bigIcon, notifyId, notifyTag, ctx);
    }


    /**
     * Show a notification with a custom builder
     */
    public static void showNotification(NotificationManager notifyMgr,
                                        NotificationCompat.Builder builder,
                                        int bigIcon,
                                        int notifyId,
                                        String notifyTag,
                                        Context ctx)
    {
        BitmapDrawable b =
                (BitmapDrawable)getDrawable(ctx.getResources(), bigIcon);
        if (b == null) {
            return;
        }
        builder.setLargeIcon(b.getBitmap());
        notifyMgr.notify(notifyTag, notifyId, builder.build());
    }


    /**
     * Set an inbox style for a notification
     */
    public static void setInboxStyle(NotificationCompat.Builder builder,
                                     String title,
                                     String content,
                                     List<String> lines)
    {
        NotificationCompat.InboxStyle style =
                new NotificationCompat.InboxStyle(builder)
                        .setBigContentTitle(title)
                        .setSummaryText(content);

        int linesSize = lines.size();
        int numLines = Math.min(linesSize, 5);
        for (int i = 0; i < numLines; ++i) {
            style.addLine(lines.get(i));
        }
        if (numLines < linesSize) {
            style.addLine("…");
            builder.setNumber(linesSize);
        }
    }
}
