/**
 * Authorizer
 *
 *  Copyright 2016 by Tjado Mäcke <tjado@maecke.de>
 *  Licensed under GNU General Public License 3.0.
 *
 * @license GPL-3.0 <https://opensource.org/licenses/GPL-3.0>
 */

package net.tjado.passwdsafe.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.RelativeLayout.LayoutParams;

import net.tjado.passwdsafe.Preferences;
import net.tjado.passwdsafe.R;

import com.mikepenz.iconics.view.IconicsTextView;
import com.unnamed.b.atv.model.TreeNode;


public class PasswdRecordListItemHolder
        extends TreeNode.BaseNodeViewHolder<PasswdRecordListItemHolder.IconTreeItem> {

    private IconicsTextView arrowView;
    private View.OnLongClickListener iconOnClickListener;

    public static class IconTreeItem {
        public int level;
        public int group_count;
        public String icon;
        public String text;
        public String uuid;
        public PasswdLocation location;

        public IconTreeItem(int level, String icon, String text, String uuid, PasswdLocation location) {
            this.level = level;
            this.icon = icon;
            this.text = text;
            this.uuid = uuid;
            this.location = location;
        }

        public void setGroupCount(int group_count) {
            this.group_count = group_count;
        }
    }

    public PasswdRecordListItemHolder(Context context) {
        super(context);
    }

    @Override
    public View createNodeView(final TreeNode node, IconTreeItem itemValues) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.passwdsafe_list_tree_item, null, false);

        // indent the entries in the layout in regard to their level
        final float scale = context.getResources().getDisplayMetrics().density;
        final int spaceWidth = (int) (itemValues.level * 26.0f * scale + 0.5f);
        final TextView tvSpacer = (TextView) view.findViewById(R.id.spacer);
        LayoutParams params = new LayoutParams(spaceWidth, LayoutParams.WRAP_CONTENT);
        tvSpacer.setLayoutParams(params);

        // set the item text
        final TextView tvItemName = (TextView) view.findViewById(R.id.item_name);
        tvItemName.setText(itemValues.text);

        final IconicsTextView iconItem = (IconicsTextView) view.findViewById(R.id.icon);
        final IconicsTextView iconUsbkbdOutput = (IconicsTextView) view.findViewById(R.id.icon_usbkbd_output);

        arrowView = (IconicsTextView) view.findViewById(R.id.arrow_icon);

        if (node.isLeaf()) {
            // do stuff here if this is a record...

            if( itemValues.icon != null ) {
                iconItem.setText("{" + itemValues.icon + "}");
            } else {
                iconItem.setText("{gmi_key}");
            }

        } else {
            // do stuff here if this is a group...

            iconItem.setText(context.getResources().getString(R.string.ic_folder));

            arrowView.setVisibility(View.VISIBLE);
            iconUsbkbdOutput.setVisibility(View.GONE);

            iconItem.setTextSize(28);

            // show the amount of items in this group
            String group_count_text = String.valueOf(itemValues.group_count);
            if( itemValues.group_count == 1) {
                group_count_text = group_count_text + " " + context.getResources().getString(R.string.entry);
            } else {
                group_count_text = group_count_text + " " + context.getResources().getString(R.string.entries);
            }
            final TextView tvGroupCount = (TextView) view.findViewById(R.id.group_count);
            tvGroupCount.setVisibility(View.VISIBLE);
            tvGroupCount.setText(group_count_text);
        }

        // hide the USB Keyboard Output button if the functionality is disabled
        SharedPreferences prefs = Preferences.getSharedPrefs(context);
        if ( ! Preferences.getAutoTypeUsbEnabled(prefs) ) {
            iconUsbkbdOutput.setVisibility(View.GONE);
        }

        iconUsbkbdOutput.setOnLongClickListener(iconOnClickListener);

        return view;
    }

    @Override
    public void toggle(boolean active) {
        arrowView.setText(context.getResources().getString(active ? R.string.ic_arrow_down : R.string.ic_arrow_right));
    }

    public void setIcon2ViewOnClickListener(View.OnLongClickListener onClickListener) {
        iconOnClickListener = onClickListener;
    }

}
