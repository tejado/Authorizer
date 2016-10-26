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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.github.johnkil.print.PrintView;
import com.unnamed.b.atv.model.TreeNode;


public class PasswdRecordListHeaderHolder extends TreeNode.BaseNodeViewHolder<PasswdRecordListItemHolder.IconTreeItem> {
    private TextView tvValue;
    private PrintView arrowView;
    private CheckBox nodeSelector;

    public PasswdRecordListHeaderHolder(Context context) {
        super(context);
    }

    @Override
    public View createNodeView(final TreeNode node, PasswdRecordListItemHolder.IconTreeItem value) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(net.tjado.passwdsafe.R.layout.layout_selectable_header, null, false);

        tvValue = (TextView) view.findViewById(net.tjado.passwdsafe.R.id.node_value);
        tvValue.setText(value.text);

        final PrintView iconView = (PrintView) view.findViewById(
                net.tjado.passwdsafe.R.id.icon);
        iconView.setIconText(context.getResources().getString(value.icon));

        arrowView = (PrintView) view.findViewById(net.tjado.passwdsafe.R.id.arrow_icon);
        if (node.isLeaf()) {
            arrowView.setVisibility(View.INVISIBLE);
        }

        nodeSelector = (CheckBox) view.findViewById(net.tjado.passwdsafe.R.id.node_selector);
        nodeSelector.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                node.setSelected(isChecked);
                for (TreeNode n : node.getChildren()) {
                    getTreeView().selectNode(n, isChecked);
                }
            }
        });
        nodeSelector.setChecked(node.isSelected());

        return view;
    }

    @Override
    public void toggle(boolean active) {
        arrowView.setIconText(context.getResources().getString(active ? net.tjado.passwdsafe.R.string.ic_keyboard_arrow_down : net.tjado.passwdsafe.R.string.ic_keyboard_arrow_right));
    }

    @Override
    public void toggleSelectionMode(boolean editModeEnabled) {
        nodeSelector.setVisibility(editModeEnabled ? View.VISIBLE : View.GONE);
        nodeSelector.setChecked(mNode.isSelected());
    }
}
