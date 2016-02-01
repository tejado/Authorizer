package com.box.androidsdk.content.auth;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.box.androidsdk.content.models.BoxCollaboration;
import com.box.androidsdk.content.utils.BoxLogUtils;
import com.box.androidsdk.content.utils.SdkUtils;
import com.box.sdk.android.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This adapter is designed to show AuthenticationInfos to allow a user to pick a previously stored authentication to use.
 */
public class AuthenticatedAccountsAdapter extends ArrayAdapter<BoxAuthentication.BoxAuthenticationInfo> {

    private static final int[] THUMB_COLORS = new int[] { 0xff9e9e9e, 0xff63d6e4, 0xffff5f5f, 0xff7ed54a, 0xffaf21f4,
            0xffff9e57, 0xffe54343, 0xff5dc8a7, 0xfff271a4, 0xff2e71b6, 0xffe26f3c, 0xff768fba, 0xff56c156, 0xffefcf2e,
            0xff4dc6fc, 0xff501785, 0xffee6832, 0xffffb11d, 0xffde7ff1 };

    private static final int CREATE_NEW_TYPE_ID = 2;

    /**
     * Construct an instance of this class.
     *
     * @param context current context.
     * @param resource a resource id. (This is not used by this implementation).
     * @param objects list of BoxAuthenticationInfo objects to display.
     */
    public AuthenticatedAccountsAdapter(Context context, int resource, List<BoxAuthentication.BoxAuthenticationInfo> objects) {
        super(context, resource, objects);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public BoxAuthentication.BoxAuthenticationInfo getItem(int position) {
        if (position == (getCount() -1)){
            return new DifferentAuthenticationInfo();
        }
        return super.getItem(position);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == (getCount() -1)){
            return CREATE_NEW_TYPE_ID;
        }
        return super.getItemViewType(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItemViewType(position ) == CREATE_NEW_TYPE_ID){
            return  LayoutInflater.from(getContext()).inflate(R.layout.boxsdk_list_item_new_account, parent, false);
        }
        View rowView = LayoutInflater.from(getContext()).inflate(R.layout.boxsdk_list_item_account, parent, false);
        ViewHolder holder = (ViewHolder) rowView.getTag();
        if (holder == null) {
            holder = new ViewHolder();
            holder.titleView = (TextView) rowView.findViewById(R.id.box_account_title);
            holder.descriptionView = (TextView) rowView.findViewById(R.id.box_account_description);
            holder.initialsView = (TextView) rowView.findViewById(R.id.box_account_initials);
            rowView.setTag(holder);
        }
        BoxAuthentication.BoxAuthenticationInfo info = getItem(position);

        if (info != null && info.getUser() != null){
            boolean hasName = !SdkUtils.isEmptyString(info.getUser().getName());
            String title = hasName  ? info.getUser().getName() : info.getUser().getLogin();
            holder.titleView.setText(title);
            if (hasName){
                holder.descriptionView.setText(info.getUser().getLogin());
            }
            setColorsThumb(holder.initialsView, position);
        } else {
            if (info != null) {
                BoxLogUtils.e("invalid account info",info.toJson());
            }
        }


        return rowView;
    }

    @Override
    public int getCount() {
        return super.getCount() + 1;
    }

    /**
     * View holder for the account views
     */
    public static class ViewHolder {
        public TextView titleView;
        public TextView descriptionView;
        public TextView initialsView;
    }

    /**
     * Sets the the background thumb color for the account view to one of the material colors
     *
     * @param initialsView view where the thumbs will be shown
     * @param position index position of item
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void setColorsThumb(TextView initialsView, int position) {
        Drawable drawable = initialsView.getResources().getDrawable(R.drawable.boxsdk_thumb_background);
        drawable.setColorFilter(THUMB_COLORS[(position) % THUMB_COLORS.length], PorterDuff.Mode.MULTIPLY);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            initialsView.setBackground(drawable);
        } else {
            initialsView.setBackgroundDrawable(drawable);
        }
    }

    /**
     * An empty auth info object to represent the container for logging in with a different account
     */
    public static class DifferentAuthenticationInfo extends BoxAuthentication.BoxAuthenticationInfo{

    }

}
