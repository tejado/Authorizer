/**
 * Authorizer
 *
 *  Copyright 2023 by Tjado Mäcke <tjado@maecke.de>
 *  Licensed under GNU General Public License 3.0.
 *
 * @license GPL-3.0 <https://opensource.org/licenses/GPL-3.0>
 */

package net.tjado.passwdsafe;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Set;


/**
 * Fragment for showing app 'about' information
 */
public class UsernamesFragment extends Fragment
{
    /**
     * Listener interface for owning activity
     */
    public interface Listener
            extends AbstractPasswdSafeFileDataFragment.Listener
    {
        /** Update the view */
        void updateViewPrefUsernames();
    }

    private Listener itsListener;

    private SharedPreferences prefs;

    UsernameAdapter usernameAdapter;

    private static final String TAG = "UsernamesFragment";

    /**
     * Create a new instance
     */
    public static UsernamesFragment newInstance()
    {
        return new UsernamesFragment();
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
        View rootView = inflater.inflate(R.layout.fragment_usernames, container, false);

        prefs = Preferences.getSharedPrefs(getContext());

        Set<String> usernames = Preferences.getUsernames(prefs);
        RecyclerView rvUsernames = rootView.findViewById(R.id.rv_usernames);
        usernameAdapter = new UsernameAdapter(usernames);

        rvUsernames.setAdapter(usernameAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        rvUsernames.setLayoutManager(linearLayoutManager);

        EditText tvNewEntry = rootView.findViewById(R.id.new_username);
        Button btnNewEntry = rootView.findViewById(R.id.btn_new_username);
        btnNewEntry.setOnClickListener(item -> {
            String username = tvNewEntry.getText().toString();
            if (username.length() > 0 && !usernames.contains(username)) {
                usernames.add(username);
                Preferences.setUsernamesPref(usernames, prefs);
                tvNewEntry.setText("");

                if(usernames.size() == 1) {
                    Preferences.setUsernameDefaultPref(username, prefs);
                }

                usernameAdapter.notifyDataSetChanged();
            }
        });

        return rootView;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        itsListener.updateViewPrefUsernames();
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        itsListener = null;
    }


    public class UsernameAdapter extends RecyclerView.Adapter<UsernamesFragment.ViewHolder> {
        private final Set<String> usernames;

        public UsernameAdapter(Set<String> usernames) {
            this.usernames = usernames;
        }

        @NonNull
        @Override
        public UsernamesFragment.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.cardview_usernamess, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UsernamesFragment.ViewHolder holder, int position) {
            String name = (String) usernames.toArray()[position];
            holder.name.setText(name);
            holder.btnDefault.setOnCheckedChangeListener(null);
            holder.btnDefault.setChecked(Preferences.getUsernameDefault(prefs).equals(name));

            holder.btnDefault.setOnCheckedChangeListener((compoundButton, checked) -> {
                Preferences.setUsernameDefaultPref(name, prefs);
                usernameAdapter.notifyDataSetChanged();
            });

            holder.btnRemove.setOnClickListener(item -> {
                if (usernames.remove(name)) {
                    Preferences.setUsernamesPref(usernames, prefs);
                    usernameAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getItemCount() {
            return usernames.size();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final RadioButton btnDefault;
        private final ImageButton btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.name);
            btnDefault = itemView.findViewById(R.id.rb_default);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }
    }

}
