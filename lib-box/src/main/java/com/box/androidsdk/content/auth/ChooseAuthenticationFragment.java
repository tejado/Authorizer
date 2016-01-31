package com.box.androidsdk.content.auth;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.box.sdk.android.R;

import java.util.ArrayList;
import java.util.Map;

/**
 * This class will show a scrollable list of accounts a user can choose to log in to, and at the end
 * an option to login as a new account. Activities using this fragment should implement OnAuthenticationChosen.
 */
public class ChooseAuthenticationFragment extends Fragment {

    private ListView mListView;
    private static final String EXTRA_BOX_AUTHENTICATION_INFOS = "boxAuthenticationInfos";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ArrayList<BoxAuthentication.BoxAuthenticationInfo> infos = getAuthenticationInfoList();
        View view = inflater.inflate(R.layout.boxsdk_choose_auth_activity, null);
        mListView = (ListView)view.findViewById(R.id.boxsdk_accounts_list);
        if (infos == null){
            getActivity().getFragmentManager().beginTransaction().remove(this).commit();
        } else {
            mListView.setAdapter(new AuthenticatedAccountsAdapter(getActivity(), R.layout.boxsdk_list_item_account, infos));
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (parent.getAdapter() instanceof AuthenticatedAccountsAdapter){
                        AuthenticatedAccountsAdapter accountsAdapter = (AuthenticatedAccountsAdapter)parent.getAdapter();
                        BoxAuthentication.BoxAuthenticationInfo info = accountsAdapter.getItem(position);
                        if (info instanceof AuthenticatedAccountsAdapter.DifferentAuthenticationInfo){
                            if (getActivity() instanceof OnAuthenticationChosen){
                                ((OnAuthenticationChosen) getActivity()).onDifferentAuthenticationChosen();
                            }

                        }
                        else {
                            if (getActivity() instanceof OnAuthenticationChosen){
                                ((OnAuthenticationChosen) getActivity()).onAuthenticationChosen(info);
                            }
                        }

                    }
                }
            });
        }
        return view;
    }

    /**
     *
     * @return a list of authentication info objects to display in the fragment.
     */
    public ArrayList<BoxAuthentication.BoxAuthenticationInfo> getAuthenticationInfoList(){
        if (getArguments() != null && getArguments().getCharSequenceArrayList(EXTRA_BOX_AUTHENTICATION_INFOS) != null){
            ArrayList<CharSequence> jsonSerialized = getArguments().getCharSequenceArrayList(EXTRA_BOX_AUTHENTICATION_INFOS);
            ArrayList<BoxAuthentication.BoxAuthenticationInfo> list = new ArrayList<BoxAuthentication.BoxAuthenticationInfo>(jsonSerialized.size());
            for (CharSequence sequence : jsonSerialized){
                BoxAuthentication.BoxAuthenticationInfo info = new BoxAuthentication.BoxAuthenticationInfo();
                info.createFromJson(sequence.toString());
                list.add(info);
            }
            return list;
        }
        Map<String, BoxAuthentication.BoxAuthenticationInfo> map = BoxAuthentication.getInstance().getStoredAuthInfo(getActivity());
        if (map != null){
            ArrayList<BoxAuthentication.BoxAuthenticationInfo> list = new ArrayList<BoxAuthentication.BoxAuthenticationInfo>(map.size());
            for (String key : map.keySet()){
                list.add(map.get(key));
            }
            return list;
        }
        return null;
    }

    /**
     *Create an instance of this fragment to display any BoxAuthenticationInfos retrieved via BoxAuthentication.getInstance().getStoredAuthInfo().
     * @param context current context
     * @return an instance of this fragment displaying auth infos via BoxAuthentication.getInstance().getStoredAuthInfo().
     */
    public static ChooseAuthenticationFragment createAuthenticationActivity(final Context context){
        ChooseAuthenticationFragment fragment = new ChooseAuthenticationFragment();
        return fragment;

    }

    /**
     * Create an instance of this fragment to display the given list of BoxAuthenticationInfos.
     * @param context current context
     * @param listOfAuthInfo a list of auth infos in the order to display to the user.
     * @return a fragment displaying list of authinfos provided above.
     */
    public static ChooseAuthenticationFragment createChooseAuthenticationFragment(final Context context, final ArrayList<BoxAuthentication.BoxAuthenticationInfo> listOfAuthInfo){
        ChooseAuthenticationFragment fragment = createAuthenticationActivity(context);
        Bundle b = fragment.getArguments();
        if (b == null){
            b = new Bundle();
        }

        ArrayList<CharSequence> jsonSerialized = new ArrayList<CharSequence>(listOfAuthInfo.size());
        for (BoxAuthentication.BoxAuthenticationInfo info : listOfAuthInfo){
            jsonSerialized.add(info.toJson());
        }
        b.putCharSequenceArrayList(EXTRA_BOX_AUTHENTICATION_INFOS, jsonSerialized);

        fragment.setArguments(b);
        return fragment;

    }


    /**
     * Interface this fragment uses to communicate to its parent activity selection of an auth info or if a "different account" was chosen.
     */
    public interface OnAuthenticationChosen {

        /**
         *
         * @param authInfo the auth info chosen by the user, returns a new empty instance of DifferentAuthenticationInfo if the user chose to use a different account.
         */
        public void onAuthenticationChosen(BoxAuthentication.BoxAuthenticationInfo authInfo);


        /**
         * Called if a user chooses to login as a "different account". If this is called the instantiater of this fragment should show appropriate ui to allow a new user to login.
         */
        public void onDifferentAuthenticationChosen();
    }

}
