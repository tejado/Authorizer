/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.Utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


/**
 * Fragment for showing app 'about' information
 */
public class LicensesFragment extends Fragment
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
            extends AbstractPasswdSafeFileDataFragment.Listener
    {
        /** Update the view */
        void updateViewLicenses();
    }

    private Listener itsListener;

    private static final String TAG = "LicensesFragment";

    /**
     * Create a new instance
     */
    public static LicensesFragment newInstance()
    {
        return new LicensesFragment();
    }

    @Override
    public void onAttach(@NonNull Context ctx)
    {
        super.onAttach(ctx);
        itsListener = (Listener)ctx;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        View rootView = inflater.inflate(R.layout.fragment_app_licenses,
                                         container, false);

        ArrayList<LicenseModel> licenses = getLicenses(
                requireContext(), "license-Authorizer.txt", "license-PasswdSafe.txt",
                "license-Android.txt", "license-AndroidAssetStudio.txt","license-AndroidTreeView.txt",
                "license-BouncyCastle", "license-FastAdapter.txt", "license-Fotoapparat.txt",
                "license-FreeOTP.txt", "license-Iconics.txt", "license-MaterialIcons.txt",
                "license-RobotoMono.txt", "license-Truth.txt", "license-ZXing.txt");

        RecyclerView rvLicenses = rootView.findViewById(R.id.rv_licenses);

        LicenseAdapter licenseAdapter = new LicenseAdapter(requireContext(), licenses);

        rvLicenses.setAdapter(licenseAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        rvLicenses.setLayoutManager(linearLayoutManager);

        return rootView;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        itsListener.updateViewLicenses();
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        itsListener = null;
    }

    /**
     * Get the licenses
     */
    public ArrayList<LicenseModel> getLicenses(Context ctx, String... assets)
    {
        ArrayList<LicenseModel> licenseArray = new ArrayList<>();

        AssetManager assetMgr = ctx.getResources().getAssets();
        for (String asset: assets) {
            String name = asset.replace("license-", "").replace(".txt", "");
            String website = null;
            String license = null;
            StringBuilder licenses = new StringBuilder();
            PasswdSafeUtil.dbginfo(TAG, asset);

            try {
                InputStream is = null;
                try {
                    is = assetMgr.open(asset);
                    BufferedReader r =
                            new BufferedReader(new InputStreamReader(is));
                    String line;

                    int i = 0;
                    while ((line = r.readLine()) != null) {
                        i++;
                        if(i == 1 && line.startsWith("Website: ")) {
                            website = line.replace("Website: ", "");
                            continue;
                        }

                        if(i == 2 && line.startsWith("License: ")) {
                            license = line.replace("License: ", "");
                            continue;
                        }

                        licenses.append(line).append("\n");

                    }
                } finally {
                    Utils.closeStreams(is, null);
                }
            } catch (Exception e) {
                Log.e(TAG, "Can't load asset: " + asset, e);
            }

            licenseArray.add(new LicenseModel(name, website, license, String.valueOf(licenses)));
        }

        return licenseArray;
    }



    public class LicenseModel {
        private String product;
        private String website;
        private String license;
        private String licenseText;

        public LicenseModel(String product, String website, String license, String licenseText) {
            this.product = product;
            this.website = website;
            this.license = license;
            this.licenseText = licenseText;
        }
    }

    public class LicenseAdapter extends RecyclerView.Adapter<LicensesFragment.ViewHolder> {

        private final Context context;
        private final ArrayList<LicenseModel> licenses;

        public LicenseAdapter(Context context, ArrayList<LicenseModel> licenses) {
            this.context = context;
            this.licenses = licenses;
        }

        @NonNull
        @Override
        public LicensesFragment.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.cardview_license, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LicensesFragment.ViewHolder holder, int position) {
            LicenseModel model = licenses.get(position);
            holder.product.setText(model.product);
            holder.website.setMovementMethod(LinkMovementMethod.getInstance());
            holder.website.setText(Html.fromHtml(String.format("<a href='%s'>Website</a>", model.website)));
            holder.license.setText(model.license);
            holder.licenseText.setText(model.licenseText);
        }

        @Override
        public int getItemCount() {
            return licenses.size();
        }

    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView btn;
        private final TextView product;
        private final TextView website;
        private final TextView license;
        private final TextView licenseText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            product = itemView.findViewById(R.id.product);
            website = itemView.findViewById(R.id.website);
            license = itemView.findViewById(R.id.license);
            licenseText = itemView.findViewById(R.id.licenseText);

            btn = itemView.findViewById(R.id.tv_license_collapse);
            btn.setPaintFlags(btn.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            btn.setOnClickListener(item -> {
                if(licenseText.getVisibility() == View.GONE) {
                    licenseText.setVisibility(View.VISIBLE);
                    btn.setText(R.string.hide_license);
                } else {
                    licenseText.setVisibility(View.GONE);
                    btn.setText(R.string.show_license);
                }
            });
        }
    }

}
