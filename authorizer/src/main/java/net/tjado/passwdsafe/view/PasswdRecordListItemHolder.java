/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.github.johnkil.print.PrintView;
import com.unnamed.b.atv.model.TreeNode;

public class PasswdRecordListItemHolder extends TreeNode.BaseNodeViewHolder<PasswdRecordListItemHolder.IconTreeItem> {
    private TextView tvValue;
    private PrintView arrowView;

    public PasswdRecordListItemHolder(Context context) {
        super(context);
    }

    @Override
    public View createNodeView(final TreeNode node, IconTreeItem value) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(net.tjado.passwdsafe.R.layout.layout_icon_node, null, false);
        tvValue = (TextView) view.findViewById(net.tjado.passwdsafe.R.id.node_value);
        tvValue.setText(value.text);

        final PrintView iconView = (PrintView) view.findViewById(
                net.tjado.passwdsafe.R.id.icon);
        iconView.setIconText(context.getResources().getString(value.icon));

        arrowView = (PrintView) view.findViewById(net.tjado.passwdsafe.R.id.arrow_icon);

        /*view.findViewById(R.id.btn_addFolder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TreeNode newFolder = new TreeNode(new PasswdRecordListItemHolder.IconTreeItem(R.string.ic_folder, "New Folder", "0", null));
                getTreeView().addNode(node, newFolder);
            }
        });

        view.findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTreeView().removeNode(node);
            }
        });*/

        //if My computer
        if (node.getLevel() == 1) {
            view.findViewById(net.tjado.passwdsafe.R.id.btn_delete).setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void toggle(boolean active) {
        arrowView.setIconText(context.getResources().getString(active ? net.tjado.passwdsafe.R.string.ic_keyboard_arrow_down : net.tjado.passwdsafe.R.string.ic_keyboard_arrow_right));
    }

    public static class IconTreeItem {
        public int icon;
        public String text;
        public String uuid;
        public PasswdLocation location;

        public IconTreeItem(int icon, String text, String uuid, PasswdLocation location) {
            this.icon = icon;
            this.text = text;
            this.uuid = uuid;
            this.location = location;
        }
    }
}
