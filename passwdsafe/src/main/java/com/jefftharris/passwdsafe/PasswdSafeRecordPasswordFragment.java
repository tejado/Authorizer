/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jefftharris.passwdsafe.file.HeaderPasswdPolicies;
import com.jefftharris.passwdsafe.file.PasswdPolicy;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.view.PasswdPolicyView;


/**
 * Fragment for showing password-specific fields of a password record
 */
public class PasswdSafeRecordPasswordFragment
        extends AbstractPasswdSafeRecordFragment
{
    private View itsPolicyRow;
    private PasswdPolicyView itsPolicy;

    /**
     * Create a new instance of the fragment
     */
    public static PasswdSafeRecordPasswordFragment newInstance(String recUuid)
    {
        PasswdSafeRecordPasswordFragment frag =
                new PasswdSafeRecordPasswordFragment();
        frag.setArguments(createArgs(recUuid));
        return frag;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View root = inflater.inflate(
                R.layout.fragment_passwdsafe_record_password, container, false);
        itsPolicyRow = root.findViewById(R.id.policy_row);
        itsPolicy = (PasswdPolicyView)root.findViewById(R.id.policy);
        itsPolicy.setGenerateEnabled(false);
        return root;
    }

    @Override
    protected void doOnCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
    }

    @Override
    protected void doRefresh()
    {
        RecordInfo info = getRecordInfo();
        if (info == null) {
            return;
        }

        PasswdPolicy policy = null;
        String policyLoc = null;
        switch (info.itsPasswdRec.getType()) {
        case NORMAL: {
            policy = info.itsPasswdRec.getPasswdPolicy();
            if (policy == null) {
                PasswdSafeApp app =
                        (PasswdSafeApp)getActivity().getApplication();
                policy = app.getDefaultPasswdPolicy();
                policyLoc = getString(R.string.default_policy);
            } else if (policy.getLocation() ==
                       PasswdPolicy.Location.RECORD_NAME) {
                HeaderPasswdPolicies hdrPolicies =
                        info.itsFileData.getHdrPasswdPolicies();
                String policyName = policy.getName();
                if (hdrPolicies != null) {
                    policy = hdrPolicies.getPasswdPolicy(policyName);
                }
                if (policy != null) {
                    policyLoc = getString(R.string.database_policy, policyName);
                }
            } else {
                policyLoc = getString(R.string.record);
            }
            break;
        }
        case ALIAS:
        case SHORTCUT: {
            break;
        }
        }

        if (policy != null) {
            itsPolicy.showLocation(policyLoc);
            itsPolicy.showPolicy(policy, -1);
        }
        GuiUtils.setVisible(itsPolicyRow, policy != null);
    }
}
