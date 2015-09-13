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
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.HeaderPasswdPolicies;
import com.jefftharris.passwdsafe.file.PasswdExpiration;
import com.jefftharris.passwdsafe.file.PasswdPolicy;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.view.PasswdPolicyView;

import org.pwsafe.lib.file.PwsRecord;

import java.util.Date;


/**
 * Fragment for showing password-specific fields of a password record
 */
public class PasswdSafeRecordPasswordFragment
        extends AbstractPasswdSafeRecordFragment
{
    private View itsPolicyRow;
    private PasswdPolicyView itsPolicy;
    private View itsPasswordTimesRow;
    private View itsExpirationTimeRow;
    private TextView itsExpirationTime;
    private View itsExpirationIntervalRow;
    private TextView itsExpirationInterval;
    private View itsPasswordModTimeRow;
    private TextView itsPasswordModTime;

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
        itsPasswordTimesRow = root.findViewById(R.id.password_times_row);
        itsExpirationTimeRow = root.findViewById(R.id.expiration_time_row);
        itsExpirationTime = (TextView)root.findViewById(R.id.expiration_time);
        itsExpirationIntervalRow =
                root.findViewById(R.id.expiration_interval_row);
        itsExpirationInterval =
                (TextView)root.findViewById(R.id.expiration_interval);
        itsPasswordModTimeRow = root.findViewById(R.id.password_mod_time_row);
        itsPasswordModTime =
                (TextView)root.findViewById(R.id.password_mod_time);
        return root;

        // TODO: spacing between fields
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
        PasswdExpiration passwdExpiry = null;
        Date lastModTime = null;
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
            passwdExpiry = info.itsFileData.getPasswdExpiry(info.itsRec);
            lastModTime = info.itsFileData.getPasswdLastModTime(info.itsRec);
            break;
        }
        case ALIAS: {
            PwsRecord recForPassword = info.itsPasswdRec.getRef();
            passwdExpiry = info.itsFileData.getPasswdExpiry(recForPassword);
            lastModTime = info.itsFileData.getPasswdLastModTime(recForPassword);
            break;
        }
        case SHORTCUT: {
            break;
        }
        }

        String expiryIntStr = null;
        if ((passwdExpiry != null) && passwdExpiry.itsIsRecurring) {
            int val = passwdExpiry.itsInterval;
            if (val != 0) {
                expiryIntStr = getResources().getQuantityString(
                        R.plurals.interval_days, val, val);
            }
        }

        if (policy != null) {
            itsPolicy.showLocation(policyLoc);
            itsPolicy.showPolicy(policy, -1);
        }
        GuiUtils.setVisible(itsPolicyRow, policy != null);

        setFieldDate(itsExpirationTime, itsExpirationTimeRow,
                     (passwdExpiry != null) ?
                             passwdExpiry.itsExpiration : null);
        setFieldText(itsExpirationInterval, itsExpirationIntervalRow,
                     expiryIntStr);
        setFieldDate(itsPasswordModTime, itsPasswordModTimeRow, lastModTime);
        //noinspection ConstantConditions
        GuiUtils.setVisible(itsPasswordTimesRow,
                            (passwdExpiry != null) || (lastModTime != null) ||
                            (expiryIntStr != null));
    }
}
