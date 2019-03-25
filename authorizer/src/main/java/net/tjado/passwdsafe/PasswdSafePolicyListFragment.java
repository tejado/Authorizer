/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;


import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.tjado.passwdsafe.file.HeaderPasswdPolicies;
import net.tjado.passwdsafe.file.PasswdFileData;
import net.tjado.passwdsafe.file.PasswdFileDataUser;
import net.tjado.passwdsafe.file.PasswdPolicy;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.view.GuiUtils;
import net.tjado.passwdsafe.lib.ObjectHolder;
import net.tjado.passwdsafe.util.Pair;
import net.tjado.passwdsafe.view.ConfirmPromptDialog;
import net.tjado.passwdsafe.view.PasswdPolicyEditDialog;
import net.tjado.passwdsafe.view.PasswdPolicyView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * Fragment showing a list of password policies
 */
public class PasswdSafePolicyListFragment extends ListFragment
        implements PasswdPolicyEditDialog.Listener,
                   ConfirmPromptDialog.Listener
{
    /** Listener interface for owning activity */
    public interface Listener
            extends AbstractPasswdSafeFileDataFragment.Listener
    {
        /** Update the view for a list of policies */
        void updateViewPolicyList();

        /** Finish editing the policies */
        void finishPolicyEdit(Runnable postSaveRun);
    }

    private static final String CONFIRM_ARG_POLICY = "policy";

    private static final String TAG = "PasswdSafePolicyListFragment";

    private Listener itsListener;
    private HeaderPasswdPolicies itsHdrPolicies;
    private boolean itsIsFileReadonly = true;

    /**
     * Create a new instance
     */
    public static PasswdSafePolicyListFragment newInstance()
    {
        return new PasswdSafePolicyListFragment();
    }

    @Override
    public void onAttach(Context ctx)
    {
        super.onAttach(ctx);
        itsListener = (Listener)ctx;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_passwdsafe_policy_list,
                                container, false);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        itsListener.updateViewPolicyList();
        refresh();
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
            inflater.inflate(R.menu.fragment_passwdsafe_policy_list, menu);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);

        MenuItem item = menu.findItem(R.id.menu_add_policy);
        if (item != null) {
            item.setVisible(!itsIsFileReadonly);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_add_policy: {
            editPolicy(null);
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int pos, long id)
    {
        l.invalidateViews();
    }

    @Override
    public void handlePolicyEditComplete(PasswdPolicy oldPolicy,
                                         PasswdPolicy newPolicy)
    {
        if (newPolicy.getLocation() == PasswdPolicy.Location.DEFAULT) {
            PasswdSafeApp app =
                    (PasswdSafeApp)getContext().getApplicationContext();
            app.setDefaultPasswdPolicy(newPolicy);
            refresh();
        } else {
            String oldName = (oldPolicy != null) ? oldPolicy.getName() : null;
            savePolicies(oldName, newPolicy);
        }
    }

    @Override
    public boolean isDuplicatePolicy(String name)
    {
        return (itsHdrPolicies != null) && itsHdrPolicies.containsPolicy(name);
    }

    @Override
    public void promptConfirmed(Bundle confirmArgs)
    {
        PasswdPolicy policy = confirmArgs.getParcelable(CONFIRM_ARG_POLICY);
        if (policy == null) {
            return;
        }

        savePolicies(policy.getName(), null);
    }

    @Override
    public void promptCanceled()
    {
    }

    /**
     * Delete a policy
     */
    private void deletePolicy(PasswdPolicy policy)
    {
        PasswdSafeUtil.dbginfo(TAG, "Delete policy: %s", policy);
        Bundle confirmArgs = new Bundle();
        confirmArgs.putParcelable(CONFIRM_ARG_POLICY, policy);
        ConfirmPromptDialog dialog = ConfirmPromptDialog.newInstance(
                getString(R.string.delete_policy_msg, policy.getName()), null,
                getString(R.string.delete), confirmArgs);
        dialog.setTargetFragment(this, 0);
        dialog.show(getFragmentManager(), "Delete policy");
    }

    /**
     * Edit or create a policy
     */
    private void editPolicy(PasswdPolicy policy)
    {
        PasswdSafeUtil.dbginfo(TAG, "Edit policy: %s", policy);
        PasswdPolicyEditDialog dlg = PasswdPolicyEditDialog.newInstance(policy);
        dlg.setTargetFragment(this, 0);
        dlg.show(getFragmentManager(), "PasswdPolicyEditDialog");
    }

    /**
     * Get the use count for a policy (-1 if not a header policy)
     */
    private int getPolicyUseCount(PasswdPolicy policy)
    {
        int useCount = -1;
        if ((policy != null) &&
            (itsHdrPolicies != null) &&
            (policy.getLocation() == PasswdPolicy.Location.HEADER)) {
            useCount = itsHdrPolicies.getPolicyUseCount(policy.getName());
        }
        return useCount;
    }

    /**
     * Save the policies
     */
    private void savePolicies(String rmPolicy, PasswdPolicy addPolicy)
    {
        final List<PasswdPolicy> newPolicies = new ArrayList<>();
        if (itsHdrPolicies != null) {
            Collection<HeaderPasswdPolicies.HdrPolicy> hdrPolicies =
                    itsHdrPolicies.getPolicies();
            for (HeaderPasswdPolicies.HdrPolicy hdrPolicy : hdrPolicies) {
                if (!TextUtils.equals(hdrPolicy.getPolicy().getName(),
                                      rmPolicy)) {
                    newPolicies.add(hdrPolicy.getPolicy());
                }
            }
        }

        final ObjectHolder<Pair<String, String>> policyRename =
                new ObjectHolder<>();
        if (addPolicy != null) {
            newPolicies.add(addPolicy);

            if (rmPolicy != null) {
                String newName = addPolicy.getName();
                if (!TextUtils.equals(newName, rmPolicy)) {
                    policyRename.set(new Pair<>(rmPolicy, newName));
                }
            }
        }

        PasswdSafeUtil.dbginfo(TAG, "savePolicies: %s, rename: %s",
                               newPolicies, policyRename);

        itsListener.useFileData(new PasswdFileDataUser()
        {
            @Override
            public void useFileData(@NonNull PasswdFileData fileData)
            {
                fileData.setHdrPasswdPolicies(
                        newPolicies.isEmpty() ? null : newPolicies,
                        policyRename.get());
            }
        });
        itsListener.finishPolicyEdit(new Runnable()
        {
            @Override
            public void run()
            {
                refresh();
            }
        });
    }

    /**
     * Refresh the list of policies
     */
    private void refresh()
    {
        ArrayList<PasswdPolicy> policies = new ArrayList<>();

        itsHdrPolicies = null;
        itsIsFileReadonly = true;
        itsListener.useFileData(new PasswdFileDataUser()
        {
            @Override
            public void useFileData(@NonNull PasswdFileData fileData)
            {
                itsHdrPolicies = fileData.getHdrPasswdPolicies();
                itsIsFileReadonly = !fileData.isV3() || !fileData.canEdit();
            }
        });

        if (itsHdrPolicies != null) {
            for (HeaderPasswdPolicies.HdrPolicy hdrPolicy:
                    itsHdrPolicies .getPolicies()) {
                policies.add(hdrPolicy.getPolicy());
            }
        }

        PasswdSafeApp app =
                (PasswdSafeApp)getContext().getApplicationContext();
        PasswdPolicy defPolicy = app.getDefaultPasswdPolicy();
        policies.add(defPolicy);

        Collections.sort(policies);

        PolicyListAdapter.PolicyItemUser listener =
                new PolicyListAdapter.PolicyItemUser()
                {
                    @Override
                    public int getPolicyUseCount(PasswdPolicy policy)
                    {
                        return PasswdSafePolicyListFragment.this
                                .getPolicyUseCount(policy);
                    }

                    @Override
                    public void editPolicy(PasswdPolicy policy)
                    {
                        PasswdSafePolicyListFragment.this.editPolicy(policy);
                    }

                    @Override
                    public void deletePolicy(PasswdPolicy policy)
                    {
                        PasswdSafePolicyListFragment.this.deletePolicy(policy);
                    }
                };

        PolicyListAdapter adapter = new PolicyListAdapter(
                policies, itsIsFileReadonly, listener, getContext());
        setListAdapter(adapter);
        GuiUtils.invalidateOptionsMenu(getActivity());
    }

    /**
     * List adapter for items
     */
    private static class PolicyListAdapter extends ArrayAdapter<PasswdPolicy>
    {
        /**
         * User of the policy adapter
         */
        public interface PolicyItemUser
        {
            int getPolicyUseCount(PasswdPolicy policy);

            void editPolicy(PasswdPolicy policy);

            void deletePolicy(PasswdPolicy policy);
        }

        private final LayoutInflater itsInflater;
        private final boolean itsIsFileReadonly;
        private final PolicyItemUser itsListener;

        /**
         * Constructor
         */
        public PolicyListAdapter(List<PasswdPolicy> policies,
                                 boolean fileReadonly,
                                 PolicyItemUser policyListener,
                                 Context ctx)
        {
            super(ctx, R.layout.passwd_policy_list_item, policies);
            itsInflater = LayoutInflater.from(ctx);
            itsIsFileReadonly = fileReadonly;
            itsListener = policyListener;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            ViewHolder itemViews;
            if (convertView == null) {
                convertView = itsInflater.inflate(
                        R.layout.passwd_policy_list_item, parent, false);
                itemViews = new ViewHolder(convertView);
                convertView.setTag(itemViews);
            } else {
                itemViews = (ViewHolder)convertView.getTag();
            }

            ListView lv = (ListView)parent;

            PasswdPolicy entry = getItem(position);
            itemViews.update(entry, lv.isItemChecked(position));
            return convertView;
        }

        /**
         * View holder class for fields in each entry's layout
         */
        private class ViewHolder
                implements View.OnClickListener, View.OnLongClickListener
        {
            private final TextView itsTitle;
            private final View itsEditBtn;
            private final View itsDeleteBtn;
            private final View itsPolicyCard;
            private final PasswdPolicyView itsPolicyView;
            private PasswdPolicy itsPolicy;

            /**
             * Constructor
             */
            public ViewHolder(View view)
            {
                itsTitle = (TextView)view.findViewById(R.id.title);
                itsEditBtn = view.findViewById(R.id.edit);
                itsEditBtn.setOnClickListener(this);
                itsEditBtn.setOnLongClickListener(this);
                itsDeleteBtn = view.findViewById(R.id.delete);
                itsDeleteBtn.setOnClickListener(this);
                itsDeleteBtn.setOnLongClickListener(this);
                itsPolicyCard = view.findViewById(R.id.policy_card);
                itsPolicyView =
                        (PasswdPolicyView)view.findViewById(R.id.policy_view);
            }

            /**
             * Update the layout fields with values from the policy
             */
            public void update(PasswdPolicy policy, boolean checked)
            {
                itsPolicy = policy;
                int useCount = itsListener.getPolicyUseCount(policy);
                itsTitle.setText(itsPolicy.toString());

                boolean canEdit = checked && !itsIsFileReadonly;
                GuiUtils.setVisible(itsEditBtn, checked);
                itsEditBtn.setEnabled(canEdit);

                boolean canDelete =
                        checked && !itsIsFileReadonly &&
                        (useCount == 0) &&
                        (policy.getLocation() == PasswdPolicy.Location.HEADER);
                GuiUtils.setVisible(itsDeleteBtn, checked);
                itsDeleteBtn.setEnabled(canDelete);

                GuiUtils.setVisible(itsPolicyCard, checked);
                if (checked) {
                    itsPolicyView.showPolicy(itsPolicy, useCount);
                }
            }

            @Override
            public void onClick(View v)
            {
                switch (v.getId()) {
                case R.id.edit: {
                    itsListener.editPolicy(itsPolicy);
                    break;
                }
                case R.id.delete: {
                    itsListener.deletePolicy(itsPolicy);
                    break;
                }
                }
            }

            @Override
            public boolean onLongClick(View v)
            {
                switch (v.getId()) {
                case R.id.edit: {
                    Toast.makeText(getContext(), R.string.edit_policy,
                                   Toast.LENGTH_SHORT).show();
                    return true;
                }
                case R.id.delete: {
                    Toast.makeText(getContext(), R.string.delete_policy,
                                   Toast.LENGTH_SHORT).show();
                    return true;
                }
                }
                return false;
            }
        }
    }
}
