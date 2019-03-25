/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib.view;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

/**
 * Generic progress dialog
 */
public class ProgressFragment extends DialogFragment
{
    public static ProgressFragment newInstance(String msg)
    {
        ProgressFragment frag = new ProgressFragment();
        Bundle args = new Bundle();
        args.putString("msg", msg);
        frag.setArguments(args);
        return frag;
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
     */
    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState)
    {
        setCancelable(false);
        ProgressDialog dlg = new ProgressDialog(getActivity());
        dlg.setIndeterminate(true);
        dlg.setMessage(getArguments().getString("msg"));
        return dlg;
    }
}
