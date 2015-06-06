/*
 * Copyright (©) 2010-2014 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.lib.view;

import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
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
     * The EnableAdapter class is a SimpleAdapter that can show its items in a
     * disabled state
     */
    @SuppressWarnings("SameParameterValue")
    public static class EnableAdapter extends SimpleAdapter
    {
        private final boolean itsIsEnabled;

        /**
         * Constructor
         */
        public EnableAdapter(Context context,
                             List<? extends Map<String, ?>> data, int resource,
                             String[] from, int[] to, boolean enabled)
        {
            super(context, data, resource, from, to);
            itsIsEnabled = enabled;
        }

        /* (non-Javadoc)
         * @see android.widget.SimpleAdapter#getView(int, android.view.View, android.view.ViewGroup)
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View v = super.getView(position, convertView, parent);
            if (!itsIsEnabled) {
                setEnabled(v);
            }
            return v;
        }

        /**
         * Set the enabled state of the view and its children
         */
        private void setEnabled(View v)
        {
            v.setEnabled(itsIsEnabled);
            if (v instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup)v;
                for (int i = 0; i < vg.getChildCount(); ++i) {
                    setEnabled(vg.getChildAt(i));
                }
            }
        }
    }


    public static String getTextViewStr(Activity act, int viewId)
    {
        TextView tv = (TextView)act.findViewById(viewId);
        return tv.getText().toString();
    }

    public static String getSpinnerStr(Activity act, int viewId)
    {
        Spinner s = (Spinner)act.findViewById(viewId);
        Object obj = s.getSelectedItem();
        return (obj == null) ? null : obj.toString();
    }


    public static void setListViewHeightBasedOnChildren(ListView listView)
    {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight +
            (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }


    public static boolean isBackKeyDown(int keyCode, KeyEvent event)
    {
        return (ApiCompat.SDK_VERSION < android.os.Build.VERSION_CODES.ECLAIR)
            && (keyCode == KeyEvent.KEYCODE_BACK)
            && (event.getRepeatCount() == 0);
    }


    public static boolean isPasswordVisible(TextView tv)
    {
        return tv.getInputType() == INPUT_TEXT_PASSWORD_VISIBLE;
    }


    public static void setPasswordVisible(TextView tv, boolean visible)
    {
        tv.setInputType(visible ? INPUT_TEXT_PASSWORD_VISIBLE :
                        INPUT_TEXT_PASSWORD);
    }


    /** Set whether a view is visible */
    public static void setVisible(View view, boolean visible)
    {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }


    /**
     * Setup the keyboard on a dialog. The initial field gets focus and shows
     * the keyboard. The final field clicks the Ok button when enter is pressed.
     */
    public static void setupDialogKeyboard(final AlertDialog dialog,
                                           TextView initialField,
                                           TextView finalField,
                                           Context ctx)
    {
        setShowKeyboardListener(dialog, initialField, ctx);
        finalField.setOnKeyListener(new OnKeyListener()
        {
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER: {
                        Button btn =
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        if (btn.isEnabled()) {
                            btn.performClick();
                        }
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
            imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
        } else {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }


    /**
     * Set a listener to show the keyboard when the dialog is shown. Only works
     * on Froyo and higher.
     */
    private static void setShowKeyboardListener(Dialog dialog, View view,
                                                Context ctx)
    {
        if (ApiCompat.SDK_VERSION >= ApiCompat.SDK_FROYO) {
            GuiUtilsFroyo.setShowKeyboardListener(dialog, view, ctx);
        }
    }


    /**
     * Invalidate the options menu on an activity
     */
    public static void invalidateOptionsMenu(Activity act)
    {
        if (ApiCompat.SDK_VERSION >= ApiCompat.SDK_HONEYCOMB) {
            GuiUtilsHoneycomb.invalidateOptionsMenu(act);
        }
    }


    /** Try to switch to the previous input method */
    public static void switchToLastInputMethod(InputMethodManager mgr,
                                               IBinder token)
    {
        if (ApiCompat.SDK_VERSION >= ApiCompat.SDK_HONEYCOMB) {
            GuiUtilsHoneycomb.switchToLastInputMethod(mgr, token);
        }
    }


    /** Set the text in a TextView as selectable */
    public static void setTextSelectable(TextView tv)
    {
        if (ApiCompat.SDK_VERSION >= ApiCompat.SDK_HONEYCOMB) {
            GuiUtilsHoneycomb.setTextSelectable(tv);
        }
    }

    /** Ensure the selected item in a ListView is visible */
    public static void ensureListViewSelectionVisible(final ListView lv,
                                                      final int pos)
    {
        lv.post(new Runnable() {
            public void run()
            {
                if ((pos <= lv.getFirstVisiblePosition()) ||
                    (pos >= lv.getLastVisiblePosition())) {
                    lv.setSelection(pos);
                }
            }
        });
    }


    /** Remove the layout_centerVertical flag if it is not supported */
    public static void removeUnsupportedCenterVertical(View v)
    {
        if (ApiCompat.SDK_VERSION <= ApiCompat.SDK_CUPCAKE) {
            RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams)v.getLayoutParams();
            params.addRule(RelativeLayout.CENTER_VERTICAL, 0);
            v.setLayoutParams(params);
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
        Notification notif;
        if (ApiCompat.SDK_VERSION == ApiCompat.SDK_CUPCAKE) {
            notif = new Notification(iconId, tickerText,
                                     System.currentTimeMillis());
            notif.setLatestEventInfo(ctx, title, content, intent);
        } else {
            BitmapDrawable b =
                    (BitmapDrawable)ctx.getResources().getDrawable(bigIcon);
            if (b == null) {
                return;
            }
            NotificationCompat.Builder builder =
                new NotificationCompat.Builder(ctx)
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
            notif = builder.build();
        }
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
        Notification notif;
        if (ApiCompat.SDK_VERSION == ApiCompat.SDK_CUPCAKE) {
            notif = new Notification(iconId, title,
                                     System.currentTimeMillis());
            notif.setLatestEventInfo(ctx, title, content, intent);
        } else {
            BitmapDrawable b =
                    (BitmapDrawable)ctx.getResources().getDrawable(bigIcon);
            if (b == null) {
                return;
            }
            NotificationCompat.Builder builder =
                new NotificationCompat.Builder(ctx)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(intent)
                .setSmallIcon(iconId)
                .setLargeIcon(b.getBitmap())
                .setTicker(title)
                .setAutoCancel(autoCancel);
            notif = builder.build();
        }
        notifyMgr.notify(notifyId, notif);
    }
}
