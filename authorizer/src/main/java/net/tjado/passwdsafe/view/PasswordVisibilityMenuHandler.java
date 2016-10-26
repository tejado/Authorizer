/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.view;

import net.tjado.passwdsafe.R;
import net.tjado.passwdsafe.lib.ApiCompat;
import net.tjado.passwdsafe.lib.view.GuiUtils;

import android.content.Context;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.TextView;

public class PasswordVisibilityMenuHandler
{
    public static void set(Context ctx, TextView... views)
    {
        for (TextView tv : views) {
            tv.setOnCreateContextMenuListener(new MenuListener(ctx, tv, views));
        }
    }

    private static class MenuListener
        implements OnCreateContextMenuListener, OnMenuItemClickListener
    {
        private static final int MENU_TOGGLE_PASSWORD = 0;

        private final Context itsContext;
        private final TextView[] itsViews;
        private final TextView itsView;

        public MenuListener(Context ctx, TextView view, TextView[] views)
        {
            itsContext = ctx;
            itsViews = views;
            itsView = view;
        }

        public void onCreateContextMenu(ContextMenu menu,
                                        View v,
                                        ContextMenuInfo menuInfo)
        {
            menu.setHeaderTitle(R.string.app_name);

            MenuItem mi;
            mi = menu.findItem(android.R.id.paste);
            if (mi == null) {
                if (ApiCompat.clipboardHasText(v.getContext())) {
                    mi = menu.add(0, android.R.id.paste, 0,
                                  android.R.string.paste);
                    mi.setOnMenuItemClickListener(this);
                }
            }

            boolean visible = GuiUtils.isPasswordVisible(itsView);
            int title = (itsViews.length > 1) ?
                (visible ? R.string.hide_passwords : R.string.show_passwords) :
                (visible ? R.string.hide_password : R.string.show_password);

            mi = menu.add(0, MENU_TOGGLE_PASSWORD, 0, title);
            mi.setOnMenuItemClickListener(this);
        }

        public boolean onMenuItemClick(MenuItem item)
        {
            boolean rc = true;
            switch (item.getItemId()) {
            case android.R.id.paste: {
                itsView.onTextContextMenuItem(android.R.id.paste);
                break;
            }
            case MENU_TOGGLE_PASSWORD: {
                boolean visible = GuiUtils.isPasswordVisible(itsView);
                for (TextView tv : itsViews) {
                    GuiUtils.setPasswordVisible(tv, !visible, itsContext);
                }
                break;
            }
            default: {
                rc = false;
                break;
            }
            }
            return rc;
        }
    }
}
