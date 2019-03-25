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
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.tjado.passwdsafe.file.PasswdFileData;
import net.tjado.passwdsafe.file.PasswdFileDataUser;
import net.tjado.passwdsafe.lib.AboutUtils;
import net.tjado.passwdsafe.lib.view.GuiUtils;
import net.tjado.passwdsafe.lib.ObjectHolder;

import java.util.Locale;

/**
 * Fragment for showing app 'about' information
 */
public class AboutFragment extends Fragment
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
            extends AbstractPasswdSafeFileDataFragment.Listener
    {
        /**
         * Update the view for the about fragment
         */
        void updateViewAbout();
    }

    private Listener itsListener;
    private View itsFileDetailsGroup;
    private TextView itsFile;
    private TextView itsPermissions;
    private TextView itsNumRecords;
    private TextView itsPasswordEnc;
    private TextView itsDatabaseVer;
    private TextView itsLastSaveBy;
    private TextView itsLastSaveApp;
    private TextView itsLastSaveTime;

    /**
     * Create a new instance
     */
    public static AboutFragment newInstance()
    {
        return new AboutFragment();
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
        View rootView = inflater.inflate(R.layout.fragment_about,
                                         container, false);

        String licenses = AboutUtils.getLicenses(
                getContext(), "license-PasswdSafe.txt",
                "license-android.txt", "license-AndroidAssetStudio.txt",
                "license-RobotoMono.txt");

        AboutUtils.updateAboutFields(rootView, licenses, getContext());
        itsFileDetailsGroup = rootView.findViewById(R.id.file_details_group);
        itsFile = (TextView)rootView.findViewById(R.id.file);
        itsPermissions = (TextView)rootView.findViewById(R.id.permissions);
        itsNumRecords = (TextView)rootView.findViewById(R.id.num_records);
        itsPasswordEnc = (TextView)
                rootView.findViewById(R.id.password_encoding);
        itsDatabaseVer = (TextView)rootView.findViewById(R.id.database_version);
        itsLastSaveBy = (TextView)rootView.findViewById(R.id.last_save_by);
        itsLastSaveApp = (TextView)rootView.findViewById(R.id.last_save_app);
        itsLastSaveTime = (TextView)rootView.findViewById(R.id.last_save_time);
        return rootView;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        itsListener.updateViewAbout();

        final ObjectHolder<Boolean> called = new ObjectHolder<>(false);
        itsListener.useFileData(new PasswdFileDataUser()
        {
            @Override
            public void useFileData(@NonNull PasswdFileData fileData)
            {
                called.set(true);
                itsFile.setText(fileData.getUri().toString());
                itsPermissions.setText(
                        fileData.canEdit() ?
                        R.string.read_write : R.string.read_only_about);
                itsNumRecords.setText(String.format(
                        Locale.getDefault(), "%d",
                        fileData.getRecords().size()));
                itsPasswordEnc.setText(fileData.getOpenPasswordEncoding());
                if (fileData.isV3()) {
                    StringBuilder build = new StringBuilder();
                    String str = fileData.getHdrLastSaveUser();
                    if (!TextUtils.isEmpty(str)) {
                        build.append(str);
                    }
                    str = fileData.getHdrLastSaveHost();
                    if (!TextUtils.isEmpty(str)) {
                        if (build.length() > 0) {
                            build.append(" on ");
                        }
                        build.append(str);
                    }

                    itsDatabaseVer.setText(fileData.getHdrVersion());
                    itsLastSaveBy.setText(build);
                    itsLastSaveApp.setText(fileData.getHdrLastSaveApp());
                    itsLastSaveTime.setText(fileData.getHdrLastSaveTime());
                } else {
                    itsDatabaseVer.setText(null);
                    itsLastSaveBy.setText(null);
                    itsLastSaveApp.setText(null);
                    itsLastSaveTime.setText(null);
                }
            }
        });
        GuiUtils.setVisible(itsFileDetailsGroup, called.get());
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        itsListener = null;
    }

}
