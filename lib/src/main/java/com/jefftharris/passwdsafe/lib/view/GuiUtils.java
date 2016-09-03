/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.lib.view;

import java.util.List;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
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

import com.jefftharris.passwdsafe.lib.ApiCompat;

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
                                        boolean autoCancel)
    {
        BitmapDrawable b =
                (BitmapDrawable)getDrawable(ctx.getResources(), bigIcon);
        if (b == null) {
            return;
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(intent)
                .setSmallIcon(iconId)
                .setLargeIcon(b.getBitmap())
                .setTicker(tickerText)
                .setAutoCancel(autoCancel);
        NotificationCompat.InboxStyle style =
                new NotificationCompat.InboxStyle(builder)
                .setBigContentTitle(title)
                .setSummaryText(content);

        int numLines = Math.min(bigLines.size(), 5);
        for (int i = 0; i < numLines; ++i) {
            style.addLine(bigLines.get(i));
        }
        if (numLines < bigLines.size()) {
            style.addLine("…");
        }

        builder.setStyle(style);
        Notification notif = builder.build();
        notifyMgr.notify(notifyId, notif);
    }


    /** Show a simple notification */
    public static void showSimpleNotification(NotificationManager notifyMgr,
                                              Context ctx,
                                              int iconId,
                                              String title,
                                              int bigIcon,
                                              String content,
                                              PendingIntent intent,
                                              int notifyId,
                                              boolean autoCancel)
    {
        BitmapDrawable b =
                (BitmapDrawable)getDrawable(ctx.getResources(), bigIcon);
        if (b == null) {
            return;
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(intent)
                .setSmallIcon(iconId)
                .setLargeIcon(b.getBitmap())
                .setTicker(title)
                .setAutoCancel(autoCancel);
        Notification notif = builder.build();
        notifyMgr.notify(notifyId, notif);
    }
}
