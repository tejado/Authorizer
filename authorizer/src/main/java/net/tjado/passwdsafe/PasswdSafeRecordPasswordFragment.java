/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;


import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import net.tjado.passwdsafe.file.HeaderPasswdPolicies;
import net.tjado.passwdsafe.file.PasswdExpiration;
import net.tjado.passwdsafe.file.PasswdHistory;
import net.tjado.passwdsafe.file.PasswdPolicy;
import net.tjado.passwdsafe.lib.view.GuiUtils;
import net.tjado.passwdsafe.view.PasswdLocation;
import net.tjado.passwdsafe.view.PasswdPolicyView;

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
    private CheckBox itsHistoryEnabledCb;
    private TextView itsHistoryMaxSizeLabel;
    private TextView itsHistoryMaxSize;
    private ListView itsHistory;


    /**
     * Create a new instance of the fragment
     */
    public static PasswdSafeRecordPasswordFragment newInstance(
            PasswdLocation location)
    {
        PasswdSafeRecordPasswordFragment frag =
                new PasswdSafeRecordPasswordFragment();
        frag.setArguments(createArgs(location));
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
        itsHistoryEnabledCb = (CheckBox)root.findViewById(R.id.history_enabled);
        itsHistoryEnabledCb.setClickable(false);
        itsHistoryMaxSizeLabel =
                (TextView)root.findViewById(R.id.history_max_size_label);
        itsHistoryMaxSize = (TextView)root.findViewById(R.id.history_max_size);
        itsHistory = (ListView)root.findViewById(R.id.history);
        itsHistory.setEnabled(false);
        return root;
    }

    @Override
    protected void doOnCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
    }

    @Override
    protected void doRefresh(@NonNull RecordInfo info)
    {
        PasswdPolicy policy = null;
        String policyLoc = null;
        PasswdExpiration passwdExpiry = null;
        Date lastModTime = null;
        PasswdHistory history = null;
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
            history = info.itsFileData.getPasswdHistory(info.itsRec);
            break;
        }
        case ALIAS: {
            PwsRecord recForPassword = info.itsPasswdRec.getRef();
            passwdExpiry = info.itsFileData.getPasswdExpiry(recForPassword);
            lastModTime = info.itsFileData.getPasswdLastModTime(recForPassword);
            history = info.itsFileData.getPasswdHistory(recForPassword);
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

        boolean historyExists = (history != null);
        boolean historyEnabled = false;
        String historyMaxSize;
        if (historyExists) {
            historyEnabled = history.isEnabled();
            historyMaxSize = Integer.toString(history.getMaxSize());
            itsHistory.setAdapter(PasswdHistory.createAdapter(history, true,
                                                              false,
                                                              getActivity()));
        } else {
            historyMaxSize = getString(R.string.n_a);
            itsHistory.setAdapter(null);
        }
        GuiUtils.setListViewHeightBasedOnChildren(itsHistory);
        itsHistoryEnabledCb.setChecked(historyEnabled);
        itsHistoryEnabledCb.setEnabled(historyExists);
        itsHistoryMaxSize.setText(historyMaxSize);
        GuiUtils.setVisible(itsHistoryMaxSize, historyExists);
        GuiUtils.setVisible(itsHistoryMaxSizeLabel, historyExists);
    }
}
