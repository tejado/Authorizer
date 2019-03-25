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

package net.tjado.passwdsafe;


import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.text.TextUtils;

import net.tjado.passwdsafe.file.PasswdFileData;
import net.tjado.passwdsafe.lib.ObjectHolder;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.util.Pair;
import net.tjado.passwdsafe.view.PasswdLocation;
import net.tjado.passwdsafe.view.PasswdRecordIconItem;
import net.tjado.passwdsafe.view.GridAutofitLayoutManager;

import org.pwsafe.lib.file.PwsRecord;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;
import com.mikepenz.iconics.Iconics;
import com.mikepenz.iconics.typeface.ITypeface;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Fragment for showing notes of a password record
 */
public class PasswdSafeRecordIconFragment
        extends AbstractPasswdSafeRecordFragment
{

    private ArrayList<PasswdRecordIconItem> icons = new ArrayList<>();
    private HashMap<String, PasswdRecordIconItem> iconsMap = new HashMap<>();
    private FastItemAdapter<PasswdRecordIconItem> mAdapter;

    private SearchView itsSearchView;

    private String currentIcon;
    private int selectedPos = 0;

    /**
     * Max allowed duration for a "click", in milliseconds.
     */
    private static final int MAX_CLICK_DURATION = 1000;

    /**
     * Max allowed distance to move during a "click", in DP.
     */
    private static final int MAX_CLICK_DISTANCE = 15;

    private long pressStartTime;
    private float pressedX;
    private float pressedY;
    private boolean stayedWithinClickDistance;

    private static final String TAG = "PasswdSafeRecordIconFragment";

    /**
     * Create a new instance of the fragment
     */
    public static PasswdSafeRecordIconFragment newInstance(
            PasswdLocation location)
    {
        PasswdSafeRecordIconFragment frag =
                new PasswdSafeRecordIconFragment();
        frag.setArguments(createArgs(location));
        return frag;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View root = inflater.inflate(R.layout.fragment_passwdsafe_record_icon, null, false);

        itsSearchView = (SearchView) root.findViewById(R.id.search_view);
        itsSearchView.setIconifiedByDefault(false);
        itsSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                onSearch(query);
                itsSearchView.clearFocus();
                return false;
            }
            @Override
            public boolean onQueryTextChange(String query) {
                onSearch(query);
                return false;
            }
        });


        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        // Init and Setup RecyclerView

        // calculate the correct column width over converting the CardView dp width to pixel
        final float scale = getResources().getDisplayMetrics().density;
        final int spaceWidth = (int) (56.0f * scale + 0.5f);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new GridAutofitLayoutManager(getActivity(), spaceWidth));
        //animator not yet working
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        mAdapter = new FastItemAdapter<>();
        configAdapter();
        recyclerView.setAdapter(mAdapter);

        // sort them... I want to have the devicon set before material design set
        final List<ITypeface> mFonts = new ArrayList<>(Iconics.getRegisteredFonts(getActivity()));
        Collections.sort(mFonts, new Comparator<ITypeface>() {
            @Override
            public int compare(final ITypeface object1, final ITypeface object2) {
                return object1.getFontName().compareTo(object2.getFontName());
            }
        });

        for (ITypeface iTypeface : mFonts) {
            PasswdSafeUtil.dbginfo(TAG, "Font: " + iTypeface.getFontName() );

            if (iTypeface.getIcons() != null) {
                for (String icon : iTypeface.getIcons()) {
                    PasswdRecordIconItem iconItem = new PasswdRecordIconItem(icon);
                    icons.add(iconItem);
                    iconsMap.put(icon, iconItem);
                }
                mAdapter.set(icons);
            }
        }

        try {
            recyclerView.scrollToPosition( mAdapter.getPosition(iconsMap.get(currentIcon)) );
        } catch (Exception e) {
            PasswdSafeUtil.dbginfo(TAG, "can't scroll...");
        }

    }


    @Override
    protected void doOnCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.fragment_passwdsafe_record_notes, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void doRefresh(@NonNull RecordInfo info)
    {
        PasswdSafeUtil.dbginfo(TAG, "doRefresh");

        if( currentIcon == null ) {
            currentIcon = info.itsFileData.getIcon(info.itsRec);

            PasswdRecordIconItem newIconItem = iconsMap.get(currentIcon);
            if( newIconItem != null ) {
                newIconItem.setHighlight();

                RecyclerView recyclerView = (RecyclerView) getView().findViewById(R.id.list);
                if( recyclerView != null && mAdapter != null ) {
                    recyclerView.scrollToPosition(mAdapter.getPosition(newIconItem));
                }
            }

        }
    }

    protected synchronized void refreshIconHighlight(String newIcon) {

        if( currentIcon != null && ! currentIcon.equals(newIcon) ) {
            PasswdRecordIconItem currentIconItem = iconsMap.get(currentIcon);
            if( currentIconItem != null ) {
                currentIconItem.unsetHighlight();
            }
        }

        PasswdRecordIconItem newIconItem = iconsMap.get(newIcon);
        if( newIconItem != null ) {
            newIconItem.setHighlight();
        }

        currentIcon = newIcon;
    }

    private void configAdapter()
    {
        //our popup on touch
        mAdapter.withOnTouchListener(new FastAdapter.OnTouchListener<PasswdRecordIconItem>()
        {
            @Override
            public boolean onTouch(View v, MotionEvent motionEvent,
                                   IAdapter<PasswdRecordIconItem> adapter, PasswdRecordIconItem item,
                                   int position)
            {
                if( itsSearchView.hasFocus() ) {
                    itsSearchView.clearFocus();
                }

                // thanks to http://stackoverflow.com/a/29933115
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        pressStartTime = System.currentTimeMillis();
                        pressedX = motionEvent.getX();
                        pressedY = motionEvent.getY();
                        stayedWithinClickDistance = true;
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        if (stayedWithinClickDistance && distance(pressedX, pressedY, motionEvent.getX(), motionEvent.getY()) > MAX_CLICK_DISTANCE) {
                            stayedWithinClickDistance = false;
                        }
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        long pressDuration = System.currentTimeMillis() - pressStartTime;
                        if (pressDuration < MAX_CLICK_DURATION && stayedWithinClickDistance) {

                            saveIconChange(item.getIcon());
                            refreshIconHighlight(item.getIcon());
                        }
                    }
                }
                return true;
            }

            private float distance(float x1, float y1, float x2, float y2) {
                float dx = x1 - x2;
                float dy = y1 - y2;
                float distanceInPx = (float) Math.sqrt(dx * dx + dy * dy);
                return pxToDp(distanceInPx);
            }

            private float pxToDp(float px) {
                return px / getResources().getDisplayMetrics().density;
            }
        });

        mAdapter.withOnBindViewHolderListener(
                new FastAdapter.OnBindViewHolderListener()
                {
                    @Override
                    public void onBindViewHolder(
                            RecyclerView.ViewHolder viewHolder, int position,
                            List payloads)
                    {
                        PasswdRecordIconItem.ViewHolder holder = (PasswdRecordIconItem.ViewHolder)viewHolder;

                        viewHolder.itemView.setSelected(selectedPos == position);

                        //as we overwrite the default listener
                        mAdapter.getItem(position).bindView(holder, payloads);
                    }

                    @Override
                    public void unBindViewHolder(
                            RecyclerView.ViewHolder viewHolder, int position)
                    {
                        PasswdRecordIconItem item = mAdapter.getItem(position);
                        if (item != null) {
                            item.unbindView((PasswdRecordIconItem.ViewHolder)viewHolder);
                        }
                    }
                });
    }

    void saveIconChange(final String itemValue) {
        final ObjectHolder<Pair<Boolean, PasswdLocation>> rc = new ObjectHolder<>();
        useRecordFile(new RecordFileUser()
        {
            @Override
            public void useFile(@Nullable RecordInfo info,
                                @NonNull PasswdFileData fileData)
            {

                PwsRecord record;
                boolean newRecord;
                if (info != null) {
                    record = info.itsRec;
                    newRecord = false;
                } else {
                    record = fileData.createRecord();
                    record.setLoaded();
                    newRecord = true;
                }

                if( fileData.isProtected(record)) {
                    return;
                }

                if (fileData.getIcon(record) != itemValue) {
                    fileData.setIcon(itemValue, record);
                }

                if (newRecord) {
                    fileData.addRecord(record);
                }

                rc.set(new Pair<>((newRecord || record.isModified()), new PasswdLocation(record, fileData)));

            }
        });

        if (rc == null || rc.get() == null) {
            return;
        }
        getListener().finishEditRecord(rc.get().first, rc.get().second, false);
    }

    void onSearch(String s) {

        if (mAdapter != null) {
            if (TextUtils.isEmpty(s)) {
                mAdapter.clear();
                mAdapter.setNewList(icons);
            } else {
                AbstractList<PasswdRecordIconItem> tmpList = new ArrayList<>();
                for (PasswdRecordIconItem icon : icons) {
                    if (icon.getIcon().toLowerCase().contains(s.toLowerCase())) {
                        tmpList.add(icon);
                    }
                }
                mAdapter.setNewList(tmpList);
            }
        }
    }

}

