/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;


import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdRecord;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.lib.Utils;
import com.jefftharris.passwdsafe.lib.view.GuiUtils;
import com.jefftharris.passwdsafe.view.PasswdLocation;

import org.pwsafe.lib.file.PwsRecord;

import java.util.Date;


/**
 * Fragment for showing basic fields of a password record
 */
public class PasswdSafeRecordBasicFragment extends Fragment
        implements View.OnClickListener
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
    {
        /** Get the file data */
        PasswdFileData getFileData();

        /** Change the location in the password file */
        void changeLocation(PasswdLocation location);
    }

    private String itsRecUuid;
    private boolean itsIsPasswordShown = false;
    private String itsHiddenPasswordStr;
    private Listener itsListener;
    private View itsBaseRow;
    private TextView itsBaseLabel;
    private TextView itsBase;
    private View itsGroupRow;
    private TextView itsGroup;
    private View itsUserRow;
    private TextView itsUser;
    private View itsPasswordRow;
    private TextView itsPassword;
    private SeekBar itsPasswordSeek;
    private View itsUrlRow;
    private TextView itsUrl;
    private View itsEmailRow;
    private TextView itsEmail;
    private View itsTimesRow;
    private View itsCreationTimeRow;
    private TextView itsCreationTime;
    private View itsLastModTimeRow;
    private TextView itsLastModTime;

    public static PasswdSafeRecordBasicFragment newInstance(String recUuid)
    {
        Bundle args = new Bundle();
        args.putString("recUuid", recUuid);
        PasswdSafeRecordBasicFragment frag = new PasswdSafeRecordBasicFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            itsRecUuid = args.getString("recUuid");
        }
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        itsListener = (Listener)activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        View root = inflater.inflate(R.layout.fragment_passwdsafe_record_basic,
                                     container, false);
        itsBaseRow = root.findViewById(R.id.base_row);
        itsBaseRow.setOnClickListener(this);
        itsBaseLabel = (TextView)root.findViewById(R.id.base_label);
        itsBase = (TextView)root.findViewById(R.id.base);
        View baseBtn = root.findViewById(R.id.base_btn);
        baseBtn.setOnClickListener(this);
        itsGroupRow = root.findViewById(R.id.group_row);
        itsGroup = (TextView)root.findViewById(R.id.group);
        itsUserRow = root.findViewById(R.id.user_row);
        itsUser = (TextView)root.findViewById(R.id.user);
        itsPasswordRow = root.findViewById(R.id.password_row);
        itsPasswordRow.setOnClickListener(this);
        itsPassword = (TextView)root.findViewById(R.id.password);
        itsPasswordSeek = (SeekBar)root.findViewById(R.id.password_seek);
        itsPasswordSeek.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener()
                {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                                                  boolean fromUser)
                    {
                        if (fromUser) {
                            updatePasswordShown(false, progress);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar)
                    {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar)
                    {
                    }
                });
        itsUrlRow = root.findViewById(R.id.url_row);
        itsUrl = (TextView)root.findViewById(R.id.url);
        itsEmailRow = root.findViewById(R.id.email_row);
        itsEmail = (TextView)root.findViewById(R.id.email);
        itsTimesRow = root.findViewById(R.id.times_row);
        itsCreationTimeRow = root.findViewById(R.id.creation_time_row);
        itsCreationTime = (TextView)root.findViewById(R.id.creation_time);
        itsLastModTimeRow = root.findViewById(R.id.last_mod_time_row);
        itsLastModTime = (TextView)root.findViewById(R.id.last_mod_time);
        return root;
    }

    @Override
    public void onResume()
    {
        super.onResume();
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
        inflater.inflate(R.menu.fragment_passwdsafe_record_basic, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        MenuItem item = menu.findItem(R.id.menu_toggle_password);
        item.setTitle(itsIsPasswordShown ?
                              R.string.hide_password : R.string.show_password);
        item.setEnabled(itsPasswordRow.getVisibility() == View.VISIBLE);

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_copy_user: {
            PasswdSafeUtil.copyToClipboard(itsUser.getText().toString(),
                                           getActivity());
            return true;
        }
        case R.id.menu_toggle_password: {
            updatePasswordShown(true, 0);
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId()) {
        case R.id.base_row:
        case R.id.base_btn: {
            showRefRec(true, 0);
            break;
        }
        case R.id.password_row: {
            updatePasswordShown(true, 0);
            break;
        }
        }
    }

    /**
     * Refresh the view
     */
    private void refresh()
    {
        if (!isAdded() || (itsListener == null)) {
            return;
        }

        PasswdFileData fileData = itsListener.getFileData();
        if (fileData == null) {
            return;
        }

        PwsRecord rec = fileData.getRecord(itsRecUuid);
        if (rec == null) {
            return;
        }

        PasswdRecord passwdRec = fileData.getPasswdRecord(rec);

        PwsRecord ref = passwdRec.getRef();
        PwsRecord recForPassword = rec;
        int hiddenId = R.string.hidden_password_normal;
        String url = null;
        String email = null;
        Date creationTime = null;
        Date lastModTime = null;
        switch (passwdRec.getType()) {
        case NORMAL: {
            itsBaseRow.setVisibility(View.GONE);
            url = fileData.getURL(rec);
            email = fileData.getEmail(rec);
            creationTime = fileData.getCreationTime(rec);
            lastModTime = fileData.getLastModTime(rec);
            break;
        }
        case ALIAS: {
            itsBaseRow.setVisibility(View.VISIBLE);
            itsBaseLabel.setText(R.string.alias_base_record_header);
            itsBase.setText(fileData.getId(ref));
            hiddenId = R.string.hidden_password_alias;
            recForPassword = ref;
            url = fileData.getURL(rec);
            email = fileData.getEmail(rec);
            creationTime = fileData.getCreationTime(recForPassword);
            lastModTime = fileData.getLastModTime(recForPassword);
            break;
        }
        case SHORTCUT: {
            itsBaseRow.setVisibility(View.VISIBLE);
            itsBaseLabel.setText(R.string.shortcut_base_record_header);
            itsBase.setText(fileData.getId(ref));
            hiddenId = R.string.hidden_password_shortcut;
            recForPassword = ref;
            creationTime = fileData.getCreationTime(recForPassword);
            lastModTime = fileData.getLastModTime(recForPassword);
            break;
        }
        }

        setFieldText(itsGroup, itsGroupRow, fileData.getGroup(rec));
        setFieldText(itsUser, itsUserRow, fileData.getUsername(rec));

        itsIsPasswordShown = false;
        itsHiddenPasswordStr = getString(hiddenId);
        String password = fileData.getPassword(recForPassword);
        setFieldText(itsPassword, itsPasswordRow,
                     ((password != null) ? itsHiddenPasswordStr : null));
        itsPasswordSeek.setMax((password != null) ? password.length() : 0);
        itsPasswordSeek.setProgress(0);

        setFieldText(itsUrl, itsUrlRow, url);
        setFieldText(itsEmail, itsEmailRow, email);

        GuiUtils.setVisible(itsTimesRow,
                            (creationTime != null) || (lastModTime != null));
        setFieldDate(itsCreationTime, itsCreationTimeRow, creationTime);
        setFieldDate(itsLastModTime, itsLastModTimeRow, lastModTime);

        GuiUtils.invalidateOptionsMenu(getActivity());
    }

    /**
     * Show a referenced record
     */
    private void showRefRec(boolean baseRef, int referencingPos)
    {
        PasswdFileData fileData = itsListener.getFileData();
        if (fileData == null) {
            return;
        }

        PwsRecord rec = fileData.getRecord(itsRecUuid);
        if (rec == null) {
            return;
        }

        PasswdRecord passwdRec = fileData.getPasswdRecord(rec);
        PwsRecord refRec = null;
        if (baseRef) {
            refRec = passwdRec.getRef();
        }
        if (refRec == null) {
            return;
        }

        PasswdLocation location = new PasswdLocation(refRec, fileData);
        itsListener.changeLocation(location);
    }

    /**
     * Update whether the password is shown
     */
    private void updatePasswordShown(boolean isToggle, int progress)
    {
        String password;
        if (isToggle) {
            itsIsPasswordShown = !itsIsPasswordShown;
            password = itsIsPasswordShown ? getPassword() : itsHiddenPasswordStr;
            itsPasswordSeek.setProgress(
                    itsIsPasswordShown ? itsPasswordSeek.getMax() : 0);
        } else if (progress == 0) {
            itsIsPasswordShown = false;
            password = itsHiddenPasswordStr;
        } else {
            itsIsPasswordShown = true;
            password = getPassword();
            if ((password != null) && (progress < password.length())) {
                password = password.substring(0, progress) + "…";
            }
        }
        itsPassword.setText(password);
        itsPassword.setTypeface(
                itsIsPasswordShown ? Typeface.MONOSPACE : Typeface.DEFAULT);
        GuiUtils.invalidateOptionsMenu(getActivity());
    }

    /**
     * Get the password
     */
    private String getPassword()
    {
        if (!isAdded() || (itsListener == null)) {
            return null;
        }

        PasswdFileData fileData = itsListener.getFileData();
        if (fileData == null) {
            return null;
        }

        // TODO: save off record and filedata?
        PwsRecord rec = fileData.getRecord(itsRecUuid);
        if (rec == null) {
            return null;
        }

        PasswdRecord passwdRec = fileData.getPasswdRecord(rec);
        switch (passwdRec.getType()) {
        case NORMAL: {
            return fileData.getPassword(rec);
        }
        case ALIAS:
        case SHORTCUT: {
            return fileData.getPassword(passwdRec.getRef());
        }
        }

        return null;
    }

    /**
     * Set the value of a text field.  The field's row is visible if the text
     * isn't null.
     */
    private void setFieldText(TextView field, View fieldRow, String text)
    {
        field.setText(text);

        if (fieldRow != null) {
            GuiUtils.setVisible(fieldRow, (text != null));
        }
    }

    /**
     * Set the value of a date field.  The field's row is visible if the date
     * isn't null
     */
    private void setFieldDate(TextView field, View fieldRow, Date date)
    {
        String str =
                (date != null) ? Utils.formatDate(date, getActivity()) : null;
        setFieldText(field, fieldRow, str);
    }
}
