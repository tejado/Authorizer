/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.content.Context;
import androidx.fragment.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;

import net.tjado.passwdsafe.file.PasswdFileDataUser;

/**
 * Base fragment for accessing password file data
 */
public abstract class AbstractPasswdSafeFileDataFragment
        <ListenerT extends AbstractPasswdSafeFileDataFragment.Listener>
        extends Fragment
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
    {
        /** Use the file data */
        void useFileData(PasswdFileDataUser user);

        /** Is the navigation drawer closed */
        boolean isNavDrawerClosed();
    }

    private ListenerT itsListener;

    @Override
    public void onAttach(Context ctx)
    {
        super.onAttach(ctx);
        //noinspection unchecked
        itsListener = (ListenerT)ctx;
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        itsListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        if ((itsListener != null) && itsListener.isNavDrawerClosed()) {
            doOnCreateOptionsMenu(menu, inflater);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Get the context listener
     */
    protected final ListenerT getListener()
    {
        return itsListener;
    }

    /**
     * Derived-class create options menu
     */
    protected abstract void doOnCreateOptionsMenu(Menu menu,
                                                  MenuInflater inflater);

    /**
     * Use the file data
     */
    protected final void useFileData(PasswdFileDataUser user)
    {
        if (isAdded() && itsListener != null) {
            itsListener.useFileData(user);
        }
    }
}
