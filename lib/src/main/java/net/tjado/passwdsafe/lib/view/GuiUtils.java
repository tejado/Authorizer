/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib.view;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.textfield.TextInputLayout;
import net.tjado.passwdsafe.lib.ApiCompat;

import java.util.List;

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
        listView.post(() -> {
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
                params.height += listView.getDividerHeight() * (numItems - 1);
            }
            listView.setLayoutParams(params);
        });
    }


    public static boolean isPasswordVisible(TextView tv)
    {
        return (tv.getInputType() & InputType.TYPE_MASK_VARIATION) ==
               InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
    }


    /**
     * Set whether the view shows a visible password
     */
    public static void setPasswordVisible(TextView tv,
                                          boolean visible,
                                          Context ctx)
    {
        int pos = tv.getSelectionStart();
        int type = tv.getInputType();
        boolean hasMultiline =
                (type & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0;
        type = visible ? INPUT_TEXT_PASSWORD_VISIBLE : INPUT_TEXT_PASSWORD;
        if (hasMultiline) {
            type |= InputType.TYPE_TEXT_FLAG_MULTI_LINE;
        }

        tv.setInputType(type);
        // Reset monospace as the input type change resets to default monospace
        // font
        TypefaceUtils.setMonospace(tv, ctx);
        if (tv instanceof EditText) {
            // Keep selection location after the type change
            ((EditText)tv).setSelection(pos);
        }
    }


    /**
     * Clear the contents of an EditText
     */
    public static void clearEditText(EditText tv)
    {
        tv.getText().clear();
        Runtime.getRuntime().gc();
    }

    /**
     * Is a view visible
     */
    public static boolean isVisible(@NonNull View view)
    {
        return view.getVisibility() == View.VISIBLE;
    }

    /** Set whether a view is visible */
    public static void setVisible(View view, boolean visible)
    {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /** Set whether a menu item is enabled */
    public static void setMenuEnabled(MenuItem item, boolean enabled)
    {
        item.setEnabled(enabled);
        var icon = item.getIcon();
        if (icon != null) {
            Drawable d = icon.mutate();
            d.setAlpha(enabled ? 255 : 138);
            item.setIcon(d);
        }
    }

    /**
     * Set a button checked without animation
     */
    public static void setCheckedNoAnim(CompoundButton view, boolean checked)
    {
        view.setChecked(checked);
        view.jumpDrawablesToCurrentState();
    }

    /**
     * Set whether a TextInputLayout is visible
     */
    public static void setTextInputVisible(final TextInputLayout view,
                                           final boolean visible)
    {
        // Use a delayed post to prevent stack overflow errors on gingerbread
        // from repeated toggles on Gingerbread
        view.post(() -> view.setVisibility(visible ? View.VISIBLE : View.GONE));
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
        setupFormKeyboard(firstField, finalField, ctx,
                          () -> {
                              if (okBtn.isEnabled()) {
                                  okBtn.performClick();
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
            firstField.post(new KeyboardViewer(firstField, ctx));
        }

        finalField.setOnKeyListener((v, keyCode, event) -> {
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
        });
    }


    /**
     * Set the keyboard visibility on a view
     */
    public static void setKeyboardVisible(View v, Context ctx, boolean visible)
    {
        InputMethodManager imm = (InputMethodManager)
                ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return;
        }
        if (visible) {
            imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
        } else {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }


    /**
     * Disable force-dark mode
     */
    public static void disableForceDark(View view)
    {
        if (ApiCompat.SDK_VERSION >= ApiCompat.SDK_Q) {
            GuiUtilsQ.disableForceDark(view);
        }
    }


    /**
     * Get a drawable resource
     */
    public static Drawable getDrawable(Resources res, int id)
    {
        return ResourcesCompat.getDrawable(res, id, null);
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
        NotificationCompat.Builder builder = createNotificationBuilder(ctx)
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
        if (!ApiCompat.areNotificationsEnabled(notifyMgr)) {
            return;
        }

        Drawable draw = getDrawable(ctx.getResources(), bigIcon);
        Bitmap icon;
        if (draw instanceof BitmapDrawable) {
            icon = ((BitmapDrawable)draw).getBitmap();
        } else if (draw != null) {
            icon = Bitmap.createBitmap(draw.getIntrinsicWidth(),
                                       draw.getIntrinsicHeight(),
                                       Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(icon);
            draw = draw.mutate();
            draw.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            draw.draw(canvas);
        } else {
            return;
        }
        builder.setLargeIcon(icon);
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

    /**
     * Create a notification builder
     */
    public static NotificationCompat.Builder createNotificationBuilder(
            Context ctx)
    {
        if (ApiCompat.SDK_VERSION >= ApiCompat.SDK_OREO) {
            return GuiUtilsOreo.createNotificationBuilder(ctx);
        }
        //noinspection deprecation
        return new NotificationCompat.Builder(ctx);
    }
}
