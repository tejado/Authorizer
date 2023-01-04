/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import net.tjado.passwdsafe.lib.view.GuiUtils;

import java.util.Objects;

/**
 * Fragment for the navigation drawer of the PasswdSafe activity
 */
public class PasswdSafeNavDrawerFragment
        extends AbstractNavDrawerFragment<PasswdSafeNavDrawerFragment.Listener>
        implements CompoundButton.OnCheckedChangeListener
{
    /** Preference for initial forced open count */
    private static final String PREF_SHOWN_DRAWER =
            "passwdsafe_navigation_drawer_shown_passwdsafe";

    /** Counter for how often the drawer is forced open for user to see
     *  changes */
    private static final int SHOW_DRAWER_COUNT = 1;

    /** Listener interface for the owning activity */
    public interface Listener
    {
        /** Show the file records */
        void showFileRecords();

        /** Show the file record errors */
        void showFileRecordErrors();

        /** Show the file password policies */
        void showFilePasswordPolicies();

        /** Show the file expired passwords */
        void showFileExpiredPasswords();

        /** Show the preferences */
        void showPreferences();

        /** Show the about dialog */
        void showAbout();

        /** Is the file writable */
        boolean isFileWritable();

        /** Set the file writable */
        void setFileWritable(boolean writable);

        /** Is the file capable of being writable */
        boolean isFileWriteCapable();
    }

    /** Mode of the navigation drawer */
    public enum Mode
    {
        /** Initial state */
        INIT,
        /** List of records */
        RECORDS_LIST,
        /** Single record */
        RECORDS_SINGLE,
        /** Action on a record */
        RECORDS_ACTION,
        /** Password policies */
        POLICIES,
        /** Record errors */
        RECORD_ERRORS,
        /** Password expirations */
        EXPIRATIONS,
        /** Preferences */
        PREFERENCES,
        /** About */
        ABOUT
    }

    private TextView itsFileName;
    private SwitchCompat itsWritableSw;
    private NavMenuItem itsSelNavItem = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        View fragView = doCreateView(inflater, container,
                                     R.layout.fragment_passwdsafe_nav_drawer);
        View header = getNavView().getHeaderView(0);
        itsFileName = header.findViewById(R.id.file_name);

        MenuItem writableItem =
                getNavView().getMenu().findItem(R.id.menu_drawer_writable);
        itsWritableSw = Objects.requireNonNull(writableItem.getActionView())
                       .findViewById(R.id.switch_item);
        itsWritableSw.setOnCheckedChangeListener(this);

        return fragView;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        itsWritableSw.setChecked(getListener().isFileWritable());
    }

    /**
     * Users of this fragment must call this method to set up the navigation
     * drawer interactions.
     *
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(DrawerLayout drawerLayout)
    {
        doSetUp(drawerLayout, PREF_SHOWN_DRAWER, SHOW_DRAWER_COUNT);
        updateView(Mode.INIT, "", false);
    }

    /**
     * Update drawer for the fragments displayed in the activity
     * @param fileNameUpdate If non-null, the file name to set in the view
     */
    public void updateView(Mode mode, String fileNameUpdate, boolean fileOpen)
    {
        boolean drawerEnabled = false;
        boolean openDrawer = false;
        int upIndicator = 0;
        NavMenuItem selNavItem = null;

        switch (mode) {
        case INIT: {
            drawerEnabled = true;
            break;
        }
        case RECORDS_LIST: {
            drawerEnabled = true;
            // If the user hasn't 'learned' about the drawer, open it
            openDrawer = shouldOpenDrawer();
            selNavItem = NavMenuItem.RECORDS;
            break;
        }
        case RECORDS_SINGLE: {
            selNavItem = NavMenuItem.RECORDS;
            break;
        }
        case RECORDS_ACTION: {
            upIndicator = R.drawable.ic_action_close_cancel;
            selNavItem = NavMenuItem.RECORDS;
            break;
        }
        case POLICIES: {
            drawerEnabled = true;
            selNavItem = NavMenuItem.PASSWORD_POLICIES;
            break;
        }
        case RECORD_ERRORS: {
            drawerEnabled = true;
            selNavItem = NavMenuItem.RECORD_ERRORS;
            break;
        }
        case EXPIRATIONS: {
            drawerEnabled = true;
            selNavItem = NavMenuItem.EXPIRED_PASSWORDS;
            break;
        }
        case PREFERENCES: {
            drawerEnabled = true;
            selNavItem = NavMenuItem.PREFERENCES;
            break;
        }
        case ABOUT: {
            drawerEnabled = true;
            selNavItem = NavMenuItem.ABOUT;
            break;
        }
        }

        updateDrawerToggle(drawerEnabled, upIndicator);

        Listener listener = getListener();
        boolean writeCapable = listener.isFileWriteCapable();

        Menu menu = getNavView().getMenu();
        for (int i = 0; i < menu.size(); ++i) {
            MenuItem item = menu.getItem(i);
            int itemId = item.getItemId();
            if (selNavItem == null) {
                item.setChecked(false);
            } else if (selNavItem.itsMenuId == itemId) {
                item.setVisible(true);
                item.setChecked(true);
            }

            if ((itemId == NavMenuItem.RECORDS.itsMenuId) ||
                (itemId == NavMenuItem.PASSWORD_POLICIES.itsMenuId) ||
                (itemId == NavMenuItem.EXPIRED_PASSWORDS.itsMenuId)) {
                item.setEnabled(fileOpen);
            } else if (itemId == R.id.menu_drawer_writable) {
                item.setVisible(fileOpen);
                item.setEnabled(writeCapable);
            }
        }
        itsSelNavItem = selNavItem;

        if (fileNameUpdate != null) {
            GuiUtils.setVisible(itsFileName,
                                !TextUtils.isEmpty(fileNameUpdate));
            itsFileName.setText(fileNameUpdate);
        }

        itsWritableSw.setEnabled(writeCapable);
        itsWritableSw.setChecked(listener.isFileWritable());

        openDrawer(openDrawer);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem)
    {
        if (menuItem.getItemId() == R.id.menu_drawer_writable) {
            if (itsWritableSw.isEnabled()) {
                itsWritableSw.toggle();
            }
            return true;
        }
        closeDrawer();

        Listener listener = getListener();
        NavMenuItem navItem = NavMenuItem.fromMenuId(menuItem.getItemId());
        if ((navItem != null) && (itsSelNavItem != navItem)) {
            switch (navItem) {
            case RECORDS: {
                listener.showFileRecords();
                break;
            }
            case RECORD_ERRORS: {
                listener.showFileRecordErrors();
                break;
            }
            case PASSWORD_POLICIES: {
                listener.showFilePasswordPolicies();
                break;
            }
            case EXPIRED_PASSWORDS: {
                listener.showFileExpiredPasswords();
                break;
            }
            case PREFERENCES: {
                listener.showPreferences();
                break;
            }
            case ABOUT: {
                listener.showAbout();
                break;
            }
            }
        }

        return true;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        getListener().setFileWritable(isChecked);
        closeDrawer();
    }

    /**
     * A menu item
     */
    private enum NavMenuItem
    {
        RECORDS              (R.id.menu_drawer_records),
        RECORD_ERRORS        (R.id.menu_drawer_record_errors),
        PASSWORD_POLICIES    (R.id.menu_drawer_passwd_policies),
        EXPIRED_PASSWORDS    (R.id.menu_drawer_expired_passwords),
        PREFERENCES          (R.id.menu_drawer_preferences),
        ABOUT                (R.id.menu_drawer_about);

        private final int itsMenuId;

        /**
         * Constructor
         */
        NavMenuItem(int menuId)
        {
            itsMenuId = menuId;
        }

        /**
         * Get the enum from a menu id
         */
        private static NavMenuItem fromMenuId(int menuId)
        {
            for (NavMenuItem item: NavMenuItem.values()) {
                if (item.itsMenuId == menuId) {
                    return item;
                }
            }
            return null;
        }
    }
}
