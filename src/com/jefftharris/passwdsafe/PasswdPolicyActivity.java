/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jefftharris.passwdsafe.file.HeaderPasswdPolicies;
import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdPolicy;
import com.jefftharris.passwdsafe.view.DialogUtils;
import com.jefftharris.passwdsafe.view.PasswdPolicyEditDialog;
import com.jefftharris.passwdsafe.view.PasswdPolicyView;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Activity for managing password policies for a file
 */
public class PasswdPolicyActivity extends AbstractPasswdFileListActivity
    implements PasswdPolicyEditDialog.Editor
{
    private static final int MENU_ADD =         0;
    private static final int MENU_EDIT =        1;
    private static final int MENU_DELETE =      2;

    private static final int DIALOG_ADD =       MAX_DIALOG + 1;
    private static final int DIALOG_EDIT =      MAX_DIALOG + 2;
    private static final int DIALOG_DELETE =    MAX_DIALOG + 3;

    private static final String SAVE_SEL_POLICY = "saveSelPolicy";

    private List<PasswdPolicy> itsPolicies;
    private HeaderPasswdPolicies itsHdrPolicies;
    private DialogValidator itsDeleteValidator;
    private PasswdPolicyEditDialog itsEditDialog;
    private String itsSelPolicyName = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passwd_policy);

        if (!accessOpenFile()) {
            finish();
            return;
        }

        setTitle(PasswdSafeApp.getAppFileTitle(getUri(), this));
        // Programmatic setting for Android 1.5
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        if (savedInstanceState != null) {
            itsSelPolicyName = savedInstanceState.getString(SAVE_SEL_POLICY);
        } else {
            PasswdPolicy defPolicy =
                getPasswdSafeApp().getDefaultPasswdPolicy();
            itsSelPolicyName = defPolicy.getName();
        }

        showPolicies();
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#saveFinished(boolean)
     */
    public void saveFinished(boolean success)
    {
        setResult(PasswdSafeApp.RESULT_MODIFIED);
        showPolicies();
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        PasswdFileData fileData = getPasswdFileData();
        boolean readonlyFile =
            (fileData == null) || !fileData.isV3() || !fileData.canEdit();
        PasswdPolicy selPolicy = getSelectedPolicy();

        boolean canEdit =
            (selPolicy != null) &&
            (!readonlyFile ||
                (selPolicy.getLocation() == PasswdPolicy.Location.DEFAULT));

        boolean canDelete =
            !readonlyFile &&
            (selPolicy != null) &&
            (selPolicy.getLocation() != PasswdPolicy.Location.DEFAULT);

        MenuItem mi;
        mi = menu.findItem(MENU_ADD);
        mi.setEnabled(!readonlyFile);
        mi = menu.findItem(MENU_EDIT);
        mi.setEnabled(canEdit);
        mi = menu.findItem(MENU_DELETE);
        mi.setEnabled(canDelete);

        return super.onPrepareOptionsMenu(menu);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, MENU_ADD, 0, R.string.add_policy);
        menu.add(0, MENU_EDIT, 0, R.string.edit_policy);
        menu.add(0, MENU_DELETE, 0, R.string.delete_policy);
        return true;
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        boolean rc = true;
        switch (item.getItemId()) {
        case MENU_ADD: {
            removeDialog(DIALOG_ADD);
            showDialog(DIALOG_ADD);
            break;
        }
        case MENU_EDIT: {
            removeDialog(DIALOG_EDIT);
            showDialog(DIALOG_EDIT);
            break;
        }
        case MENU_DELETE: {
            removeDialog(DIALOG_DELETE);
            showDialog(DIALOG_DELETE);
            break;
        }
        default: {
            rc = super.onOptionsItemSelected(item);
            break;
        }
        }
        return rc;
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.view.PasswdPolicyEditDialog.Editor#onPolicyEditComplete(com.jefftharris.passwdsafe.file.PasswdPolicy, com.jefftharris.passwdsafe.file.PasswdPolicy)
     */
    public void onPolicyEditComplete(PasswdPolicy oldPolicy,
                                     PasswdPolicy newPolicy)
    {
        if (newPolicy.getLocation() == PasswdPolicy.Location.DEFAULT) {
            getPasswdSafeApp().setDefaultPasswdPolicy(newPolicy);
            showPolicies();
        } else {
            if (oldPolicy != null) {
                itsPolicies.remove(oldPolicy);
            }
            itsPolicies.add(newPolicy);
            Collections.sort(itsPolicies);
            itsSelPolicyName = newPolicy.getName();
            savePolicies();

            // TODO: if rename, need to modify all users of policy to new name
        }
    }

    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.view.PasswdPolicyEditDialog.Editor#isDuplicatePolicy(java.lang.String)
     */
    public boolean isDuplicatePolicy(String name)
    {
        return itsHdrPolicies.containsPolicy(name);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString(SAVE_SEL_POLICY, itsSelPolicyName);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog = null;
        switch (id) {
        case DIALOG_ADD: {
            itsEditDialog = new PasswdPolicyEditDialog(this);
            dialog = itsEditDialog.create(null, this);
            break;
        }
        case DIALOG_EDIT: {
            itsEditDialog = new PasswdPolicyEditDialog(this);
            dialog = itsEditDialog.create(getSelectedPolicy(), this);
            break;
        }
        case DIALOG_DELETE: {
            AbstractDialogClickListener dlgClick =
                new AbstractDialogClickListener()
            {
                @Override
                public final void onOkClicked(DialogInterface dialog)
                {
                    dialog.dismiss();
                    deletePolicy();
                }
            };

            PasswdPolicy policy = getSelectedPolicy();
            String prompt = getString(R.string.delete_policy_msg,
                                      policy.getName());
            String title = getString(R.string.delete_policy_title);
            DialogUtils.DialogData data =
                DialogUtils.createDeletePrompt(this, dlgClick, title, prompt);
            dialog = data.itsDialog;
            itsDeleteValidator = data.itsValidator;
            break;

        }
        default: {
            dialog = super.onCreateDialog(id);
            break;
        }
        }
        return dialog;
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
     */
    @Override
    protected void onPrepareDialog(int id, Dialog dialog)
    {
        switch (id) {
        case DIALOG_ADD:
        case DIALOG_EDIT: {
            itsEditDialog.reset();
            break;
        }
        case DIALOG_DELETE: {
            itsDeleteValidator.reset();
            break;
        }
        default: {
            super.onPrepareDialog(id, dialog);
            break;
        }
        }
    }


    /* (non-Javadoc)
     * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
     */
    @Override
    protected void onListItemClick(ListView l, View v, int pos, long id)
    {
        GuiUtils.invalidateOptionsMenu(this);
        itsSelPolicyName = null;
        PasswdPolicy selPolicy = getPolicy(pos);
        if (selPolicy != null) {
            itsSelPolicyName = selPolicy.getName();
        }
        showPolicy(selPolicy);
        GuiUtils.ensureListViewSelectionVisible(l, pos);
    }


    /** Show the password policies */
    private final void showPolicies()
    {
        GuiUtils.invalidateOptionsMenu(this);

        itsHdrPolicies = null;
        PasswdFileData fileData = getPasswdFileData();
        if (fileData != null) {
            itsHdrPolicies = fileData.getHdrPasswdPolicies();
        }
        itsPolicies = new ArrayList<PasswdPolicy>();
        if (itsHdrPolicies != null) {
            for (HeaderPasswdPolicies.HdrPolicy hdrPolicy:
                 itsHdrPolicies.getPolicies()) {
                itsPolicies.add(hdrPolicy.getPolicy());
            }
        }

        PasswdPolicy defPolicy = getPasswdSafeApp().getDefaultPasswdPolicy();
        itsPolicies.add(defPolicy);
        Collections.sort(itsPolicies);
        int selPos = 0;
        PasswdPolicy selPolicy = defPolicy;
        if (itsSelPolicyName != null) {
            int i = 0;
            for (PasswdPolicy policy: itsPolicies) {
                if (policy.getName().equals(itsSelPolicyName)) {
                    selPos = i;
                    selPolicy = policy;
                    break;
                }
                ++i;
            }
        }

        setListAdapter(new ArrayAdapter<PasswdPolicy>(
            this, android.R.layout.simple_list_item_single_choice,
            itsPolicies));
        getListView().setItemChecked(selPos, true);
        showPolicy(selPolicy);
        GuiUtils.ensureListViewSelectionVisible(getListView(), selPos);
    }

    /** Show the details of a policy */
    private final void showPolicy(PasswdPolicy policy)
    {
        PasswdPolicyView view =
            (PasswdPolicyView)findViewById(R.id.policy_view);
        view.showPolicy(policy);
    }


    /** Delete the currently selected policy */
    private final void deletePolicy()
    {
        PasswdPolicy policy = getSelectedPolicy();
        if (policy.getLocation() != PasswdPolicy.Location.DEFAULT) {
            itsPolicies.remove(policy);
            itsSelPolicyName = null;
            savePolicies();
        }
    }


    /** Save the policies */
    private final void savePolicies()
    {
        PasswdFileData fileData = getPasswdFileData();
        if (fileData != null) {
            List<PasswdPolicy> hdrPolicies =
                new ArrayList<PasswdPolicy>(itsPolicies.size());
            for (PasswdPolicy policy: itsPolicies) {
                if (policy.getLocation() == PasswdPolicy.Location.HEADER) {
                    hdrPolicies.add(policy);
                }
            }
            fileData.setHdrPasswdPolicies(
                hdrPolicies.isEmpty() ? null : hdrPolicies);
            getPasswdFile().save();
        }
        showPolicies();
    }

    /** Get the currently selected policy */
    private final PasswdPolicy getSelectedPolicy()
    {
        return getPolicy(getListView().getCheckedItemPosition());
    }

    /** Get a policy */
    private final PasswdPolicy getPolicy(int pos)
    {
        if ((itsPolicies != null) &&
            (pos >= 0) &&
            (pos < itsPolicies.size())) {
            return itsPolicies.get(pos);
        }
        return null;
    }
}
