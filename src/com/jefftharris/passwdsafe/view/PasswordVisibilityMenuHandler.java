/*
 * Copyright (©) 2011 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.view;

import com.jefftharris.passwdsafe.GuiUtils;
import com.jefftharris.passwdsafe.R;

import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.TextView;

public class PasswordVisibilityMenuHandler
    implements OnCreateContextMenuListener, OnMenuItemClickListener
{
    private TextView[] itsViews;

    public static void set(TextView view)
    {
        new PasswordVisibilityMenuHandler(view, null);
    }

    public static void set(TextView view1, TextView view2)
    {
        new PasswordVisibilityMenuHandler(view1, view2);
    }

    private PasswordVisibilityMenuHandler(TextView view1, TextView view2)
    {
        if (view2 == null) {
            itsViews = new TextView[] { view1 };
        } else {
            itsViews = new TextView[] { view1, view2 };
        }

        for (TextView tv : itsViews) {
            tv.setOnCreateContextMenuListener(this);
        }
    }

    public boolean onMenuItemClick(MenuItem item)
    {
        boolean visible = GuiUtils.isPasswordVisible(itsViews[0]);
        for (TextView tv : itsViews) {
            GuiUtils.setPasswordVisible(tv, !visible);
        }
        return true;
    }

    public void onCreateContextMenu(ContextMenu menu,
                                    View v,
                                    ContextMenuInfo menuInfo)
    {
        menu.setHeaderTitle(R.string.app_name);
        boolean visible = GuiUtils.isPasswordVisible(itsViews[0]);
        int title = (itsViews.length > 1) ?
            (visible ? R.string.hide_passwords : R.string.show_passwords) :
            (visible ? R.string.hide_password : R.string.show_password);

        MenuItem mi = menu.add(0, 0, 0, title);
        mi.setOnMenuItemClickListener(this);
    }
}
