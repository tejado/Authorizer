/*
 * Copyright 2014 Mike Penz
 * Modified work Copyright 2016 Tjado Mäcke <tjado@maecke.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * https://github.com/mikepenz/Android-Iconics
 */

package net.tjado.passwdsafe.view;


import android.graphics.Color;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.utils.ViewHolderFactory;
import com.mikepenz.iconics.view.IconicsImageView;

import net.tjado.passwdsafe.R;

import java.util.List;

public class PasswdRecordIconItem
        extends AbstractItem<PasswdRecordIconItem, PasswdRecordIconItem.ViewHolder>
{
    //the static ViewHolderFactory which will be used to generate the ViewHolder for this Item
    private static final ViewHolderFactory<? extends ViewHolder> FACTORY = new ItemFactory();

    private String icon;
    private Integer color;
    private IconicsImageView itsIconicsImageView;

    public String getIcon() {
        return icon;
    }

    public PasswdRecordIconItem withIcon(String icon) {
        this.icon = icon;
        return this;
    }

    public PasswdRecordIconItem(String icon) {
        this.icon = icon;
    }

    @Override
    public int getType() {
        return R.id.item_row_icon;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.row_icon;
    }

    @Override
    public void bindView(ViewHolder holder, List payloads) {
        super.bindView(holder, payloads);

        holder.image.setIcon(icon);

        holder.image.setColorRes(R.color.treeview_icons);
        if( color == null ) {
            holder.image.setColorRes(R.color.treeview_icons);
        } else {
            holder.image.setColor(color);
        }
        holder.image.setPaddingDp(0);
        holder.image.setContourWidthDp(0);
        holder.image.setContourColor(Color.TRANSPARENT);
        holder.image.setBackgroundColor(Color.TRANSPARENT);

        //holder.image.setBackgroundColorRes(color);
        //holder.image.setContourColorRes(color);

        //as we want to respect the bounds of the original font in the icon list
        holder.image.getIcon().respectFontBounds(true);

        itsIconicsImageView = holder.image;
    }

    public void setHighlight()
    {
        if( itsIconicsImageView == null) {
            color = Color.WHITE;
        } else {
            itsIconicsImageView.setColor(Color.WHITE);
        }
    }

    public void unsetHighlight()
    {
        if( itsIconicsImageView == null) {
            color = null;
        } else {
            itsIconicsImageView.setColorRes(R.color.treeview_icons);
        }
    }

    /**
     * our ItemFactory implementation which creates the ViewHolder for our adapter.
     * It is highly recommended to implement a ViewHolderFactory as it is 0-1ms faster for ViewHolder creation,
     * and it is also many many times more efficient if you define custom listeners on views within your item.
     */
    protected static class ItemFactory implements ViewHolderFactory<ViewHolder> {
        public ViewHolder create(View v) {
            return new ViewHolder(v);
        }
    }

    /**
     * return our ViewHolderFactory implementation here
     *
     * @return
     */
    @Override
    public ViewHolderFactory<? extends ViewHolder> getFactory() {
        return FACTORY;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        public IconicsImageView image;

        ViewHolder(final View itemView) {
            super(itemView);
            image = (IconicsImageView) itemView.findViewById(R.id.icon);
        }
    }
}