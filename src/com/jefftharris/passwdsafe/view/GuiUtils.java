/*
 * Copyright (©) 2010-2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jefftharris.passwdsafe.file.PasswdHistory;
import com.jefftharris.passwdsafe.util.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
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

/**
 * @author jharris
 *
 */
public final class GuiUtils
{
    private static final int SDK_CUPCAKE =
        android.os.Build.VERSION_CODES.CUPCAKE;
    private static final int SDK_FROYO =
        android.os.Build.VERSION_CODES.FROYO;
    private static final int SDK_HONEYCOMB = 11;

    public static final int SDK_VERSION;
    static {
        int sdk;
        try {
            sdk = Integer.parseInt(android.os.Build.VERSION.SDK);
        } catch (NumberFormatException e) {
            // Default back to android 1.5
            sdk = SDK_CUPCAKE;
        }
        SDK_VERSION = sdk;
    }


    private static final String PASSWD = "passwd";
    private static final String DATE = "date";

    public static final int INPUT_TEXT_PASSWORD =
        InputType.TYPE_CLASS_TEXT |
        InputType.TYPE_TEXT_VARIATION_PASSWORD;
    public static final int INPUT_TEXT_PASSWORD_VISIBLE =
        InputType.TYPE_CLASS_TEXT |
        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;


    /**
     * The EnableAdapter class is a SimpleAdapter that can show its items in a
     * disabled state
     */
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


    public static ListAdapter createPasswdHistoryAdapter(PasswdHistory history,
                                                         Context context,
                                                         boolean enabled)
    {
        ArrayList<HashMap<String, Object>> histData =
            new ArrayList<HashMap<String, Object>>();
        for (PasswdHistory.Entry entry : history.getPasswds()) {
            HashMap<String, Object> entryData =
                new HashMap<String, Object>();
            entryData.put(PASSWD, entry.getPasswd());
            entryData.put(DATE, Utils.formatDate(entry.getDate(), context));
            histData.add(entryData);
        }

        ListAdapter adapter =
            new EnableAdapter(context, histData,
                              android.R.layout.simple_list_item_2,
                              new String[] { PASSWD, DATE },
                              new int[] { android.R.id.text1,
                                          android.R.id.text2 },
                              enabled);
        return adapter;
    }


    public static boolean isBackKeyDown(int keyCode, KeyEvent event)
    {
        return (SDK_VERSION < android.os.Build.VERSION_CODES.ECLAIR)
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
    public static void setShowKeyboardListener(Dialog dialog, View view,
                                               Context ctx)
    {
        if (SDK_VERSION >= SDK_FROYO) {
            GuiUtilsFroyo.setShowKeyboardListener(dialog, view, ctx);
        }
    }


    /**
     * Invalidate the options menu on an activity
     */
    public static void invalidateOptionsMenu(Activity act)
    {
        if (SDK_VERSION >= SDK_HONEYCOMB) {
            GuiUtilsHoneycomb.invalidateOptionsMenu(act);
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
        if (SDK_VERSION <= SDK_CUPCAKE) {
            RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams)v.getLayoutParams();
            params.addRule(RelativeLayout.CENTER_VERTICAL, 0);
            v.setLayoutParams(params);
        }
    }
}
